import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Parameter;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchRequest.BatchRequestBuilder;
import com.restfb.batch.BatchResponse;
import com.restfb.types.*;

public class FacebookSearch {

	private final String MY_APP_ID = "597537903622855";
	private final String MY_APP_SECRET = "5819eb4347788ca91fcfcc067fb6d37e";
	//private final AccessToken accessToken = new DefaultFacebookClient().obtainAppAccessToken(MY_APP_ID, MY_APP_SECRET);
	private final String MY_AUTH_TOKEN = "CAACEdEose0cBANZAYtESqf6K3ZCTPWgmbxJ3ZBlZCn9jqx9KO2o5SW01kuysfUbFQFW91RMyUUPV6A1zvenAZBgvskGot2t54qltXZBBlawVgrAGNFLeDVRkUgOcvHhEG3AtZBEZA8QWgJA7Cda8ZAl6yF2XlANZCz0fMFDZBSwA7ewXZAhZB1CwNAcv0XRpvtLr4bkqxojj1ss2X7wZDZD";
	private int numFriends;
	private boolean verbose = false;
	private double totOutputs;
	private int outputCount;
	private FacebookClient facebookClient;


	public FacebookSearch() {
		facebookClient = new DefaultFacebookClient(MY_AUTH_TOKEN);
		//facebookClient = new DefaultFacebookClient(accessToken.getAccessToken());
	}

	private void testSingleObjectFetch() {
		System.out.println("#=================================================");
		System.out.println("# Testing single object fetch...");
		System.out.println("#=================================================");
		
		User user = facebookClient.fetchObject("me", User.class);
		Page page = facebookClient.fetchObject("cocacola", Page.class);
		
		System.out.println("User name: " + user.getName());
		System.out.println("Page likes: " + page.getLikes());
	}
	
	private void testMultipleObjectsFetch() {
		System.out.println("#=================================================");
		System.out.println("# Testing multiple objects fetch...");
		System.out.println("#=================================================");
		
		FetchObjectsResults fetchObjectsResults = facebookClient.fetchObjects(Arrays.asList("me", "cocacola"), FetchObjectsResults.class);
		System.out.println("User name: " + fetchObjectsResults.me.getName());
		System.out.println("Page likes: " + fetchObjectsResults.page.getLikes());
	}
	
	private void testConnectionsFetch() {
		System.out.println("#=================================================");
		System.out.println("# Testing connections fetch...");
		System.out.println("#=================================================");
		
		Connection<User> myFriends = facebookClient.fetchConnection("me/friends", User.class);
		Connection<Post> myFeed = facebookClient.fetchConnection("me/feed", Post.class);
	
		System.out.println("Count of my friends: " + myFriends.getData().size());
		System.out.println("First item in my feed: " + myFeed.getData().get(0));
		
		for (List<Post> myFeedConnectionPage : myFeed) {
			for (Post post : myFeedConnectionPage) {
				System.out.println("Post: " + post);
			}
		}
	}
	
	private void testSearch() {
		System.out.println("#=================================================");
		System.out.println("# Testing search...");
		System.out.println("#=================================================");
		
		Connection<Post> publicSearch = facebookClient.fetchConnection("search", Post.class,
				Parameter.with("q", "zynga"), Parameter.with("type", "post"));
		Connection<User> targetedSearch = facebookClient.fetchConnection("me/home", User.class,
				Parameter.with("q", "Snow"), Parameter.with("type", "user"));
	
		//System.out.println("Public search for 'zynga', first ten search results...");
		//for (int i=0; i<10; i++) {
		//	System.out.println("  - " + publicSearch.getData().get(i).getMessage());	
		//}
		System.out.println("Posts on my wall by friends named Snow: " + targetedSearch.getData().size());
		for (List<User> targetedSearchConnectionPage : targetedSearch) {
			for (User user : targetedSearchConnectionPage) {
				System.out.println("  - " + user.getName());	
			}
		}
	}
	
	private void testInsightsFetch() {
		System.out.println("#=================================================");
		System.out.println("# Testing insights fetch...");
		System.out.println("#=================================================");
		
		Connection<Insight> insights = facebookClient.fetchConnection("2439131959/insights", Insight.class);
		for (Insight insight : insights.getData()) {
			System.out.println(insight.getName());
		}
	}
	
