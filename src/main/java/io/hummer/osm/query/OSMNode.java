package io.hummer.osm.query;

import io.hummer.osm.model.Point;

import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
@XmlRootElement(name="node")
public class OSMNode extends OSMElement {

	@XmlAttribute
	double lat;
	@XmlAttribute
	double lon;
	@XmlAttribute
	String version;

	public OSMNode() {}

	public OSMNode(String id, double lat, double lon,
			String version, Map<String, String> tags) {
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		this.version = version;
		this.addTags(tags);
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(lat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lon);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		OSMNode other = (OSMNode) obj;
		if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
			return false;
		if (Double.doubleToLongBits(lon) != Double.doubleToLongBits(other.lon))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	
}
