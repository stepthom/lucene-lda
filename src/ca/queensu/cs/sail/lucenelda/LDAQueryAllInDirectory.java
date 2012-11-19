/*
####################################################################################
Stephen W. Thomas
sthomas@cs.queensu.ca
Queen's University

LDAQueryAllInDirectory.java

(Invoked from command line, or via main() method.)

This command-line class reads all queries in the given directory, and throws them against a
specified (prebuilt) index using LDA. The results are output in a given output directory. 
There are two options: K, and scoringCode.
See below for the specification.

####################################################################################
*/


package ca.queensu.cs.sail.lucenelda;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.PrintWriter;

import java.util.HashMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.payloads.AveragePayloadFunction;
import org.apache.lucene.search.payloads.PayloadTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.search.BooleanQuery;

import org.apache.commons.io.FileUtils;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

public class LDAQueryAllInDirectory {
	
	private static int maxHits = 500;
	private static IndexReader reader = null;
	private static IndexSearcher searcher = null;
	static LDAHelper lda = null;
	
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
		
		UnflaggedOption opt0a = new UnflaggedOption("LDAIndexDir").setStringParser(
				JSAP.STRING_PARSER).setRequired(true);
		opt0a.setHelp("The directory containing the pre-build LDA index.");

		UnflaggedOption opt1 = new UnflaggedOption("queryDir").setStringParser(
				JSAP.STRING_PARSER).setRequired(true);
		opt1.setHelp("The input directory containing queries to run against the specified index.");

		UnflaggedOption opt2 = new UnflaggedOption("resultsDir").setStringParser(
				JSAP.STRING_PARSER).setRequired(true);
		opt2.setHelp("The output directory for the results of each query: one file per original query in queryDirName.");

		FlaggedOption opt3 = new FlaggedOption("K")
				.setStringParser(JSAP.INTEGER_PARSER).setRequired(false)
				.setLongFlag("K").setDefault("0");
		opt3.setHelp("If multiple LDA configuration were run (i.e., multiple Ks), then specify which one to use."
				+ "Default: the lowest K.");
		
		FlaggedOption opt4 = new FlaggedOption("scoringCode")
		.setStringParser(JSAP.INTEGER_PARSER).setRequired(false)
		.setLongFlag("scoringCode").setDefault("1");
		opt4.setHelp("An integer code that specifies the scoring metric that should be used. "
		+ "1=conditional probability.");

		Switch sw0 = new Switch("help").setDefault("false").setLongFlag("help");
		sw0.setHelp("Prints this message.");

		jsap.registerParameter(sw0);
		jsap.registerParameter(opt0);
		jsap.registerParameter(opt0a);
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
		
		String indexDirName  	= config.getString("indexDir");
		String LDAIndexName		= config.getString("LDAIndexDir");
		String queryDirName  	= config.getString("queryDir");
		String resultsDirName 	= config.getString("resultsDir");
		int K             		= config.getInt("K");
		int scoringCode   		= config.getInt("scoringCode");

        // Make sure output directory exists
        File outDirF = new File(resultsDirName);
        if (!outDirF.exists()){
            outDirF.mkdirs();
        }
		
