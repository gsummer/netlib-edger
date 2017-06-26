package org.networklibrary.edger.parsing;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.networklibrary.core.parsing.FileBasedParser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.types.EdgeData;
import org.networklibrary.core.types.EdgeTypes;

public class EncodeParser extends FileBasedParser<EdgeData> {
	
	public final static String EDGE_TYPE = EdgeTypes.REGULATES_TRANSCRIPTION;
	public final static String SOURCE_NAME = "ENCODE";
	
	@Override
	public Collection<EdgeData> parse() throws ParsingErrorException {
		String line = readLine();
		List<EdgeData> res = null;
		
		if(line != null && !line.isEmpty()){
		
			res = new LinkedList<EdgeData>();
			
			String[] values = line.split(" ",-1); // proximal uses space
			if(values.length == 1) // distal uses tabs
				values = line.split("\t",-1); // encode sucks
	
			String from = "http://identifiers.org/hgnc.symbol/" + values[0];
			String to = "http://identifiers.org/hgnc.symbol/" + values[2];
			
			Map<String,Object> props = new HashMap<String,Object>();
			props.put("category", values[1]);
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

	@Override
	protected boolean hasHeader() {
		return false;
	}

	@Override
	protected void parseHeader(String header) {
	}

}
