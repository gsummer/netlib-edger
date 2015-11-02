package org.networklibrary.edger.parsing;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.networklibrary.core.parsing.FileBasedParser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.types.EdgeData;

public class DgiDbParser extends FileBasedParser<EdgeData> {

	protected static final Logger log = Logger.getLogger(DisgenetParser.class.getName());

	public static String SOURCE_NAME = "DGIdb";
	public static String EDGE_TYPE = "drug_targeting";

	private List<String> columns = null;
	
	@Override
	public Collection<EdgeData> parse() throws ParsingErrorException {
		String line = readLine();
		List<EdgeData> res = null;

		if(line != null && !line.isEmpty()){
			res = new LinkedList<EdgeData>();

			String[] values = line.split("\\t",-1);

			if(values.length != columns.size()){
				throw new IllegalArgumentException("number of elements in row does not match number of columns " + line);
			}

			
			Map<String,Object> props = new HashMap<String,Object>();
			
			props.put("data_source",SOURCE_NAME);
			
			String from = values[5];
			String to = values[0];
			
			
			props.put(columns.get(2), values[2]);
			props.put(columns.get(4), values[4]);

			res.add(new EdgeData(from,to,EDGE_TYPE,props));
		}
		return res;
	}

	@Override
	public boolean hasExtraParameters() {
		return true;
	}

	@Override
	public void takeExtraParameters(List<String> extras) {
	}

	@Override
	protected boolean hasHeader() {
		return true;
	}

	@Override
	protected void parseHeader(String header) {
		columns = Arrays.asList(header.split("\\t",-1));
		
	}

}
