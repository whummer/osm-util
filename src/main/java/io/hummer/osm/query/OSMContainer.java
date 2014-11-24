package io.hummer.osm.query;

import io.hummer.osm.util.Util;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
@XmlRootElement(name="osm")
@XmlSeeAlso({OSMNode.class, OSMWay.class})
public class OSMContainer {
	@XmlAttribute
	String version = "0.6";
	@XmlMixed
	List<OSMElement> elements = new LinkedList<OSMElement>();

	public OSMContainer() {}
	public OSMContainer(List<OSMElement> els) {
		this.elements.addAll(els);
	}

	public List<OSMElement> getElements() {
		return elements;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends OSMElement> List<T> getElements(Class<T> clazz) {
		List<T> result = new LinkedList<T>();
		for(OSMElement e : elements) {
			if(e.getClass().isAssignableFrom(clazz)) {
				result.add((T) e);
			}
		}
		return result;
	}

	public String toXML() {
		OSMContainer copy = new OSMContainer();
		copy.elements.addAll(getElements(OSMNode.class));
		for(OSMWay w : getElements(OSMWay.class)) {
			OSMWay wayCopy = new OSMWay();
			wayCopy.id = w.id;
			wayCopy.tags = w.tags;
			copy.elements.add(wayCopy);
			for(OSMNode n : w.nodes) {
				wayCopy.nodes.add(new OSMNode.OSMNodeRef(n.id));
			}
		}
		return Util.toString(copy, true);
	}
}
