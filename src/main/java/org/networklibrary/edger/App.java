package org.networklibrary.edger;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.edger.config.EdgerConfigManager;

/**
 * Hello world!
 *
 */
public class App
{
	protected static final Logger log = Logger.getLogger(App.class.getName());

	public static void main( String[] args )
	{
		Options options = new Options();
		Option help = OptionBuilder.withDescription("Help message").create("help");
		Option dbop = OptionBuilder.withArgName("[URL]").hasArg().withDescription("Neo4j instance to prime").withLongOpt("target").withType(String.class).create("db");
		Option typeop = OptionBuilder.withArgName("[TYPE]").hasArg().withDescription("Types available:").withType(String.class).create("t");
		Option parserClassesOp = OptionBuilder.withArgName("[CLASS:TYPE]").hasArg().withDescription("Additional parser classes to load (e.g. org.my.EdgeParser:MYPARSER").withLongOpt("parsers").withType(String.class).create("p");
		Option extraOps = OptionBuilder.hasArg().withDescription("Extra configuration parameters for the import").withType(String.class).create("x");

		Option newNodeOps = new Option("new_nodes",false,"unknown primary ids will create new nodes");
		
		Option dictionaryOp = OptionBuilder.hasArg().withDescription("Dictionary file to use").withLongOpt("dictionary").withType(String.class).create("d");
		

		options.addOption(help);
		options.addOption(dbop);
		options.addOption(typeop);
		options.addOption(extraOps);
		options.addOption(parserClassesOp);
		options.addOption(newNodeOps);
		options.addOption(dictionaryOp);

		CommandLineParser parser = new GnuParser();
		try {

			CommandLine line = parser.parse( options, args );

			if(line.hasOption("p")) {
				String[] classes = line.getOptionValue("p").split("\\s+");
				for(String cltype : classes) {
					String[] values = cltype.split(":", 2);
					try {
						EdgeImporter.addParser(values[1], "", Class.forName(values[0]));
					} catch(ClassNotFoundException e) {
						throw new ParsingErrorException("Class for custom parser not found", e);
					}
				}
			}
			if(line.hasOption("help") || args.length == 0){
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "netlib-edger [OPTIONS] [FILE]", options );
				System.out.println(EdgeImporter.printSupportedTypes());
				return;
			}

			String db = null;
			if(line.hasOption("db")){
				db = line.getOptionValue("db");
			}

			String type = null;
			if(line.hasOption("t")){
				type = line.getOptionValue("t");
			}

			List<String> extras = null;
			if(line.hasOption("x")){
				extras = Arrays.asList(line.getOptionValues("x"));
			}
			
			String dictionary = null;
			if(line.hasOption("d")){
				dictionary = line.getOptionValue("d");
			}

			boolean newNodes = line.hasOption("new_nodes");

			List<String> inputFiles = line.getArgList();

			EdgerConfigManager confMgr = new EdgerConfigManager(type,dictionary,newNodes);

			EdgeImporter ei = new EdgeImporter(db,inputFiles,confMgr,extras);

			ei.execute();

		}
		catch( ParseException exp ) {
			// oops, something went wrong
			exp.printStackTrace();
			System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
			System.exit(-1);
		} catch (ParsingErrorException e) {
			e.printStackTrace();
			System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
			System.exit(-2);
		}
	}
}
