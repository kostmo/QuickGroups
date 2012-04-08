package com.kostmo.grouper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;

import com.kostmo.grouper.persistence.PostgresData;

@WebServlet("/tags")
public class TagSearchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {

	}
	
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/json");
		try {
			writeGrouperPage(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void writeGrouperPage(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String partial_tag = request.getParameter("term");
		Connection postgres_connection = PostgresData.getPostgresConnection(this);
		JSONArray results = PostgresData.searchTags(postgres_connection, partial_tag);

		StringWriter json_out = new StringWriter();
		results.writeJSONString(json_out);

		PrintWriter out = response.getWriter();
		out.append( json_out.toString() );
	}

	@Override
	public void destroy() {
		super.destroy();
	}
}