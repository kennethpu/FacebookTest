import java.util.Date;
import com.restfb.types.*;

public class LocationEntry {
	Date date;
	Place place;
	String name;
	String city;
	String state;
	String country;
	String time;
	
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
}
