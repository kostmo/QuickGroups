package com.kostmo.grouper.servlets.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.kostmo.grouper.LdapHelper.MisconfigurationException;
import com.kostmo.grouper.persistence.Group;
import com.kostmo.grouper.persistence.GroupMember;
import com.kostmo.grouper.persistence.PostgresData;

@WebServlet("/api/members")
public class GroupMembersServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;


	@Override
	public void init() throws ServletException {

	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest request,
		HttpServletResponse response) throws ServletException, IOException {

		JSONObject json_output_object = new JSONObject();
		json_output_object.put("success", false);
		
		String group_id_parameter = request.getParameter("group_id");
		if (group_id_parameter != null) {
			long requested_group_id = Long.parseLong(group_id_parameter);

			try {
				Connection postgres_connection = PostgresData.getPostgresConnection(this);
				Group g = PostgresData.loadSingleGroup(postgres_connection, request.getRemoteUser(), requested_group_id);

				Map<String, GroupMember> group_members = PostgresData.loadGroupMembers(postgres_connection, g.id);
				g.group_members.addAll(group_members.values());
				
				JSONObject jo = g.asJsonObject();
				jo.put("mine", g.owner.equals(request.getRemoteUser()));


				json_output_object.put("group", g.asJsonObject());
				json_output_object.put("success", true);
				
			} catch (SQLException e) {
				e.printStackTrace();
				json_output_object.put("error", e.getMessage());
			} catch (MisconfigurationException e) {
				e.printStackTrace();
				json_output_object.put("error", e.getMessage());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				json_output_object.put("error", e.getMessage());
			}
		}

		StringWriter json_out = new StringWriter();
		json_output_object.writeJSONString(json_out);

		response.setContentType("text/json");
		
		PrintWriter out = response.getWriter();
		out.append( json_out.toString() );
	}

	@Override
	public void destroy() {
		super.destroy();
	}
}