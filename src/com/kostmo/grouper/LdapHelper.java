package com.kostmo.grouper;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.HttpServlet;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;

public class LdapHelper {
	
	public static Properties getLdapProperties(HttpServlet servlet) {

		Properties ldap_properties = new Properties();
		try {
			ldap_properties.load(servlet.getServletContext().getResourceAsStream("/WEB-INF/ldap.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ldap_properties;
	}
	
	public static SearchResult getLdapSearchResult(Properties ldap_properties, String filter) throws LDAPException {

		
		String[] attributes = getLdapAttributes(ldap_properties);
		
		LDAPConnection ldap_connection = new LDAPConnection(
				ldap_properties.getProperty("host"),
				Integer.parseInt(ldap_properties.getProperty("port")),
				ldap_properties.getProperty("user"),
				ldap_properties.getProperty("password"));

		return ldap_connection.search(
				ldap_properties.getProperty("baseDN"),
				SearchScope.ONE,
				filter,
				attributes);
	}
	
	/**
	 * Returns an array of Strings where the first element is the "alias" field name in ActiveDirectory,
	 * and the second element is the "name" field name.
	 * @param ldap_properties
	 * @return
	 * @throws LDAPException
	 */
	public static String[] getLdapAttributes(Properties ldap_properties) throws LDAPException {

		String[] attributes = new String[] {
				ldap_properties.getProperty("alias_field"),
				ldap_properties.getProperty("name_field")
		};
		
		return attributes;
	}
}
