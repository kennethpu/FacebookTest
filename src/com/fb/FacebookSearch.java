package com.fb;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import location_history.LocationHistoryEntry;
import location_history.LocationHistoryThread;
import location_history.LocationHistorySet;
import location_history.LocationHistorySetEntry;

import com.restfb.Connection;
import com.restfb.DefaultJsonMapper;
import com.restfb.FacebookClient;
import com.restfb.JsonMapper;
import com.restfb.Parameter;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.restfb.batch.BatchRequest.BatchRequestBuilder;
import com.restfb.types.Checkin;
import com.restfb.types.Photo;
import com.restfb.types.StatusMessage;
import com.restfb.types.User;

import com.fb.ExecuteSearch;

public class FacebookSearch {

	// restFB stuff to perform actual facebook queries
	private FacebookClient facebookClient;

	private BufferedReader br;

	// Number of user's facebook friends
	private int numFriends;

	// Variables for debug outputs
	// NOTE: by default, program outputs percentage toward completion.
	// Set verbose = true for more specific print statements
	private boolean verbose = false;
	private double totOutputs;
	private int outputCount;

	/**
	 * Default constructor
	 */
	public FacebookSearch(FacebookClient facebookClient) {
		this.facebookClient = facebookClient;
		this.br = new BufferedReader( new InputStreamReader(System.in));
	}

	public List<String> getQueryIDs() {
		boolean loop = true;
		List<String> ids = new LinkedList<String>();
		String name = "";
		boolean inputValid = false;

		while (loop) {
			inputValid = false;
			while(!inputValid) {
				name = "";
				System.out.print("First name and last name of query user (Hit <Enter> twice when done): "); 
				try {
					name = br.readLine();
				} catch ( Exception e) {}
				
				if (ExecuteSearch.friendsList.containsKey(name) && !ids.contains(ExecuteSearch.friendsList.get(name))) {
					inputValid = true;
					ids.add(ExecuteSearch.friendsList.get(name));
				} else if (name.equals("") && ids.size() > 0){
					inputValid = true;
					loop = false;
				} else {
					System.out.println("Invalid input!");
				}
			}
		}
		return ids;
	}
	
	//Work in progress, get a the locations a friend has been to
	public void getFriendLocationHistory(){
					
		System.out.println("#===========================================================");
		System.out.println("# getLocationHistory()");
		System.out.println("#===========================================================");
		List<String> queryIDs = getQueryIDs();
		
		long start = System.currentTimeMillis();
		
		// Initialize data structure for storing BatchRequests
		List<BatchRequest> batchRequests = new ArrayList<BatchRequest>();
		for (String id : queryIDs) {
			BatchRequest req = new BatchRequestBuilder(id).parameters(Parameter.with("fields", "name, id, hometown, location")).build();
			batchRequests.add(req);
			/*User u = facebookClient.fetchObject(id, User.class,
					Parameter.with("fields", "name, id, hometown, location"));
			LocationHistoryThread t = new LocationHistoryThread(u, facebookClient);
			t.start();
			threads.add(t);*/
		}
		
		// Execute batch request
		List<BatchResponse> batchResponses =
				  facebookClient.executeBatch((BatchRequest[]) batchRequests.toArray(new BatchRequest[0]));
		
		LocationHistorySet<LocationHistorySetEntry> locHistSet = new LocationHistorySet<LocationHistorySetEntry>();
		SortedSet<LocationHistorySetEntry> outputSet = Collections.synchronizedSortedSet(locHistSet);

		// Process batch responses
		List<LocationHistoryThread> threads = new ArrayList<LocationHistoryThread>();
		JsonMapper jsonMapper = new DefaultJsonMapper();
		BatchResponse batchResponse;
		int batchSize = batchResponses.size();
		for (int i=0; i<batchSize; i++) {
			batchResponse = batchResponses.get(i);
			if (batchResponse.getCode() != 200) {
				System.out.println("ERROR: Batch request failed: " + batchResponse);
				continue;
			}
			User u = jsonMapper.toJavaObject(batchResponse.getBody(), User.class);
			LocationHistoryThread t = new LocationHistoryThread(u, facebookClient, outputSet);
			t.start();
			threads.add(t);
		}
		
		// Wait for all batch request threads to finish executing
		for (LocationHistoryThread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {}
		}
		
		int i = 0;
		Iterator<LocationHistorySetEntry> entryItr = outputSet.iterator();

		while (entryItr.hasNext()) {
			i++;
			LocationHistorySetEntry entry = entryItr.next();
			System.out.printf("[%-2d] %s\n", i, entry.getName());
			System.out.printf("  [Current] %s\n", entry.getCurLocation());
			Iterator<LocationHistoryEntry> locationItr = entry.getTreeSet().iterator();
			LocationHistoryEntry lastLoc = null;
			while (locationItr.hasNext()) {
				LocationHistoryEntry l = locationItr.next();

				// If the current location is the same as the previous one and
				// the associated time-stamp is within 1 hour of the previous, skip
				if (lastLoc != null
						&& (l.name.equals(lastLoc.name))
						&& ((lastLoc.date.getTime() - l.date.getTime()) < 60 * 60 * 1000))
					continue;
				lastLoc = l;

				System.out.printf("  [%s] %s%s%s %s\n", l.time, l.name, l.city,
						l.state, l.country);
			}
			System.out.printf("  [Hometown] %s\n", entry.getHometown());
		}
		
		long end = System.currentTimeMillis();
		long duration = (end-start)/1000;
		System.out.printf("Duration: %ds\n", duration);
	}

