package com.kostmo.grouper.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.kostmo.grouper.LdapHelper.MisconfigurationException;
import com.kostmo.grouper.persistence.Group;
import com.kostmo.grouper.persistence.PostgresData;

@WebServlet("/save")
public class SaveDataServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;


	@Override
	public void init() throws ServletException {

	}

	// ========================================================================
	@SuppressWarnings({ "unchecked" })
	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String action_type = request.getParameter("action");
		
		JSONObject result_object = null;
		if ("insert".equals(action_type)) {
			result_object = insertAction(request);
		} else if ("copy".equals(action_type)) {
			result_object = copyAction(request);
		} else if ("delete".equals(action_type)) {
			result_object = deleteAction(request);
		} else if ("modify".equals(action_type)) {
			result_object = modifyAction(request);
		} else if ("bequeath".equals(action_type)) {
			result_object = bequeathAction(request);
		} else {
			result_object = new JSONObject();
			result_object.put("success", false);
			result_object.put("message", "Invalid action.");
		}

		StringWriter json_out = new StringWriter();
		result_object.writeJSONString(json_out);

		response.setContentType("text/json");
		PrintWriter out = response.getWriter();
		out.append( json_out.toString() );
	}

	// ========================================================================
	@SuppressWarnings("unchecked")
	private JSONObject copyAction(HttpServletRequest request) {


		String group_id_string = request.getParameter("group_id");
		long source_group_id = Long.parseLong(group_id_string);
		
		String new_label = request.getParameter("label");
		

		JSONObject json_output_object = new JSONObject();
		json_output_object.put("success", false);
		
		try {
			Connection postgres_connection = PostgresData.getPostgresConnection(this);

			long new_group_id = PostgresData.copyGroup(postgres_connection, source_group_id, new_label, request.getRemoteUser());

			json_output_object.put("success", new_group_id >= 0);
			json_output_object.put("new_group_id", new_group_id);
			
		} catch (SQLException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		} catch (MisconfigurationException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		}

		return json_output_object;
	}

	// ========================================================================
	@SuppressWarnings("unchecked")
	private JSONObject bequeathAction(HttpServletRequest request) {

		String group_id_string = request.getParameter("group_id");
		long group_id = Long.parseLong(group_id_string);
		
		String new_owner = request.getParameter("new_owner");

		JSONObject json_output_object = new JSONObject();
		try {
			Connection postgres_connection = PostgresData.getPostgresConnection(this);
			PostgresData.bequeathGroup(postgres_connection, group_id, request.getRemoteUser(), new_owner);

			json_output_object.put("success", true);
			
		} catch (SQLException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		} catch (MisconfigurationException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		}

		return json_output_object;
	}
	
	
	// ========================================================================
	@SuppressWarnings("unchecked")
	private JSONObject deleteAction(HttpServletRequest request) {


		String group_id_string = request.getParameter("group_id");
		long group_id = Long.parseLong(group_id_string);
		

		JSONObject json_output_object = new JSONObject();
		try {
			Connection postgres_connection = PostgresData.getPostgresConnection(this);
			PostgresData.deleteGroup(postgres_connection, group_id, request.getRemoteUser());

			json_output_object.put("success", true);
			
		} catch (SQLException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		} catch (MisconfigurationException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		}

		return json_output_object;
	}

	// ========================================================================
	@SuppressWarnings("unchecked")
	private JSONObject insertAction(HttpServletRequest request) {

		String json_input_string = request.getParameter("json");
		JSONParser parser = new JSONParser();
		JSONObject json_input_object = null;
		try {
			json_input_object = (JSONObject) parser.parse(json_input_string);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		int entry_set_size = json_input_object.entrySet().size();


		JSONObject json_output_object = new JSONObject();
		try {
			Connection postgres_connection = PostgresData.getPostgresConnection(this);
			
			long group_id = -2;
			for (Object entry : json_input_object.keySet()) {
				String group_name = entry.toString();
				JSONObject group_json_object = (JSONObject) json_input_object.get(entry);
				
				group_id = PostgresData.insertNewGroup(postgres_connection, Group.newFromJSON(group_json_object, request.getRemoteUser()));
			}

			json_output_object.put("success", true);
			json_output_object.put("created_new_group", true);
			json_output_object.put("new_group_id", group_id);
			json_output_object.put("count", entry_set_size);
			
		} catch (SQLException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		} catch (MisconfigurationException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		}

		return json_output_object;
	}

	// ========================================================================
	@SuppressWarnings("unchecked")
	private JSONObject modifyAction(HttpServletRequest request) {

		String json_input_string = request.getParameter("json");
		JSONParser parser = new JSONParser();
		JSONObject json_input_object = null;
		try {
			json_input_object = (JSONObject) parser.parse(json_input_string);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		int entry_set_size = json_input_object.entrySet().size();

		
		JSONObject json_output_object = new JSONObject();
		try {
			Connection postgres_connection = PostgresData.getPostgresConnection(this);
			
			for (Object entry : json_input_object.keySet()) {

				JSONObject group_json_object = (JSONObject) json_input_object.get(entry);
				PostgresData.updateGroup(postgres_connection, Group.newFromJSON(group_json_object, request.getRemoteUser()));
			}
			
			json_output_object.put("success", new Boolean(true));

		} catch (SQLException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		} catch (MisconfigurationException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			json_output_object.put("message", e.getMessage());
		}
		
		json_output_object.put("count", new Integer(entry_set_size));
		return json_output_object;
	}

	// ========================================================================
	@Override
	public void destroy() {
		super.destroy();
	}
}