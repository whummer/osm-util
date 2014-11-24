package io.hummer.osm.query;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
@XmlRootElement(name="way")
public class OSMWay extends OSMElement {

	@XmlElement(name="nd")
	List<OSMNode> nodes = new ArrayList<OSMNode>();

	public List<OSMNode> getNodes() {
		return nodes;
	}

	@Override
	public String toString() {
		return "OSMWay[" + nodes.size() + "] [" +
				(nodes.isEmpty() ? "" : "nodes=" + nodes + ", ") +
				(tags.isEmpty() ? "" : "tags=" + tags + " ") +
				"]";
	}

}