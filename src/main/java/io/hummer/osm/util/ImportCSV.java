package io.hummer.osm.util;

import io.hummer.osm.query.OSMContainer;
import io.hummer.osm.query.OSMElement;
import io.hummer.osm.query.OSMNode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportCSV {

	public ImportCSV(String file) {
		
	}

	public static enum Column {
		ID, LATITUDE, LONGITUDE, NAME, VERSION
	}

	public static List<OSMNode> importNodes(
			String file, Map<Column,Integer> colMapping) {
		return importNodes(file, colMapping, null);
	}

	public static List<OSMNode> importNodes(
			String file, Map<Column,Integer> colMapping, Map<String,String> tags) {
		return importNodes(file, colMapping, tags, ",", false);
	}
	/**
	 * Import a CSV file.
	 * @param file the file to import
	 * @param colMapping 0-based column mapping
	 * @param separator the separator
	 * @return
	 */
	public static List<OSMNode> importNodes(
			String file, Map<Column,Integer> colMapping, 
			Map<String,String> tags,
			String separator, boolean skipHeaderLine) {
		List<OSMNode> result = new ArrayList<OSMNode>();
		int count = 0;
		int startLine = skipHeaderLine ? 1 : 0;
		for(String line: Util.readFileLines(file)) {
			if(count >= startLine) {
				String[] parts = line.split(separator);
				String id = colMapping.containsKey(Column.ID) ? 
						parts[colMapping.get(Column.ID)] : "" + (-count - 1);
				String version = colMapping.containsKey(Column.VERSION) ? 
						parts[colMapping.get(Column.VERSION)] : null;
				double lat = colMapping.containsKey(Column.LATITUDE) ? 
						Double.parseDouble(parts[colMapping.get(Column.LATITUDE)]) : 0;
				double lon = colMapping.containsKey(Column.LONGITUDE) ? 
						Double.parseDouble(parts[colMapping.get(Column.LONGITUDE)]) : 0;
				OSMNode n = new OSMNode(id, lat, lon, version, tags);
				result.add(n);
			}
			count++;
		}
		return result;
	}

	public static String renderXML(List<OSMNode> nodes) {
		OSMContainer c = new OSMContainer();
		for(OSMNode n : nodes) {
			c.getElements().add((OSMElement)n);
		}
		return Util.toString(c, true);
	}

	public static List<OSMNode> eliminateDuplicates(List<OSMNode> nodes) {
		List<OSMNode> result = new ArrayList<OSMNode>();
		for(OSMNode n1: nodes) {
			if(!result.contains(n1)) {
				result.add(n1);
			}
		}
		return result;
	}
	
	public static List<OSMNode> findDuplicates(List<OSMNode> nodes) {
		List<OSMNode> result = new ArrayList<OSMNode>();
		for(int i = 0; i < nodes.size(); i++) {
			OSMNode n1 = nodes.get(i);
			for(int j = i + 1; j < nodes.size(); j++) {
				OSMNode n2 = nodes.get(j);
				if(n1.equals(n2)) {
					result.add(n1);
				}
			}
		}
		return result;
	}

	public static void main(String[] args) {
		Map<Column,Integer> map = new HashMap<ImportCSV.Column, Integer>();
		map.put(Column.LATITUDE, 6);
		map.put(Column.LONGITUDE, 7);
		Map<String,String> tags = new HashMap<String, String>();
		tags.put("highway", "street_lamp");
		List<OSMNode> nodes = importNodes(
				"/tmp/ncc_Streetlights.csv", map, tags, ",", true);
		nodes = eliminateDuplicates(nodes);
		String xml = renderXML(nodes);
		//System.out.println(xml);
		Util.write(new File("/tmp/ncc_Streetlights.osm"), xml);
	}
}
