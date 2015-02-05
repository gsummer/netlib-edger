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

public class StringLinkParser extends FileBasedParser<EdgeData> {

	protected static final Logger log = Logger.getLogger(StringLinkParser.class.getName());

	public static String EDGE_TYPE = "ppi";
	public static String SOURCE_NAME = "string_links";

	private List<String> columns = null;

	protected int cutoff = 0;

	public StringLinkParser(){
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

			int combined_score = Integer.valueOf(values[values.length-1]);
			if(combined_score > cutoff){
				Map<String,Object> props = new HashMap<String,Object>();
				for(int i = 2; i < values.length; ++i){
					if(!values[i].isEmpty()){
						props.put(columns.get(i), Integer.valueOf(values[i]));
					}
				}
				props.put("data_source",SOURCE_NAME);
				String from = values[0].replace("9606.","");
				String to = values[1].replace("9606.","");

				// TODO really? only one?
				res.add(new EdgeData(from,to,EDGE_TYPE,props));
			}

		}

		return res;
	}

	protected void parseHeader(String header) {
		columns = Arrays.asList(header.split("\\s",-1));
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
				}
			}
		}

		log.info("using a cutoff of " + cutoff);

	}
}
