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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
public class OSMCachedQuerier {

	private final double maxVicinity = Constants.MAX_VICINITY;
	private final double queryVicinity = Constants.DEFAULT_QUERY_VICINITY;
	private int requestCount;
	private TiledMapAbstract<List<OSMElement>> cache = new TiledMapOSM();

	static final String OVERPASS_API = "http://www.overpass-api.de/api/";
	static final String OPENSTREETMAP_API_06 = "http://www.openstreetmap.org/api/0.6/";

	static {
		if(!new File(Constants.TMP_DIR).exists()) {
			new File(Constants.TMP_DIR).mkdir();
		}
	}

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
		Tile totalTile = new Tile();
		totalTile.left = lon - vicinityRange;
		totalTile.bottom = lat - vicinityRange;
		totalTile.right = lon + vicinityRange;
		totalTile.top = lat + vicinityRange;

		List<OSMElement> result = new LinkedList<OSMElement>();
		//System.out.println("Could not find OSM element for tile " + t);
		double vicinityToGrab = queryVicinity;
		int maxTileSizeReductions = 10;
		for(int i = 0; i < maxTileSizeReductions; i ++) {
			try {
				result = new LinkedList<OSMElement>();
				List<Tile> tiles = Tile.makeTiles(totalTile, vicinityToGrab * 2);
				for(Tile t1 : tiles) {
					Map.Entry<Tile,List<OSMElement>> tmpEntry = cache.findCacheEntry(t1);
					List<OSMElement> tmp = null;
					if(tmpEntry == null) {
						tmp = retrieveAndCacheTile(t1);
					} else {
						tmp = tmpEntry.getValue();
						if(!tmpEntry.getKey().equals(t1)) {
//							long time = System.currentTimeMillis();
							tmp = trimToTile(tmp, t1);
//							System.out.println("trimmed to tile (ms): " + 
//									(System.currentTimeMillis() - time));
						}
					}
					//System.out.println("Elements for tile " + t1 + ": " + tmp.size());
					for(OSMElement e : tmp) {
						/* do NOT make this check -> too costly! */
						//if(!result.contains(e)) {
							result.add(e);
						//}
					}
				}
				break;
			} catch (RuntimeException e) {
				if(i >= maxTileSizeReductions - 1) {
					throw e;
				}
				/* re-try with reduced vicinity */
				vicinityToGrab /= 1.5;
				System.out.println("WARN: Re-trying OSM query with vicinity " + vicinityToGrab);
			}
		}

		//System.out.println("result before: " + result.size());
		//int sizeBefore = result.size();
		if(trimToTile) {
			System.out.println("Trimming result (" + result.size() + " elements) to tile " + totalTile);
			result = trimToTile(result, totalTile);
		}
		//System.out.println("result before/after: " + sizeBefore + "/" + result.size());

		return result;
	}

	private List<OSMElement> retrieveAndCacheTile(Tile tile) {
		Document doc = getXML(tile, 0, 1.0);
		List<OSMElement> result = getElements(doc);
		cache.put(tile, result);
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
				copy.id = w.id;
				/* add nodes/lines which intersect with the given tile */
				for(int i = 0; i < w.nodes.size() - 1; i ++) {
					OSMNode n1 = w.nodes.get(i);
					OSMNode n2 = w.nodes.get(i + 1);
					Line l = new Line(n1.lon, n1.lat, n2.lon, n2.lat);
					if(t.containsOrIntersects(l)) {
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

//	private Document getXML(double lat, double lon, double vicinityRange, 
//			int numRetries, double tileSizeDivisorOnRetry) {
//		Tile t = new Tile(lon - vicinityRange,
//				lat - vicinityRange,
//				lon + vicinityRange,
//				lat + vicinityRange);
//		return getXML(t, numRetries, tileSizeDivisorOnRetry);
//	}
//	private Document getXML(Tile t) {
//		return getXML(t, 0, 1.0);
//	}
	private Document getXML(Tile t, int numRetries, double tileSizeDivisorOnRetry) {

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
						return getXML(t.centeredDivideBy(tileSizeDivisorOnRetry), 
								numRetries - 1, tileSizeDivisorOnRetry);
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
				way.id = ((Element)item).getAttribute("id");
				result.add(way);
				way.addTags(getTags(item));
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
