import com.restfb.Facebook;
import com.restfb.types.*;

public class FetchObjectsResults {
	@Facebook
	User me;

	@Facebook("cocacola")
	Page page;
}