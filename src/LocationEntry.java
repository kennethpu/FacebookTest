import java.util.Comparator;
import java.util.Date;
import com.restfb.types.*;

public class LocationEntry implements Comparator {
	Date date;
	Place place;
	String name;
	String city;
	String state;
	String country;
	String time;
	
	LocationEntry(){}
	LocationEntry(Date d, Place p) {
		date = d;
		place = p;
		Location location = p.getLocation();
		name = p.getName();
		city = (location.getCity() != null && !location.getCity().equals("")) ? " " + location.getCity() : "";
		state = (location.getState() != null && !location.getState().equals("")) ? " " + location.getState() : "";
		//country = (location.getCountry() != null && !location.getCountry().equals("")) ? " " + location.getCountry() : "";
		country = location.getCountry();
		time = date.toString();
	}
	
//	@Override
//	public int compare(LocationEntry a, LocationEntry b) {
//		return (b.date.compareTo(a.date));
//	}

	@Override
	public int compare(Object arg0, Object arg1) {
		LocationEntry a = (LocationEntry) arg0;
		LocationEntry b = (LocationEntry) arg1;
		return (b.date.compareTo(a.date));
	}
	 
}