		// Open the serialized LDAHelper object
		FileInputStream fis  = null;
		ObjectInputStream in = null;
		try{
			fis = new FileInputStream(LDAIndexName);
			in = new ObjectInputStream(fis);
			lda = (LDAHelper)in.readObject();
			in.close();
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
		
		int idx = lda.which(K);
		
		// Open the index
		File indexDir = new File(indexDirName);
		Directory dir = NIOFSDirectory.open(indexDir);
		
		reader   = IndexReader.open(dir, true);
		searcher = new IndexSearcher(reader);

        BooleanQuery.setMaxClauseCount(8092);
		
		// Run every query in the directory!
		File queryDir = new File(queryDirName);
		File[] files  = queryDir.listFiles();
		for (int i = 0; i < files.length; ++i){
			File f = files[i];
			if (f.isDirectory() || f.isHidden() || !f.exists() || !f.canRead()){
				continue;
			}
			String query = FileUtils.readFileToString(f);

            // tmp hack: make sure query doesn't have numbers or punctuation
            query = query.replaceAll("\\^.", " ");
            query = query.replaceAll("[1234567890\\p{Punct}\\n]", " ");
			
			// Make sure the query isn't blank
			if (!query.matches("^\\s*$")){
				
				// Open the output file for results
				String outFile 		= resultsDirName + "/" + f.getName();
				FileWriter fwriter 	= new FileWriter(outFile);
				PrintWriter out 	= new PrintWriter(fwriter);
				
				logger.info("Executing query for " + f.toString() + "; results will be placed in " + outFile.toString());
				
				
				// First, we need to find all the topics in the query: for each term, find out all topics that contain this term;
				// Then, take the union of all in the topics of all the terms
				// Then, build the Boolean query below with multiple PayloadTermQuery()s
				float queryOpt[]    = new float[lda.scens.get(idx).K];
				BooleanQuery bquery = new BooleanQuery();
							
				// For each topic, sum up the scores of the words in the query
				String[] querySplitParts = query.split("\\s+");
				for (int k = 0; k < lda.scens.get(idx).K; ++k){
					float score = 0.0f;
					for (int j = 0; j<querySplitParts.length; ++j){
						if (lda.scens.get(idx).termMap.containsKey(querySplitParts[j])){ // Make sure the key exists in the map (it might not, due to vocab mismatch)
							int idx2 = lda.scens.get(idx).termMap.get(querySplitParts[j]);
							score = score + lda.scens.get(idx).phi[k][idx2];
						}
					}
					
					// Now, add this topic to the list, if the score is nonzero
					if (score > 0.01f){
						//System.out.printf("Adding topic %d with score %f\n", k, score);
						PayloadTermQuery fsq1 = new PayloadTermQuery(new Term("topicspayload"+lda.scens.get(idx).K, "p"+k), new AveragePayloadFunction(), false);
						bquery.add(fsq1, Occur.SHOULD);						
						queryOpt[k] = score;
					}
				}
				
				//logger.info("LDA query: " + bquery);

				// Actually execute the query
				TopDocs hits = searcher.search(bquery, maxHits);
				ScoreDoc[] scoreDocs = hits.scoreDocs;
				
				logger.info("Found " +hits.totalHits + " hits\n");
				
				// Save a filename -> docID mapping
				HashMap<String, Integer> hm = new HashMap<String, Integer>();
				for (int n = 0; n < scoreDocs.length; ++n) {
					ScoreDoc sd = scoreDocs[n];
					int docId = sd.doc;
					Document d = searcher.doc(docId);
					String fileName = d.get("file");
					hm.put(fileName,  docId);
				}
				
				// Rerank the results, based on the custom LDA scoring scheme
				HashMap<String, Float> sorted = lda.reRank(searcher, hits, queryOpt, lda.scens.get(idx).K);
			
				
				// Print our results
				int counter = 0;
                int numToOutput = Math.min(maxHits, sorted.size());
				for (String fileName : sorted.keySet()){
                    if (counter >= numToOutput){
                        break;
                    }
					float score      = sorted.get(fileName);
					int docId 		 = hm.get(fileName);
					Document d 		 = searcher.doc(docId);

					out.printf("%s,%4.3f\n", fileName, score);
					++counter;
				}
				out.close();
			}
		}
		
		// Close the index to save memory
		reader.close();
	}
	
	/* Use JSAP to display command-line usage information */
	private static void displayHelp(JSAPResult config, JSAP jsap) {
		System.err.println();
		System.err.println("Usage: java " + LDAQueryAllInDirectory.class.getName());
		System.err.println("                " + jsap.getUsage());
		System.err.println();
		System.err.println(jsap.getHelp());
		System.err.println();
	}
}
