package org.stwerff.mslraws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chap.memo.memoNodes.MemoNode;

public class MailServlet extends HttpServlet {
	private static final long serialVersionUID = -4215556699052474459L;
	private static final Logger log = Logger.getLogger("msl-raw-images");
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		MemoNode baseNode = MemoNode.getRootNode().getChildByStringValue("mailList");
		if (MemoNode.getRootNode().getChildByStringValue("newImagesFlag").getPropertyValue("new").equals("true")){
			MemoNode.getRootNode().getChildByStringValue("newImagesFlag").setPropertyValue("new","false");
			sendMail(baseNode.getChildren());
		}
	}

	public void sendMail(ArrayList<MemoNode> addresses){
		Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        String msgBody = "Hi, some new images have been posted to the MSL website RAW's section.";

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress("ludo@stwerff.xs4all.nl", "MSL-Raw-Images Mailer"));
            msg.setSubject("New MSL raw images have been posted!");
            msg.setText(msgBody);
            msg.addRecipient(Message.RecipientType.TO,
                    new InternetAddress("ludo@stwerff.xs4all.nl", "MSL-Raw-Images Recipients"));
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
