package io.hummer.osm.query;

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
	
	public List<OSMElement> getElements() {
		return elements;
	}
}
