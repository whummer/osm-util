package io.hummer.osm.util;

import io.hummer.osm.model.Point;

/**
 * Used to convert lat/lon coordinates to "world coordinates" 
 * used by Google Maps.
 * 
 * @author Waldemar Hummer
 */
public abstract class MapProjection {

	public abstract Point fromLatLngToPoint(double lat, double lon);

	public abstract Point fromPointToLatLng(double xPixel, double yPixel);

	/* HELPER METHODS */
	
	protected double degreesToRadians(double deg) {
		return deg * (Math.PI / 180);
	}

	protected double radiansToDegrees(double rad) {
		return rad / (Math.PI / 180);
	}

	protected double bound(double value, double opt_min, double opt_max) {
		value = Math.max(value, opt_min);
		value = Math.min(value, opt_max);
		return value;
	}

}
