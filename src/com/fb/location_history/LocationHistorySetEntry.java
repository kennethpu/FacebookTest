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
	private String hometown;						// User hometown
	private TreeSet<LocationHistoryEntry> treeSet;	// Set of locations user has been to
	
	/*
	 * Constructors
	 */
	public LocationHistorySetEntry() {}
	public LocationHistorySetEntry(String name, String current, String hometown, TreeSet<LocationHistoryEntry> treeSet) {
		this.name = name;
		this.current = current;
		this.hometown = hometown;
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
	public String getHometown() {
		return hometown;
	}
	public TreeSet<LocationHistoryEntry> getTreeSet() {
		return treeSet;
	}
}
