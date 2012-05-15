package com.kostmo.grouper.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.kostmo.grouper.LdapHelper;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;

@WebServlet("/lookup")
public class NameLookupServlet extends HttpServlet {
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
		final String[] attributes = LdapHelper.getLdapAttributes(ldap_properties);	

		Collection<String> aliases = Arrays.asList( request.getParameter("aliases").split(",") );
		String multi_name_query = StringUtils.join(Collections2.transform(aliases, new Function<String, String>() {
			@Override
			public String apply(String alias) {
				return "(" + attributes[0] + "=" + alias + ")";
			}
		}), "");

		JSONObject names = new JSONObject();
		try {

			String ad_query_filter = "(|" + multi_name_query + ")";
			SearchResult searchResult = LdapHelper.getGroupFileteredLdapSearchResult(ldap_properties, ad_query_filter);
			for (SearchResultEntry e : searchResult.getSearchEntries())
				names.put(e.getAttributeValue(attributes[0]).toLowerCase(), e.getAttributeValue(attributes[1]));

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