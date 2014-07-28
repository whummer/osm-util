package io.hummer.osm.query;

import io.hummer.osm.model.Tile;
import io.hummer.osm.model.TiledMapAbstract;
import io.hummer.osm.util.Util;

import java.io.File;
import java.util.List;

import org.w3c.dom.Document;

/**
 * TiledMap implementation for OpenStreetMap.
 * @author Waldemar Hummer
 */
public class TiledMapOSM extends TiledMapAbstract<List<OSMElement>> {
	private static final long serialVersionUID = 1L;
	private static final int MAX_ENTRIES = 30;

	public TiledMapOSM() {
		super(MAX_ENTRIES);
	}

	public List<OSMElement> loadLazily(Tile t1) {
		File f = new File(OSMCachedQuerier.getCacheFileName(t1));
		System.out.println("Loading tile from file system: " + f);
		Document doc = Util.readCompressedXmlFile(f);
		return OSMCachedQuerier.getElements(doc);
	}

}
