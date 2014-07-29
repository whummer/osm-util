package io.hummer.osm.query;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
public class OSMWay extends OSMElement {
	final List<OSMNode> nodes = new ArrayList<OSMNode>();

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