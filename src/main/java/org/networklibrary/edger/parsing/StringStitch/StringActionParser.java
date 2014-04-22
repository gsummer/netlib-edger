package org.networklibrary.edger.parsing.StringStitch;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.networklibrary.core.parsing.Parser;
import org.networklibrary.core.types.EdgeData;

public class StringActionParser implements Parser<EdgeData> {
	public static String SOURCE_NAME = "string_actions";
	
	private List<String> columns = null;
	
	public StringActionParser(){
	}
	
	public Collection<EdgeData> parse(String line) {
		List<EdgeData> res = null;
		
		if(!line.isEmpty()){
			res = new LinkedList<EdgeData>();
			
			String[] values = line.split("\\s",-1);
			
			if(values.length != columns.size()){
				throw new IllegalArgumentException("number of elements in row does not match number of columns " + line);
			}
			
//			item_id_a	item_id_b	mode	action	a_is_acting	score	sources	transferred_sources
			
			Map<String,Object> props = new HashMap<String,Object>();
			
			String oSource = values[7];
			if(oSource.isEmpty() || oSource == null){
				oSource = values[6];
			}
			
			if(!values[3].isEmpty()){
				props.put("action", values[3]);
			}
			
			props.put("score", Integer.parseInt(values[5]));
			props.put("orig_source", oSource);
			
			props.put("data_source",SOURCE_NAME);
			String from = values[0].replace("9606.","");
			String to = values[1].replace("9606.","");
			
			res.add(new EdgeData(from,to,values[2],props));
			if("0".equals(values[4])){
				res.add(new EdgeData(to,from,values[2],props));
			}
			
		}
		
		return res;
	}

	public void parseHeader(String header) {
		columns = Arrays.asList(header.split("\\t",-1));
	}

	public boolean hasHeader() {
		return true;
	}

	@Override
	public boolean hasExtraParameters() {
		return false;
	}

	@Override
	public void takeExtraParameters(List<String> extras) {
	}

}
