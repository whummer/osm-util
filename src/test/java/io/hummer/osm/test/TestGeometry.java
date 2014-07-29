package io.hummer.osm.test;

import static org.junit.Assert.*;

import java.util.List;

import io.hummer.osm.OpenStreetMap;
import io.hummer.osm.model.Line;
import io.hummer.osm.model.Point;
import io.hummer.osm.model.Tile;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestGeometry {

	@Test
	public void testIntersect() {
		Line l1 = new Line(0, 0, 10, 10);
		Line l2 = new Line(0, 10, 10, 0);
		Line l3 = new Line(20, 20, 30, 30);
		Tile t1 = new Tile(1, 1, 9, 9);
		Tile t2 = new Tile(0, 5, 4, 9);

		assertTrue(l1.intersects(l2));
		assertTrue(l2.intersects(l1));
		assertTrue(t1.containsOrIntersects(l1));
		assertTrue(t1.containsOrIntersects(l2));

		assertFalse(l1.intersects(l3));
		assertFalse(l2.intersects(l3));
		assertFalse(t1.containsOrIntersects(l3));

		assertFalse(t2.containsOrIntersects(l1));
	}

	@Test
	public void testDivideTile() {
		Tile t1 = new Tile(1, 1, 9, 9);
		Tile t2 = new Tile(0, 5, 4, 9);
		Tile t3 = new Tile(0, 0, 3, 3);
		double range = 0.00000000000001;
		
		Tile tmp1 = t1.centeredDivideBy(1.5);
		assertEquals(t1.getCenter(), tmp1.getCenter(), range);
		Tile tmp2 = t2.centeredDivideBy(2.0);
		assertEquals(t2.getCenter(), tmp2.getCenter(), range);

		List<Tile> tiles1 = Tile.makeTiles(t1, 1);
		Assert.assertEquals(t1.getSizeX() * t1.getSizeY(), tiles1.size(), range);
		List<Tile> tiles2 = Tile.makeTiles(t2, 1);
		Assert.assertEquals(t2.getSizeX() * t2.getSizeY(), tiles2.size(), range);
		List<Tile> tiles3 = Tile.makeTiles(t3, 1.5);
		Assert.assertEquals(4, tiles3.size(), range);
		List<Tile> tiles4 = Tile.makeTiles(t3, 2);
		Assert.assertEquals(4, tiles4.size(), range);
		System.out.println(tiles4);
	}

	private static void assertEquals(Point d1, Point d2, double range) {
		Assert.assertEquals(d1.x, d2.x, range);
		Assert.assertEquals(d1.y, d2.y, range);
	}

	@Test
	@Ignore
	public void testIsInTunnel() {

		/* Tauerntunnel in Austria */
		assertTrue(OpenStreetMap.isInTunnel(47.22379, 13.42406));
		assertTrue(OpenStreetMap.isInTunnel(47.23869,13.42196));
		assertTrue(OpenStreetMap.isInTunnel(47.22379, 13.42370));
		assertFalse(OpenStreetMap.isInTunnel(47.23869,13.42337));
		assertFalse(OpenStreetMap.isInTunnel(47.22379, 13.42500));

		/* small tunnel in Italy */
		assertTrue(OpenStreetMap.isInTunnel(43.92284, 12.81109));
		assertFalse(OpenStreetMap.isInTunnel(43.92310, 12.81241));
		assertFalse(OpenStreetMap.isInTunnel(43.92226, 12.81014));

		/* small tunnel in Switzerland */
		assertFalse(OpenStreetMap.isInTunnel(47.33595227215317,8.526139511984137));
		assertTrue(OpenStreetMap.isInTunnel(47.33475,8.52402));

	}
}
