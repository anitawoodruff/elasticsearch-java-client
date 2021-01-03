package com.winterwell.es.client.query;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * Convenience utils
 * @author daniel
 *
 */
public class ESQueryBuilders {

	/**
	 * Convenience for a shared value that indicates unset/undefined. This is NOT part of the ElasticSearch API itself.
	 */
	public static final String UNSET = "unset";

	/**
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-match-all-query.html
	 * @return
	 */
	public static ESQueryBuilder match_all() {
		return new ESQueryBuilder(new ArrayMap("match_all", new ArrayMap()));
	}
	
	/**
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-match-all-query.html
	 * @return
	 */
	public static ESQueryBuilder match_none() {
		return new ESQueryBuilder(new ArrayMap("match_none", new ArrayMap()));
	}
	
	/**
	 * Combine several ES queries via bool.must (i.e. AND).
	 * @param queries Can be Maps, QueryBuilder objects, or ESQueryBuilder objects. Can contain nulls.
	 * If there is a single non-null query, the same query will be returned (i.e. no superfluous bool wrapper is added).
	 * @return and all the input queries. null if all inputs were null.
	 */
	public static ESQueryBuilder must(Object... queries) {
		// filter out nulls
		List queryList = Containers.filterNulls(Arrays.asList(queries));
		if (queryList.isEmpty()) {
			return null;
		}
		// standardise
		List<ESQueryBuilder> esqs = Containers.apply(queryList, ESQueryBuilder::make);
		// just one?
		if (queryList.size()==1) {
			return esqs.get(0);
		}
		// combine
		List<Map> maps = Containers.apply(esqs, esq -> esq.toJson2());
		Map must = new ArrayMap("bool", new ArrayMap("must", maps));
		return new ESQueryBuilder(must);
	}

	/**
	 * Find exact term matches. 
	 * 
	 * Note: When querying full text fields, use the match query instead, which understands how the field has been analyzed.
	 * 
	 * https://www.elastic.co/guide/en/elasticsearch/reference/6.2/query-dsl-term-query.html
	 * @param field
	 * @param value
	 * @return
	 */
	public static ESQueryBuilder termQuery(String field, Object value) {
		Map must = new ArrayMap("term", new ArrayMap(field, value));
		return new ESQueryBuilder(must);
	}
	
	/**
	 * match or multi_match with * all fields
	 * https://www.elastic.co/guide/en/elasticsearch/reference/6.2/query-dsl-match-query.html
	 */
	public static ESQueryBuilder matchQuery(String optionalField, Object value) {
		if (optionalField != null) {
			Map must = new ArrayMap("match", new ArrayMap(optionalField, value));
			return new ESQueryBuilder(must);
		}
		Map must = new ArrayMap("multi_match", new ArrayMap(
				"query", value,
				"fields", Arrays.asList("*")));
		return new ESQueryBuilder(must);		
	}
	
	/**
	 * @param field
	 * @param start NB: older than WELL_OLD=1900 is ignored
	 * @param end NB: beyond WELL_FUTURE=3000AD is ignored
	 * @return
	 */
	public static ESQueryBuilder dateRangeQuery(String field, Time start, Time end) {
		assert field != null;
		if (start==null && end==null) throw new NullPointerException("Must provide one of start/end");
		if (start !=null && end !=null && ! end.isAfter(start)) {
			throw new IllegalArgumentException("Empty range :"+start+" to "+end);
		}
		Map rq = new ArrayMap();
		if (start!=null && start.isAfter(TimeUtils.WELL_OLD)) {
			rq.put("from", start.toISOString());
			rq.put("include_lower", true);
		}
		if (end!=null && end.isBefore(TimeUtils.WELL_FUTURE)) {
			rq.put("to", end.toISOString());
			rq.put("include_upper", true);
		}
		Map must = new ArrayMap("range", 
				new ArrayMap(field, rq));
		return new ESQueryBuilder(must);
	}

	public static BoolQueryBuilder boolQuery() {
		return new BoolQueryBuilder();
	}

	/**
	 * 
	 * Note: to test for a missing field, use this inside {@link BoolQueryBuilder#mustNot(ESQueryBuilder)}
	 * 
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-range-query.html
	 * 
	 * @param field
	 * @return true if one or more non-null values are present for this field
	 */
	public static ESQueryBuilder existsQuery(String field) {
		Map must = new ArrayMap("exists", new ArrayMap("field", field));
		return new ESQueryBuilder(must);
	}

	/**
	 * @param field
	 * @param min > this Can be null if max is set.
	 * @param max < this Can be null if min is set.
	 * @param inclusive If true, use <= and >=.
	 * @return
	 */
	public static ESQueryBuilder rangeQuery(String field, Number min, Number max, boolean inclusive) {
		assert field != null;
		if (min==null && max==null) throw new NullPointerException("Must provide one of min/max");
		if (min !=null && max !=null && MathUtils.compare(min, max) != -1) {
			throw new IllegalArgumentException("Empty range for "+field+": "+min+" to "+max);
		}
		Map rq = new ArrayMap();
		if (min!=null) {
			rq.put(inclusive? "gte" : "gt", min);
		}
		if (max!=null) {
			rq.put(inclusive? "lte" : "lt", max);
		}
		Map must = new ArrayMap("range", 
				new ArrayMap(field, rq));
		return new ESQueryBuilder(must);
	}

	public static MoreLikeThisQueryBuilder similar(String like, List<String> fields) {		    
		return new MoreLikeThisQueryBuilder(like).setFields(fields);
	}

	/**
	 * // Is this the best for keywords??
	 * https://www.elastic.co/guide/en/elasticsearch/reference/6.2/query-dsl-simple-query-string-query.html
	 * @param q
	 * @return
	 */
	public static ESQueryBuilder simpleQueryStringQuery(String q) {
		Map must = new ArrayMap("simple_query_string", new ArrayMap(
				"query", q,
				"default_operator", "and"));
		return new ESQueryBuilder(must);		
	}

	/**
	 * See https://www.elastic.co/guide/en/elasticsearch/reference/7.5/query-dsl-prefix-query.html
	 * Index setup can speed these up!
	 * 
	 * @param field
	 * @param prefix
	 * @return
	 */
	public static ESQueryBuilder prefixQuery(String field, String prefix) {
		Map qmap = new ArrayMap("prefix", new ArrayMap(field, prefix));
		return new ESQueryBuilder(qmap);
	}

	/**
	 * See https://www.elastic.co/guide/en/elasticsearch/reference/7.9/query-dsl-match-query-phrase.html
	 * @param phrase
	 */
	public static ESQueryBuilder matchPhrase(String phrase) {
		Map qmap = new ArrayMap("match_phrase", new ArrayMap("message", phrase));
		return new ESQueryBuilder(qmap);
	}

}
