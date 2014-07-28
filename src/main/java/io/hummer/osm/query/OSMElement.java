package io.hummer.osm.query;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
public class OSMElement {
	Map<String, String> tags = new HashMap<String, String>();
	String id;
	
	public Map<String, String> getTags() {
		return tags;
	}
	public String getId() {
		return id;
	}
}
