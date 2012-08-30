package org.stwerff.mslraws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stwerff.mslraws.images.InitListener;

import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.chap.memo.memoNodes.MemoNode;

public class MailServlet extends HttpServlet {
	private static final long serialVersionUID = -4215556699052474459L;
	private static final Logger log = Logger.getLogger("msl-raw-images");
	
	
	public int checkAndCount(MemoNode parent, String label){
		MemoNode node = parent.getChildByStringValue(label);
		return node!=null?node.getChildren().size():0;
	}
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue("msl-raw-images");
		MemoNode newNode = baseNode.getChildByStringValue("newImages");
		if (newNode == null) return;
		log.warning("newNode:"+newNode.getId().toString());
				
		int fullCount = checkAndCount(newNode,"full");
		int dsCount = checkAndCount(newNode,"downscaled");
		int subframeCount = checkAndCount(newNode,"subframe");
		int thumbCount = checkAndCount(newNode,"thumbnail");
		if (fullCount+dsCount+subframeCount+thumbCount == 0) return;
		log.warning("Found:F:"+fullCount+" S:"+subframeCount+" D:"+dsCount+" T:"+thumbCount);
		
		ArrayList<MemoNode> sols = newNode.getChildrenByRegEx(Pattern.compile("[0-9]+"), -1);
		String solCounts="";
		for (MemoNode sol : sols){
			solCounts+="sol"+sol.getStringValue()+":"+sol.getChildren().size()+" ";
			log.warning("sol"+sol.getStringValue()+":"+sol.getChildren().size());
		}
		String mail = "New MSL raw images have arrived:\n"
					  +solCounts
					  +"They consist of:\n\n"
					  +fullCount+" full images\n"
					  +dsCount+" downscaled images\n"
					  +subframeCount+" subframe images\n"
					  +thumbCount+" thumbnails\n\n"
					  +"Please check them at: http://msl-raw-images.appspot.com/lists.html";
		sendMail(mail);
		
		String tweet = "F:"+fullCount+" D:"+dsCount+" S:"+subframeCount+" T:"+thumbCount;
		String solstring = " for "+solCounts;
		String url = " see: http://msl-raw-images.appspot.com/lists.html";
		if (tweet.length()+solstring.length()<=140){
			tweet = tweet.concat(solstring);
		}
		if (tweet.length()+url.length()<=140){
			tweet = tweet.concat(url);
		}
		sendTweet(tweet);
		for (MemoNode child: newNode.getChildren()){
			newNode.delChild(child);
		}
		MemoNode.flushDB();
	}

	public void sendTweet(String text){
		if (InitListener.tf != null){
			Twitter twitter = InitListener.tf.getInstance();
			try {
				twitter.updateStatus(text);
			} catch (TwitterException e) {
				e.printStackTrace();
				log.severe("Twitter error!"+e.getLocalizedMessage());
			}
		} else {
			log.severe("Twitter not correctly initialized!");
		}
	}
	public void sendMail(String msgBody){
		MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue("mailList");
		ArrayList<MemoNode> addresses = baseNode.getChildren();
		Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress("ludo.stellingwerff@gmail.com", "MSL-Raw-Images Mailer"));
            msg.setSubject("New MSL raw images have been posted!");
            msg.setText(msgBody);
            msg.addRecipient(Message.RecipientType.TO,
                    new InternetAddress("ludo.stellingwerff@gmail.com", "MSL-Raw-Images Recipients"));
            for (MemoNode address : addresses){
            	String addr = address.getStringValue();
            	msg.addRecipient(Message.RecipientType.BCC,
            		new InternetAddress(addr,addr));
            	log.warning("Adding:"+addr);
            }
            Transport.send(msg);
            log.warning("Sent mail!");
        } catch (Exception e) {
           System.out.println("Error sending mail!");
        }
	}
}
