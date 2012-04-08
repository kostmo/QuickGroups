package com.kostmo.grouper.persistence;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.http.HttpServlet;

import org.json.simple.JSONArray;

import com.kostmo.grouper.LdapHelper.MisconfigurationException;

public class PostgresData {

	public static Connection getPostgresConnection(HttpServlet servlet) throws SQLException, MisconfigurationException, ClassNotFoundException {

		String location = "/WEB-INF/postgresql.properties";
		Properties postgres_properties = new Properties();
		try {
			postgres_properties.load(servlet.getServletContext().getResourceAsStream(location));
		} catch (IOException e) {
			throw new MisconfigurationException( String.format("You need to create the file \"%s\"", location) );
		}
		
		Class.forName("org.postgresql.Driver");
		return DriverManager.getConnection(postgres_properties.getProperty("jdbc_url"), postgres_properties);
	}

	// ========================================================================
	public static JSONArray searchTags(Connection con, String prefix) throws SQLException {

		String query = "SELECT btrim(label) FROM \"ViewTagHistogram\" WHERE label LIKE ? AND count > 0 ORDER BY count DESC";
		PreparedStatement query_statement = con.prepareStatement(query);
		
		// Escape percents and underscores
		String escaped_prefix = prefix.replace("%", "\\%");
		escaped_prefix = escaped_prefix.replace("_", "\\_");
		query_statement.setString(1, escaped_prefix + "%");

		JSONArray matches = new JSONArray();
		try {
			ResultSet rs = query_statement.executeQuery();
			while (rs.next())
				matches.add(rs.getString(1));

		} catch (SQLException e ) {
			e.printStackTrace();
		} finally {
			if (query_statement != null) { query_statement.close(); }
		}

		return matches;
	}
	
	// ========================================================================
	public static Map<Long, Group> loadGroups(Connection con, String username) throws SQLException {

		String query = "SELECT * FROM \"ViewGroupsWithAggregateInfo\" WHERE owner=? OR is_public";
		PreparedStatement query_statement = con.prepareStatement(query);
		query_statement.setString(1, username);

		Map<Long, Group> groups = new HashMap<Long, Group>();
		try {
			ResultSet rs = query_statement.executeQuery();
			while (rs.next()) {
				Group g = Group.newFromResultSet(rs);
				groups.put(g.id, g);
			}
		} catch (SQLException e ) {
			e.printStackTrace();
		} finally {
			if (query_statement != null) { query_statement.close(); }
		}

		return groups;
	}

	// ========================================================================
	public static Map<String, Long> loadGroupTags(Connection con, long group_id) throws SQLException {
		
		Map<String, Long> existing_tags = new HashMap<String, Long>();
		String query = "SELECT label, tag_id FROM \"ViewGroupTags\" WHERE group_id=?";
		PreparedStatement query_statement = con.prepareStatement(query);
		query_statement.setLong(1, group_id);

		ResultSet rs = query_statement.executeQuery();
		while (rs.next())
			existing_tags.put(rs.getString(1), rs.getLong(2));

		return existing_tags;
	}

	// ========================================================================
	public static Map<String, GroupMember> loadGroupMembers(Connection con, long group_id) throws SQLException {

		Map<String, GroupMember> group_members = new HashMap<String, GroupMember>();
		String query = "SELECT * FROM \"membership\" WHERE group_id=?";
		PreparedStatement query_statement = con.prepareStatement(query);
		query_statement.setLong(1, group_id);
		
		ResultSet rs = query_statement.executeQuery();
		while (rs.next()) {
			GroupMember member = GroupMember.newFromResultSet(rs);
			group_members.put(member.alias, member);
		}

		return group_members;
	}

	// ========================================================================
	public static long copyGroup(Connection con, long source_group_id, String new_group_name, String new_owner) throws SQLException {

		// Makes sure source group is either public or owned by the current user
		PreparedStatement group_permission_statement = con.prepareStatement("SELECT is_public, owner FROM groups WHERE id=?");
		group_permission_statement.setLong(1, source_group_id);
		ResultSet rs = group_permission_statement.executeQuery();
		if (rs.next()) {
			boolean is_public = rs.getBoolean("is_public");
			String owner = rs.getString("owner").trim();

			if ( !(owner.equals(new_owner) || is_public) ) {
				System.out.println("Refusing to copy; user is not the owner and group is not public.");
				return -1;
			}

		} else {
			System.out.println("Could not find source group.");
			return -1;
		}

		String group_insertion_query = "INSERT INTO \"groups\" (label, is_public, is_self_serve, owner) (SELECT ?, is_public, is_self_serve, ? FROM groups WHERE id=?)";
		PreparedStatement group_insertion_statement = con.prepareStatement(group_insertion_query, Statement.RETURN_GENERATED_KEYS);

		String member_insertion_query = "INSERT INTO \"membership\" (alias, group_id, set_by) (SELECT alias, ?, ? FROM membership WHERE group_id=?)";
		PreparedStatement member_insertion_statement = con.prepareStatement(member_insertion_query);

		group_insertion_statement.setString(1, new_group_name);
		group_insertion_statement.setString(2, new_owner);
		group_insertion_statement.setLong(3, source_group_id);


		long new_group_id = -2;
		try {

			int status = group_insertion_statement.executeUpdate();
			ResultSet rs2 = group_insertion_statement.getGeneratedKeys();
			while (rs2.next()) {
				new_group_id = rs.getLong(1);
				System.out.println("\tAutogenerated Key: " + new_group_id);
			}

			member_insertion_statement.setLong(1, new_group_id);
			member_insertion_statement.setString(2, new_owner);
			member_insertion_statement.setLong(3, source_group_id);

			int status2 = member_insertion_statement.executeUpdate();

		} catch (SQLException e ) {
			e.printStackTrace();
		} finally {
			if (group_insertion_statement != null) { group_insertion_statement.close(); }
			if (member_insertion_statement != null) { member_insertion_statement.close(); }
		}

		return new_group_id;
	}

