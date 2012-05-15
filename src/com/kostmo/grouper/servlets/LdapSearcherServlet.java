package com.kostmo.grouper.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.kostmo.grouper.LdapHelper;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;

@WebServlet("/search")
public class LdapSearcherServlet extends HttpServlet {
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
		
		final String partial_name = request.getParameter("term");
		String field = request.getParameter("field");

		Collection<String> rules = new ArrayList<String>();
		if ("alias".equals(field) || "both".equals(field))
			rules.add(attributes[0]);
		if ("name".equals(field) || "both".equals(field))
			rules.add(attributes[1]);
		
		
		
		String multi_rule_query = StringUtils.join(Collections2.transform(rules, new Function<String, String>() {
			@Override
			public String apply(String rule) {
				return "(" + rule + "=" + partial_name + "*)";
			}
		}), "");

		
		
		
		JSONArray names = new JSONArray();
		try {

			String subfilter = "(|" + multi_rule_query +")";
			SearchResult searchResult = LdapHelper.getGroupFileteredLdapSearchResult(ldap_properties, subfilter);
//			System.out.println(searchResult.getEntryCount() + " entries returned.");
			for (SearchResultEntry e : searchResult.getSearchEntries()) {
				JSONObject result_entry = new JSONObject();
				result_entry.put("value", e.getAttributeValue(attributes[0]).toLowerCase());
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