package io.hummer.osm.query;

import io.hummer.osm.Constants;
import io.hummer.osm.model.Line;
import io.hummer.osm.model.Tile;
import io.hummer.osm.model.TiledMapAbstract;
import io.hummer.osm.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
public class OSMCachedQuerier {

	private final double maxVicinity = 0.02;
	private int requestCount;
	private TiledMapAbstract<List<OSMElement>> cache = new TiledMapOSM();

	static final String OVERPASS_API = "http://www.overpass-api.de/api/";
	static final String OPENSTREETMAP_API_06 = "http://www.openstreetmap.org/api/0.6/";

	public OSMCachedQuerier() {
		this(true);
	}
	public OSMCachedQuerier(boolean loadLazily) {
		readCacheFiles(loadLazily);
	}

	private void readCacheFiles(boolean loadLazily) {
		String filePattern = ".*bbox_(.+),(.+),(.+),(.+)\\.gz";
		@SuppressWarnings("unchecked")
		Collection<File> files = FileUtils.listFiles(
				new File(Constants.TMP_DIR), new String[]{"gz"}, false);
		int count = 0;
		for(File f : files) {
			String fName = f.getName();
			if(fName.matches(filePattern)) {
				Tile t = new Tile();
				t.left = Double.parseDouble(fName.replaceAll(filePattern, "$1"));
				t.bottom = Double.parseDouble(fName.replaceAll(filePattern, "$2"));
				t.right = Double.parseDouble(fName.replaceAll(filePattern, "$3"));
				t.top = Double.parseDouble(fName.replaceAll(filePattern, "$4"));
				cache.put(t, null);
				count ++;
			}
		}
		if(count > 0) {
			System.out.println("Read " + count + " existing OpenStreetMap " +
					"files from local cache. Cache size: " + cache.size());
		}
	}

	public List<OSMElement> getElements(double lat, double lon, double vicinityRange) {
		return getElements(lat, lon, vicinityRange, true);
	}
	public List<OSMElement> getElements(double lat, double lon, 
			double vicinityRange, boolean trimToTile) {
		if(vicinityRange > maxVicinity) {
			throw new RuntimeException("Max. vicinity (" + maxVicinity + 
					") exceeded: " + vicinityRange);
		}
		Tile t = new Tile();
		t.left = lon - vicinityRange;
		t.bottom = lat - vicinityRange;
		t.right = lon + vicinityRange;
		t.top = lat + vicinityRange;

		List<OSMElement> result = cache.findInCache(t);
		if(result == null) {
			//System.out.println("Could not find OSM element for tile " + t);
			double vicinityToGrab = maxVicinity;
			Document doc = getXML(lat, lon, vicinityToGrab);
			result = getElements(doc);
			Tile grabbedTile = new Tile();
			grabbedTile.left = lon - vicinityToGrab;
			grabbedTile.bottom = lat - vicinityToGrab;
			grabbedTile.right = lon + vicinityToGrab;
			grabbedTile.top = lat + vicinityToGrab;
			cache.put(grabbedTile, result);
		}

		//System.out.println("result before: " + result.size());
		//int sizeBefore = result.size();
		if(trimToTile) {
			result = trimToTile(result, t);
		}
		//System.out.println("result before/after: " + sizeBefore + "/" + result.size());

		return result;
	}

	private List<OSMElement> trimToTile(List<OSMElement> in, Tile t) {
		List<OSMElement> result = new ArrayList<OSMElement>();
		Set<String> refEls = new HashSet<String>();
		for(OSMElement e : in) {
			if(e instanceof OSMNode) {
				OSMNode n = (OSMNode)e;
				result.add(n);
			} else if(e instanceof OSMWay) {
				OSMWay w = (OSMWay)e;
				OSMWay copy = new OSMWay();
				/* add nodes/lines which intersect with the given tile */
				for(int i = 0; i < w.nodes.size() - 1; i ++) {
					OSMNode n1 = w.nodes.get(i);
					OSMNode n2 = w.nodes.get(i + 1);
					Line l = new Line(n1.toPoint(), n2.toPoint());
					if(t.intersects(l)) {
						if(!copy.nodes.contains(n1))
							copy.nodes.add(n1);
						if(!copy.nodes.contains(n2))
							copy.nodes.add(n2);
						refEls.add(n1.id);
						refEls.add(n2.id);
					}
				}
				if(!copy.nodes.isEmpty()) {
					copy.tags = w.tags;
					result.add(copy);
				}
			} else {
				throw new RuntimeException("Unexpected type: " + e);
			}

			/* remove unreferenced nodes which are outside the tile */
			for(int i = 0; i < result.size(); i ++) {
				OSMElement el = result.get(i);
				if(el instanceof OSMNode) {
					OSMNode n = (OSMNode)el;
					if(!refEls.contains(n.id) && !t.contains(n.toPoint())) {
						result.remove(i--);
					}
				}
			}
		}
		return result;
	}

