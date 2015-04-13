package org.networklibrary.edger.parsing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.networklibrary.core.parsing.FileBasedParser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.types.EdgeData;

public class TabFileParser extends FileBasedParser<EdgeData>{
	protected static final Logger log = Logger.getLogger(TabFileParser.class.getName());
	protected static final String DEFAULT_EDGETYPE = "pp";

	protected List<String> columns = null;
	protected boolean header = false;
	protected int typeCol = -1;
	protected String edgeType = null;
	protected String source = "unknown";
	protected String sep = "\t";
	protected int currI = 0;

	public boolean hasHeader() {
		return header;
	}

	public void parseHeader(String header) {
		columns = Arrays.asList(header.split(sep,-1));

		if(typeCol != -1)
			edgeType = columns.get(typeCol);
		
		if(edgeType == null)
			edgeType = DEFAULT_EDGETYPE;

	}

	@Override
	public boolean hasExtraParameters() {
		return true;
	}

	@Override
	public void takeExtraParameters(List<String> extras) {
		log.info("processing extra parameters: " + extras.toString());

		for(String extra : extras){
			String values[] = extra.split("=",-1);

			switch(values[0]) {
			case "header":
				header = Boolean.parseBoolean(values[1]);
				break;
			case "edgetype":
				if(isNumeric(values[1]))
					typeCol = Integer.parseInt(values[1]);
				else
					edgeType = values[1];

				break;
			case "source":
				this.source = values[1];
				break;
				
			case "sep":
				this.sep = values[1];
				break;
			}
		}

		log.info("using a header: " + header);
		log.info("source: " + source);

		if(typeCol != -1)
			log.info("typeCol = " + typeCol);

		if(edgeType != null)
			log.info("edgetype = " + edgeType);

	}

	@Override
	public Collection<EdgeData> parse() throws ParsingErrorException {
		List<EdgeData> res = null;

		String line = readLine();
		++currI;

		if(currI % 10000 == 0)
			System.out.println("at line: " + currI);

		if(line != null && !line.isEmpty()){
			res = new ArrayList<EdgeData>();
			
			if(line.charAt(0) == '#')
				return res;

			String[] values = line.split(sep,-1);

			Map<String,Object> props = new HashMap<String,Object>();
			props.put("data_source",source);

			for(int i = 2; i < values.length; ++i){
				if(i == typeCol)
					continue;

				String colname = null;
				if(header) {
					if(i >= columns.size()) {
						log.warning("More columns in line than in header: " + line);
						continue;
					}
					colname = columns.get(i);
				} else {
					colname = "col" + i;
				}

				if(isNumeric(values[i]))
					props.put(colname, Double.parseDouble(values[i]));
				else
					props.put(colname, values[i]);

			}
			String type = edgeType;
			if(typeCol > -1) {
				type = values[typeCol];
			}
			res.add(new EdgeData(values[0],values[1],type,props));

		}

		return res;
	}

	public static boolean isNumeric(String str)
	{
		return str.matches("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");  //match a number with optional '-' and decimal and scientific notation.
	}
}
