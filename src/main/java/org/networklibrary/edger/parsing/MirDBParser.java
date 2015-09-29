package org.networklibrary.edger.parsing;

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
import org.networklibrary.core.types.EdgeTypes;

public class MirDBParser extends FileBasedParser<EdgeData> {

	protected static final Logger log = Logger.getLogger(MirDBParser.class.getName());

	public static String EDGE_TYPE = EdgeTypes.MIRNA_TARGETS;
	public static String SOURCE_NAME = "miRDB";


	protected double cutoff = 80.0;
	protected Set<String> organisms = new HashSet<String>();

	@Override
	public Collection<EdgeData> parse() throws ParsingErrorException {
		String line = readLine();
		List<EdgeData> res = null;

		if(!line.isEmpty()){
			res = new LinkedList<EdgeData>();

			String[] values = line.split("\\t",-1);

			double score = Double.valueOf(values[2]);

			String from = values[0];
			String to = values[1];

			if(organisms.contains(from.substring(0, 3))){
				if(score >= cutoff){
					Map<String,Object> props = new HashMap<String,Object>();

					props.put("score", score);
					props.put("data_source",SOURCE_NAME);

					// TODO really? only one?
					res.add(new EdgeData(from,to,EDGE_TYPE,props));
				}
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

		log.info("processing extra parameters: " + extras);
		if(extras != null) {
			for(String extra : extras){
				String values[] = extra.split("=",-1);

				switch(values[0]) {
				case "cutoff":
					cutoff = Double.valueOf(values[1]);
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
		return false;
	}

	@Override
	protected void parseHeader(String header) {
	}

}
