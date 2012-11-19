/*
####################################################################################
Stephen W. Thomas
sthomas@cs.queensu.ca
Queen's University

IndexDirectory.java

(Invoked from command line, or via main() method.)

Indexes a directory, and garbles up an LDA execution for that directory (i.e., does not
run LDA itself.) See IndexDirectoryRunLDA.main() if you need to run LDA as well.

This class relies on SimpleIndexer for the dirty work. Here, we just parse command lines, 
create an LDAHelper instance, check if files exist, and then hand off to SimpleIndexer.

####################################################################################
 */

package ca.queensu.cs.sail.lucenelda;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

public class IndexDirectory {

	private static final Logger logger = Logger.getRootLogger();

	public static void main(String[] args) throws Exception {

		// Set up the Apache log4j logger, only if we need to (another class or test case or ant
		// may have already set up the logger.)
		if (!logger.getAllAppenders().hasMoreElements()) {
			BasicConfigurator.configure();
			logger.setLevel(Level.INFO);
		}

		
		// Use the JSAP library to intelligently set up and parse our command
		// line options
		JSAP jsap = new JSAP();

		UnflaggedOption opt1 = new UnflaggedOption("inDir").setStringParser(
				JSAP.STRING_PARSER).setRequired(true);
		opt1.setHelp("The input directory containing files on which to build the index");

		UnflaggedOption opt2 = new UnflaggedOption("outIndexDir").setStringParser(
				JSAP.STRING_PARSER).setRequired(true);
		opt2.setHelp("The output directory for the resultant Lucene index");
		
		UnflaggedOption opt2a = new UnflaggedOption("outLDAIndex").setStringParser(
				JSAP.STRING_PARSER).setRequired(true);
		opt2a.setHelp("The output directory for the resultant LDA index");

		FlaggedOption opt3 = new FlaggedOption("fileCodes")
				.setStringParser(JSAP.STRING_PARSER).setRequired(false)
				.setLongFlag("fileCodes");
		opt3.setHelp("A file containing a mapping between filenames and some key. "
				+ "If specified, the query results will list the key instead of the filename.");

		Switch sw0 = new Switch("help").setDefault("false").setLongFlag("help");
		sw0.setHelp("Prints this message.");

		FlaggedOption opt4 = new FlaggedOption("ldaConfig")
				.setStringParser(JSAP.STRING_PARSER).setRequired(false)
				.setList(true).setListSeparator(',')
				.setAllowMultipleDeclarations(true).setLongFlag("ldaConfig");
		opt4.setHelp("Add an LDA configuration in pairs of \"K,dirName\". "
				+ "E.g.: \"32,input/ldaOutput\" (without quotes). Can do this multiple times, one for each configuration.");

		jsap.registerParameter(sw0);
		jsap.registerParameter(opt1);
		jsap.registerParameter(opt2);
		jsap.registerParameter(opt2a);
		jsap.registerParameter(opt3);
		jsap.registerParameter(opt4);

		// check whether the command line was valid, and if it wasn't,
		// display usage information and exit.
		JSAPResult config = jsap.parse(args);
		if (!config.success()) {
			for (java.util.Iterator errs = config.getErrorMessageIterator(); errs
					.hasNext();) {
				System.err.println("Error: " + errs.next());
			}
			displayHelp(config, jsap);
			return;
		}

		if (config.getBoolean("help")) {
			displayHelp(config, jsap);
			return;
		}

		String inDirName  		= config.getString("inDir");
		String outDirName 		= config.getString("outIndexDir");
		String outLDAIndexName 	= config.getString("outLDAIndex");

		// Make sure the specified input directory exists
		if (!(new File((inDirName)).exists())) {
			logger.error("Error: " + inDirName + " does not exist.");
			return;
		}

		// If the output directory already exists, remove it (or else Lucene
		// will complain later)
		File indexDir = new File(outDirName);
		if (indexDir.exists()) {
			logger.info("Deleting index directory " + indexDir.toString());
			FileUtils.deleteDirectory(indexDir);
		}

		// Read the fileCodes, if there is one
		if (config.getString("fileCodes") != null) {
			// TODO: this functionality currently is placed in SimpleIndexer. Is that
			// the best place for it? Maybe we should move it here?
		}

		// Now, for each LDA config on the command line. Since the command line
		// has the form
		// K,dirName, then we need to get the string array (from the JSAP
		// config) and treat them as pairs.
		LDAHelper ldaHelper = new LDAHelper();
		String ldas[] = config.getStringArray("ldaConfig");
		for (int i = 0; i < ldas.length; i += 2) {
			int thisK = Integer.parseInt(ldas[i]);
			String thisInDirName = ldas[i + 1];
			ldaHelper.addScenario(thisK, thisInDirName);
		}
		
		// Serialize the LDAHelper object
		try{
			FileOutputStream fos    = new FileOutputStream(outLDAIndexName);
			ObjectOutputStream out  = new ObjectOutputStream(fos);
			out.writeObject(ldaHelper);
			out.close();
		}
		catch(IOException ex){
			ex.printStackTrace();
			return;
		}

		// Build the index with the options specified.
		SimpleIndexer.indexDirectory(inDirName, outDirName,
				config.getString("fileCodes"), ldaHelper);

		logger.info("Done indexing directory");
	}

	/* Use JSAP to display command-line usage information */
	private static void displayHelp(JSAPResult config, JSAP jsap) {
		System.err.println();
		System.err.println("Usage: java " + IndexDirectory.class.getName());
		System.err.println("                " + jsap.getUsage());
		System.err.println();
		System.err.println(jsap.getHelp());
		System.err.println();
	}
}