	/**
	 * Outputs a list of facebook friends and their corresponding location
	 * history, including hometown and current location if available
	 * 
	 * Required Permissions: 
	 * - user_friends 
	 * - friends_status 
	 * - friends_photos 
	 * - friends_hometown 
	 * - friends_location
	 */
	public void getLocationHistory() {
		long start = System.currentTimeMillis();
		System.out.println("#===========================================================");
		System.out.println("# getLocationHistory()");
		System.out.println("#===========================================================");

		// Get friends data
		Connection<User> myFriends = facebookClient.fetchConnection(
				"me/friends", User.class,
				Parameter.with("fields", "name, id, hometown, location"));
		numFriends = myFriends.getData().size();

		System.out.println("Friend Count: " + numFriends);

		// Iterate through friends, outputting their location history
		int i = 0;
		for (User f : myFriends.getData()) {
			Connection<Checkin> checkins = facebookClient.fetchConnection(
					f.getId() + "/locations", Checkin.class);
			Connection<Photo> photos = facebookClient.fetchConnection(f.getId()
					+ "/photos", Photo.class);

			if (checkins.getData().size() == 0 && f.getLocation() == null
					&& f.getHometown() == null && photos.getData().size() == 0)
				continue;

			// Output friend name
			i++;
			System.out.printf("[%-3d] %-30s\n", i, f.getName());

			// Initialize sorted data structure for storing friend's location data
			TreeSet<LocationHistoryEntry> treeSet = new TreeSet<LocationHistoryEntry>();

			// Print friend's current location or 'unknown' if not found
			String curLocation = (f.getLocation() != null) ? f.getLocation()
					.getName() : "unknown";
			System.out.printf("  [Current] %s\n", curLocation);

			// List of previously accessed Checkin lists (to address an issue
			// where fetch would occasionally loop infinitely through the lists
			List<List<Checkin>> prev_checkins_list = new ArrayList<List<Checkin>>();

			// Iterate through friend's checkins and add valid locations to set
			for (List<Checkin> checkins_list : checkins) {
				// If accessing a Checkin list that has previously been checked,
				// skip
				if (prev_checkins_list.contains(checkins_list))
					break;
				prev_checkins_list.add(checkins_list);

				for (Checkin checkin : checkins_list) {
					// If Checkin's corresponding place, location, or country is
					// null, skip
					if (checkin.getPlace() == null
							|| checkin.getPlace().getLocation() == null
							|| checkin.getPlace().getLocation().getCountry() == null)
						continue;

					treeSet.add(new LocationHistoryEntry(checkin.getCreatedTime(),
							checkin.getPlace()));
				}
			}

			// List of previously accessed Photo lists (to address an issue
			// where fetch would occasionally loop infinitely through the lists
			List<List<Photo>> prev_photos_list = new ArrayList<List<Photo>>();

			// Iterate through friend's photos and add valid locations to set
			for (List<Photo> photos_list : photos) {
				// If accessing a Photo list that has previously been checked,
				// skip
				if (prev_checkins_list.contains(photos_list))
					break;
				prev_photos_list.add(photos_list);

				for (Photo photo : photos_list) {
					// If Photo's corresponding place, location, or country is
					// null, skip
					if (photo.getPlace() == null
							|| photo.getPlace().getLocation() == null
							|| photo.getPlace().getLocation().getCountry() == null)
						continue;

					treeSet.add(new LocationHistoryEntry(photo.getCreatedTime(), photo
							.getPlace()));
				}
			}

			// Iterate through location set, outputting valid locations
			Iterator<LocationHistoryEntry> locationItr = treeSet.iterator();
			LocationHistoryEntry lastLoc = null;
			while (locationItr.hasNext()) {
				LocationHistoryEntry l = locationItr.next();

				// If the current location is the same as the previous one and
				// the
				// associated time-stamp is within 1 hour of the previous, skip
				if (lastLoc != null
						&& (l.name.equals(lastLoc.name))
						&& ((lastLoc.date.getTime() - l.date.getTime()) < 60 * 60 * 1000))
					continue;
				lastLoc = l;

				System.out.printf("  [%s] %s%s%s %s\n", l.time, l.name, l.city,
						l.state, l.country);
			}

			// Print friend's hometown or 'unknown' if not found
			String hometown = (f.getHometown() != null) ? f.getHometown()
					.getName() : "unknown";
			System.out.printf("  [Hometown] %s\n", hometown);
			System.out.println();
		}

		long end = System.currentTimeMillis();
		long duration = (end - start) / 1000;
		System.out.printf("Duration: %ds\n", duration);
	}

