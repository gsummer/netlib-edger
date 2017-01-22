package org.networklibrary.edger.parsing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
import java.util.Scanner;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.networklibrary.core.config.Dictionary;
import org.networklibrary.core.parsing.Parser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.types.EdgeData;
import org.networklibrary.core.types.EdgeTypes;

public class TfeParser implements Parser<EdgeData> {

	protected static final Logger log = Logger.getLogger(TfeParser.class.getName());

	public final static String SOURCE_NAME = "TFe";
	
	static final String TFe_URL = "http://cisreg.cmmt.ubc.ca/cgi-bin/tfe/api.pl?";
	static final String ALL_CODES = TFe_URL + "code=all-tfids";
	static final String TARGETS = TFe_URL + "code=targets&tfid=";
	static final String ENSG = TFe_URL + "code=ensembl-gene-id&tfid=";

	private List<String> tfeIDs = null;
	private Iterator<String> currTFeID = null;
	private int tfeIDsDone = 0;
	private int percentile10 = 0;
	private String cachePath = null;
	private Dictionary dictionary = null;
	
	@Override
	public void setDataSource(String location) throws ParsingErrorException {
		// ignore what ever comes around (because as of now a dummy is required
		try {
			boolean isCache = cachePath != null && new File(cachePath, "alltfeids").exists();
			if(!isCache || !new File(cachePath, "alltfeids").exists()) {
				log.info("Loading all TFe IDs");
				tfeIDs = Request.Get(ALL_CODES).execute().handleResponse(new TFeIDsResponseHandler());
				if(cachePath != null) {
					BufferedWriter w = new BufferedWriter(new FileWriter(new File(cachePath, "alltfeids")));
					w.write(StringUtils.join(tfeIDs, "\n"));
					w.close();
				}
			} else {
				log.info("Using cached TFe IDs");
				tfeIDs = new ArrayList<String>();
				Scanner s = new Scanner(new File(cachePath, "alltfeids"));
				while (s.hasNext()){
				    tfeIDs.add(s.next());
				}
				s.close();
			}
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
			boolean isCache = cachePath != null && new File(cachePath, tfeID).exists();
			
			String from = "";
			if(!isCache) {
				from = Request.Get(ENSG + tfeID).execute().handleResponse(new EnsgResponseHandler());
				if(cachePath != null) {
					BufferedWriter w = new BufferedWriter(new FileWriter(new File(cachePath, tfeID + ".from")));
					w.write(from);
					w.close();
				}
				res = Request.Get(TARGETS + tfeID).execute().handleResponse(new TargetsResponseHandler(tfeID, from));
			} else {
				File fromFile = new File(cachePath, tfeID + ".from");
				if(!fromFile.exists()) {
					from = Request.Get(ENSG + tfeID).execute().handleResponse(new EnsgResponseHandler());
					BufferedWriter w = new BufferedWriter(new FileWriter(new File(cachePath, tfeID + ".from")));
					w.write(from);
					w.close();
				} else {
					BufferedReader r = new BufferedReader(new FileReader(fromFile));
					from = r.readLine();
					r.close();
				}
				res = parseTF(tfeID, from, FileUtils.readFileToString(new File(cachePath, tfeID)), false);
			}

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
		return true;
	}

	@Override
	public void takeExtraParameters(List<String> extras) {
		log.info("processing extra parameters: " + extras);

		if(extras != null) {
			for(String extra : extras){
				String values[] = extra.split("=",-1);
	
				switch(values[0]) {
				case "cachepath":
					cachePath = values[1];
					new File(cachePath).mkdirs();
					break;
				}
			}
		}

		log.info("Caching API requests to " + cachePath);
	}

	private List<EdgeData> parseTF(String tfeID, String from, String content, boolean writeCache) throws IOException {
		List<String> columns = new ArrayList<String>();
		columns.add("Entrez Gene ID");
		columns.add("Target gene symbol");
		columns.add("Acting TF complex");
		columns.add("Regulatory effect on target");
		columns.add("Pubmed ID");
		columns.add("Source (user or auto)");

		List<EdgeData> res = new LinkedList<EdgeData>();
		
		if(writeCache && cachePath != null) {
			FileUtils.writeStringToFile(new File(cachePath, tfeID), content);
		}
		
		if(!"".equals(content)) {
			for(String line : content.split("\n")) {
				String[] values = line.split("\t",-1);
				String to = values[0];
				
				Map<String,Object> props = new HashMap<String,Object>();
				for(int i = 2; i < values.length; ++i){
					if(!values[i].isEmpty()){
						props.put(columns.get(i), values[i]);
					}
				}
				props.put("data_source",SOURCE_NAME);
				
				String type = EdgeTypes.REGULATES_TRANSCRIPTION;
				if("UP-REGULATION".equals(values[columns.indexOf("Regulatory effect on target")])) {
					type = EdgeTypes.ACTIVATES_TRANSCRIPTION;
				}
				if("DOWN-REGULATION".equals(values[columns.indexOf("Regulatory effect on target")])) {
					type = EdgeTypes.INHIBITS_TRANSCRIPTION;
				}
				
				res.add(new EdgeData(from, to, type, props));
			}
		}
		
		return res;
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
		private String tfeID;
		
		public TargetsResponseHandler(String tfeID, String from){
			this.from = from;
			this.tfeID = tfeID;
		}
		
		@Override
		public List<EdgeData> handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {

			int code = response.getStatusLine().getStatusCode();
			if(code >= 200 && code <= 300){
				String content = EntityUtils.toString(response.getEntity());
				return parseTF(tfeID, from, content,true);
			} else {
				return null;
			}
		}

	}

	@Override
	public void setDictionary(Dictionary dictionary) {
		this.dictionary = dictionary;		
	}
}
