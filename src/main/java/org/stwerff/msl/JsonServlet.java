package org.stwerff.msl;

import java.io.InputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stwerff.mslagents.MaxSolAgent;
import org.stwerff.mslagents.SolAgent;
import org.stwerff.mslagents.StatsAgent;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.config.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class JsonServlet extends HttpServlet {

	private static final long serialVersionUID = 18317478915312L;
	static final ObjectMapper om = new ObjectMapper();
	static final DateTimeFormatter WRITE_HTTPDATE = DateTimeFormat
			.forPattern("E, dd MMM yyyy HH:mm:ss 'GMT'")
			.withLocale(java.util.Locale.ENGLISH)
			.withZone(DateTimeZone.forID("GMT"));

	static final DateTimeFormatter READ_HTTPDATE = DateTimeFormat
			.forPattern("E, dd MMM yyyy HH:mm:ss")
			.withLocale(java.util.Locale.ENGLISH)
			.withZone(DateTimeZone.forID("GMT"));

	static int maxSol = -1;

	public void doPost(HttpServletRequest req, HttpServletResponse res) {
		handle(req, res);
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) {
		handle(req, res);
	}

	public void handle(HttpServletRequest req, HttpServletResponse res) {
		res.setContentType("application/json");
		InputStream is = getServletContext().getResourceAsStream(
				"/WEB-INF/eve.yaml");
		Config config = new Config(is);
		try {
			AgentFactory factory = new AgentFactory(config);
			SolAgent agent = null;
			int sol = -1;
			try {
				sol = Integer.parseInt(req.getParameter("sol"));
			} catch (Exception e) {
			}
			if (maxSol == -1 || sol == -1) {
				MaxSolAgent maxsol = (MaxSolAgent) factory.getAgent("max");
				maxSol = maxsol.getMaxSol();

				if (sol == -1) {
					StatsAgent stats = (StatsAgent) factory.getAgent("stats");
					res.setHeader("Cache-Control",
							"max-age=0,public,must-revalidate,proxy-revalidate");
					res.getWriter().println(
							"{\"sol\":" + maxSol + ",\"count\":"
									+ stats.getTotalCount() + "}");
					return;
				}
			}

			if (sol > maxSol) {
				System.err.println("Strange, too large sol requested!");
				return;
			}
			if (!factory.hasAgent("sol_" + sol)) {
				agent = (SolAgent) factory.createAgent(
						org.stwerff.mslagents.SolAgent.class, "sol_" + sol);
			} else {
				try {
					agent = (SolAgent) factory.getAgent("sol_" + sol);
				} catch (Exception e) {
					factory.deleteAgent("sol_" + sol);
					agent = (SolAgent) factory.createAgent(
							org.stwerff.mslagents.SolAgent.class, "sol_" + sol);
				}
			}
			String lastChange = DateTime.parse(agent.getLastChange()).toString(
					WRITE_HTTPDATE);
			try {
				// Sun, 06 Nov 1994 08:49:37 GMT
				if (req.getHeader("If-Modified-Since") != null) {
					DateTime ifmod = DateTime.parse(
							req.getHeader("If-Modified-Since").replace(" GMT",
									""), READ_HTTPDATE);
					if (!ifmod.isBefore(DateTime.parse(lastChange,
							WRITE_HTTPDATE))) {
						res.setHeader("Last-Modified", lastChange);
						res.setStatus(304);
						return;
					}
				}
			} catch (Exception e) {
				System.err.println("Exception:" + e);
				e.printStackTrace();
			}
			ArrayNode list = agent.getList();
			lastChange = DateTime.parse(agent.getLastChange()).toString(
					WRITE_HTTPDATE);
			res.setHeader("Last-Modified", lastChange);
			res.setContentLength(list.toString().length());
			res.setHeader("Cache-Control",
					"max-age=60,public,must-revalidate,proxy-revalidate");
			res.getWriter().write(list.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
