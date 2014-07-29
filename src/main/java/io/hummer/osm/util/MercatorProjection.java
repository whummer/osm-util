package io.hummer.osm.util;

import io.hummer.osm.model.Point;

/**
 * Used to convert lat/lon coordinates to "world coordinates" 
 * used by Google Maps.
 * 
 * @author Waldemar Hummer
 */
public class MercatorProjection extends MapProjection {

	private double tileCount;
	private Point pixelOrigin;
	private double pixelsPerLonDegree;
	private double pixelsPerLonRadian;

	public MercatorProjection(int zoomLevel) {
		tileCount = 1 << zoomLevel;
		pixelOrigin = new Point(tileCount / 2.0, tileCount / 2.0);
		pixelsPerLonDegree = tileCount / 360.0;
		pixelsPerLonRadian = tileCount / (2.0 * Math.PI);
	}

	public Point fromLatLngToPoint(double lat, double lon) {
		Point point = new Point(0, 0);
		Point origin = pixelOrigin;

		point.x = origin.x + lon * pixelsPerLonDegree;

		// Truncating to 0.9999 effectively limits latitude to 89.189. This is
		// about a third of a tile past the edge of the world tile.
		double siny = bound(Math.sin(degreesToRadians(lat)), -0.9999, 0.9999);
		point.y = origin.y + 0.5 * Math.log((1.0 + siny) / (1.0 - siny))
				* -pixelsPerLonRadian;
		return point;
	}

	public Point fromPointToLatLng(double xPixel, double yPixel) {
		Point origin = pixelOrigin;
		double lng = (xPixel - origin.x) / pixelsPerLonDegree;
		double latRadians = (yPixel - origin.y) / -pixelsPerLonRadian;
		double lat = radiansToDegrees(2.0 * 
				Math.atan(Math.exp(latRadians))
				- Math.PI / 2.0);
		return new Point(lat, lng);
	}

	public double getPixelsPerLonDegree() {
		return pixelsPerLonDegree;
	}
	public double getPixelsPerLonRadian() {
		return pixelsPerLonRadian;
	}
	public double getTileCount() {
		return tileCount;
	}
}
