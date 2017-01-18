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

public class StitchActionsParser extends FileBasedParser<EdgeData> {
	public static String SOURCE_NAME = "stitch_actions";

	protected static final Logger log = Logger.getLogger(StitchActionsParser.class.getName());

	private List<String> columns = null;

	private String organism = null;

	public StitchActionsParser(){
	}

	public Collection<EdgeData> parse() throws ParsingErrorException {
		String line = readLine();
		List<EdgeData> res = null;

		if(!line.isEmpty()){
			res = new LinkedList<EdgeData>();

			String[] values = line.split("\\s",-1);

			if(values.length != columns.size()){
				throw new IllegalArgumentException("number of elements in row does not match number of columns " + line);
			}

			//			item_id_a       item_id_b       mode    action  a_is_acting     score
			
			if(organism == null || (values[0].startsWith(organism) && values[1].startsWith(organism))){
				
				Map<String,Object> props = new HashMap<String,Object>();

				if(!values[3].isEmpty()){
					props.put("action", values[3]);
				}

				props.put("score", Integer.parseInt(values[5]));
				props.put("data_source",SOURCE_NAME);

				
				String from = values[0].replaceFirst("[0-9]+\\.", "");
				String to = values[1].replaceFirst("[0-9]+\\.", "");

				res.add(new EdgeData(from,to,values[2],props));
				if("0".equals(values[4]) && !from.equals(to)){
					res.add(new EdgeData(to,from,values[2],props));
				}
			}

		}

		return res;
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

		if(extras != null) {
			for(String extra : extras){
				String values[] = extra.split("=",-1);

				switch(values[0]) {
				case "organism":
					organism = values[1];
					break;
				}
			}
		}

		log.info("using a organism of " + organism);
	}
}