package com.kostmo.grouper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;

@WebServlet("/search")
public class SearcherServlet extends HttpServlet {
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

	
	@SuppressWarnings("unchecked")
	void writeGrouperPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		Properties ldap_properties = LdapHelper.getLdapProperties(this);
		String[] attributes = LdapHelper.getLdapAttributes(ldap_properties);	
		
		String partial_name = request.getParameter("term");
		String field = request.getParameter("field");
		String ad_search_field = "alias".equals(field) ? attributes[0] : attributes[1];
		
		JSONArray names = new JSONArray();
		try {

			String filter = "(" + ad_search_field + "=" + partial_name +"*)";
			if (partial_name != null)
				filter = "(" + ad_search_field + "=" + partial_name +"*)";

			SearchResult searchResult = LdapHelper.getLdapSearchResult(ldap_properties, filter);

			System.out.println(searchResult.getEntryCount() + " entries returned.");
			for (SearchResultEntry e : searchResult.getSearchEntries()) {
				JSONObject result_entry = new JSONObject();
				result_entry.put("value", e.getAttributeValue(attributes[0]));
				result_entry.put("label", e.getAttributeValue(attributes[1]));
				names.add(result_entry);
			}

		} catch (LDAPException e) {
			e.printStackTrace();
		}



		StringWriter json_out = new StringWriter();
		names.writeJSONString(json_out);


		PrintWriter out = response.getWriter();
		out.append( json_out.toString() );
	}

	@Override
	public void destroy() {
		super.destroy();
	}
}