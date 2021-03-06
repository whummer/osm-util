package io.hummer.osm.model;


import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * A map whose keys are Tile objects. Given a Tile t1 as key, 
 * this map contains key t1 iff there is a Tile t2 such that
 * t1 is contained in t2.
 * 
 * This map is used for caching/loading map tiles for various
 * types of maps (e.g., OpenStreetMap, Cell network coverage maps, ...)
 * 
 * @author Waldemar Hummer
 *
 * @param <V>
 */
@XmlType
public abstract class TiledMapAbstract<V> {

	@XmlAttribute
	protected int maxEntries = 0; /* 0=unbounded */
	@XmlElement
	protected HashMap<Tile, V> values = new HashMap<Tile, V>();

	public TiledMapAbstract() {
		this(0);
	}
	public TiledMapAbstract(int maxEntries) {
		this.maxEntries = maxEntries;
	}

	/**
	 * Determine whether a tile is in the cache, and return the tile value. 
	 * If necessary, the tile value/content belonging to the given
	 * key will be lazily loaded (implemented by subclasses).
	 * @param t the tile
	 * @return
	 */
	public V findInCache(Tile t) {
		Map.Entry<Tile,V> entry = findCacheEntry(t);
		if(entry == null)
			return null;
		return entry.getValue();
	}

	public Map.Entry<Tile,V> findCacheEntry(Tile t) {
		for(Map.Entry<Tile,V> entry : values.entrySet()) {
			if(t.containedIn(entry.getKey())) {
				/* load lazily if necessary. */
				if(entry.getValue() == null) {
					V els = loadLazily(entry.getKey());
					put(entry.getKey(), els);
				}
				return entry;
			}
		}
		return null;
	}

	public abstract V loadLazily(Tile t);

	public V put(Tile key, V value) {
		if(!values.isEmpty() && maxEntries > 0 
				&& values.size() + 1 > maxEntries) {
			Tile someKey = values.keySet().iterator().next();
			values.remove(someKey);
		}
		return values.put(key, value);
	}

	public int size() {
		return values.size();
	}
}
