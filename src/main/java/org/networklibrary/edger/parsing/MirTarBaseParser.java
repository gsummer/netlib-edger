package org.networklibrary.edger.parsing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.networklibrary.core.parsing.FileBasedParser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.types.EdgeData;
import org.networklibrary.core.types.EdgeTypes;

public class MirTarBaseParser extends FileBasedParser<EdgeData> {
	public final static String EDGE_TYPE = EdgeTypes.MIR_TARGETING;
	public final static String SOURCE_NAME = "miRTarBase";

	private String carryOver = null;

	private List<String> columns = null;

	@Override
	public Collection<EdgeData> parse() throws ParsingErrorException {

		List<EdgeData> res = null;
		
		Map<String,Object> props = new HashMap<String,Object>();
		props.put("data_source",SOURCE_NAME);
		
		String from = null;
		String to = null;
		
		String currMirId = null;
		String lineMirId = null;

		
		ArrayList<String> experiments = new ArrayList<String>();
		ArrayList<String> supportTypes = new ArrayList<String>();
		ArrayList<String> publications = new ArrayList<String>();
		
		String line = null;
		
		do {
			if(carryOver != null){
				line = carryOver;
				carryOver = null;
			}
			else {
				line = readLine();
			}
	
			if(line != null && !line.isEmpty()){
				if(res == null){
					res = new LinkedList<EdgeData>();
				}

				String[] values = line.split("\\t",-1);

				lineMirId = values[0];
				
				if(currMirId == null){
					currMirId = lineMirId;
				}
				
				if(!currMirId.equals(lineMirId)){
					break;
				}
				

				from = values[1];
				to = values[4];

				experiments.add(values[6]);
				supportTypes.add(values[7]);
				publications.add(values[8]);
				
			}
//			line = readLine();
		} while(currMirId.equals(lineMirId) && line != null);

		props.put(columns.get(6), experiments.toArray(new String[experiments.size()]));
		props.put(columns.get(7), supportTypes.toArray(new String[supportTypes.size()]));
		props.put(columns.get(8), publications.toArray(new String[publications.size()]));
		props.put(columns.get(0), currMirId);
		
		res.add(new EdgeData(from, to, EDGE_TYPE, props));

		if(line != null && !line.isEmpty()){
			carryOver = line;
		}
		
		return res;
	}

	protected void putProp(String key, String value,Map<String,Object> props){
		if(!props.containsKey(key)){
			props.put(key, new HashSet<String>());
		}
		((Set<String>)props.get(key)).add(value);
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
		return true;
	}
	
	@Override
	protected void parseHeader(String header) {
		columns = Arrays.asList(header.split("\\t",-1));
	}
	
	@Override
	public boolean ready() throws ParsingErrorException {
		return super.ready() || carryOver != null;
	}
}
