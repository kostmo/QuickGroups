package com.kostmo.grouper.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.json.simple.JSONObject;

public class GroupMember implements JsonSerializable {

	long group_id;
	String alias;
	String set_by;
	Date modified;
	
	GroupMember(long group_id, String alias, String set_by) {
		this.group_id = group_id;
		this.alias = alias;
		this.set_by = set_by;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject asJsonObject() {
		JSONObject json_output_object = new JSONObject();
		json_output_object.put("alias", alias);
		json_output_object.put("set_by", set_by);
		json_output_object.put("modified", modified);
		return json_output_object;
	}
	
	public static GroupMember newFromResultSet(ResultSet rs) throws SQLException {
		return new GroupMember(
				rs.getLong("group_id"),
				rs.getString("alias").trim(),
				rs.getString("set_by").trim()
				);
	}
}