import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Post;
import facebook4j.Reading;
import facebook4j.ResponseList;
import facebook4j.auth.AccessToken;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.*;


public class SocialTextAnalysis {

	
	HashMap<String,float[]> scores;
	public SocialTextAnalysis(){
		scores = new HashMap<String,float[]>();
	}
	
	public void printTopTen(TreeMap<Float, String> t){
		
		System.out.println("_________________________________");
		int i=0;
		for(Entry<Float, String> topTweets:t.entrySet()){
			if(i++>=10)
				break;
			System.out.println(i+" . "+topTweets.getValue()+"|||||||Confidence:"+topTweets.getKey());
		}
		System.out.println();
		System.out.println();
	}
	
	
	public void PrepareSentimentScores() throws FileNotFoundException{
		
		File scoresFile = new File("twitter_sentiment_list.csv");
		Scanner sc = new Scanner(scoresFile);
		sc.nextLine();
		while(sc.hasNextLine()){
			String[] line = sc.nextLine().split(",");
			float[] subScores = new float[2];
			subScores[0] = Float.parseFloat(line[1]);
			subScores[1] = Float.parseFloat(line[2]);
			scores.put(line[0], subScores);
		}
		
	}
	
	
	public class TweetAnalysis{	
	
		String consumerKey;
		String consumerSecret;
		String accessToken;
		String tokenSecret;
		
		ConfigurationBuilder cb;
		TwitterFactory tf;
		Twitter twitterInstance;
		Query query;
	
		TreeMap<Float,String> topPositiveTweets;
		TreeMap<Float,String> topNegativeTweets;
		TreeMap<Float,String> topNeutralTweets;
		
		int totalTweetCount;
		int totalPosTweetCount;
		int totalNegTweetCount;
		int totalNeutTweetCount;
		
		
		
		public TweetAnalysis(String a,String b,String c,String d){
			consumerKey = new String(a);
			consumerSecret= new String(b);
			accessToken= new String(c);
			tokenSecret= new String(d);
			topPositiveTweets = new TreeMap<Float,String>(Collections.reverseOrder());
			topNegativeTweets= new TreeMap<Float,String>(Collections.reverseOrder());
			topNeutralTweets= new TreeMap<Float,String>(Collections.reverseOrder());
		}
		
		public void Authenticate() {
			try{
			    cb = new ConfigurationBuilder();
			    cb.setDebugEnabled(true)
			          .setOAuthConsumerKey(consumerKey)
			          .setOAuthConsumerSecret(consumerSecret)
			          .setOAuthAccessToken(accessToken)
			          .setOAuthAccessTokenSecret(tokenSecret);
			    tf = new TwitterFactory(cb.build());
			    twitterInstance = tf.getInstance();
				}
				catch(Exception te) {
		            te.printStackTrace();
		            System.out.println("Twitter app authentication failed: " + te.getMessage());
		            System.exit(-1);
		        }
		}	
		
		public void ExtractTweets(String keyword){
			try {	
					long since_Id=-1;
					for(int paging=1;paging<=10;paging++){
						Query query = new Query(keyword+"&lang:en");
			            query.setCount(100);
			            query.setMaxId(since_Id);
			            QueryResult result;
			            result = twitterInstance.search(query);
			            since_Id = result.getSinceId();
			            List<Status> tweets = result.getTweets();
			            for (Status tweet : tweets) {
			                //System.out.println(tweet.getText());
			                AssignSentiScores(tweet.getText());
			            }
					}
		            
			            
		        } catch (TwitterException te) {
		            te.printStackTrace();
		            System.out.println("Tweet Search failed: " + te.getMessage());
		            System.exit(-1);
		        }
		}
		
		
		
		public void AssignSentiScores(String rawTweetText){
			List<String> tweetTokens = Twokenize.tokenizeRawTweetText(rawTweetText);
			float sumPos=0,sumNeg=0,probPos=0,probNeg=0,probNeut=0;
			
			for(String s:tweetTokens){
				
				if(scores.containsKey(s)){
					sumPos+=scores.get(s)[0];
					sumNeg+=scores.get(s)[1];
				}
				
			}
			probPos = (float) (1.0/(Math.exp(sumNeg-sumPos)+1));
			probNeg = (float) (1.0 - probPos);
			probNeut = Math.abs(1-Math.abs(probPos - probNeg));
			
			totalTweetCount++;
			if(probPos>0.9){
				topPositiveTweets.put(probPos, rawTweetText);
				totalPosTweetCount++;
			}
				
			if(probNeg>0.5){
				topNegativeTweets.put(probNeg, rawTweetText);
				totalNegTweetCount++;
			}
				
			if(probNeut>0.6){
				topNeutralTweets.put(probNeut, rawTweetText);
				totalNeutTweetCount++;
			}
						
		}
		
	}
	
	
	public class FacebookAnalysis{
		String appId;
		String appSecret;
		String accessToken;
		
		Facebook facebook; 
		
		TreeMap<Float,String> topPositiveNewsFeeds;
		TreeMap<Float,String> topNegativeNewsFeeds;
		TreeMap<Float,String> topNeutralNewsFeeds;
		
		int totalNewsFeedCount;
		int totalPosNewsFeedCount;
		int totalNegNewsFeedCount;
		int totalNeutNewsFeedCount;
		
		
		
		public FacebookAnalysis(String a,String b,String c){
			appId = new String(a);
			appSecret= new String(b);
			accessToken= new String(c);
			
			topPositiveNewsFeeds = new TreeMap<Float,String>(Collections.reverseOrder());
			topNegativeNewsFeeds= new TreeMap<Float,String>(Collections.reverseOrder());
			topNeutralNewsFeeds= new TreeMap<Float,String>(Collections.reverseOrder());
		}
		
