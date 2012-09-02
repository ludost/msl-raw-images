package org.stwerff.mslraws.images;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.stwerff.mslraws.LandingPageServlet;

import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import com.chap.memo.memoNodes.MemoNode;
import com.eaio.uuid.UUID;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class InitListener  implements ServletContextListener {

	public static TwitterFactory tf=null;
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		initData();
		preLoadList();
		initTwitter();
	}

	public static void initTwitter(){
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey("zqabV9xmGSxx3EFWUBDdA")
		  .setOAuthConsumerSecret("HMNudmnopG1xsoHebr3VKdwFyPErQUkS7XM6dJYj1kE")
		  .setOAuthAccessToken("784328444-PdhkGZ25pA1lz2atQHFJHfeiPB45IGLGw1fW3m9d")
		  .setOAuthAccessTokenSecret("oUxXVYqWtDqrufP4jLkaJPcT6UsNdwFemEBBu5llvU");
		tf = new TwitterFactory(cb.build());
	}
	public static void preLoadList(){
		MemcacheService memCache = MemcacheServiceFactory.getMemcacheService();
		if (memCache.get("quickServe") != null && memCache.get("quickServe") != ""){
			UUID uuid = new UUID((String)memCache.get("quickServe"));
			LandingPageServlet.quickServe = new MemoNode(uuid);
		}
	}
	public static void initData(){
		MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue("msl-raw-images");
		if (baseNode == null){
			baseNode = MemoNode.getRootNode().addChild(new MemoNode("msl-raw-images"));
			baseNode.addChild(new MemoNode("sols"));
			baseNode.addChild(new MemoNode("NLA"));
			baseNode.addChild(new MemoNode("NRA"));
			baseNode.addChild(new MemoNode("NLB"));
			baseNode.addChild(new MemoNode("NRB"));
			baseNode.addChild(new MemoNode("MH"));
			baseNode.addChild(new MemoNode("ML"));
			baseNode.addChild(new MemoNode("MD"));
			baseNode.addChild(new MemoNode("MR"));
			baseNode.addChild(new MemoNode("RRA"));
			baseNode.addChild(new MemoNode("RLA"));
			baseNode.addChild(new MemoNode("RRB"));
			baseNode.addChild(new MemoNode("RLB"));
			baseNode.addChild(new MemoNode("FRA"));
			baseNode.addChild(new MemoNode("FLA"));
			baseNode.addChild(new MemoNode("FRB"));
			baseNode.addChild(new MemoNode("FLB"));
			baseNode.addChild(new MemoNode("CR0"));
		}
		baseNode = MemoNode.getRootNode().getChildByStringValue("mailList");
		if (baseNode == null){
			MemoNode.getRootNode().addChild(new MemoNode("mailList")).addChild(new MemoNode("ludo@stwerff.xs4all.nl"));
			MemoNode.getRootNode().addChild(new MemoNode("newImagesFlag")).setPropertyValue("new", "false");
			MemoNode.flushDB();
		}
		
	}
}
