package com.fb;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.fb.location_history.LocationHistoryEntry;
import com.fb.location_history.LocationHistorySet;
import com.fb.location_history.LocationHistorySetEntry;
import com.fb.location_history.LocationHistoryThread;
import com.restfb.Connection;
import com.restfb.DefaultJsonMapper;
import com.restfb.FacebookClient;
import com.restfb.JsonMapper;
import com.restfb.Parameter;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchRequest.BatchRequestBuilder;
import com.restfb.batch.BatchResponse;
import com.restfb.types.StatusMessage;
import com.restfb.types.User;

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

	/**
	 * Queries user for a list of Facebook users to perform operations on,
	 * then returns a list of their corresponding Facebook IDs
	 */
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
	 * @throws UnsupportedEncodingException 
	 * @throws FileNotFoundException 
	 */
	public void getFriendLocationHistory() throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter("test.html", "UTF-8");
		
		System.out.println("#===========================================================");
		System.out.println("# getFriendLocationHistory()");
		System.out.println("#===========================================================");
		
		// Get list user ids to perform query for
		List<String> queryIDs = getQueryIDs();
		
		long start = System.currentTimeMillis();
		
		// Initialize data structure for storing BatchRequests
		List<BatchRequest> batchRequests = new ArrayList<BatchRequest>();
		
		// Build a batch request to retrieve relevant data for the users specified
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
		
		
		
		// Initialize data structure (synchronized + sorted) for containing the output
		LocationHistorySet<LocationHistorySetEntry> locHistSet = new LocationHistorySet<LocationHistorySetEntry>();
		SortedSet<LocationHistorySetEntry> outputSet = Collections.synchronizedSortedSet(locHistSet);

		// Initialize helper class to reconstruct restfb classes from batch response
		JsonMapper jsonMapper = new DefaultJsonMapper();
		
		// Initialize data structure to keep track of threads
		List<LocationHistoryThread> threads = new ArrayList<LocationHistoryThread>();

		// Process batch responses
		BatchResponse batchResponse;
		int batchSize = batchResponses.size();
		for (int i=0; i<batchSize; i++) {
			batchResponse = batchResponses.get(i);
			if (batchResponse.getCode() != 200) {
				System.out.println("ERROR: Batch request failed: " + batchResponse);
				continue;
			}
			// Reconstruct User from batch response
			User u = jsonMapper.toJavaObject(batchResponse.getBody(), User.class);
			
			// Spawm a new LocationHistoryThread to perform the processing steps for this user
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
		
		// Iterate through output set and print results to screen
		int i = 0;
		Iterator<LocationHistorySetEntry> entryItr = outputSet.iterator();
		while (entryItr.hasNext()) {
			i++;
			LocationHistorySetEntry entry = entryItr.next();
			
			// Print user name
			System.out.printf("[%-2d] %s\n", i, entry.getName());
			
			// Print current location or 'unknown' if not found
			if (entry.getCurLocation().equals("unknown")) {
				System.out.printf("                       [Current] %s\n", entry.getCurLocation());
			} else {
				System.out.printf("                       [Current] %-75s <%f, %f>\n", entry.getCurLocation(), entry.getCurLat(), entry.getCurLong());
			}
			
			// Iterate through visited locations and print them
			Iterator<LocationHistoryEntry> locationItr = entry.getTreeSet().iterator();
			LocationHistoryEntry lastLoc = null;
			while (locationItr.hasNext()) {
				LocationHistoryEntry l = locationItr.next();

				// If the current location is the same as the previous one and
				// the associated time-stamp is within 1 hour of the previous, skip
				if (lastLoc != null
						&& (l.getName().equals(lastLoc.getName()))
						&& ((lastLoc.getDate().getTime() - l.getDate().getTime()) < 30 * 60 * 1000))
					continue;
				lastLoc = l;
				String location = String.format("%s%s%s %s",  l.getName(), l.getCity(), l.getState(), l.getCountry());
				System.out.printf("  [%s] %-75s <%f, %f>\n", l.getTime(), location, l.getLatitude(), l.getLongitude());
			}

			// Print hometown or 'unknown' if not found
			if (entry.getHometown().equals("unknown")) {
				System.out.printf("                      [Hometown] %s\n", entry.getHometown());
			} else {

				System.out.printf("                      [Hometown] %-75s <%f, %f>\n", entry.getHometown(), entry.getHomeLat(), entry.getHomeLong());
			}
		}
		
		writer.printf(
		"<html>\n" +
		"  <head>\n" + 
		"    <title>Simple Map</title>\n" + 
		"    <meta name=\"viewport\" content=\"initial-scale=1.0, user-scalable=no\">\n" +
		"    <meta charset=\"utf-8\">\n" +
		"    <style>\n" +
		"      html, body, #map-canvas {\n" +
		"        height: 100%%;\n" + 
		"        margin: 0px;\n" +
		"        padding: 0p\n" +
		"      }\n" +
		"    </style>\n" +
		"    <script src=\"https://maps.googleapis.com/maps/api/js?v=3.exp&sensor=false\"></script>\n" +
		"    <script>\n\n" +
		"var map;\n" +
		"var infowindow;\n");
		int j = 1;
		for (LocationHistorySetEntry lhs_entry : outputSet) {
			writer.printf("var markers%d = new Array();\n", j++);
		}
		writer.printf(
		"function initialize() { \n" +
		"  var mapOptions = {\n" +
		"    center: new google.maps.LatLng(%f, %f),\n" +
		"    zoom: 8,\n" +
		"    mapTypeId: google.maps.MapTypeId.TERRAIN\n" +
		"  };\n" +

		"  map = new google.maps.Map(document.getElementById('map-canvas'),\n" +
		"      mapOptions);\n", outputSet.last().getCurLat(), outputSet.last().getCurLong());
		
		String[] colors = {"yellow", "red", "blue", "green", "orange", "pink", "purple"};
		int tail = 1;
		for (LocationHistorySetEntry lhs_entry : outputSet) {
			writer.printf(
			"  var marker%d_c = new google.maps.Marker({\n" +
			"    icon: 'http://maps.google.com/mapfiles/ms/icons/%s-dot.png',\n" +
			"    position: new google.maps.LatLng(%f, %f),\n" +
			"    map: map,\n" +
			"    title:\"[Current] %s\"\n" + 
			"  });\n" + 
			"  markers%d[0] = marker%d_c;\n", tail, colors[tail%colors.length], lhs_entry.getCurLat(), lhs_entry.getCurLong(), lhs_entry.getCurLocation(), tail, tail); 

			int tail2 = 1;
			LocationHistoryEntry lastLoc = null;
			for (LocationHistoryEntry lh_entry : lhs_entry.getTreeSet()) {
				if (lastLoc != null
						&& (lh_entry.getName().equals(lastLoc.getName()))
						&& ((lastLoc.getDate().getTime() - lh_entry.getDate().getTime()) < 30 * 60 * 1000))
					continue;
				lastLoc = lh_entry;
				writer.printf(
				"  var marker%d_%d = new google.maps.Marker({\n" +
				"    icon: 'http://maps.google.com/mapfiles/ms/icons/%s.png',\n" +
				"    position:new google.maps.LatLng(%f, %f),\n" +
				"    map: map,\n" +
				"    title:\"[%s] %s%s%s %s\"\n" + 
				"  });\n" + 
				"  markers%d[%d] = marker%d_%d;\n", tail, tail2, colors[tail%colors.length], lh_entry.getLatitude(), lh_entry.getLongitude(), lh_entry.getTime(), 
				lh_entry.getName(), lh_entry.getCity(), lh_entry.getState(), lh_entry.getCountry(), tail, tail2, tail, tail2++); 
			}
	
			writer.printf(
			"  var marker%d_h = new google.maps.Marker({\n" +
			"    icon: 'http://maps.google.com/mapfiles/ms/icons/%s-dot.png',\n" +
			"    position: new google.maps.LatLng(%f, %f),\n" +
			"    map: map,\n" +
			"    title:\"[Hometown] %s\"\n" + 
			"  });\n" +
			"  markers%d[%d] = marker%d_h;\n", tail, colors[tail%colors.length], lhs_entry.getHomeLat(), lhs_entry.getHomeLong(), lhs_entry.getHometown(), tail, tail2, tail++); 
		}
		
		writer.printf(
		"  var count = markers1.length-1;\n" +
		"  infowindow = new google.maps.InfoWindow();\n" +
		"  infowindow.setContent(markers1[count].getTitle());\n" +
		"  map.panTo(markers1[count].getPosition());\n" +
		"  zoomIn(15, map.getZoom());\n" +
		"  markers1[count].setAnimation(google.maps.Animation.BOUNCE);\n" +
		"  infowindow.open(map, markers1[count]);\n" +
		"  google.maps.event.addListenerOnce(map, 'idle', function () {\n" +
		"    setTimeout(function(){autoPan(count)}, 3000);\n" +
		"  });\n" +
		"}\n\n" +
		"function autoPan(count) {\n" +
		"  var oldMarker = markers1[count];\n" + 
		"  var newMarker = markers1[((count + markers1.length - 1) %% markers1.length)];\n" +
		"  if (map.getBounds().contains(newMarker.getPosition())) {\n" +
		"    infowindow.close();\n" +
		"    infowindow.setContent(newMarker.getTitle());\n" +
		"    map.panTo(newMarker.getPosition());\n" +
		"    oldMarker.setAnimation(null);\n" +
		"    newMarker.setZIndex(google.maps.Marker.MAX_ZINDEX + 1);\n" +
		"    newMarker.setAnimation(google.maps.Animation.BOUNCE);\n" +
		"    infowindow.open(map, newMarker);\n" +
		"    setTimeout(function(){autoPan(((count + markers1.length - 1) %% markers1.length))}, 3000);\n" +
		"  } else {\n" +
		"    oldMarker.setAnimation(null);\n" +
		"    infowindow.close();\n" +
		"    zoomOut(newMarker, map.getZoom());\n" +
		"    google.maps.event.addListenerOnce(map, 'idle', function () {\n" +
		"      map.panTo(newMarker.getPosition());\n" +
		"      zoomIn(15, map.getZoom());\n" +
		"      google.maps.event.addListenerOnce(map, 'idle', function () {\n" +
		"        infowindow.setContent(newMarker.getTitle());\n" +
		"        newMarker.setZIndex(google.maps.Marker.MAX_ZINDEX + 1);\n" +
		"        newMarker.setAnimation(google.maps.Animation.BOUNCE);\n" +
		"        infowindow.open(map, newMarker);\n" +
		"        setTimeout(function(){autoPan(((count + markers1.length - 1) %% markers1.length))}, 3000);\n" +
		"      });\n" +
		"    });\n" +
		"  }\n" +
		"}\n\n" +
		"function zoomIn(target, cnt) {\n" +
		"  if (cnt >= target) {\n" +
		"    return;\n" +
		"  } else {\n" +
		"    google.maps.event.addListenerOnce(map, 'zoom_changed', function(){\n" +
		"      zoomIn(target, cnt + 1);\n" +
		"    });\n" +
		"    setTimeout(function(){map.setZoom(cnt)}, 80);\n" +
		"  }\n" +
		"}\n" +  
		"function zoomOut(newMarker, cnt) {\n" +
		"  if (map.getBounds().contains(newMarker.getPosition())) {\n" +
		"    return;\n" +
		"  } else {\n" +
		"    google.maps.event.addListenerOnce(map, 'zoom_changed', function(){\n" +
		"      zoomOut(newMarker, cnt - 1);\n" +
		"    });\n" +
		"    setTimeout(function(){map.setZoom(cnt)}, 80);\n" +
		"  }\n" + 
		"}\n" +  
		"google.maps.event.addDomListener(window, 'load', initialize);\n" +
		"    </script>\n" +
		"  </head>\n" +
		"  <body>\n" +
		"    <div id=\"map-canvas\"></div>\n" +
		"  </body>\n" +
		"</html>\n");
		writer.close();
		long end = System.currentTimeMillis();
		long duration = (end-start)/1000;
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

		System.out.println("#===========================================================");
		System.out.println("# getFriendStatuses()");
		System.out.println("#===========================================================");

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