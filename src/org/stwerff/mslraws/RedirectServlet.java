package org.stwerff.mslraws;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RedirectServlet extends HttpServlet {
	private static final long serialVersionUID = 401580643181293945L;
	private static final String url = "./landing.json";
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.sendRedirect(url);
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.sendRedirect(url);
	}
}
