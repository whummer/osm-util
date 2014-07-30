package io.hummer.osm.model;



/**
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
public class Line {
	/* start point */
	double startX, startY;
	/* end point */
	double endX, endY;

	public Line(double startX, double startY, double endX, double endY) {
		this.startX = startX;
		this.startY = startY;
		this.endX = endX;
		this.endY = endY;
	}

	public Line(Point start, Point end) {
		this(start.x, start.y, end.x, end.y);
	}

	/**
	 * See http://en.wikipedia.org/wiki/Line%E2%80%93line_intersection
	 */
	public boolean intersects(Line l) {
		double threshold = 0.000000000000000001;
		return isLineIntersection(startX, startY, endX, endY, 
				l.startX, l.startY, l.endX, l.endY, threshold);
	}

	/**
	 * Based on:
	 * http://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect
	 */
	private boolean isLineIntersection(double p0_x, double p0_y, double p1_x, double p1_y,
			double p2_x, double p2_y, double p3_x, double p3_y, double threshold) {

		double s02_x, s02_y, s10_x, s10_y, s32_x, s32_y, s_numer, t_numer, denom;
		s10_x = p1_x - p0_x;
		s10_y = p1_y - p0_y;
		s32_x = p3_x - p2_x;
		s32_y = p3_y - p2_y;

		denom = s10_x * s32_y - s32_x * s10_y;
		if (Math.abs(denom) <= threshold)
			return false; // Collinear
		boolean denomPositive = denom > 0;

		s02_x = p0_x - p2_x;
		s02_y = p0_y - p2_y;
		s_numer = s10_x * s02_y - s10_y * s02_x;
		if ((s_numer < 0) == denomPositive)
			return false; // No collision

		t_numer = s32_x * s02_y - s32_y * s02_x;
		if ((t_numer < 0) == denomPositive)
			return false; // No collision

		if (((s_numer > denom) == denomPositive)
				|| ((t_numer > denom) == denomPositive))
			return false; // No collision
		// Collision detected

		return true;
	}
	
	@Override
	public String toString() {
		return "Line[" +
				"(" + startX + "," + startY + ")->" +
				"(" + endX + "," + endY + ")]";
	}
}