		public void Authenticate() {
			try{
				facebook = new FacebookFactory().getInstance();
				facebook.setOAuthAppId(appId, appSecret);
				facebook.setOAuthPermissions("email, publish_stream, id, name, first_name, last_name, read_stream , generic");
				facebook.setOAuthAccessToken(new AccessToken(accessToken));
				
				}
				catch(Exception fe) {
		            fe.printStackTrace();
		            System.out.println("Facebook app authentication failed: " + fe.getMessage());
		            System.exit(-1);
		        }
		}	
		
		public void ExtractNewsFeeds(String keyword){
			try {	
						ResponseList<Post> results = facebook.searchPosts(keyword,new Reading().limit(1000));
			                       
			            for (Post NewsFeed : results) {
			                //System.out.println(NewsFeed.getText());
			                AssignSentiScores(NewsFeed.getMessage());
			            }
					
		            
			            
		        } catch (FacebookException fe) {
		            fe.printStackTrace();
		            System.out.println("NewsFeed Search failed: " + fe.getMessage());
		            System.exit(-1);
		        }
		}
		
		
		
		public void AssignSentiScores(String rawNewsFeedText){
			List<String> NewsFeedTokens = Twokenize.tokenizeRawTweetText(rawNewsFeedText);
			float sumPos=0,sumNeg=0,probPos=0,probNeg=0,probNeut=0;
			
			for(String s:NewsFeedTokens){
				
				if(scores.containsKey(s)){
					sumPos+=scores.get(s)[0];
					sumNeg+=scores.get(s)[1];
				}
				
			}
			probPos = (float) (1.0/(Math.exp(sumNeg-sumPos)+1));
			probNeg = (float) (1.0 - probPos);
			probNeut = Math.abs(1-Math.abs(probPos - probNeg));
			
			totalNewsFeedCount++;
			if(probPos>0.9){
				topPositiveNewsFeeds.put(probPos, rawNewsFeedText);
				totalPosNewsFeedCount++;
			}
				
			if(probNeg>0.5){
				topNegativeNewsFeeds.put(probNeg, rawNewsFeedText);
				totalNegNewsFeedCount++;
			}
				
			if(probNeut>0.6){
				topNeutralNewsFeeds.put(probNeut, rawNewsFeedText);
				totalNeutNewsFeedCount++;
			}
						
		}
	}
	
	
	public static void main(String args[]) throws FileNotFoundException{
		
		Scanner sc = new Scanner(System.in);
		File configFile = new File("input.config");
		Scanner sc2 = new Scanner(configFile);
		String[][] keys = new String[7][2];
				
		int i=0;
		while(sc2.hasNextLine()&&i<7){
			keys[i++] = sc2.nextLine().split(" ");
		}
	
		SocialTextAnalysis soc = new SocialTextAnalysis();
		TweetAnalysis tw = soc.new TweetAnalysis(keys[0][1], keys[1][1], keys[2][1], keys[3][1]);
		tw.Authenticate();
		soc.PrepareSentimentScores();
		System.out.println("Enter keyword to search:");
		String keyword = sc.nextLine();
		System.out.println("Collecting tweets...");
		tw.ExtractTweets(keyword);
		
		System.out.println();
		System.out.println("Total tweets collected:"+tw.totalTweetCount);
		
		System.out.println();
		System.out.println("Overall % of positive tweets: "+ ((float)tw.totalPosTweetCount/(float)tw.totalTweetCount)*100.0);
		System.out.println("Overall % of Negative tweets: "+ ((float)tw.totalNegTweetCount/(float)tw.totalTweetCount)*100.0);
		System.out.println("Overall % of Neutral tweets: "+ ((float)tw.totalNeutTweetCount/(float)tw.totalTweetCount)*100.0);
		System.out.println();
		
		System.out.println("Top positive tweets");
		soc.printTopTen(tw.topPositiveTweets);
		
		
		System.out.println("Top Negative tweets");
		soc.printTopTen(tw.topNegativeTweets);
		
		
		System.out.println("Top Neutral tweets");
		soc.printTopTen(tw.topNeutralTweets);
				
		
		// from Facebook feed
		/*
		FacebookAnalysis fw = soc.new FacebookAnalysis(keys[4][1], keys[5][1], keys[6][1]);
		fw.Authenticate();
		System.out.println("Collecting facebook newsfeeds...");
		fw.ExtractNewsFeeds(keyword);
		
		System.out.println();
		System.out.println("Total newsfeeds collected:"+fw.totalNewsFeedCount);
		
		System.out.println();
		System.out.println("Overall % of positive newsfeeds: "+ ((float)fw.totalPosNewsFeedCount/(float)fw.totalNewsFeedCount)*100.0);
		System.out.println("Overall % of Negative newsfeeds: "+ ((float)fw.totalNegNewsFeedCount/(float)fw.totalNewsFeedCount)*100.0);
		System.out.println("Overall % of Neutral newsfeeds: "+ ((float)fw.totalNeutNewsFeedCount/(float)fw.totalNewsFeedCount)*100.0);
		System.out.println();
		
		System.out.println("Top positive newsfeeds");
		soc.printTopTen(fw.topPositiveNewsFeeds);
		
		
		System.out.println("Top Negative newsfeeds");
		soc.printTopTen(fw.topNegativeNewsFeeds);
		
		
		System.out.println("Top Neutral newsfeeds");
		soc.printTopTen(fw.topNeutralNewsFeeds);
		
		*/
		sc.close();
		sc2.close();
	}
}
