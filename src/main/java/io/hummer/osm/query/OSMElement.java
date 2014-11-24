package io.hummer.osm.query;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * @author Waldemar Hummer (hummer@dsg.tuwien.ac.at)
 */
@XmlSeeAlso({OSMElement.TagEntry.class})
public class OSMElement {

	@XmlElement(name="tag")
	protected List<TagEntry> tags = new LinkedList<TagEntry>();
	@XmlAttribute
	protected String id;

	@XmlRootElement
	public static class TagEntry {
		@XmlAttribute(name = "k")
		String key;
		@XmlAttribute(name = "v")
		String value;

		public TagEntry() {}
		public TagEntry(String key, String value) {
			this.key = key;
			this.value = value;
		}
		public String toString() {
			return "TagEntry [k=" + key + ", v=" + value + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TagEntry other = (TagEntry) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
		
	}

	public List<TagEntry> getTags() {
		return tags;
	}
	public String getId() {
		return id;
	}
	public Map<String,String> getTagsAsMap() {
		Map<String, String> result = new HashMap<String, String>();
		for(TagEntry e : tags) {
			if(result.containsKey(e.key)) {
				throw new IllegalArgumentException("Duplicate key: '" + e.key + "'");
			}
			result.put(e.key, e.value);
		}
		return result;
	}

	public void addTags(Map<String, String> tagsMap) {
		if(tagsMap != null) {
			for(String key : tagsMap.keySet()) {
				tags.add(new TagEntry(key, tagsMap.get(key)));
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((tags == null) ? 0 : tags.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OSMElement other = (OSMElement) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (tags == null) {
			if (other.tags != null)
				return false;
		} else if (!tags.equals(other.tags))
			return false;
		return true;
	}


}
