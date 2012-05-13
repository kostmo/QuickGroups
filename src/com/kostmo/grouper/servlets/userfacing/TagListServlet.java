package com.kostmo.grouper.servlets.userfacing;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.kostmo.grouper.persistence.PostgresData;
import com.kostmo.grouper.persistence.Tag;

@WebServlet("/taglist")
public class TagListServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {

	}
	
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		try {
			writeGrouperPage(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void writeGrouperPage(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Connection postgres_connection = PostgresData.getPostgresConnection(this);
		
		boolean include_unused = request.getParameterMap().containsKey("unused");
		Collection<Tag> tags = PostgresData.getFullTagsHistogram(postgres_connection, include_unused);
		

		PrintWriter out = response.getWriter();
		
		out.append("<table>");
		out.append("<tr><th>Tag</th><th>Count</th></tr>");
		for (Tag tag : tags)
			out.append( String.format("<tr><td>%s</td><td>%d</td></tr>", tag.name, tag.usage_count) );

		out.append("</table>");
	}

	@Override
	public void destroy() {
		super.destroy();
	}
}