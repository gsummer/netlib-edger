package org.networklibrary.edger.parsing.StringStitch;

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
import org.networklibrary.core.types.EdgeTypes;

public class StringActionParser extends FileBasedParser<EdgeData> {

	protected static final Logger log = Logger.getLogger(StringLinkParser.class.getName());

	public static String SOURCE_NAME = "string_actions";

	private List<String> columns = null;

	private int cutoff = 0;
	private int speciesCode = -1;
	
	private Map<String, String> edgeTypes;
	
	public StringActionParser(){
		edgeTypes = new HashMap<String, String>();
		edgeTypes.put("binding", EdgeTypes.INTERACTS_WITH);
		edgeTypes.put("ptmod", EdgeTypes.MODIFIES_PROTEIN);
		edgeTypes.put("activation", EdgeTypes.ACTIVATES);
		edgeTypes.put("expression", EdgeTypes.REGULATES_TRANSCRIPTION);
		edgeTypes.put("reaction", EdgeTypes.IN_SAME_REACTION);
		edgeTypes.put("catalysis", EdgeTypes.CATALYZES);
	}

	public Collection<EdgeData> parse() throws ParsingErrorException {
		String line = readLine();
		List<EdgeData> res = null;

		if(!line.isEmpty()){
			res = new LinkedList<EdgeData>();

			String[] values = line.split("\\t",-1);

			if(values.length != columns.size()){
				throw new IllegalArgumentException("number of elements in row does not match number of columns " + line);
			}

			//	item_id_a	item_id_b	mode	action	a_is_acting	score	sources	transferred_sources

			int score = Integer.parseInt(values[6]);

			if(score > cutoff){

				Map<String,Object> props = new HashMap<String,Object>();

				//Only parse source attribute if actions.detailed available
				if(values.length > 7) {
					String oSource = values[8];
					if(oSource.isEmpty() || oSource == null){
						oSource = values[7];
					}
					props.put("orig_source", oSource);
				}

				if(!values[3].isEmpty()){
					props.put("action", values[3]);
				}

				props.put("score", score);
				
				props.put("mode", values[2]);
				
				String type = getEdgeType(values[2]);
				
				props.put("data_source",SOURCE_NAME);
				if(speciesCode == -1 || (values[0].startsWith(speciesCode + ".") && values[1].startsWith(speciesCode + "."))) {
					String from = values[0].replaceFirst("[0-9]+\\.","");
					String to = values[1].replaceFirst("[0-9]+\\.","");
					
					res.add(new EdgeData(from,to,type,props));
					if("f".equals(values[4])){
						res.add(new EdgeData(to,from,type,props));
					}
				} else {
					log.info("Skipping line, species doesn't match " + speciesCode);
				}
			}

		}

		return res;
	}

	private String getEdgeType(String stringType) {
		String type = edgeTypes.get(stringType);
		if(type == null) {
			type = EdgeTypes.INTERACTS_WITH;
		}
		return type;
	}
	
	protected void parseHeader(String header) {
		columns = Arrays.asList(header.split("\\t",-1));
	}

	protected boolean hasHeader() {
		return true;
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
					cutoff = Integer.parseInt(values[1]);
					break;
				case "speciesCode":
					speciesCode = Integer.parseInt(values[1]);
					break;
				}
			}
		}
		log.info("using a cutoff of " + cutoff);
	}

}
