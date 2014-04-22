package org.networklibrary.edger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.networklibrary.core.config.ConfigManager;
import org.networklibrary.core.parsing.Parser;
import org.networklibrary.core.storage.StorageEngine;
import org.networklibrary.core.types.EdgeData;
import org.networklibrary.edger.parsing.StringStitch.StringActionParser;
import org.networklibrary.edger.parsing.StringStitch.StringLinkParser;
import org.networklibrary.edger.storage.EdgeStorageEngine;

public class EdgeImporter {

	protected static final Logger log = Logger.getLogger(EdgeImporter.class.getName());

	private static Map<String,Class> parsers = new HashMap<String,Class>();
	private static Map<String,String> supported = new HashMap<String,String>();
	static {
		parsers.put("STRING_Links", StringLinkParser.class);
		parsers.put("STRING_Actions", StringActionParser.class);
		supported.put("STRING_Links", "STRING Links");
		supported.put("STRING_Actions", "STRING Actions");
	}

	private String db;
	private String type;
	private List<String> fileLocs;
	private ConfigManager confMgr;
	private List<String> extras;

	public EdgeImporter(String db, String type, List<String> fileLocs,ConfigManager confMgr, List<String> extras) {
		this.db = db;
		this.type = type;
		this.fileLocs = fileLocs;
		this.confMgr = confMgr;
		this.extras = extras;
	}

	public void execute() throws IOException {

		log.info("connecting to db: " + getDb());

		GraphDatabaseService g = new RestGraphDatabase(db);

		StorageEngine<EdgeData> se = new EdgeStorageEngine(g,confMgr);

		long start = System.nanoTime();

		for(String fileLoc : getFileLocations()){
			BufferedReader reader = new BufferedReader(new FileReader(fileLoc));

			Parser<EdgeData> p = makeParser();

			if(p.hasHeader())
				p.parseHeader(reader.readLine());
			
			if(p.hasExtraParameters())
				p.takeExtraParameters(extras);

			if(p != null){
				while(reader.ready()){
					String line = reader.readLine();
					se.storeAll(p.parse(line));
				}
			}

			se.finishUp();
			reader.close();
			long end = System.nanoTime();
			long elapsed = end - start;
			log.info("finished " + fileLoc + " in " + (elapsed/1000000000));
		}
	}

	protected Parser<EdgeData> makeParser(){
		Parser<EdgeData> p = null;

		try {
			log.info("Have type = " + getType() + " -> parser = " + parsers.get(getType()));		
			p = (Parser<EdgeData>)getParsers().get(getType()).newInstance();
		} catch (InstantiationException e) {
			log.severe("InstantiationException when creating parser for: " + getType() + ": " + e.getMessage());
		} catch (IllegalAccessException e) {
			log.severe("IllegalAccessException when creating parser for: " + getType() + ": " + e.getMessage());
		}

		return p;
	}

	protected String getType() {
		return type;
	}

	protected List<String> getFileLocations() {
		return fileLocs;
	}

	protected String getDb() {
		return db;
	}

	public static String printSupportedTypes() {
		StringBuilder buff = new StringBuilder();

		for(Entry<String,Class> p : parsers.entrySet() ){
			buff.append("\t" + p.getKey() + " = " + supported.get(p.getKey()));
			buff.append("\n");
		}

		return buff.toString();
	}

	protected ConfigManager getConfMgr() {
		return confMgr;
	}
	
	protected Map<String,Class> getParsers(){
		return parsers;
	}
}
