package org.networklibrary.edger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.networklibrary.core.config.ConfigManager;
import org.networklibrary.core.parsing.Parser;
import org.networklibrary.core.storage.StorageEngine;
import org.networklibrary.core.types.EdgeData;
import org.networklibrary.edger.parsing.StringLinkParser;
import org.networklibrary.edger.storage.EdgeStorageEngine;

public class EdgeImporter {
	private static Map<String,Class> parsers = new HashMap<String,Class>();
	private static Map<String,String> supported = new HashMap<String,String>();
	static {
		parsers.put("STRING", StringLinkParser.class);
		supported.put("STRING", "STRING Links");
	}

	private String db;
	private String type;
	private String fileLoc;
	private ConfigManager confMgr;

	public EdgeImporter(String db, String type, String fileLoc,ConfigManager confMgr) {
		this.db = db;
		this.type = type;
		this.fileLoc = fileLoc;
		this.confMgr = confMgr;
	}

	public void execute() throws IOException {

		System.out.println("connecting to db: " + getDb());
		
		GraphDatabaseService g = new RestGraphDatabase(db);
		
		StorageEngine<EdgeData> se = new EdgeStorageEngine(g,confMgr);

		long start = System.nanoTime();
		BufferedReader reader = new BufferedReader(new FileReader(getFileLocation()));

		Parser p = makeParser();
		
		if(p.hasHeader())
			p.parseHeader(reader.readLine());
		
		if(p != null){
			while(reader.ready()){
				String line = reader.readLine();
				se.storeAll(p.parse(line));
			}
		}
		
		reader.close();
		long end = System.nanoTime();
		long elapsed = end - start;
		System.out.println("finished " + getFileLocation() + " in " + (elapsed/1000000000));
		se.finishUp();

	}

	protected Parser makeParser(){
		Parser p = null;

		try {
			System.out.println(getType());
			System.out.println(parsers);

			p = (Parser)parsers.get(getType()).newInstance();
		} catch (InstantiationException e) {
			System.err.println("error during parser setup: " + getType());
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.err.println("error during parser setup: " + getType());
			e.printStackTrace();
		}

		return p;
	}

	protected String getType() {
		return type;
	}

	protected String getFileLocation() {
		return fileLoc;
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
	
	

}