	// ========================================================================
	public static void makeTagAssociations(Connection con, Collection<String> tags, long group_id, String owner) throws SQLException {

		PreparedStatement tag_existence_query = con.prepareStatement("SELECT id FROM tags WHERE btrim(label)=?");

		String tag_insertion_query = "INSERT INTO \"tags\" (label, creator) VALUES (?, ?)";
		PreparedStatement tag_insertion_statement = con.prepareStatement(tag_insertion_query, Statement.RETURN_GENERATED_KEYS);
		tag_insertion_statement.setString(2, owner);

		String tag_association_query = "INSERT INTO \"tag_associations\" (group_id, tag_id, author) VALUES (?, ?, ?)";
		PreparedStatement tag_association_statement = con.prepareStatement(tag_association_query);
		tag_association_statement.setString(3, owner);
		
		
		tag_association_statement.setLong(1, group_id);
		tag_association_statement.setString(3, owner);
		
		// Insert new tags, make tag-group associations
		for (String tag : tags) {

			tag_existence_query.setString(1, tag);
			ResultSet rs_existence = tag_existence_query.executeQuery();
			long tag_id = -1;
			while (rs_existence.next()) {
				tag_id = rs_existence.getLong(1);
			}
			
			if (tag_id < 0) {
				tag_insertion_statement.setString(1, tag);
				int status_tag_insertion = tag_insertion_statement.executeUpdate();

				ResultSet rs_insertion = tag_insertion_statement.getGeneratedKeys();
				while (rs_insertion.next()) {
					tag_id = rs_insertion.getLong(1);
					System.out.println("\tAutogenerated tag Key: " + tag_id);
				}
			}					

			tag_association_statement.setLong(2, tag_id);
			int status_tag_association = tag_association_statement.executeUpdate();
		}
	}

	// ========================================================================
	public static long insertNewGroup(Connection con, Group group_object) throws SQLException {

		String group_insertion_query = "INSERT INTO \"groups\" (label, is_public, is_self_serve, owner) VALUES (?, ?, ?, ?)";
		PreparedStatement group_insertion_statement = con.prepareStatement(group_insertion_query, Statement.RETURN_GENERATED_KEYS);

		String member_insertion_query = "INSERT INTO \"membership\" (alias, group_id, set_by) VALUES (?, ?, ?)";
		PreparedStatement member_insertion_statement = con.prepareStatement(member_insertion_query);


		group_insertion_statement.setString(1, group_object.label);
		group_insertion_statement.setBoolean(2, group_object.is_public );
		group_insertion_statement.setBoolean(3, group_object.is_self_serve );
		group_insertion_statement.setString(4, group_object.owner);

		long group_id = -2;
		try {

			int status = group_insertion_statement.executeUpdate();

			ResultSet rs = group_insertion_statement.getGeneratedKeys();
			while (rs.next()) {
				group_id = rs.getLong(1);
				System.out.println("\tAutogenerated Key: " + group_id);
			}

			
			makeTagAssociations(con, group_object.tags, group_id, group_object.owner);
			
			
			
			for (GroupMember member : group_object.group_members) {
				member_insertion_statement.setString(1, member.alias);
				member_insertion_statement.setLong(2, group_id);
				member_insertion_statement.setString(3, member.set_by);

				int status2 = member_insertion_statement.executeUpdate();
			}


		} catch (SQLException e ) {
			e.printStackTrace();
		} finally {
			if (group_insertion_statement != null) { group_insertion_statement.close(); }
			if (member_insertion_statement != null) { member_insertion_statement.close(); }
		}

		return group_id;
	}

	// ========================================================================
	public static void reconcileGroupTags(Connection con, Group modified_group) throws SQLException {

		Map<String, Long> existing_tags = loadGroupTags(con, modified_group.id);
		System.out.println("Tags for group '" + modified_group.label + "' (id '" + modified_group.id + "') known in database: " + existing_tags.keySet());
		
		
		// Delete tag associations for tags that are no longer present
		String tag_deletion_query = "DELETE FROM \"tag_associations\" WHERE group_id=? AND tag_id=?";
		PreparedStatement tag_deletion_statement = con.prepareStatement(tag_deletion_query);
		for (Entry<String, Long> existing_tag : existing_tags.entrySet()) {
			if ( !modified_group.tags.contains(existing_tag.getKey()) ) {
				tag_deletion_statement.setLong(1, modified_group.id);
				tag_deletion_statement.setLong(2, existing_tag.getValue());
				
				int deletion_status = tag_deletion_statement.executeUpdate();
			}
		}

		
		
		// Add new tags
		Collection<String> tags_to_add = new HashSet<String>();
		for (String pending_tag : modified_group.tags)
			if (!existing_tags.containsKey(pending_tag))
				tags_to_add.add(pending_tag);
		
		System.out.println("About to add tags to group '" + modified_group.label + "': " + tags_to_add);
		makeTagAssociations(con, tags_to_add, modified_group.id, modified_group.owner);
	}
	
