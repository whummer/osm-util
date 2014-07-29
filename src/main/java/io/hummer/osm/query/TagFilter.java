package io.hummer.osm.query;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class TagFilter {

	private static class TagFilterAtomic extends TagFilter {
		private String key;
		private Object valueOrNull;
		public TagFilterAtomic(String key, Object valueOrNull) {
			this.key = key;
			this.valueOrNull = valueOrNull;
		}
		public <T extends OSMElement> boolean matches(T el) {
			return el.getTagsAsMap().containsKey(key) && 
				(valueOrNull == null ||
				valueOrNull.equals(el.getTagsAsMap().get(key)));
		}
	}
	private static abstract class TagFilterComposite extends TagFilter {
		List<TagFilter> filters;
	}
	private static class TagFilterOR extends TagFilterComposite {
		public TagFilterOR(TagFilter ... filters) {
			this.filters = Arrays.asList(filters);
		}
		public <T extends OSMElement> boolean matches(T el) {
			if(filters.isEmpty())
				return true;
			for(TagFilter f : filters) {
				if(f.matches(el))
					return true;
			}
			return false;
		}
	}
	private static class TagFilterAND extends TagFilterComposite {
		public TagFilterAND(TagFilter ... filters) {
			this.filters = Arrays.asList(filters);
		}
		public <T extends OSMElement> boolean matches(T el) {
			for(TagFilter f : filters) {
				if(!f.matches(el))
					return false;
			}
			return true;
		}
	}

	public abstract <T extends OSMElement> boolean matches(T el);

	public static TagFilter contains(String key, Object valueOrNull) {
		return new TagFilterAtomic(key, valueOrNull);
	}
	public static TagFilter or(TagFilter ... filters) {
		return new TagFilterOR(filters);
	}
	public static TagFilter and(TagFilter ... filters) {
		return new TagFilterAND(filters);
	}
	public static TagFilter and(Map<String, Object> tags) {
		List<TagFilter> tagFilters = new LinkedList<TagFilter>();
		if(tags != null) {
			for(String key: tags.keySet()) {
				tagFilters.add(new TagFilterAtomic(key, tags.get(key)));
			}
		}
		return new TagFilterAND(tagFilters.toArray(new TagFilter[0]));
	}

	public static <T extends OSMElement> List<T> filterForTag(
			List<T> els, TagFilter filter, boolean maxOneResult) {
		List<T> result = new LinkedList<T>();
		for(T el : els) {
			if(filter.matches(el)) {
				result.add(el);
				if(maxOneResult) {
					return result;
				}
			}
		}
		return result;
	}


}
