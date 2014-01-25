package com.fb.location_history;

import java.util.TreeSet;

/** 
 * Class to represent a user and his/her corresponding location history
 */
public class LocationHistorySetEntry implements Comparable<LocationHistorySetEntry>{
	/*
	 * Instance Variables
	 */
	private String name;							// User name
	private String current;							// User current location
	private float current_lat;						// User current latitude
	private float current_long;						// User current longitude
	private String hometown;						// User hometown
	private float hometown_lat;						// User hometown latitude
	private float hometown_long;					// User hometown longitude
	private TreeSet<LocationHistoryEntry> treeSet;	// Set of locations user has been to
	
	/*
	 * Constructors
	 */
	public LocationHistorySetEntry() {}
	public LocationHistorySetEntry(String name, String current, float curLat, float curLong, String hometown, float homeLat, float homeLong, TreeSet<LocationHistoryEntry> treeSet) {
		this.name = name;
		this.current = current;
		this.hometown = hometown;
		this.current_lat = curLat;
		this.current_long = curLong;
		this.hometown_lat = homeLat;
		this.hometown_long = homeLong;
		this.treeSet = treeSet;
	}
	
	/*
	 *	Comparison Definition 
	 */
	public int compareTo(LocationHistorySetEntry arg) {
		// LocationHistorySetEntries are sorted based on the number of locations a user has been to (more places -> 'greater')
		if (arg.getTreeSet().size() < this.treeSet.size()) {
			return 1;
		} else {
			return -1;
		}
	}
	
	/*
	 * Getter Methods
	 */
	public String getName() {
		return name;
	}
	public String getCurLocation() {
		return current;
	}
	public float getCurLat() {
		return current_lat;
	}
	public float getCurLong() {
		return current_long;
	}
	public String getHometown() {
		return hometown;
	}
	public float getHomeLat() {
		return hometown_lat;
	}
	public float getHomeLong() {
		return hometown_long;
	}
	public TreeSet<LocationHistoryEntry> getTreeSet() {
		return treeSet;
	}
}