	// ========================================================================
	public static void reconcileGroupMembers(Connection con, Group modified_group) throws SQLException {
		

		Map<String, GroupMember> existing_group_members = loadGroupMembers(con, modified_group.id);
		
		// Delete members that are no longer present
		String member_deletion_query = "DELETE FROM \"membership\" WHERE group_id=? AND alias=?";
		PreparedStatement member_deletion_statement = con.prepareStatement(member_deletion_query);
		for (GroupMember existing_member : existing_group_members.values()) {
			if ( !modified_group.hasMember(existing_member.alias) ) {

				member_deletion_statement.setLong(1, modified_group.id);
				member_deletion_statement.setString(2, existing_member.alias);
				int status1 = member_deletion_statement.executeUpdate();
			}
		}

		// Add new members
		String member_insertion_query = "INSERT INTO \"membership\" (alias, group_id, set_by) VALUES (?, ?, ?)";
		PreparedStatement member_insertion_statement = con.prepareStatement(member_insertion_query);
		for (GroupMember member : modified_group.group_members) {
			if (!existing_group_members.keySet().contains(member.alias)) {

				member_insertion_statement.setString(1, member.alias);
				member_insertion_statement.setLong(2, member.group_id);
				member_insertion_statement.setString(3, member.set_by);
				int status2 = member_insertion_statement.executeUpdate();
			}
		}
	}
	
	// ========================================================================
	public static boolean updateGroup(Connection con, Group modified_group) throws SQLException {

		// Makes sure source group is either public or owned by the current user
		PreparedStatement group_permission_statement = con.prepareStatement("SELECT is_public, is_self_serve, owner FROM groups WHERE id=?");
		group_permission_statement.setLong(1, modified_group.id);
		ResultSet rs = group_permission_statement.executeQuery();

		boolean is_public_in_database;
		boolean is_self_serve_in_database;
		String owner_in_database;
		if (rs.next()) {
			is_public_in_database = rs.getBoolean("is_public");
			is_self_serve_in_database = rs.getBoolean("is_self_serve");
			owner_in_database = rs.getString("owner").trim();

			if ( !(owner_in_database.equals(modified_group.owner) || (is_public_in_database && is_self_serve_in_database)) ) {
				System.out.println("Refusing to update; user is not the owner and group is not public and self-serve.");
				return false;
			}

		} else {
			System.out.println("Could not find group to update in database.");
			return false;
		}


		if ( owner_in_database.equals(modified_group.owner) ) {

			// Only the owner of the group can update group metadata.
			String group_update_query = "UPDATE \"groups\" SET label=?, is_public=?, is_self_serve=? WHERE id=?";
			PreparedStatement group_update_statement = con.prepareStatement(group_update_query);

			group_update_statement.setString(1, modified_group.label);
			group_update_statement.setBoolean(2, modified_group.is_public );
			group_update_statement.setBoolean(3, modified_group.is_self_serve );
			group_update_statement.setLong(4, modified_group.id );

			try {

				int status = group_update_statement.executeUpdate();

			} catch (SQLException e ) {
				e.printStackTrace();
			} finally {
				if (group_update_statement != null) { group_update_statement.close(); }
			}

			reconcileGroupMembers(con, modified_group);
			reconcileGroupTags(con, modified_group);

		} else if (is_public_in_database && is_self_serve_in_database) {
			// If you are not the owner, you can only add or remove yourself, and only then
			// if the group is both public and self-serve.

			// TODO
			System.err.println("Not implemented yet.");
		}

		return true;
	}

	// ========================================================================
	public static long deleteGroup(Connection con, long group_id) throws SQLException {

		String member_deletion_query = "DELETE FROM \"membership\" WHERE group_id=?";
		PreparedStatement member_deletion_statement = con.prepareStatement(member_deletion_query);

		String group_deletion_query = "DELETE FROM \"groups\" WHERE id=?";
		PreparedStatement group_deletion_statement = con.prepareStatement(group_deletion_query);

		try {

			member_deletion_statement.setLong(1, group_id);
			int status1 = member_deletion_statement.executeUpdate();

			group_deletion_statement.setLong(1, group_id);
			int status2 = group_deletion_statement.executeUpdate();

		} catch (SQLException e ) {
			e.printStackTrace();
		} finally {
			if (member_deletion_statement != null) { member_deletion_statement.close(); }
			if (group_deletion_statement != null) { group_deletion_statement.close(); }
		}

		return -1;
	}
}