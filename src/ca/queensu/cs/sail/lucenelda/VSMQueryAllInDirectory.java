/*
####################################################################################
Stephen W. Thomas
sthomas@cs.queensu.ca
Queen's University

VSMQueryAllInDirectory.java

(Invoked from command line, or via main() method.)

This command-line class reads all queries in the given directory, and throws them against a
specified (prebuilt) index using VSM. The results are output in a given output directory. 
There are two options: weightingCode and scoringCode.
See below for the specification.

####################################################################################
*/

package ca.queensu.cs.sail.lucenelda;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.search.BooleanQuery;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

public class VSMQueryAllInDirectory {

    // The maximum number of hits returned	
	private static int maxHits = 500;

    // All parts of the Lucene way of executing queries
	private static IndexReader   reader     = null;
	private static IndexSearcher searcher   = null;
	private static QueryParser   parser     = null;
	private static SimpleAnalyzer analyzer = null;

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
		
		UnflaggedOption opt0 = new UnflaggedOption("indexDir").setStringParser(
				JSAP.STRING_PARSER).setRequired(true);
		opt0.setHelp("The directory containing the pre-build Lucene index.");

		UnflaggedOption opt1 = new UnflaggedOption("queryDir").setStringParser(
				JSAP.STRING_PARSER).setRequired(true);
		opt1.setHelp("The input directory containing queries to run against the specified index.");

		UnflaggedOption opt2 = new UnflaggedOption("resultsDir").setStringParser(
				JSAP.STRING_PARSER).setRequired(true);
		opt2.setHelp("The output directory for the results of each query: one file per original query in queryDirName.");

		FlaggedOption opt3 = new FlaggedOption("weightingCode")
				.setStringParser(JSAP.INTEGER_PARSER).setRequired(false)
				.setLongFlag("weightingCode").setDefault("1");
		opt3.setHelp("An integer code that specifies the term weighting option that should be used. "
				+ "1=Linear, 2=Sublinear, 3=Boolean.");
		
		FlaggedOption opt4 = new FlaggedOption("scoringCode")
		.setStringParser(JSAP.INTEGER_PARSER).setRequired(false)
		.setLongFlag("scoringCode").setDefault("1");
		opt4.setHelp("An integer code that specifies the scoring metric that should be used. "
		+ "1=Cosine, 2=Overlap.");

		Switch sw0 = new Switch("help").setDefault("false").setLongFlag("help");
		sw0.setHelp("Prints this message.");

		jsap.registerParameter(sw0);
		jsap.registerParameter(opt0);
		jsap.registerParameter(opt1);
		jsap.registerParameter(opt2);
		jsap.registerParameter(opt3);
		jsap.registerParameter(opt4);

		// check whether the command line was valid, and if it wasn't,
		// display usage information and exit.
		JSAPResult config = jsap.parse(args);
		if (!config.success()) {
			for (java.util.Iterator errs = config.getErrorMessageIterator(); errs
					.hasNext();) {
				logger.error("Error: " + errs.next());
			}
			displayHelp(config, jsap);
			return;
		}

		if (config.getBoolean("help")) {
			displayHelp(config, jsap);
			return;
		}

		// Read in the command line parameters
		String indexDirName  	= config.getString("indexDir");
		String queryDirName  	= config.getString("queryDir");
		String resultsDirName 	= config.getString("resultsDir");
		int weightingCode 		= config.getInt("weightingCode");
		int scoringCode   		= config.getInt("scoringCode");

		// Make sure the output file exists
        File outDirF = new File(resultsDirName);
        if (!outDirF.exists()){
            outDirF.mkdirs();
        }

		// Set the weighting and scoring options, based on the user input
		VSMSimilarity vsmSimiliarty = new VSMSimilarity();
		switch (weightingCode){
		    case 1:
			    vsmSimiliarty.doBasic = true;
				break;
			case 2:
				vsmSimiliarty.doSublinear = true;
				break;
			case 3: 
				vsmSimiliarty.doBoolean = true;
				break;
            default:
                logger.error("Error: " + weightingCode + " is not a valid weighting code.");
			    return;
			}

		switch (scoringCode){
			case 1:
				vsmSimiliarty.doCosine = true;
				break;
			case 2:
				vsmSimiliarty.doOverlap = true;
				break;
            default:
            	logger.error("Error: " + scoringCode + " is not a valid scoring code.");
			    return;
		}

		
		// Open the index
		File indexDir = new File(indexDirName);
        if (!indexDir.isDirectory()){
        	logger.error("Error: " + indexDir.toString() + " is not a directory.");
		    return;
        }
		Directory dir = NIOFSDirectory.open(indexDir);

		// Open the index, and set up the required Lucene objects: readers, searchers, and analyzers
		logger.info("Reading the index");
		reader   = IndexReader.open(dir, true);
		searcher = new IndexSearcher(reader);
		analyzer = new SimpleAnalyzer();

		// We only need to search the "data" field, and we'll use a simple MultiFieldQuery
		Version v = Version.LUCENE_35;
		String[] fields = {"data"};
		parser = new MultiFieldQueryParser(v, fields, analyzer);

		// Don't want to return too many matches; this magic number could probably be reduced
		// to improve performance even more.
        BooleanQuery.setMaxClauseCount(8192);
		
		// Open the query directory, and run every query in the directory!
		File queryDir = new File(queryDirName);
        if (!queryDir.isDirectory()){
        	logger.error("Error: " + queryDir.toString() + " is not a directory.");
		    return;
        }

		File[] files  = queryDir.listFiles();
		for (int i = 0; i < files.length; ++i){
			File f = files[i];
			if (f.isDirectory() || f.isHidden() || !f.exists() || !f.canRead()){
				continue;
			}
			String query = FileUtils.readFileToString(f);

            // Make sure query doesn't have numbers or punctuation
            query = query.replaceAll("\\^.", " ");
            query = query.replaceAll("[1234567890\\p{Punct}\\n]", " ");
			
			// Skip blank queries
			if (!query.matches("^\\s*$")){
				
				// Build a simple query that says "match the text in the bug"
				Query q1 = parser.parse(query);
				
				// Execute the query with the .search method.
				searcher.setSimilarity(vsmSimiliarty);
				TopDocs hits = searcher.search(q1, maxHits);
				ScoreDoc[] scoreDocs = hits.scoreDocs;
				
				// Write the results to the output file
				String outFile = resultsDirName + "/" + f.getName();
				FileWriter fwriter = new FileWriter(outFile);
				PrintWriter out    = new PrintWriter(fwriter);
				
				logger.info("Executing query for " + f.toString() + "; results will be placed in " + outFile.toString());
				//logger.info("Found  " + scoreDocs.length + " matches.");
				
				int counter = 0;
				for (int n = 0; n < scoreDocs.length; ++n) {
					ScoreDoc sd = scoreDocs[n];
					float score = sd.score;
					int docId = sd.doc;
					Document d = searcher.doc(docId);
					String fileName = d.get("file");
					out.printf("%s,%4.3f\n", fileName, score);
					++counter;
				}
				out.close();
			}
		}
		
		// Close the index to save memory.
		reader.close();
		
	}
	
	/* Use JSAP to display command-line usage information */
	private static void displayHelp(JSAPResult config, JSAP jsap) {
		System.err.println();
		System.err.println("Usage: java " + VSMQueryAllInDirectory.class.getName());
		System.err.println("                " + jsap.getUsage());
		System.err.println();
		System.err.println(jsap.getHelp());
		System.err.println();
	}
}
