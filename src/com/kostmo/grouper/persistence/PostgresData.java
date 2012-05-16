package com.kostmo.grouper.persistence;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.http.HttpServlet;

import org.apache.commons.lang3.StringUtils;
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
	public static Collection<Tag> getFullTagsHistogram(Connection con, boolean include_unused) throws SQLException {

		Collection<Tag> tags = new ArrayList<Tag>();
		
		String where_clause = !include_unused ? "WHERE count > 0" : "";
		String query = "SELECT label, count FROM \"ViewTagHistogram\" " + where_clause + " ORDER BY count DESC, label ASC";
		PreparedStatement query_statement = con.prepareStatement(query);

		ResultSet rs = query_statement.executeQuery();
		while (rs.next())
			tags.add( new Tag(rs.getString(1), rs.getInt(2)) );
		query_statement.close();

		return tags;
	}
	
	// ========================================================================
	@SuppressWarnings("unchecked")
	public static JSONArray searchTags(Connection con, String prefix) throws SQLException {

		String query = "SELECT label FROM \"ViewTagHistogram\" WHERE label LIKE ? AND count > 0 ORDER BY count DESC, label ASC";
		PreparedStatement query_statement = con.prepareStatement(query);
		
		// Escape percents and underscores
		String escaped_prefix = prefix.replace("%", "\\%");
		escaped_prefix = escaped_prefix.replace("_", "\\_");
		query_statement.setString(1, escaped_prefix + "%");

		JSONArray matches = new JSONArray();

		ResultSet rs = query_statement.executeQuery();
		while (rs.next())
			matches.add(rs.getString(1));

		query_statement.close();

		return matches;
	}
	
	// ========================================================================
	public static Group loadSingleGroup(Connection con, long group_id) throws SQLException {

		String query = "SELECT * FROM \"ViewGroupsWithAggregateInfo\" WHERE id=?";
		PreparedStatement query_statement = con.prepareStatement(query);
		query_statement.setLong(1, group_id);
		ResultSet rs = query_statement.executeQuery();
		if (rs.next())
			return Group.newFromResultSet(rs);

		return null;
	}

	// ========================================================================
	public static Group loadSingleGroup(Connection con, String username, long group_id) throws SQLException {

		String query = "SELECT * FROM \"ViewGroupsWithAggregateInfo\" WHERE (owner=? OR is_public) AND id=?";
		PreparedStatement query_statement = con.prepareStatement(query);
		query_statement.setString(1, username);
		query_statement.setLong(2, group_id);
		
		Group group = null;
		
		ResultSet rs = query_statement.executeQuery();
		if (rs.next()) {
			group = Group.newFromResultSet(rs);
		}

		query_statement.close();

		return group;
	}
	
	// ========================================================================
	public static Map<Long, Group> loadGroups(Connection con, String username) throws SQLException {

		String query = "SELECT * FROM \"ViewGroupsWithAggregateInfo\" WHERE owner=? OR is_public";
		PreparedStatement query_statement = con.prepareStatement(query);
		query_statement.setString(1, username);

		Map<Long, Group> groups = new HashMap<Long, Group>();

		ResultSet rs = query_statement.executeQuery();
		while (rs.next()) {
			Group g = Group.newFromResultSet(rs);
			groups.put(g.id, g);
		}

		query_statement.close();

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
	
	
	static final String[] PROFICIENCY_FIELD_NAMES = new String[] {"name", "description", "alternate_description"};
	// ========================================================================
	public static Map<Integer, Map<String, String>> loadProficiencyLabels(Connection con) throws SQLException {

		Map<Integer, Map<String, String>> proficiency_labels = new HashMap<Integer, Map<String, String>>();
		String query = "SELECT rating," + StringUtils.join(PROFICIENCY_FIELD_NAMES, ",") + " FROM \"proficiency_levels\"";
		PreparedStatement query_statement = con.prepareStatement(query);
		ResultSet rs = query_statement.executeQuery();
		while (rs.next()) {
			Map<String, String> text_map = new HashMap<String, String>();
			for (String field_name : PROFICIENCY_FIELD_NAMES) {
				String field_value = rs.getString(field_name);
				if (field_value != null)
					text_map.put(field_name, field_value.trim());
			}
			
			proficiency_labels.put(rs.getInt(1), text_map);
		}

		return proficiency_labels;
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
			if ( !(owner.equalsIgnoreCase(new_owner) || is_public) )
				throw new SQLException("Refusing to copy; user is not the owner and group is not public.");
		} else
			throw new SQLException("Could not find source group.");

		String group_insertion_query = "INSERT INTO \"groups\" (label, is_public, is_self_serve, is_skill, owner) (SELECT ?, is_public, is_self_serve, is_skill, ? FROM groups WHERE id=?)";
		PreparedStatement group_insertion_statement = con.prepareStatement(group_insertion_query, Statement.RETURN_GENERATED_KEYS);
		group_insertion_statement.setString(1, new_group_name);
		group_insertion_statement.setString(2, new_owner);
		group_insertion_statement.setLong(3, source_group_id);

		long new_group_id = -2;

		int status = group_insertion_statement.executeUpdate();
		ResultSet rs2 = group_insertion_statement.getGeneratedKeys();
		if (rs2.next()) {
			new_group_id = rs2.getLong(1);
			System.out.println("\tAutogenerated Key: " + new_group_id);
		} else {
			throw new SQLException("Failed to insert new group!");
		}
		
		
		String member_insertion_query = "INSERT INTO \"membership\" (alias, group_id, set_by) (SELECT alias, ?, ? FROM membership WHERE group_id=?)";
		PreparedStatement member_insertion_statement = con.prepareStatement(member_insertion_query);
		member_insertion_statement.setLong(1, new_group_id);
		member_insertion_statement.setString(2, new_owner);
		member_insertion_statement.setLong(3, source_group_id);

		int status2 = member_insertion_statement.executeUpdate();


		String tag_association_query = "INSERT INTO \"tag_associations\" (group_id, tag_id, author) (SELECT group_id, tag_id, ? FROM \"tag_associations\" WHERE group_id=?)";
		PreparedStatement tag_association_statement = con.prepareStatement(tag_association_query);
		tag_association_statement.setString(1, new_owner);
		tag_association_statement.setLong(2, new_group_id);

		int status3 = tag_association_statement.executeUpdate();

		return new_group_id;
	}

	// ========================================================================
	public static void makeTagAssociations(Connection con, Collection<String> tags, long group_id, String owner) throws SQLException {

		PreparedStatement tag_existence_query = con.prepareStatement("SELECT id FROM tags WHERE btrim(label) LIKE ?");

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

			tag_existence_query.setString(1, tag.toLowerCase());
			ResultSet rs_existence = tag_existence_query.executeQuery();
			long tag_id = -1;
			while (rs_existence.next()) {
				tag_id = rs_existence.getLong(1);
			}
			
			if (tag_id < 0) {
				tag_insertion_statement.setString(1, tag.toLowerCase());
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

		String group_insertion_query = "INSERT INTO \"groups\" (label, is_public, is_self_serve, is_skill, owner) VALUES (?, ?, ?, ?, ?)";
		PreparedStatement group_insertion_statement = con.prepareStatement(group_insertion_query, Statement.RETURN_GENERATED_KEYS);

		String member_insertion_query = "INSERT INTO \"membership\" (alias, group_id, set_by, proficiency) VALUES (?, ?, ?, ?)";
		PreparedStatement member_insertion_statement = con.prepareStatement(member_insertion_query);


		group_insertion_statement.setString(1, group_object.label);
		group_insertion_statement.setBoolean(2, group_object.is_public );
		group_insertion_statement.setBoolean(3, group_object.is_self_serve );
		group_insertion_statement.setBoolean(4, group_object.is_skill );
		group_insertion_statement.setString(5, group_object.owner);

		long group_id = -2;


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
			member_insertion_statement.setInt(4, member.proficiency);

			int status2 = member_insertion_statement.executeUpdate();
		}

		return group_id;
	}

	// ========================================================================
	public static void deleteTagAssociationsByKey(Connection con, Collection<Long> keys, long group_id) throws SQLException {
		
		String tag_deletion_query = "DELETE FROM \"tag_associations\" WHERE group_id=? AND tag_id=?";
		PreparedStatement tag_deletion_statement = con.prepareStatement(tag_deletion_query);
		for (Long tag_id : keys) {
			tag_deletion_statement.setLong(1, group_id);
			tag_deletion_statement.setLong(2, tag_id);
			int deletion_status = tag_deletion_statement.executeUpdate();
		}
	}
	
	// ========================================================================
	public static void reconcileGroupTags(Connection con, Group modified_group) throws SQLException {

		Map<String, Long> existing_tags = loadGroupTags(con, modified_group.id);
		
		// Delete tag associations for tags that are no longer present
		Collection<Long> tag_ids_to_delete = new HashSet<Long>();
		for (Entry<String, Long> existing_tag : existing_tags.entrySet())
			if ( !modified_group.tags.contains(existing_tag.getKey()) )
				tag_ids_to_delete.add(existing_tag.getValue());

		deleteTagAssociationsByKey(con, tag_ids_to_delete, modified_group.id);
		
		
		// Add new tags
		Collection<String> tags_to_add = new HashSet<String>();
		for (String pending_tag : modified_group.tags)
			if (!existing_tags.containsKey(pending_tag))
				tags_to_add.add(pending_tag);

		makeTagAssociations(con, tags_to_add, modified_group.id, modified_group.owner);
	}
	
	// ========================================================================
	public static void reconcileGroupMembers(Connection con, Group modified_group, String alias_restriction) throws SQLException {

		Map<String, GroupMember> existing_group_members = loadGroupMembers(con, modified_group.id);
		
		// Delete members that are no longer present
		String member_deletion_query = "DELETE FROM \"membership\" WHERE group_id=? AND alias=?";
		PreparedStatement member_deletion_statement = con.prepareStatement(member_deletion_query);
		for (GroupMember existing_member : existing_group_members.values()) {
			if ( !modified_group.hasMember(existing_member.alias) ) {

				if (alias_restriction == null || existing_member.alias.equalsIgnoreCase(alias_restriction)) {
				
					member_deletion_statement.setLong(1, modified_group.id);
					member_deletion_statement.setString(2, existing_member.alias);
					int status1 = member_deletion_statement.executeUpdate();
				}
			}
		}

		// Add new members
		String member_insertion_query = "INSERT INTO \"membership\" (alias, group_id, set_by, proficiency) VALUES (?, ?, ?, ?)";
		PreparedStatement member_insertion_statement = con.prepareStatement(member_insertion_query);
		

		// Update properties (e.g. "proficiency") of existing members
		String member_update_query = "UPDATE \"membership\" SET proficiency=? WHERE alias=? AND group_id=?";
		PreparedStatement member_update_statement = con.prepareStatement(member_update_query);
		
		for (GroupMember member : modified_group.group_members) {
			if (!existing_group_members.keySet().contains(member.alias)) {


				if (alias_restriction == null || member.alias.equalsIgnoreCase(alias_restriction)) {
				
					member_insertion_statement.setString(1, member.alias);
					member_insertion_statement.setLong(2, member.group_id);
					member_insertion_statement.setString(3, member.set_by);
					member_insertion_statement.setInt(4, member.proficiency);
					
					System.out.println(member.alias + " is a new member of group \"" + modified_group.label + "\". Proficiency: " + member.proficiency);
					
					int status2 = member_insertion_statement.executeUpdate();
				
				}
					
			} else {


				if (alias_restriction == null || member.alias.equalsIgnoreCase(alias_restriction)) {
				
					member_update_statement.setInt(1, member.proficiency);
					member_update_statement.setString(2, member.alias);
					member_update_statement.setLong(3, member.group_id);
					
					int status2 = member_update_statement.executeUpdate();
				}
			}
		}
	}

	// ========================================================================
	public static class PermissionsException extends Exception {
		PermissionsException(String msg) {
			super(msg);
		}
	}
	
	// ========================================================================
	public static boolean updateGroup(Connection con, Group modified_group, String user_alias) throws SQLException, PermissionsException {

		// Makes sure source group is either public or owned by the current user
		Group group_in_database = loadSingleGroup(con, modified_group.id);
		if (group_in_database != null) {

			if ( !(group_in_database.owner.equalsIgnoreCase(modified_group.owner) || (group_in_database.is_public && group_in_database.is_self_serve)) ) {

				throw new PermissionsException("Refusing to update; user is not the owner and group is not public and self-serve.");
			}

		} else {
			throw new SQLException("Could not find group to update in database.");
		}


		if ( group_in_database.owner.equalsIgnoreCase(modified_group.owner) ) {

			// Only the owner of the group can update group metadata.
			String group_update_query = "UPDATE \"groups\" SET label=?, is_public=?, is_self_serve=?, is_skill=? WHERE id=?";
			PreparedStatement group_update_statement = con.prepareStatement(group_update_query);

			group_update_statement.setString(1, modified_group.label);
			group_update_statement.setBoolean(2, modified_group.is_public );
			group_update_statement.setBoolean(3, modified_group.is_self_serve );
			group_update_statement.setBoolean(4, modified_group.is_skill );
			group_update_statement.setLong(5, modified_group.id );

			int status = group_update_statement.executeUpdate();

			reconcileGroupMembers(con, modified_group, null);
			reconcileGroupTags(con, modified_group);

		} else if (group_in_database.is_public && group_in_database.is_self_serve) {
			// If you are not the owner, you can only add or remove yourself, and only then
			// if the group is both public and self-serve.

			
			reconcileGroupMembers(con, modified_group, user_alias);

//			throw new PermissionsException("Not implemented yet.");

		} else {
			return false;
		}

		return true;
	}

	// ========================================================================
	public static void bequeathGroup(Connection con, long group_id, String current_user, String new_owner) throws SQLException {

		System.out.println("Pre-checking group " + group_id + " in database...");
		
		// Makes sure source group is owned by the current user
		Group group_in_database = loadSingleGroup(con, group_id);
		if (group_in_database != null) {
			if ( !(group_in_database.owner.equalsIgnoreCase(current_user)) )
				throw new SQLException("Refusing to transfer ownership; user is not the owner.");
		} else
			throw new SQLException("Could not find group to update in database.");


		// Only the owner of the group can update group metadata.
		String group_update_query = "UPDATE \"groups\" SET owner=? WHERE id=?";
		PreparedStatement group_update_statement = con.prepareStatement(group_update_query);

		group_update_statement.setString(1, new_owner);
		group_update_statement.setLong(2, group_id);

		int status = group_update_statement.executeUpdate();
	}
	
	// ========================================================================
	public static void deleteGroup(Connection con, long group_id, String current_user) throws SQLException, PermissionsException {

		// Makes sure source group is owned by the current user
		Group group_in_database = loadSingleGroup(con, group_id);
		if (group_in_database != null) {
			if ( !(group_in_database.owner.equalsIgnoreCase(current_user)) )
				throw new PermissionsException("Refusing to delete; user is not the owner of group.");
		} else
			throw new SQLException("Could not find group to update in database.");

		String tag_deletion_query = "DELETE FROM \"tag_associations\" WHERE group_id=?";
		PreparedStatement tag_deletion_statement = con.prepareStatement(tag_deletion_query);
		
		String member_deletion_query = "DELETE FROM \"membership\" WHERE group_id=?";
		PreparedStatement member_deletion_statement = con.prepareStatement(member_deletion_query);

		String group_deletion_query = "DELETE FROM \"groups\" WHERE id=?";
		PreparedStatement group_deletion_statement = con.prepareStatement(group_deletion_query);

		tag_deletion_statement.setLong(1, group_id);
		int status0 = tag_deletion_statement.executeUpdate();
		
		member_deletion_statement.setLong(1, group_id);
		int status1 = member_deletion_statement.executeUpdate();

		group_deletion_statement.setLong(1, group_id);
		int status2 = group_deletion_statement.executeUpdate();
	}
}