package com.kostmo.grouper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import javax.servlet.http.HttpServlet;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;

public class LdapHelper {
	
	@SuppressWarnings("serial")
	public static class MisconfigurationException extends Exception {
		public MisconfigurationException(String format) {
			super(format);
		}
	}

	public static Properties getLdapProperties(HttpServlet servlet) throws MisconfigurationException {

		String location = "/WEB-INF/ldap.properties";
		Properties ldap_properties = new Properties();
		try {
			ldap_properties.load(servlet.getServletContext().getResourceAsStream(location));
		} catch (IOException e) {
			throw new MisconfigurationException( String.format("You need to create the file \"%s\"", location) );
		}
		
		return ldap_properties;
	}
	
	public static SearchResult getLdapSearchResult(Properties ldap_properties, String member_filter) throws LDAPException {

		String[] attributes = getLdapAttributes(ldap_properties);
		LDAPConnection ldap_connection = new LDAPConnection(
				ldap_properties.getProperty("host"),
				Integer.parseInt(ldap_properties.getProperty("port")),
				ldap_properties.getProperty("user"),
				ldap_properties.getProperty("password"));

		Collection<String> group_common_names = Arrays.asList( ldap_properties.getProperty("group_common_names").split(",") );
		final String group_base_name = ldap_properties.getProperty("group_base_dn");
		
		String group_filter = StringUtils.join(Collections2.transform(group_common_names, new Function<String, String>() {
			@Override
			public String apply(String group_name) {
				return "(memberOf=CN=" + group_name + "," + group_base_name + ")";
			}
		}), "");
		
		String filter_with_groups = "(&" + member_filter + "(|" + group_filter + "))";
		return ldap_connection.search(
				ldap_properties.getProperty("baseDN"),
				SearchScope.SUB,
				Filter.create(filter_with_groups),
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