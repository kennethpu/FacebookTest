package location_history;

import java.util.Comparator;
import java.util.TreeSet;

import com.fb.common.SearchSetEntry;

public class LocationHistorySetEntry implements Comparable<LocationHistorySetEntry>{
	private String name;
	private String current;
	private String hometown;
	private TreeSet<LocationHistoryEntry> treeSet;
	
	public LocationHistorySetEntry() {}
	public LocationHistorySetEntry(String name, String current, String hometown, TreeSet<LocationHistoryEntry> treeSet) {
		this.name = name;
		this.current = current;
		this.hometown = hometown;
		this.treeSet = treeSet;
	}
	
	public int compareTo(LocationHistorySetEntry arg) {
		if (arg.getTreeSet().size() < this.treeSet.size()) {
			return 1;
		} else {
			return -1;
		}
	}
	
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
