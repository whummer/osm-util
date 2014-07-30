package io.hummer.osm.model;


import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
@XmlRootElement
public class Tile implements Serializable {
	private static final long serialVersionUID = 1L;

	public double left, bottom, right, top;

	public Tile() { }
	public Tile(double left, double bottom, double right, double top) {
		this.left = left;
		this.bottom = bottom;
		this.right = right;
		this.top = top;
	}

	public Point getCenter() {
		return new Point(
				left + ((right - left) / 2.0),
				bottom + ((top - bottom) / 2.0));
	}

	public boolean containedIn(Tile t) {
		return containedIn(t, false);
	}
	private boolean containedIn(Tile t, boolean verbose) {
		boolean contained = (t.left <= left && t.bottom <= bottom 
				&& t.right >= right && t.top >= top);
		if(verbose) {
			if(!contained) {
				System.out.println("Tile " + this + " is NOT contained in " + t);
				//Util.dumpStackTrace();
			} else {
				System.out.println("Tile " + this + " IS contained in " + t);
			}
		}
		return contained;
	}

	public boolean exactEquals(Tile t) {
		return t.left == left && t.bottom == bottom 
				&& t.right == right && t.top == top;
	}

	public boolean contains(Point p) {
		return left <= p.x && bottom <= p.y
				&& right >= p.x && top >= p.y;
	}

	private boolean contains(Line l) {
		return new Tile(
				Math.min(l.startX, l.endX), 
				Math.max(l.startY, l.endY),
				Math.max(l.startX, l.endX),
				Math.min(l.startY, l.endY)).containedIn(this);
	}

	public boolean containsOrIntersects(Line l) {
		if(contains(l))
			return true;
		for(Line b : getBorderLines()) {
			if(l.intersects(b))
				return true;
		}
		return false;
	}

	public List<Line> getBorderLines() {
		return Arrays.asList(
				new Line(left, bottom, left, top),
				new Line(left, bottom, right, bottom),
				new Line(right, bottom, right, top),
				new Line(left, top, right, top));
	}

	@Override
	public int hashCode() {
		return (int)(left + top + bottom + right);
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Tile))
			return false;
		Tile t = (Tile)o;
		return exactEquals(t);
	}

	@Override
	public String toString() {
		return "Tile [left=" + left + ", bottom=" + bottom + ", right=" + right
				+ ", top=" + top + "]";
	}
	public static List<Tile> makeTiles(Tile t, double tileSize) {
		List<Tile> result = new LinkedList<Tile>();
		for(double left = t.left; left < t.right; left += tileSize) {
			for(double top = t.top; top > t.bottom; top -= tileSize) {
				Tile t1 = new Tile(left, top - tileSize, left + tileSize, top);
				if(t1.right > t.right) {
					t1.right = t.right;
				}
				if(t1.bottom < t.bottom) {
					t1.bottom = t.bottom;
				}
				result.add(t1);
			}
		}
		return result;
	}
	public Tile centeredDivideBy(double tileSizeDivisorOnRetry) {
		double oldSize = right - left;
		Point c = getCenter();
		double newSize = oldSize / tileSizeDivisorOnRetry;
		double left = c.x - newSize;
		double right = c.x + newSize;
		double bottom = c.y - newSize;
		double top = c.y + newSize;
		return new Tile(left, bottom, right, top);
	}
	public double getSizeX() {
		return Math.abs(left - right);
	}
	public double getSizeY() {
		return Math.abs(left - right);
	}

}
