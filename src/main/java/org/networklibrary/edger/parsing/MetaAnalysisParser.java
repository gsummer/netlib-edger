package org.networklibrary.edger.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.networklibrary.core.parsing.FileBasedParser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.types.EdgeData;

public class MetaAnalysisParser extends FileBasedParser<EdgeData>{

	protected static final Logger log = Logger.getLogger(TargetScanParser.class.getName());

	public final static String SOURCE_NAME = "MetaAnalysis";

	private List<String> columns = null;

	public MetaAnalysisParser()  {
	}

	@Override
	public Collection<EdgeData> parse() throws ParsingErrorException {

		String line = readLine();
		List<EdgeData> res = null;

		if(line != null && !line.isEmpty()){
			res = new ArrayList<EdgeData>();
			String[] values = line.split("\\t",-1);

			if(values.length != columns.size()){
				throw new IllegalArgumentException("number of elements in row does not match number of columns " + line);
			}

			Map<String,Object> props = new HashMap<String,Object>();
			props.put("data_source",SOURCE_NAME);

			//			from	to	type	intervention	species	quality	articleid	pmid

			for(int i = 3; i < values.length; ++i){
				if(!values[i].isEmpty()){
					props.put(columns.get(i), values[i]);
				}
			}

			res.add(new EdgeData(values[0], values[1], values[2], props));
//			System.out.println(line);
		}

		return res;
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
		//		columns = Arrays.asList(header.split("\\t",-1));
		columns = new ArrayList<String>();

		for(String colname : header.split("\\t",-1)){
			columns.add(checkDictionary(colname));
		}
	}

}
