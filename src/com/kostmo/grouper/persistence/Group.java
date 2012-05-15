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
	

	public static final String KEY_ID = "id";
	public static final String KEY_LABEL = "label";
	public static final String KEY_IS_PUBLIC = "is_public";
	public static final String KEY_IS_SELF_SERVE = "is_self_serve";
	public static final String KEY_IS_SKILL = "is_skill";
	public static final String KEY_OWNER = "owner";
	public static final String KEY_TAGS = "tags";
	
	public final long id;
	public final String label;
	public final boolean is_public, is_self_serve, is_skill;
	public final String owner;
	public Date created, modified;
	public final Set<GroupMember> group_members = new HashSet<GroupMember>();
	public final Set<String> tags = new HashSet<String>();

	public Group(long id, String label, boolean is_public, boolean is_self_serve, boolean is_skill, String owner) {
		this.id = id;
		this.label = label;
		this.is_public = is_public;
		this.is_self_serve = is_self_serve;
		this.is_skill = is_skill;
		this.owner = owner;
	}

	@SuppressWarnings("unchecked")
	public static Group newFromJSON(JSONObject json_object, String owner_alias) throws SQLException {

		Group group = new Group(
				(Long) json_object.get(KEY_ID),
				json_object.get(KEY_LABEL).toString(),
				(Boolean) json_object.get(KEY_IS_PUBLIC),
				(Boolean) json_object.get(KEY_IS_SELF_SERVE),
				(Boolean) json_object.get(KEY_IS_SKILL),
				owner_alias
			);

		JSONArray tags = (JSONArray) json_object.get(KEY_TAGS);
		group.tags.addAll(tags);
		
		JSONObject members_as_dicts = (JSONObject) json_object.get("members_as_dicts");
		for (Object item_key : members_as_dicts.keySet()) {
			
			JSONObject member_as_dict = (JSONObject) members_as_dicts.get(item_key);
			
			String alias = item_key.toString();
			GroupMember member = new GroupMember(
					group.id,
					alias,
					owner_alias,
					Integer.parseInt(member_as_dict.get(GroupMember.KEY_PROFICIENCY).toString()));
			group.group_members.add(member);
		}
		
		return group;
	}
	

	public boolean hasMember(String alias) {
		for (GroupMember group_member : group_members)
			if (group_member.alias.equalsIgnoreCase(alias))
				return true;
		
		return false;
	}
	
	public static Group newFromResultSet(ResultSet rs) throws SQLException {
		Group g = new Group(
				rs.getLong(KEY_ID),
				rs.getString(KEY_LABEL).trim(),
				rs.getBoolean(KEY_IS_PUBLIC),
				rs.getBoolean(KEY_IS_SELF_SERVE),
				rs.getBoolean(KEY_IS_SKILL),
				rs.getString(KEY_OWNER).trim()
			);
		String taglist_string = rs.getString("taglist");
		if (taglist_string != null)
			g.tags.addAll(Arrays.asList(taglist_string.split(",")));
		return g;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject asJsonObject() {
		JSONObject json_output_object = new JSONObject();
		json_output_object.put(KEY_ID, id);
		json_output_object.put(KEY_LABEL, label);
		json_output_object.put(KEY_IS_PUBLIC, is_public);
		json_output_object.put(KEY_IS_SELF_SERVE, is_self_serve);
		json_output_object.put(KEY_IS_SKILL, is_skill);
		json_output_object.put(KEY_OWNER, owner);
		
		JSONArray tags_array = new JSONArray();
		tags_array.addAll(tags);
		json_output_object.put(KEY_TAGS, tags_array);
		
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
            .append(is_skill)
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
            .append(is_skill, rhs.is_skill)
            .append(owner, rhs.owner)
            .isEquals();
	}
}