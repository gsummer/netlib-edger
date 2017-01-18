package org.networklibrary.edger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.networklibrary.core.parsing.Parser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.storage.StorageEngine;
import org.networklibrary.core.types.EdgeData;
import org.networklibrary.edger.config.EdgerConfigManager;
import org.networklibrary.edger.parsing.DgiDbParser;
import org.networklibrary.edger.parsing.DisgenetParser;
import org.networklibrary.edger.parsing.EncodeParser;
import org.networklibrary.edger.parsing.GmtParser;
import org.networklibrary.edger.parsing.MetaAnalysisParser;
import org.networklibrary.edger.parsing.MicroTCDSParser;
import org.networklibrary.edger.parsing.MirDBParser;
import org.networklibrary.edger.parsing.MirTarBaseParser;
import org.networklibrary.edger.parsing.TabFileParser;
import org.networklibrary.edger.parsing.TargetScanParser;
import org.networklibrary.edger.parsing.TargetScanSitesParser;
import org.networklibrary.edger.parsing.TfeParser;
import org.networklibrary.edger.parsing.StringStitch.StitchActionsParser;
import org.networklibrary.edger.parsing.StringStitch.StitchChemChemParser;
import org.networklibrary.edger.parsing.StringStitch.StitchProteinChemParser;
import org.networklibrary.edger.parsing.StringStitch.StringActionParser;
import org.networklibrary.edger.parsing.StringStitch.StringLinkParser;
import org.networklibrary.edger.parsing.gpml.GpmlParser;
import org.networklibrary.edger.storage.EdgeStorageEngine;

public class EdgeImporter {

	protected static final Logger log = Logger.getLogger(EdgeImporter.class.getName());

	private static Map<String,Class> parsers = new HashMap<String,Class>();
	private static Map<String,String> supported = new HashMap<String,String>();
	static {
		addParser("STRING_Links","STRING Links",StringLinkParser.class);
		addParser("STRING_Actions","STRING Actions",StringActionParser.class);
		addParser("ENCODE","Encode Proximal or Distal",EncodeParser.class);
		addParser("MIRTARBASE","miRTarBase miRNA targeting", MirTarBaseParser.class);
		addParser("TARGETSCAN", "TargetScan (requires miR family file via -x)", TargetScanParser.class);
		addParser("TARGETSCANSITES", "TargetScan Conserved Sites import",TargetScanSitesParser.class);
		addParser("TFE","TFe import (requires a dummy filename) from the website", TfeParser.class);
		addParser("STITCHACTIONS", "STITCH Actions", StitchActionsParser.class);
		addParser("STITCHCHEMCHEM", "STITCH Chem Chem Interactions", StitchChemChemParser.class);
		addParser("STITCHPROTCHEM", "STITCh Protein Chem Interactions",StitchProteinChemParser.class);
		addParser("METAANALYSIS", "MetaAnalysis graph", MetaAnalysisParser.class);
		addParser("TAB","Tab files", TabFileParser.class);
		addParser("WP", "WikiPathway GPML", GpmlParser.class);
		addParser("DGN", "Disgenet Gene-Disease Associations",DisgenetParser.class);
		addParser("MIRDB", "miRDB", MirDBParser.class);
		addParser("MICROTCDS", "microT CDS from DIANA", MicroTCDSParser.class);
		addParser("GMT", "GMT file importer", GmtParser.class);
		addParser("DGIDB","DGIdb interactions importer",DgiDbParser.class);
	}

	public static void addParser(String cmd, String name, Class parser){
		parsers.put(cmd,parser);
		supported.put(cmd, name);
	}
	
	private String db;
	private List<String> fileLocs;
	private EdgerConfigManager confMgr;
	private List<String> extras;

	public EdgeImporter(String db, List<String> fileLocs,EdgerConfigManager confMgr, List<String> extras) {
		this.db = db;
		this.fileLocs = fileLocs;
		this.confMgr = confMgr;
		this.extras = extras;
	}

	public void execute() throws ParsingErrorException {

		log.info("connecting to db: " + getDb());

		if(getDb() == null || getDb().isEmpty()){
			log.severe("no db supplied!");
			return;
		}
		
		GraphDatabaseService g = new GraphDatabaseFactory().newEmbeddedDatabase(getDb());

		StorageEngine<EdgeData> se = new EdgeStorageEngine(g,confMgr);

		long start = System.nanoTime();

		for(String fileLoc : getFileLocations()){
			try {
				Parser<EdgeData> p = makeParser();

				if(p.hasExtraParameters())
					p.takeExtraParameters(extras);

				p.setDataSource(fileLoc);

				if(p != null){
					while(p.ready()){
						se.storeAll(p.parse());
					}
				}

				se.finishUp();

				long end = System.nanoTime();
				long elapsed = end - start;
				log.info("finished " + fileLoc + " in " + (elapsed/1000000000));
			} catch (ParsingErrorException e){
				log.severe("error during parsing of location="+fileLoc+ ": " + e.getMessage());
				g.shutdown();
				throw e;
			}
		}
		g.shutdown();
	}

	protected Parser<EdgeData> makeParser() throws ParsingErrorException {
		Parser<EdgeData> p = null;

		try {
			log.info("Have type = " + getType() + " -> parser = " + parsers.get(getType()));		
			p = (Parser<EdgeData>)getParsers().get(getType()).newInstance();
			p.setDictionary(confMgr);
		} catch (InstantiationException e) {
			log.severe("InstantiationException when creating parser for: " + getType() + ": " + e.getMessage());
			throw new ParsingErrorException(e.getMessage());
		} catch (IllegalAccessException e) {
			log.severe("IllegalAccessException when creating parser for: " + getType() + ": " + e.getMessage());
			throw new ParsingErrorException(e.getMessage());
		}

		return p;
	}

	protected String getType() {
		return getConfig().getType();
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

	protected EdgerConfigManager getConfig() {
		return confMgr;
	}

	protected Map<String,Class> getParsers(){
		return parsers;
	}
}
