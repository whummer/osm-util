package io.hummer.osm;

/**
 * OSM constants.
 * @author Waldemar Hummer
 */
public class Constants {

	/**
	 * Default latitude/longitude vicinity considered when 
	 * treating a "point" on the map like a "mini tile".
	 */
	public static double DEFAULT_VICINITY = 0.0005;
	/**
	 * Default vicinity of tiles received by queries.
	 */
	public static double DEFAULT_QUERY_VICINITY = 0.02;
	/**
	 * Maximum latitude/longitude vicinity allowed for 
	 * tile service requests.
	 */
	public static double MAX_VICINITY = 0.1;
	/** Temporary data storage directory */
	public static final String TMP_DIR = "tmp";

}