	private void search(String searchText, int numResults) {
		Connection<Post> publicSearch = facebookClient.fetchConnection("search", Post.class,
				Parameter.with("q", searchText), Parameter.with("type", "post"));
		System.out.println("Public search for '" + searchText + "', first " + numResults + " search results...");
		for (int i=0; i<numResults; i++) {
			System.out.println("["+(i+1)+"] - " + publicSearch.getData().get(i).getMessage());	
		}
	}

	private void mostMutualFriends() {
		long start = System.currentTimeMillis();
		Connection<User> myFriends = facebookClient.fetchConnection("me/friends", User.class);
		TreeSet<Friend> treeSet = new TreeSet<Friend>(new Comparator<Friend>(){
			public int compare(Friend a, Friend b) {
				return (b.mutualFriends >= a.mutualFriends) ? 1 : -1;
			}
		}); 
		
		SortedSet<Friend> topFriends = Collections.synchronizedSortedSet(treeSet);
		
		numFriends = myFriends.getData().size();
		if (!verbose) {
			totOutputs = Math.ceil(numFriends/50.0)*6;
			outputCount = 0;
		}
		System.out.println("Friend Count: " + numFriends);
		
		//List<BatchRequest> batchList;
		List<BatchExecutor> threads = new ArrayList<BatchExecutor>();
		for (int i=0; i<numFriends; i+=50) {
			BatchExecutor b = new BatchExecutor(myFriends.getData(), i, topFriends);
			b.start();
			threads.add(b);
			/**int endIndex = (numFriends-i > 50) ? i+50 : numFriends-1;
			try {
				batchList = buildUserBatchRequest(myFriends.getData().subList(i,endIndex));
			} catch (Exception e) {
				System.out.println("ERROR: " + e.getMessage());
				return;
			}
			System.out.println("Executing batch request...");
			List<BatchResponse> batchResponses =
					  facebookClient.executeBatch((BatchRequest[]) batchList.toArray(new BatchRequest[0]));
			System.out.println("Executing batch request...DONE");

			processUserBatchResponse(batchResponses, myFriends.getData().subList(i,endIndex), topFriends);
			*/
		}
		
		for (BatchExecutor b : threads) {
			try {
				b.join();
			} catch (InterruptedException e) {}
		}
		
		/**int i = 1;
		for (User friend : myFriends.getData()) {
			id = friend.getId();
			Connection<User> mutualFriends = facebookClient.fetchConnection("me/mutualfriends/"+id, User.class);
			//System.out.printf("%-30s Mutual Friends: %s\n", friend.getName(), mutualFriends.getData().size());
			topFriends.add(new Friend(friend.getName(), mutualFriends.getData().size()));
			System.out.printf("Going through friend list...[%03d/%03d(%05.2f%%)]\n", i, numFriends, (i)*100.0/numFriends);
			i++;
		}*/
		
		outputSortedSet(topFriends);
		
		long end = System.currentTimeMillis();
		long duration = (end-start)/1000;
		System.out.printf("Duration: %ds\n", duration);
	}
	
	private List<BatchRequest> buildUserBatchRequest(List<User> users, int start, int end) throws Exception {
		if (verbose) {
			System.out.printf("Building batch request[%03d-%03d]...\n", start, end);
		} else {
			synchronized(this) {
				System.out.printf("Executing...[%05.2f%%]\n", ++outputCount*100.0/totOutputs);
			}
		}
		
		List<BatchRequest> batchRequests = new ArrayList<BatchRequest>();
		int batchSize = users.size();
		if (batchSize > 50) {
			throw new Exception("Too many requests, maximum batch size is 50.");
		}
		for (int i = 0; i < batchSize; i++) {
			//System.out.printf("Building batch request...[%02d/%02d(%05.2f%%)]\n", i+1, batchSize, (i+1)*100.0/batchSize);
			BatchRequest request = new BatchRequestBuilder("me/mutualfriends/"+users.get(i).getId()).build();
			batchRequests.add(request);
		}
		if (verbose) {
			System.out.printf("Building batch request[%03d-%03d]...DONE\n", start, end);
		} else {
			synchronized(this) {
				System.out.printf("Executing...[%05.2f%%]\n", ++outputCount*100.0/totOutputs);
			}
		}
		return batchRequests;
	}
	
