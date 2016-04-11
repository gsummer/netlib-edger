package org.networklibrary.edger.parsing;

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

public class DisgenetParser extends FileBasedParser<EdgeData> {

	protected static final Logger log = Logger.getLogger(DisgenetParser.class.getName());

	public static String SOURCE_NAME = "disgenet";
	public static String EDGE_TYPE = EdgeTypes.DGN_ASSOCIATION;
	
	private List<String> columns = null;

	protected double cutoff = 0;

	@Override
	public Collection<EdgeData> parse() throws ParsingErrorException {
		String line = readLine();
		List<EdgeData> res = null;

		if(!line.isEmpty()){
			res = new LinkedList<EdgeData>();

			String[] values = line.split("\\t",-1);

			if(values.length != columns.size()){
				System.out.println(line);
				throw new IllegalArgumentException("number of elements in row does not match number of columns " + line);
			}

			//	geneId	geneSymbol	geneName	diseaseId	diseaseName	score	NumberOfPubmeds	associationType	source

			double score = Double.valueOf(values[5]);
			if(score > cutoff){
				Map<String,Object> props = new HashMap<String,Object>();

				props.put("score",score);
				props.put(columns.get(6), Integer.valueOf(values[6]));
				props.put(columns.get(7), values[7]);
				String[] sources = values[8].split(", ",-1);
				
				props.put("associations", getAssociations(values[7]));
				props.put(columns.get(8), sources);
				props.put("data_source",SOURCE_NAME);

				res.add(new EdgeData(values[1],values[3],EDGE_TYPE,props));
			}

		}

		return res;
	}
	
	private String[] getAssociations(String disgenetType){
		return disgenetType.split(", ");
	}

//	private String getEdgeType(String disgenetType) {
//		switch(disgenetType) {
//		case "Biomarker":
//			return EdgeTypes.BIOMARKER_OF;
//		case "GeneticVariation":
//			return EdgeTypes.GENETIC_VARIATION_FOR;
//		case "PostTranslationalModification":
//			return EdgeTypes.POST_TRANSLATIONAL_MODIFICATION_IN;
//		case "AlteredExpression":
//			return EdgeTypes.ALTERED_EXPRESSION_IN;
//		case "Therapeutic":
//			return EdgeTypes.THERAPEUTIC_ROLE_IN;
//		}
//		
//		log.warning("Edge type for " + disgenetType + " not found!");
//		return disgenetType;
//	}
	
	@Override
	public boolean hasExtraParameters() {
		return true;
	}

	@Override
	public void takeExtraParameters(List<String> extras) {
		if(extras != null){
			log.info("processing extra parameters: " + extras.toString());

			for(String extra : extras){
				String values[] = extra.split("=",-1);

				switch(values[0]) {
				case "cutoff":
					cutoff = Double.parseDouble(values[1]);
					break;
				}
			}

			log.info("using a cutoff of " + cutoff);
		}
	}

	@Override
	protected boolean hasHeader() {
		return true;
	}

	@Override
	protected void parseHeader(String header) {
		columns = Arrays.asList(header.split("\\s",-1));	
	}

}
