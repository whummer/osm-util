package io.hummer.osm.query;

import io.hummer.osm.model.Point;

import java.util.Map;

/**
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
public class OSMNode extends OSMElement {

	double lat;
	double lon;
	String version;

	public OSMNode(String id, double lat, double lon,
			String version, Map<String, String> tags) {
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		this.version = version;
		this.tags.putAll(tags);
	}

	public Point toPoint() {
		return new Point(lon, lat);
	}

	@Override
	public String toString() {
		return "OSMNode [" +
				"id=" + id + ", " +
				"lat=" + lat + ", " +
				"lon=" + lon +
				(tags.isEmpty() ? "" : ", tags=" + tags) +
			"]";
	}

	public boolean isSameLocationAs(OSMNode n) {
		return lat == n.lat && lon == n.lon;
	}

}
