package com.kostmo.grouper.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.json.simple.JSONObject;

public class GroupMember implements JsonSerializable {

	public static final String KEY_PROFICIENCY = "proficiency";
	public static final String KEY_SET_BY = "set_by";
	public static final String KEY_ALIAS = "alias";
	public static final String KEY_MODIFIED = "modified";
	
	final long group_id;
	final String alias;
	final String set_by;
	final int proficiency;
	Date modified;

	GroupMember(long group_id, String alias, String set_by) {
		this(group_id, alias, set_by, 0);
	}
	
	GroupMember(long group_id, String alias, String set_by, int proficiency) {
		this.group_id = group_id;
		this.alias = alias;
		this.set_by = set_by;
		this.proficiency = proficiency;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject asJsonObject() {
		JSONObject json_output_object = new JSONObject();
		json_output_object.put(KEY_ALIAS, alias);
		json_output_object.put(KEY_SET_BY, set_by);
		json_output_object.put(KEY_PROFICIENCY, proficiency);
		json_output_object.put(KEY_MODIFIED, modified);
		return json_output_object;
	}
	
	public static GroupMember newFromResultSet(ResultSet rs) throws SQLException {
		return new GroupMember(
				rs.getLong("group_id"),
				rs.getString(KEY_ALIAS).trim(),
				rs.getString(KEY_SET_BY).trim(),
				rs.getInt(KEY_PROFICIENCY)
				);
	}
}