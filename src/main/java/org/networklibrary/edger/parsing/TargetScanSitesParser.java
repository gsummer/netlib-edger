package org.networklibrary.edger.parsing;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.networklibrary.core.parsing.FileBasedParser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.types.EdgeData;
import org.networklibrary.core.types.EdgeTypes;

public class TargetScanSitesParser extends FileBasedParser<EdgeData> {

	public final static String EDGE_TYPE = EdgeTypes.MIRNA_TARGETS;
	public final static String SOURCE_NAME = "TargetScanSites";
	
	private List<String> columns = null;
		
	@Override
	protected boolean hasHeader() {
		return true;
	}

	@Override
	protected void parseHeader(String header) {
		columns = Arrays.asList(header.split("\\t",-1));
	}

	@Override
	public Collection<EdgeData> parse() throws ParsingErrorException {
		
		String line = readLine();
		List<EdgeData> res = null;
		
		if(!line.isEmpty()){
		
			res = new LinkedList<EdgeData>();
			
			String[] values = line.split("\\t",-1);
			
			String from = values[0];
			
			String to = values[4];
			
			Map<String,Object> props = new HashMap<String,Object>();
			for(int i = 5; i < values.length; ++i){
				if(!values[i].isEmpty()){
					props.put(columns.get(i), Integer.valueOf(values[i]));
				}
			}
			props.put("data_source",SOURCE_NAME);
			

			res.add(new EdgeData(from, to, EDGE_TYPE, props));
		
		}
		return res;
	}

	@Override
	public boolean hasExtraParameters() {
		return false;
	}

	@Override
	public void takeExtraParameters(List<String> extras) {	
	}

}
