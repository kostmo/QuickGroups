package com.kostmo.grouper;

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

@WebServlet("/load")
public class LoadDataServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;


	@Override
	public void init() throws ServletException {

	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest request,
		HttpServletResponse response) throws ServletException, IOException {



		String action = request.getParameter("action");
		

		JSONObject json_output_object = new JSONObject();
		json_output_object.put("success", false);
		
		JSONArray json_group_array = new JSONArray();
		
		// TODO
		if ("exportAll".equals(action)) {

			String format = request.getParameter("format").toLowerCase();
			response.setHeader("Content-Disposition", "attachment; filename=exported_groups." + format);
			response.setContentType("text/" + format);
			
			if ("xml".equals(format)) {

				
				json_group_array.add(1);
				
				
			} else if ("json".equals(format)) {

				
				json_group_array.add(2);
			} else if ("csv".equals(format)) {

				
				json_group_array.add(3);
			}
			
		} else {
			
			Map<Long, Group> groups = null;
			
			try {
				Connection postgres_connection = PostgresData.getPostgresConnection(this);
				groups = PostgresData.loadGroups(postgres_connection, request.getRemoteUser());
				
				for (Group g : groups.values()) {
					
					Map<String, GroupMember> group_members = PostgresData.loadGroupMembers(postgres_connection, g.id);
					g.group_members.addAll(group_members.values());
					
					JSONObject jo = g.asJsonObject();
					jo.put("mine", g.owner.equals(request.getRemoteUser()));
					json_group_array.add(jo);
				}
				

				json_output_object.put("success", true);
				
			} catch (SQLException e) {
				e.printStackTrace();
				json_output_object.put("error", e.getMessage());
			} catch (MisconfigurationException e) {
				e.printStackTrace();
				json_output_object.put("error", e.getMessage());
			}
		}

		json_output_object.put("groups", json_group_array);

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