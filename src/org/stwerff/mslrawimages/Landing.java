package org.stwerff.mslrawimages;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Landing extends HttpServlet {
	private static final long serialVersionUID = 5332829006207917493L;
	private static final Logger log = Logger.getLogger("msl-raw-images");
	static final ObjectMapper om = new ObjectMapper();
	static final JsonFactory f = new JsonFactory();

	static String quickServe = "";

	public void doPost(HttpServletRequest req, HttpServletResponse res) {
		handle(req, res);
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) {
		handle(req, res);
	}

	public void handle(HttpServletRequest req, HttpServletResponse res) {
		res.setContentType("application/json");
		try {
			int sol = -1;
			try {
				sol = Integer.parseInt(req.getParameter("sol"));
			} catch (Exception e) {
			}
			
			if (sol < 0) {
				if ("".equals(quickServe)) {
					StorageIntf store = new GoogleCloudStorage();
					Reader reader = store
							.getReader("/gs/msl-raw-images/MaxSol");
					JsonNode ret = om.readTree(reader);
					int maxSol = ret.get("maxsol").asInt();
					
					log.warning("Regenerating quickServe: maxSol:" + maxSol);

					StringWriter writer = new StringWriter();
					f.setCodec(om);
					JsonGenerator g = f.createJsonGenerator(writer);
					g.writeStartArray();

					for (int i = 0; i < maxSol; i++) {
						//TODO: parallelizeren, hoe? 
						try {
							Iterator<JsonNode> iter = getSol(i).iterator();
							while (iter.hasNext()) {
								g.writeTree(iter.next());
							}
						} catch (Exception e) {
						}
					}
					g.writeEndArray();
					g.flush();

					quickServe = writer.toString();
				}
				if (!"cron".equals(req.getParameter("src"))){
					res.setContentLength(quickServe.length());
					res.getWriter().append(quickServe);
				}
			} else {
				String data = getSol(sol).toString();
				if (!"cron".equals(req.getParameter("src"))){
					res.setContentLength(data.length());
					res.getWriter().append(data);
				}
			}
		} catch (Exception e) {
			log.severe("Exception getting landing:" + e.getLocalizedMessage());
		}
	}

	public JsonNode getSol(int sol) throws JsonProcessingException, IOException {
		StorageIntf store = new GoogleCloudStorage();
		Reader reader = store.getReader(store.createFilename(sol));
		return om.readTree(reader);
	}
}