	/**
	 * Outputs a list of facebook friends and their most recent facebook
	 * statuses, excluding null statuses, links, and statuses with more than two
	 * lines
	 * 
	 * Parameters: - num_statuses : number of statuses to show per user
	 * 
	 * Required Permissions: - user_friends - friends_status - read_stream
	 */
	public void getFriendStatuses(int num_statuses) {
		long start = System.currentTimeMillis();

		System.out
				.println("#===========================================================");
		System.out.println("# getFriendStatuses()");
		System.out
				.println("#===========================================================");

		// if num_statuses <= 0, output ALL statuses
		if (num_statuses <= 0)
			num_statuses = Integer.MAX_VALUE;

		// Get friends data
		Connection<User> myFriends = facebookClient.fetchConnection(
				"me/friends", User.class);
		numFriends = myFriends.getData().size();

		// Print statements for debugging purposes
		System.out.println("Friend Count: " + numFriends);

		// Iterate through friends, outputting their facebook statuses
		int i = 0;
		int j;
		for (User f : myFriends.getData()) {
			// Get friends statuses
			Connection<StatusMessage> statuses = facebookClient
					.fetchConnection(f.getId() + "/statuses",
							StatusMessage.class);

			// If status data is empty, skip friend
			if (statuses.getData().size() == 0)
				continue;

			// Output up to <num_statuses> statuses for current friend
			i++;
			System.out.printf("[%-3d] %-30s\n", i, f.getName());
			j = 1;
			for (List<StatusMessage> status_list : statuses) {
				for (StatusMessage status : status_list) {
					String message = status.getMessage();
					// Skip status message if:
					// 1) message is null
					// 2) message contains a link (http)
					// 3) message contains more than 2 lines
					if ((message == null) || (message.indexOf("http") != -1)
							|| (message.split("\r\n|\r|\n").length > 2)) {
						continue;
					}
					System.out.println("Status: " + message);
					j++;

					// Break out of loop if the number of statuses printed
					// is greater than num_statuses
					if (j > num_statuses)
						break;
				}
				// Break out of loop if the number of statuses printed
				// is greater than num_statuses
				if (j > num_statuses)
					break;
			}
			System.out.println();
		}

		long end = System.currentTimeMillis();
		long duration = (end - start) / 1000;
		System.out.printf("Duration: %ds\n", duration);
	}

	/**
	 * Outputs a list of facebook friends and their associated educational
	 * history
	 * 
	 * Required Permissions: - user_friends - friends_education_history
	 */
	public void getFriendEducation() {
		long start = System.currentTimeMillis();

		System.out
				.println("#===========================================================");
		System.out.println("# getFriendEducation()");
		System.out
				.println("#===========================================================");

		// Get friends data
		Connection<User> myFriends = facebookClient.fetchConnection(
				"me/friends", User.class,
				Parameter.with("fields", "name, education"));
		numFriends = myFriends.getData().size();

		// Print statements for debugging purposes
		System.out.println("Friend Count: " + numFriends);

		int i = 0;
		for (User f : myFriends.getData()) {
			i++;
			System.out.printf("[%-3d] %-30s\n", i, f.getName());
			for (User.Education edu : f.getEducation()) {
				System.out.printf("  - [%15s] %-40s\n", edu.getType(), edu
						.getSchool().getName());
			}
			System.out.println();
		}

		long end = System.currentTimeMillis();
		long duration = (end - start) / 1000;
		System.out.printf("Duration: %ds\n", duration);
	}