	private void processUserBatchResponse(List<BatchResponse> batchResponses, List<User> friends, SortedSet<Friend> friendSet, int start, int end) {
		if (verbose) {
			System.out.printf("Processing batch response[%03d-%03d]...\n", start, end);
		} else {
			synchronized(this) {
				System.out.printf("Executing...[%05.2f%%]\n", ++outputCount*100.0/totOutputs);
			}
		}
		BatchResponse fResponse;
		int batchSize = batchResponses.size();
		for (int i=0; i<batchSize; i++) {
			fResponse = batchResponses.get(i);
			if (fResponse.getCode() != 200) {
				System.out.println("ERROR: Batch request failed: " + fResponse);
				continue;
			}
			Connection<User> mutualFriends = 
					new Connection<User>(facebookClient, fResponse.getBody(), User.class);
			//System.out.printf("Adding friend to list...[%02d/%02d(%05.2f%%)]\n", i+1, batchSize, (i+1)*100.0/batchSize);
			friendSet.add(new Friend(friends.get(i).getName(), mutualFriends.getData().size()));
		}
		if (verbose) {
			System.out.printf("Processing batch response[%03d-%03d]...DONE\n", start, end);
		} else {
			synchronized(this) {
				System.out.printf("Executing...[%05.2f%%]\n", ++outputCount*100.0/totOutputs);
			}
		}	
	}
	
	private void outputSortedSet(SortedSet<Friend> friendSet) {
		System.out.println("#===========================================================");
		System.out.println("# Outputting sorted list...");
		System.out.println("#===========================================================");
		
		int i = 0;
		Iterator<Friend> friendItr = friendSet.iterator();
		int prevMF = 0;
		
		while(friendItr.hasNext()) {
			Friend f = friendItr.next();
			if (f.mutualFriends != prevMF) {
				i++;
				prevMF = f.mutualFriends;
			}
			System.out.printf("[%-3d] %-30s MutualFriends: %s\n", i, f.name, f.mutualFriends);
		}
	}
	
	class Friend{
		int mutualFriends;
		String name;
		
		Friend(String n, int mf) {
			mutualFriends = mf;
			name = n;
		}
	}
	
	class BatchExecutor extends Thread {
		List<User> users;
		int startIndex;
		SortedSet<Friend> topFriends;
		List<BatchRequest> batchList;
		public BatchExecutor(List<User> u, int s, SortedSet<Friend> tf) {
			users = u;
			startIndex = s;
			topFriends = tf;
		}
		public void run() {
			int endIndex = (numFriends-startIndex > 50) ? startIndex+50 : numFriends-1;
			try {
				batchList = buildUserBatchRequest(users.subList(startIndex,endIndex), startIndex, endIndex);
			} catch (Exception e) {
				System.out.println("ERROR: " + e.getMessage());
				return;
			}
			if (verbose) {
				System.out.printf("Executing batch request[%03d-%03d]...\n", startIndex, endIndex);
			} else {
				synchronized(this) {
					System.out.printf("Executing...[%05.2f%%]\n", ++outputCount*100.0/totOutputs);
				}
			}
			List<BatchResponse> batchResponses =
					  facebookClient.executeBatch((BatchRequest[]) batchList.toArray(new BatchRequest[0]));
			if (verbose) {
				System.out.printf("Executing batch request[%03d-%03d]...DONE\n", startIndex, endIndex);
			} else {
				synchronized(this) {
					System.out.printf("Executing...[%05.2f%%]\n", ++outputCount*100.0/totOutputs);
				}
			}
			processUserBatchResponse(batchResponses, users.subList(startIndex,endIndex), topFriends, startIndex, endIndex);			
		}
		
	}
	
	public void test() {
		//testSingleObjectFetch();
		//testMultipleObjectsFetch();
		//testConnectionsFetch();
		//testSearch();
		//testInsightsFetch();
		//search("haters", 10);
		mostMutualFriends();
		System.out.println("Hi Kenny");
	}
	
	public static void main(String[] args) {
		FacebookSearch fbSearch = new FacebookSearch();
		fbSearch.test();
	}
}