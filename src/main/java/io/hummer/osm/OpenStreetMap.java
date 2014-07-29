package io.hummer.osm;

import io.hummer.osm.model.Point;
import io.hummer.osm.query.OSMCachedQuerier;
import io.hummer.osm.query.OSMElement;
import io.hummer.osm.query.OSMNode;
import io.hummer.osm.query.OSMWay;
import io.hummer.osm.util.Util;

import java.util.LinkedList;
import java.util.List;

/**
 * Loosely based on:
 * http://wiki.openstreetmap.org/wiki/Java_Access_Example
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
public class OpenStreetMap {

	private static final OSMCachedQuerier querier = new OSMCachedQuerier();

	public static boolean isInTunnel(double lat, double lon) {
		return isInTunnel(lat, lon, Constants.DEFAULT_VICINITY);
	}
	public static boolean isInTunnel(double lat, double lon, double vicinity) {
		try {
			List<? extends OSMElement> list = getOSMWaysInVicinity(lat, lon, vicinity);
			return containsTag(list, "tunnel", null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public static double getTunnelLength(double lat, double lon) {
		return getTunnelLength(lat, lon, Constants.DEFAULT_VICINITY);
	}
	public static double getTunnelLength(double lat, double lon, double vicinity) {
		try {
			/* get tunnel elements within small tile */
			List<OSMWay> listSmall = getOSMWaysInVicinity(lat, lon, vicinity, true);
			listSmall = filterForTag(listSmall, "tunnel", null);
			List<String> tunnelWayIDs = new LinkedList<String>();
			if(listSmall.size() > 1) {
				System.err.println("WARN: Multiple tunnel ways found here (" + 
						listSmall.size() + "): " + lat + "," + lon); // TODO use logger for output
			}
			for(OSMWay w : listSmall) {
				tunnelWayIDs.add(w.getId());
			}
			/* get tunnel elements within extended (larger) tile */
			List<OSMWay> listLarge = getOSMWaysInVicinity(lat, lon, vicinity, false);
			listLarge = filterForTag(listLarge, "tunnel", null);
			if(listLarge.isEmpty()) {
				throw new RuntimeException("There seems to be no tunnel here: " + lat + "," + lon);
			}
			/* find all tunnel paths (possibly consisting of multiple ways) */
			List<List<OSMWay>> ways = getConsecutiveWays(listLarge);
			if(ways.size() > 1) {
				System.err.println("WARN: Multiple tunnels found here (" + 
						ways.size() + "): " + lat + "," + lon); // TODO use logger for output
				System.out.println(ways);
			}
			/* filter correct tunnel path(s) by intersecting small with large */
			List<OSMWay> listFinal = null;
			for(List<OSMWay> tmp : ways) {
				for(OSMWay w : tmp) {
					if(tunnelWayIDs.contains(w.getId())) {
						listFinal = tmp;
						break;
					}
				}
			}
			return getTunnelLength(listFinal);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static List<List<OSMWay>> getConsecutiveWays(List<OSMWay> ways) {
		List<List<OSMWay>> result = new LinkedList<List<OSMWay>>();
		/* init */
		for(OSMWay w : ways) {
			List<OSMWay> tmp = new LinkedList<OSMWay>();
			tmp.add(w);
			result.add(tmp);
		}
		/* merge */
		for(int i = 0; i < result.size(); i ++) {
			List<OSMWay> l1 = result.get(i);
			for(int j = 0; j < result.size(); j ++) {
				List<OSMWay> l2 = result.get(j);
				if(l1 != l2) {
					List<OSMNode> tmp1 = l1.get(l1.size() - 1).getNodes();
					List<OSMNode> tmp2 = l2.get(l2.size() - 1).getNodes();
					OSMNode n1 = tmp1.get(0);
					OSMNode n2 = tmp2.get(0);
					OSMNode n3 = tmp1.get(tmp1.size() - 1);
					OSMNode n4 = tmp2.get(tmp2.size() - 1);
					if(n1.isSameLocationAs(n4)) {
						l2.addAll(l1);
						result.remove(i);
						i--; j--;
					} else if(n2.isSameLocationAs(n3)) {
						l1.addAll(l2);
						result.remove(j);
						i--; j--;
					}
				}
			}
		}
		return result;
	}

	private static double getTunnelLength(List<OSMWay> ways) {
		double length = 0;
		for(OSMWay way : ways) {
			length += getTunnelLength(way);
		}
		return length;
	}
	private static double getTunnelLength(OSMWay way) {
		double length = 0;
		for(int i = 0; i < way.getNodes().size() - 1; i ++) {
			length += getDistance(
					way.getNodes().get(i), way.getNodes().get(i + 1));
		}
		return length;
	}

	public static double getDistance(OSMNode n1, OSMNode n2) {
		return getDistance(n1.toPoint(), n2.toPoint());
	}
	public static double getDistance(Point n1, Point n2) {
		double lat1 = n1.y;
		double lat2 = n2.y;
		double lon1 = n1.x;
		double lon2 = n2.x;

		double R = 6371; // km
		double dLat = Util.toRadian(lat2-lat1);
		double dLon = Util.toRadian(lon2-lon1);
		lat1 = Util.toRadian(lat1);
		lat2 = Util.toRadian(lat2);

		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		        Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2); 
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		return R * c;
	}

	public static List<OSMWay> getOSMWaysInVicinity(double lat, double lon,
			double vicinityRange) throws Exception {
		return getOSMWaysInVicinity(lat, lon, vicinityRange, true);
	}
	public static List<OSMWay> getOSMWaysInVicinity(double lat, double lon,
			double vicinityRange, boolean trimToTile) throws Exception {
		return getOSMElementsInVicinity(lat, lon, vicinityRange, OSMWay.class, trimToTile);
	}

	public static List<OSMNode> getOSMNodesInVicinity(double lat, double lon,
			double vicinityRange) throws Exception {
		return getOSMElementsInVicinity(lat, lon, vicinityRange, OSMNode.class, true);
	}

	@SuppressWarnings("unchecked")
	private static <T extends OSMElement> List<T> getOSMElementsInVicinity(double lat, double lon,
			double vicinityRange, Class<? extends T> clazz, boolean trimToTile) throws Exception {
		List<T> result = new LinkedList<T>();
		List<OSMElement> elements = querier.getElements(lat, lon, vicinityRange, trimToTile);
		for(OSMElement e : elements) {
			if(clazz.isAssignableFrom(e.getClass())) {
				result.add((T)e);
			}
		}
		return result;
	}

	private static boolean containsTag(List<? extends OSMElement> els, String key, String valueOrNull) {
		return !filterForTag(els, key, valueOrNull, true).isEmpty();
	}

	private static <T extends OSMElement> List<T> filterForTag(
			List<T> els, String key, String valueOrNull) {
		return filterForTag(els, key, valueOrNull, false);
	}

	private static <T extends OSMElement> List<T> filterForTag(
			List<T> els, String key, String valueOrNull, boolean maxOneResult) {
		List<T> result = new LinkedList<T>();
		for(T el : els) {
			if(el.getTags().containsKey(key) && 
					(valueOrNull == null || 
					valueOrNull.equals(el.getTags().get(key)))) {
				result.add(el);
				if(maxOneResult) {
					return result;
				}
			}
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
//		double coords[] = { 46.028345, 8.933825 };
		double[] coords = new double[]{43.92284, 12.81109};
		double vicinity = 0.0005;
		List<? extends OSMElement> list = getOSMWaysInVicinity(coords[0],
				coords[1], vicinity);
		for (OSMElement osmEl : list) {
			System.out.println(osmEl);
		}
		System.out.println("contains tunnels: " + 
				containsTag(list, "tunnel", null));

		coords = new double[]{43.92150, 12.80999};
		list = getOSMWaysInVicinity(coords[0], coords[1], vicinity);
		System.out.println("contains tunnels: " + 
				containsTag(list, "tunnel", null));

//		getCountryMap("switzerland");
//		NodeEnvelopeQueryFactoryImpl fac = new NodeEnvelopeQueryFactoryImpl();
//		org.apache.lucene.search.Query q = fac.build();
	}

	
	
	
	
	
	
	
	
	
	
	
//	public static Object getCountryMap(String country) {
//		java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.WARNING);
//		java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.WARNING);
//		PropertyConfigurator.configure(OpenStreetMap.class.getResource("/log4j.properties"));
//		JclLogger log = (JclLogger)LogFactory.getLog("httpclient.wire.header");
//		System.out.println(log);
////		if(log instanceof DefaultServiceLog) {
//			DefaultServiceLog.setLogLevel("WARN");
////		}
////		System.exit(0);
//
//		InstantiatedOsmXmlParser parser = InstantiatedOsmXmlParser
//				.newInstance();
//		HttpClient httpClient = new DefaultHttpClient();
//		HttpResponse httpResponse;
//
//		//String url = "http://download.geofabrik.de/europe/" + country + "-latest.osm.bz2";
//
//		String file = "switzerland-latest.osm.bz2";
//
//		try {
//			Reader reader = new InputStreamReader(
//					new BZip2CompressorInputStream(
//							new FileInputStream(file)), "UTF8");
//			InstantiatedOsmXmlParserDelta d = parser.parse(reader);
//			System.out.println(d);
//			reader.close();
//			return d;
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//	}

	// public static OSMNode getNode(String nodeId) throws IOException,
	// ParserConfigurationException, SAXException {
	// String string = "http://www.openstreetmap.org/api/0.6/node/" + nodeId;
	// URL osm = new URL(string);
	// HttpURLConnection connection = (HttpURLConnection) osm.openConnection();
	//
	// DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	// DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	// Document document = docBuilder.parse(connection.getInputStream());
	// System.out.println(Util.toString(document));
	// List<OSMNode> nodes = getNodes(document);
	// if (!nodes.isEmpty()) {
	// return nodes.iterator().next();
	// }
	// return null;
	// }

	// /**
	// *
	// * @param query the overpass query
	// * @return the nodes in the formulated query
	// * @throws IOException
	// * @throws ParserConfigurationException
	// * @throws SAXException
	// */
	// public static Document getNodesViaOverpass(String query) throws
	// IOException, ParserConfigurationException, SAXException {
	// String hostname = OVERPASS_API;
	// String queryString = readFileAsString(query);
	//
	// URL osm = new URL(hostname);
	// HttpURLConnection connection = (HttpURLConnection) osm.openConnection();
	// connection.setDoInput(true);
	// connection.setDoOutput(true);
	// connection.setRequestProperty("Content-Type",
	// "application/x-www-form-urlencoded");
	//
	// DataOutputStream printout = new
	// DataOutputStream(connection.getOutputStream());
	// printout.writeBytes("data=" + URLEncoder.encode(queryString, "utf-8"));
	// printout.flush();
	// printout.close();
	//
	// DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
	// DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	// return docBuilder.parse(connection.getInputStream());
	// }

	// /**
	// * @param filePath
	// * @return
	// * @throws java.io.IOException
	// */
	// private static String readFileAsString(String filePath) throws
	// java.io.IOException {
	// StringBuffer fileData = new StringBuffer(1000);
	// BufferedReader reader = new BufferedReader(new FileReader(filePath));
	// char[] buf = new char[1024];
	// int numRead = 0;
	// while ((numRead = reader.read(buf)) != -1) {
	// String readData = String.valueOf(buf, 0, numRead);
	// fileData.append(readData);
	// buf = new char[1024];
	// }
	// reader.close();
	// return fileData.toString();
	// }

}
