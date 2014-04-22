package org.networklibrary.edger.parsing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.networklibrary.core.parsing.Parser;
import org.networklibrary.core.types.EdgeData;

public class TargetScanParser implements Parser<EdgeData> {
	protected static final Logger log = Logger.getLogger(TargetScanParser.class.getName());

	public final static String EDGE_TYPE = "miR-targeting";
	public final static String SOURCE_NAME = "TargetScan";
	
	private List<String> columns = null;
	private Map<String,List<String>> miRFamilies = null;

	@Override
	public boolean hasHeader() {
		return true;
	}

	@Override
	public void parseHeader(String header) {
		columns = Arrays.asList(header.split("\\t",-1));
	}

	@Override
	public Collection<EdgeData> parse(String line) {

		List<EdgeData> res = null;
		
		if(!line.isEmpty()){
		
			res = new LinkedList<EdgeData>();
			
			String[] values = line.split("\\t",-1);
			
			String froms = values[0];
			
			String to = values[1];
			
			Map<String,Object> props = new HashMap<String,Object>();
			for(int i = 4; i < values.length; ++i){
				if(!values[i].isEmpty()){
					props.put(columns.get(i), Integer.valueOf(values[i]));
				}
			}
			props.put("data_source",SOURCE_NAME);
			
			if(miRFamilies != null){
				for(String from : miRFamilies.get(froms)){
					res.add(new EdgeData(from, to, EDGE_TYPE, props));
				}
			} else {
				res.add(new EdgeData(froms, to, EDGE_TYPE, props));
			}
		
		}
		

		return res;
	}

	@Override
	public boolean hasExtraParameters() {
		return true;
	}

	@Override
	public void takeExtraParameters(List<String> extras) {
		String famFile = extras.get(0);
		log.info("using " + famFile + " as miRNA families file");
		
		try {
			miRFamilies = new HashMap<String,List<String>>();
			BufferedReader in = new BufferedReader(new FileReader(famFile));
			
			while(in.ready()){
				String line = in.readLine();
				String[] values = line.split("\t",-1);
				
				if(!miRFamilies.containsKey(values[0])){
					miRFamilies.put(values[0], new ArrayList<String>());
				}
				
				miRFamilies.get(values[0]).add(values[6]);
				
			}
			in.close();
		} catch (IOException e) {
			log.warning("failed to open miRNA families file " + famFile);
		}
		
	}

}
