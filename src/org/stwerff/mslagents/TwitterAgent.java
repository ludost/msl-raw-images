package org.stwerff.mslagents;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import com.almende.eve.agent.Agent;
import com.almende.eve.json.annotation.Name;

public class TwitterAgent extends Agent {
	public static TwitterFactory tf=null;

	public TwitterAgent(){
		if (tf == null){
			ConfigurationBuilder cb = new ConfigurationBuilder();
				cb.setDebugEnabled(true)
		  		.setOAuthConsumerKey("zqabV9xmGSxx3EFWUBDdA")
		  		.setOAuthConsumerSecret("HMNudmnopG1xsoHebr3VKdwFyPErQUkS7XM6dJYj1kE")
		  		.setOAuthAccessToken("784328444-PdhkGZ25pA1lz2atQHFJHfeiPB45IGLGw1fW3m9d")
		  		.setOAuthAccessTokenSecret("oUxXVYqWtDqrufP4jLkaJPcT6UsNdwFemEBBu5llvU");
			tf = new TwitterFactory(cb.build());
		}
	}

	public void sendMessage(@Name("tweet") String tweet){
		try {
			Twitter twitter = tf.getInstance();
			twitter.updateStatus(tweet);
		} catch (TwitterException e) {
			e.printStackTrace();
			System.err.println("Twitter error!"+e.getLocalizedMessage());
		}
	}
	@Override
	public String getDescription() {
		return "Agent which can send tweets on behalf of 'MSLRawImages'";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
