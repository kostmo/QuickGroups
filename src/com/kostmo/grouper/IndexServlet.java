package com.kostmo.grouper;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.kostmo.grouper.LdapHelper.MisconfigurationException;

@WebServlet("/index")
public class IndexServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {

	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

	    String url="/index.jsp"; //relative url for display jsp page
	    ServletContext sc = getServletContext();
	    RequestDispatcher rd = sc.getRequestDispatcher(url);

	    try {
			String company_domain = LdapHelper.getLdapProperties(this).getProperty("company_domain");
		    request.setAttribute("company_domain", company_domain );
		    
		} catch (MisconfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    rd.forward(request, response);
	}
}