	protected static String getCacheFileName(Tile t) {
		return Constants.TMP_DIR + "/bbox_" + getCoordinatesSpec(t) + ".gz";
	}
	private static String getCoordinatesSpec(Tile t) {
		DecimalFormat format = new DecimalFormat(
				"##0.0000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH)); //$NON-NLS-1$
		return 	format.format(t.left) + "," + 
				format.format(t.bottom) + "," + 
				format.format(t.right) + "," + 
				format.format(t.top);
	}

	private Document getXML(double lat, double lon, double vicinityRange) {
		return getXML(lat, lon, vicinityRange, 2);
	}
	private Document getXML(double lat, double lon, double vicinityRange, int numRetries) {

		Tile t = new Tile(lon - vicinityRange,
				lat - vicinityRange,
				lon + vicinityRange,
				lat + vicinityRange);
		String spec = getCoordinatesSpec(t);

		InputStream stream = null;

		try {

			/* check if file exist locally */
			String file = getCacheFileName(t);
			if(!new File(file).exists()) {
				new File(Constants.TMP_DIR).mkdir();
//				String baseURL = OVERPASS_API;
				String baseURL = OPENSTREETMAP_API_06;
				String url = baseURL + "map?bbox=" + spec;
				System.out.println(url);
				URL osm = new URL(url);
				HttpURLConnection connection = (HttpURLConnection) osm.openConnection();
				connection.addRequestProperty("Accept-Encoding", "compress, gzip");
				try {
					stream = connection.getInputStream();
				} catch (Exception e) {
					if(numRetries > 0) {
						Thread.sleep(2000);
						return getXML(lat, lon, vicinityRange/1.5, numRetries - 1);
					} else {
						throw new RuntimeException("Unable to fetch (after multiple attempts): " + url);
					}
				}
				FileOutputStream fos = new FileOutputStream(file);
				IOUtils.copy(stream, fos);

				requestCount ++;
				if(requestCount % 10 == 0) {
					System.out.println("Made request #" + requestCount);
				}

			}

			return Util.readCompressedXmlFile(new File(file));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	

	/**
	 * Parse all OSM elements (nodes, ways) from an XML document.
	 */
	protected static List<OSMElement> getElements(Document doc) {
		List<OSMElement> result = new ArrayList<OSMElement>();
		List<OSMNode> nodes = getNodes(doc);
		List<OSMWay> ways = getWays(doc, nodes);
		result.addAll(nodes);
		result.addAll(ways);
		return result;
	}

	/**
	 * Parse all OSM ways from an XML document.
	 */
	private static List<OSMWay> getWays(Document xmlDocument, List<OSMNode> nodes) {
		List<OSMWay> result = new ArrayList<OSMWay>();
		Map<String,OSMNode> nodeMap = new HashMap<String, OSMNode>();
		for(OSMNode n : nodes) {
			nodeMap.put(n.id, n);
		}
		Node osmRoot = xmlDocument.getFirstChild();
		NodeList osmXMLNodes = osmRoot.getChildNodes();
		for (int i = 1; i < osmXMLNodes.getLength(); i++) {
			Node item = osmXMLNodes.item(i);
			if (item.getNodeName().equals("way")) {
				OSMWay way = new OSMWay();
				result.add(way);
				way.tags.putAll(getTags(item));
				NodeList wayNodes = item.getChildNodes();
				for (int j = 1; j < wayNodes.getLength(); j++) {
					Node wayItem = wayNodes.item(j);
					if(wayItem.getNodeName().equals("nd")) {
						String ref = wayItem.getAttributes().getNamedItem("ref").getNodeValue();
						OSMNode node = nodeMap.get(ref);
						if(node != null) {
							way.nodes.add(node);
						} else {
							throw new RuntimeException("Invalid node reference in OSM <way> element: " + ref);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Parse all OSM nodes from an XML document.
	 */
	@SuppressWarnings("nls")
	private static List<OSMNode> getNodes(Document xmlDocument) {
		List<OSMNode> osmNodes = new ArrayList<OSMNode>();
		Node osmRoot = xmlDocument.getFirstChild();
		NodeList osmXMLNodes = osmRoot.getChildNodes();
		for (int i = 1; i < osmXMLNodes.getLength(); i++) {
			Node item = osmXMLNodes.item(i);
			if (item.getNodeName().equals("node")) {
				Map<String,String> attrs = getAttributes(item);
				String id = attrs.get("id");
				String latitude = attrs.get("lat");
				String longitude = attrs.get("lon");
				String version = "0";
				if (attrs.get("version") != null) {
					version = attrs.get("version");
				}
				Map<String,String> tags = getTags(item);
				osmNodes.add(
						new OSMNode(id, Double.parseDouble(latitude), 
								Double.parseDouble(longitude),
								version, tags)
				);
			}
		}
		return osmNodes;
	}

	/**
	 * Get the <tag k=... v=...> elements attached to an XML node, as a map.
	 */
	private static Map<String,String> getTags(Node node) {
		Map<String, String> tags = new HashMap<String, String>();
		NodeList tagXMLNodes = node.getChildNodes();
		for (int j = 1; j < tagXMLNodes.getLength(); j++) {
			Node tagItem = tagXMLNodes.item(j);
			if(tagItem.getNodeName().equals("tag")) {
				NamedNodeMap tagAttributes = tagItem.getAttributes();
				if (tagAttributes != null) {
					tags.put(
							tagAttributes.getNamedItem("k").getNodeValue(),
							tagAttributes.getNamedItem("v").getNodeValue());
				}
			}
		}
		return tags;
	}

	/**
	 * Get the attributes attached to an XML node, as a map.
	 */
	private static Map<String,String> getAttributes(Node node) {
		Map<String, String> attrs = new HashMap<String, String>();
		NamedNodeMap attrNodes = node.getAttributes();
		for (int j = 1; j < attrNodes.getLength(); j++) {
			Node tagItem = attrNodes.item(j);
			attrs.put(tagItem.getNodeName(), tagItem.getNodeValue());
		}
		return attrs;
	}

}
