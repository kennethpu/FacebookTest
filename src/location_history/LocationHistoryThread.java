package location_history;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.fb.common.SearchThread;
import com.restfb.Connection;
import com.restfb.FacebookClient;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.restfb.batch.BatchRequest.BatchRequestBuilder;
import com.restfb.types.Checkin;
import com.restfb.types.Photo;
import com.restfb.types.User;

public class LocationHistoryThread extends SearchThread{

	public LocationHistoryThread(User user, FacebookClient facebookClient) {
		this.user = user;
		this.facebookClient = facebookClient;
	}
	
	public List<BatchRequest> buildBatchRequest() throws Exception {
		// Initialize data structure for storing BatchRequests
		List<BatchRequest> batchRequests = new ArrayList<BatchRequest>();
		
		// Get friend's checkins
		BatchRequest locationRequest = new BatchRequestBuilder(user.getId()+"/locations").build();
		batchRequests.add(locationRequest);
		
		// Get friend's photos
		BatchRequest photoRequest = new BatchRequestBuilder(user.getId()+"/photos").build();
		batchRequests.add(photoRequest);
		
		return batchRequests;
	}
	
	public void processBatchResponse(List<BatchResponse> batchResponses) {
		Connection<Checkin> checkins = new Connection<Checkin>(facebookClient, batchResponses.get(0).getBody(), Checkin.class);
		Connection<Photo> photos = new Connection<Photo>(facebookClient, batchResponses.get(1).getBody(), Photo.class);
		
		// If friend has no relevant data (no hometown, current location,
		// checkins, or photos) skip
		if (checkins.getData().size() == 0 && user.getLocation() == null && user.getHometown() == null && photos.getData().size() == 0)
			return;
		
		// Initialize sorted data structure for storing friend's location
		// data
		TreeSet<LocationHistoryEntry> treeSet = new TreeSet(new LocationHistoryEntry());

		// Print friend's current location or 'unknown' if not found
		String curLocation = (user.getLocation() != null) ? user.getLocation().getName() : "unknown";
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
		String hometown = (user.getHometown() != null) ? user.getHometown().getName() : "unknown";
		System.out.printf("  [Hometown] %s\n", hometown);
		System.out.println();
	}
	
	public void run() {
		// Build batch request
		if (verbose) {
			System.out.printf("[LocationHistory] Building batch request for User: %s...\n", user.getName());
		}
		try {
			batchList = buildBatchRequest();
		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
			return;
		}
		if (verbose) {
			System.out.printf("[LocationHistory] Building batch request for User: %s...DONE\n", user.getName());
		}
		
		if (verbose) {
			System.out.printf("[LocationHistory] Executing batch request for User: %s...\n", user.getName());
		}
		// Execute batch request
		List<BatchResponse> batchResponses =
				  facebookClient.executeBatch((BatchRequest[]) batchList.toArray(new BatchRequest[0]));
		if (verbose) {
			System.out.printf("[LocationHistory] Executing batch request for User: %s...DONE\n", user.getName());
		}
		
		if (verbose) {
			System.out.printf("[LocationHistory] Processing batch response for User: %s...\n", user.getName());
		}
		// Process batch responses
		processBatchResponse(batchResponses);
		if (verbose) {
			System.out.printf("[LocationHistory] Processing batch response for User: %s...DONE\n", user.getName());
		}
	}
}
