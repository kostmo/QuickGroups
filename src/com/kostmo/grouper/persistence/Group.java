package com.kostmo.grouper.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class Group implements JsonSerializable {
	public final long id;
	public final String label;
	public final boolean is_public, is_self_serve;
	public final String owner;
	public Date created, modified;
	public final Set<GroupMember> group_members = new HashSet<GroupMember>();
	public final Set<String> tags = new HashSet<String>();

	public Group(long id, String label, boolean is_public, boolean is_self_serve, String owner) {
		this.id = id;
		this.label = label;
		this.is_public = is_public;
		this.is_self_serve = is_self_serve;
		this.owner = owner;
	}

	@SuppressWarnings("unchecked")
	public static Group newFromJSON(JSONObject json_object, String owner_alias) throws SQLException {

		Group g = new Group(
				(Long) json_object.get("id"),
				json_object.get("label").toString(),
				(Boolean) json_object.get("is_public"),
				(Boolean) json_object.get("is_self_serve"),
				owner_alias
			);

		JSONArray tags = (JSONArray) json_object.get("tags");
		g.tags.addAll(tags);
		
		JSONObject members_as_dicts = (JSONObject) json_object.get("members_as_dicts");
		for (Object item_key : members_as_dicts.keySet()) {
			String alias = item_key.toString();
			GroupMember member = new GroupMember(g.id, alias, owner_alias);
			g.group_members.add(member);
		}
		
		return g;
	}
	

	public boolean hasMember(String alias) {
		for (GroupMember group_member : group_members)
			if (group_member.alias.equals(alias))
				return true;
		
		return false;
	}
	
	public static Group newFromResultSet(ResultSet rs) throws SQLException {
		Group g = new Group(
				rs.getLong("id"),
				rs.getString("label").trim(),
				rs.getBoolean("is_public"),
				rs.getBoolean("is_self_serve"),
				rs.getString("owner").trim()
			);
		g.tags.addAll(Arrays.asList(rs.getString("taglist").split(",")));
		return g;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject asJsonObject() {
		JSONObject json_output_object = new JSONObject();
		json_output_object.put("id", id);
		json_output_object.put("label", label);
		json_output_object.put("is_public", is_public);
		json_output_object.put("is_self_serve", is_self_serve);
		json_output_object.put("owner", owner);
		
		JSONArray tags_array = new JSONArray();
		tags_array.addAll(tags);
		json_output_object.put("tags", tags_array);
		
		JSONObject members_as_dicts = new JSONObject();
		for (GroupMember member : this.group_members)
			members_as_dicts.put(member.alias, member.asJsonObject());
		json_output_object.put("members_as_dicts", members_as_dicts);

		return json_output_object;
	}
	
	@Override
	public int hashCode() {
        return new HashCodeBuilder(17, 31) // two randomly chosen prime numbers
            .append(id)
            .append(label)
            .append(is_public)
            .append(is_self_serve)
            .append(owner)
            .toHashCode();
    }
	
	@Override
	public boolean equals(Object obj) {
		
		if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != getClass())
            return false;

		Group rhs = (Group) obj;
        return new EqualsBuilder()
            .append(id, rhs.id)
            .append(label, rhs.label)
            .append(is_public, rhs.is_public)
            .append(is_self_serve, rhs.is_self_serve)
            .append(owner, rhs.owner)
            .isEquals();
	}
}