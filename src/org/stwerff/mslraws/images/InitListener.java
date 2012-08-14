package org.stwerff.mslraws.images;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.chap.memo.memoNodes.MemoNode;

public class InitListener  implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		initDemoData();
	}

	public static void initDemoData(){
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
