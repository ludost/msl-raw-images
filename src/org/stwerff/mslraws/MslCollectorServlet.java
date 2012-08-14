package org.stwerff.mslraws;

import java.io.IOException;
import javax.servlet.http.*;

import org.stwerff.mslraws.images.InitListener;
import org.stwerff.mslraws.parser.SiteParser;

import com.chap.memo.memoNodes.MemoNode;

@SuppressWarnings("serial")
public class MslCollectorServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/plain");
		InitListener.initDemoData();
		System.out.println("Collecting images!");
		
		int i=0;
		while(true){
			if (!SiteParser.fetch("http://mars.jpl.nasa.gov/msl/multimedia/raw/",i++)) break;
			if (i>10000){
				System.out.println("Are we really on sol 10000 by now? Whow!");
				break;
			}
		}
		MemoNode.flushDB();
	}
}
