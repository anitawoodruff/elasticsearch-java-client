package com.winterwell.es.client;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.winterwell.es.ESTest;
import com.winterwell.utils.Dep;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.time.TUnit;

public class SearchRequestScrollOverAllTest extends ESTest {

	@Test
	public void testSetSize() {
		BulkRequestBuilderTest brbt = new BulkRequestBuilderTest();
		List<String> ids = brbt.testBulkIndexMany2();
		String index = BulkRequestBuilderTest.INDEX;
		// now search
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		SearchRequest s = esc.prepareSearch(index);
		s.setSize(6);
		SearchRequestScrollOverAll scroller = new SearchRequestScrollOverAll(esc, s, TUnit.MINUTE.dt);
		scroller.setSize(10);
		int total = 0;
		String out = "";
		for (List<Map> list : scroller) {
			total += list.size();
			out += list.size()+"\t"+total+"\n";
		}
		assert total == 10;
		out = StrUtils.compactWhitespace(out);
		assert out.equals("6 6 4 10") : out;
	}

}
