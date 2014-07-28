package io.hummer.osm.model;


import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
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

	public boolean intersects(Line l) {
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
				new Line(left, top, left, bottom),
				new Line(left, bottom, right, bottom),
				new Line(right, top, right, bottom),
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
//		return containedIn(t);
		return exactEquals(t);
	}

	@Override
	public String toString() {
		return "Tile [left=" + left + ", bottom=" + bottom + ", right=" + right
				+ ", top=" + top + "]";
	}

}
