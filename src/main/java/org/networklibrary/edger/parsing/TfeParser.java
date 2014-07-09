package org.networklibrary.edger.parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.networklibrary.core.parsing.Parser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.types.EdgeData;

public class TfeParser implements Parser<EdgeData> {

	protected static final Logger log = Logger.getLogger(TfeParser.class.getName());

	public final static String EDGE_TYPE = "TF_targeting";
	public final static String SOURCE_NAME = "TFe";
	
	static final String TFe_URL = "http://cisreg.cmmt.ubc.ca/cgi-bin/tfe/api.pl?";
	static final String ALL_CODES = TFe_URL + "code=all-tfids";
	static final String TARGETS = TFe_URL + "code=targets&tfid=";
	static final String ENSG = TFe_URL + "code=ensembl-gene-id&tfid=";

	private List<String> tfeIDs = null;
	private Iterator<String> currTFeID = null;
	private int tfeIDsDone = 0;
	private int percentile10 = 0;
	
	@Override
	public void setDataSource(String location) throws ParsingErrorException {
		// ignore what ever comes around (because as of now a dummy is required
		try {
			tfeIDs = Request.Get(ALL_CODES).execute().handleResponse(new TFeIDsResponseHandler());
		} catch (IOException e) {
			throw new ParsingErrorException("failure to test TFe",e);
		}
		
		if(tfeIDs == null){
			throw new ParsingErrorException("could not retrieve any TFe IDs");
		}
		
		percentile10 = tfeIDs.size() / 10;
		log.info("having " + tfeIDs.size() + " tfeIds to deal with");
		
		currTFeID = tfeIDs.iterator();
	}

	@Override
	public boolean ready() throws ParsingErrorException {
		return currTFeID.hasNext();
	}

	@Override
	public Collection<EdgeData> parse() throws ParsingErrorException {
		
		List<EdgeData> res = null;
		String tfeID = currTFeID.next();
		try {
			String from = Request.Get(ENSG + tfeID).execute().handleResponse(new EnsgResponseHandler());
			res = Request.Get(TARGETS + tfeID).execute().handleResponse(new TargetsResponseHandler(from));
			++tfeIDsDone;
			
			if(tfeIDsDone % percentile10 == 0){
				log.info("finished " + tfeIDsDone + " tfeids of " + tfeIDs.size());
			}
		} catch(IOException e){
			throw new ParsingErrorException("failed to retrieve ensg and targets for " + tfeID,e);
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

	public class TFeIDsResponseHandler implements ResponseHandler<List<String>> {

		@Override
		public List<String> handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {

			int code = response.getStatusLine().getStatusCode();

			if(code >= 200 && code <= 300){

				return Arrays.asList(IOUtils.toString(response.getEntity().getContent()).split("\n"));
			} else {
				return null;
			}
		}
	}
	
	public class EnsgResponseHandler implements ResponseHandler<String> {

		@Override
		public String handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {

			int code = response.getStatusLine().getStatusCode();

			if(code >= 200 && code <= 300){
				return IOUtils.toString(response.getEntity().getContent()).trim();
			} else {
				return null;
			}
		}
	}
	
	public class TargetsResponseHandler implements ResponseHandler<List<EdgeData>> {

		private String from;

		public TargetsResponseHandler(String from){
			this.from = from;
		}
		
		@Override
		public List<EdgeData> handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {

			int code = response.getStatusLine().getStatusCode();
			List<String> columns = new ArrayList<String>();
			columns.add("Entrez Gene ID");
			columns.add("Target gene symbol");
			columns.add("Acting TF complex");
			columns.add("Regulatory effect on target");
			columns.add("Pubmed ID");
			columns.add("Source (user or auto)");

			if(code >= 200 && code <= 300){
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				List<EdgeData> res = new LinkedList<EdgeData>();
				
				while(reader.ready()){
					String line = reader.readLine();
					String[] values = line.split("\t",-1);
					String to = values[0];
					
					Map<String,Object> props = new HashMap<String,Object>();
					for(int i = 2; i < values.length; ++i){
						if(!values[i].isEmpty()){
							props.put(columns.get(i), values[i]);
						}
					}
					props.put("data_source",SOURCE_NAME);
					
					res.add(new EdgeData(from, to, EDGE_TYPE, props));
				}
				
				return res;
			} else {
				return null;
			}
		}

	}
}