	/**
	 * Outputs a list of facebook friends sorted by number of mutual friends
	 * 
	 * Parameters: - maxn : number of ranks to show (not the number of results,
	 * as tied results collectively count as one rank) NOTE: changing maxn has
	 * very little effect on execution time, as the program must still grab data
	 * for ALL friends
	 * 
	 * Required Permissions: - user_friends
	 */
	public void mostMutualFriends(int maxn) {
		long start = System.currentTimeMillis();

		System.out
				.println("#===========================================================");
		System.out.printf("# mostMutualFriends() : %s\n", (maxn <= 0) ? "ALL"
				: "TOP " + maxn);
		System.out
				.println("#===========================================================");

		// Get friends data
		Connection<User> myFriends = getFriends();
		numFriends = myFriends.getData().size();

		// Initialize data structure (synchronized + sorted) for storing
		// Friends
		TreeSet<Friend> treeSet = new TreeSet<Friend>(new Comparator<Friend>() {
			public int compare(Friend a, Friend b) {
				return (b.mutualFriends >= a.mutualFriends) ? 1 : -1;
			}
		});
		SortedSet<Friend> topFriends = Collections
				.synchronizedSortedSet(treeSet);

		// Print statements for debugging purposes
		if (!verbose) {
			totOutputs = Math.ceil(numFriends / 50.0) * 2;
			outputCount = 0;
		}
		System.out.println("Friend Count: " + numFriends);

		// Spawn the required number of threads to execute all batch requests
		// in parallel
		List<BatchExecutor> threads = new ArrayList<BatchExecutor>();
		for (int i = 0; i < numFriends; i += 50) {
			BatchExecutor b = new BatchExecutor(myFriends.getData(), i,
					topFriends, numFriends, facebookClient);
			b.start();
			threads.add(b);
			if (!verbose)
				System.out.printf("Executing...[%05.2f%%]\n", ++outputCount
						* 100.0 / totOutputs);
		}

		// Wait for all batch request threads to finish executing
		for (BatchExecutor b : threads) {
			try {
				b.join();
			} catch (InterruptedException e) {
			}
			if (!verbose)
				System.out.printf("Executing...[%05.2f%%]\n", ++outputCount
						* 100.0 / totOutputs);
		}

		// Output sorted list
		outputSortedSet(topFriends, maxn);

		long end = System.currentTimeMillis();
		long duration = (end - start) / 1000;
		System.out.printf("Duration: %ds\n", duration);
	}

	/**
	 * Get facebook friend list
	 * 
	 * Returns: - Connection<User> of friends
	 */
	private Connection<User> getFriends() {
		return facebookClient.fetchConnection("me/friends", User.class);
	}

	/**
	 * Builds a batch request from a given list of facebook friends
	 * 
	 * Parameters: - friendSet : set containing friend entries sorted by number
	 * of mutual friends - maxn : number of ranks to show (not the number of
	 * results, as tied results collectively count as one rank)
	 */
	public void outputSortedSet(SortedSet<Friend> friendSet, int maxn) {
		System.out
				.println("#===========================================================");
		String msg = (maxn <= 0) ? "# Outputting full sorted list..."
				: ("# Outputting top " + maxn + " results...");
		System.out.println(msg);
		System.out
				.println("#===========================================================");

		// if maxn <= 0, output ALL results
		if (maxn <= 0)
			maxn = Integer.MAX_VALUE;

		// Iterate through set, outputting entries until either displayed
		// ranks has reached maxn or there are no more entries to display
		int i = 0;
		Iterator<Friend> friendItr = friendSet.iterator();
		int prevMF = -1;
		while (friendItr.hasNext()) {
			Friend f = friendItr.next();
			if (f.mutualFriends != prevMF) {
				i++;
				prevMF = f.mutualFriends;
			}

			if (i > maxn)
				break;

			System.out.printf("[%-3d] %-30s MutualFriends: %s\n", i, f.name,
					f.mutualFriends);
		}
	}
}