package org.networklibrary.edger.parsing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.networklibrary.core.parsing.FileBasedParser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.types.EdgeData;

public class TargetScanParser extends FileBasedParser<EdgeData> {
	protected static final Logger log = Logger.getLogger(TargetScanParser.class.getName());

	public final static String EDGE_TYPE = "miR-targeting";
	public final static String SOURCE_NAME = "TargetScan";
	
	private List<String> columns = null;
	private Map<String,List<String>> miRFamilies = null;
	private Set<String> organisms = null;
	
	private String carryOver = null;
	
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

		List<EdgeData> res = null;
		
		Map<String,Object> props = new HashMap<String,Object>();
		props.put("data_source",SOURCE_NAME);
		
		String fromFam = null;
		String to = null;
		
		String currMirId = null;
		String lineMirId = null;
		
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

				lineMirId = values[0] + values[1];
				
				if(currMirId == null){
					currMirId = lineMirId;
				}
				
				if(!currMirId.equals(lineMirId)){
					break;
				}
				

				fromFam = values[0];
				to = values[1];
				
//				for(int i = 4; i < values.length; ++i){
//				if(!values[i].isEmpty()){
//					props.put(columns.get(i), Integer.valueOf(values[i]));
//				}
//			}

			}
//			line = readLine();
		} while(currMirId.equals(lineMirId) && line != null);


		if(miRFamilies != null){
			for(String from : miRFamilies.get(fromFam)){
				res.add(new EdgeData(from, to, EDGE_TYPE, props));
			}
		} else {
			res.add(new EdgeData(fromFam, to, EDGE_TYPE, props));
		}
		
		if(line != null && !line.isEmpty()){
			carryOver = line;
		}
		
		return res;
	}
	
	@Override
	public boolean ready() throws ParsingErrorException {
		return super.ready() || carryOver != null;
	}

	@Override
	public boolean hasExtraParameters() {
		return true;
	}

	@Override
	public void takeExtraParameters(List<String> extras) {
		log.info("processing extra parameters: " + extras.toString());
		String famFile = extras.get(0);
		log.info("using " + famFile + " as miRNA families file");
		
		if(extras.size() > 1){
			organisms =  new HashSet<String>();
			for(int i = 1; i < extras.size(); ++i){
				organisms.add(extras.get(i));
			}
		}
		
		try {
			miRFamilies = new HashMap<String,List<String>>();
			BufferedReader in = new BufferedReader(new FileReader(famFile));
			
			while(in.ready()){
				String line = in.readLine();
				String[] values = line.split("\t",-1);
				
				if(organisms != null && !organisms.contains(values[2])){
					continue;
				}
				
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
