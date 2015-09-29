package org.networklibrary.edger.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.networklibrary.core.parsing.FileBasedParser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.types.EdgeData;
import org.networklibrary.core.types.EdgeTypes;

public class MicroTCDSParser extends FileBasedParser<EdgeData> {

	protected static final Logger log = Logger.getLogger(MirDBParser.class.getName());

	public static String EDGE_TYPE = EdgeTypes.MIRNA_TARGETS;
	public static String SOURCE_NAME = "microTCDS";


	protected double cutoff = 0.7;
	protected Set<String> organisms = new HashSet<String>();

	private String carryOver = null;


	@Override
	public Collection<EdgeData> parse() throws ParsingErrorException {

		List<EdgeData> res = new ArrayList<EdgeData>();

		Map<String,Object> props = new HashMap<String,Object>();
		props.put("data_source",SOURCE_NAME);

		String line = null;

		// should cover the very first line
		if(line == null && carryOver == null){
			line = readLine();
		} 

		// there should be a carryover
		if(carryOver != null){
			line = carryOver;
			carryOver = null;
		}

		if(line != null && !line.isEmpty()){
			// parse the interaction line
			String[] values = line.split(",",-1);

			String to 	= values[1].substring(0, findBracket(values[1]));
			String from = values[2].substring(0, findBracket(values[2]));

			double score = Double.valueOf(values[3]);
			props.put("miTG", score);

			if(score >= cutoff && organisms.contains(from.substring(0, 3))){
				res.add(new EdgeData(from,to,EDGE_TYPE,props));
			}
		}


		// next we read over the CDS and UTR lines
		// need to check for line != null because this loop will hit the EOF
		do{
			line = readLine();
		} while(line != null && (line.startsWith("UTR") || line.startsWith("CDS")));

		// if there is a line (should be an interaction) then save it as carryOver
		if(line != null && !line.isEmpty()){
			carryOver = line;
		}

		return res;
	}

	protected int findBracket(String str){
		return str.indexOf("(");
	}

	@Override
	public boolean hasExtraParameters() {

		return true;
	}

	@Override
	public void takeExtraParameters(List<String> extras) {
		log.info("processing extra parameters: " + extras);
		if(extras != null) {
			for(String extra : extras){
				String values[] = extra.split("=",-1);

				switch(values[0]) {
				case "cutoff":
					cutoff = Double.parseDouble(values[1]);
					break;

				case "organism":
					organisms.add(values[1]);
					break;
				}
			}
		}

		log.info("using organisms:"  + organisms);
		log.info("using a cutoff of " + cutoff);

	}

	@Override
	protected boolean hasHeader() {
		return true;
	}

	@Override
	protected void parseHeader(String header) {
		// nothing i actually need	
	}

	@Override
	public boolean ready() throws ParsingErrorException {
		return super.ready() || carryOver != null;
	}
}
