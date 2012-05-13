package com.kostmo.grouper.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

import com.kostmo.grouper.LdapHelper;
import com.kostmo.grouper.LdapHelper.MisconfigurationException;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;

@WebServlet("/heirarchy")
public class HeirarchyQueryServlet extends HttpServlet {
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

	
	public static class OrgHeirarchyRetriever implements Runnable {
		
		public final Collection<String> aliases = new HashSet<String>();
		
		public boolean print = true;
		HttpServlet servlet;
		Person toplevel_manager;
		OrgHeirarchyRetriever(HttpServlet servlet, Person toplevel_manager) {
			this.servlet = servlet;
			this.toplevel_manager = toplevel_manager;
		}

		public void discoverHeirarchyRecursive(HttpServlet servlet, Person person, int depth) throws LDAPException, MisconfigurationException {

			aliases.add(person.alias);
			
			if (print) {
				for (int i=0; i<depth; i++)
					System.out.print( '\t' );
				
				System.out.println( person.full_name );
			}
			
			Collection<Person> direct_reports = person.getDirectReports(servlet);
			for (Person direct_report : direct_reports)
				discoverHeirarchyRecursive(servlet, direct_report, depth + 1);
		}

		@Override
		public void run() {

			try {
				discoverHeirarchyRecursive(servlet, toplevel_manager, 0);
			} catch (LDAPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MisconfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static class Person {

		public static final String MANAGER_FIELD_NAME = "manager";
		public static final String[] ENHANCED_ATTRIBUTES = new String[] {"sAMAccountName", "name", "distinguishedName", "manager"};
		
		public String alias, full_name, dn;
		Person manager;
		
		Person(String alias, String full_name, String dn) {
			this.alias = alias;
			this.full_name = full_name;
			this.dn = dn;
		}
		
		public static Person fromSearchResultEntry(SearchResultEntry e) {
			Person p = new Person(
					e.getAttributeValue(ENHANCED_ATTRIBUTES[0]),
					e.getAttributeValue(ENHANCED_ATTRIBUTES[1]),
					e.getAttributeValue(ENHANCED_ATTRIBUTES[2]));
			
			return p;
		}

		Collection<Person> getDirectReports(HttpServlet servlet) throws MisconfigurationException, LDAPException {

			Collection<Person> direct_reports = new ArrayList<Person>();

			String subfilter = "(" + MANAGER_FIELD_NAME + "=" + this.dn + ")";
			Properties ldap_properties = LdapHelper.getLdapProperties(servlet);
			SearchResult searchResult = LdapHelper.getLdapSearchResult(ldap_properties, subfilter, ENHANCED_ATTRIBUTES);
			for (SearchResultEntry e : searchResult.getSearchEntries()) {

				Person p = fromSearchResultEntry(e);
				direct_reports.add(p);
			}

			return direct_reports;
		}
		
		@Override
		public String toString() {
			return "alias: "  + alias + "; full_name: " + full_name + "; dn: " + dn;
		}
	}
	
	void writeGrouperPage(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String manager_alias = request.getParameter("manager");
		Properties ldap_properties = LdapHelper.getLdapProperties(this);
		SearchResult searchResult = LdapHelper.getLdapSearchResult(ldap_properties, "(" + Person.ENHANCED_ATTRIBUTES[0] + "=" + manager_alias+ ")", Person.ENHANCED_ATTRIBUTES);


		Person toplevel_manager = null;
		for (SearchResultEntry e : searchResult.getSearchEntries()) {
			toplevel_manager = Person.fromSearchResultEntry(e);
			break;
		}

		OrgHeirarchyRetriever heirarchy_retriever = new OrgHeirarchyRetriever(this, toplevel_manager);
		heirarchy_retriever.print = false;
		heirarchy_retriever.run();
		
		System.out.println( StringUtils.join(heirarchy_retriever.aliases, ", ") );
		

		JSONObject heirarchy = new JSONObject();
		StringWriter json_out = new StringWriter();
		heirarchy.writeJSONString(json_out);

		PrintWriter out = response.getWriter();
		out.append( json_out.toString() );
	}

	@Override
	public void destroy() {
		super.destroy();
	}
}