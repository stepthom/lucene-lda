/*
####################################################################################
Stephen W. Thomas
sthomas@cs.queensu.ca
Queen's University

VSMQueryAllInDirectory.java

This class reads all queries in the given directory, and throws them against a
specified index using VSM. The results are output in a given output directory. 
There are two options: weightingCode and scoringCode.
See below for the specification.

To save typing on the command line, a directory structure is assumed:

"inDir": the base directory, whose name is passed to this class via command line
"inDir/bugs": the directory holding all the queries, one per file
"inDir/index": the directory holding the pre-built Lucene index

"outDir": the output directory, whose name is passed to this class via command line


####################################################################################
*/


package ca.queensu.cs.sail.doofuslarge;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

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
import org.apache.lucene.search.Explanation;

public class VSMQueryAllInDirectory {

    // The maximum number of hits returned	
	private static int maxHits = 500;

    // All parts of the Lucene way of executing queries
	private static IndexReader   reader     = null;
	private static IndexSearcher searcher   = null;
	private static QueryParser   parser     = null;
	private static SourceCodeAnalyzer analyzer = null;

	
	public static void main(String[] args) throws Exception {
		if (args.length != 4){
			System.err.println("Usage: VSMQueryAllInDirectory inDirName outDirName weightingCode scoringCode");
			System.err.println("weightingCode: 1=Linear, 2=Sublinear, 3=Boolean");
			System.err.println("scoringCode:   1=Cosine, 2=Overlap");
			return;
		}
		
		String inDir  = args[0];
		String outDir = args[1];
		int weightingCode = Integer.parseInt(args[2]);
		int scoringCode   = Integer.parseInt(args[3]);

        File outDirF = new File(outDir);
        if (!outDirF.exists()){
            outDirF.mkdir();
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
                System.err.println("Error: " + weightingCode + " is not a valid weighting code.");
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
                System.err.println("Error: " + scoringCode + " is not a valid scoring code.");
			    return;
		}

		
		// Open the index
		File indexDir = new File(inDir + "/index");
        if (!indexDir.isDirectory()){
            System.err.println("Error: " + indexDir.toString() + " is not a directory.");
		    return;
        }
		Directory dir = NIOFSDirectory.open(indexDir);

		
		reader   = IndexReader.open(dir, true);
		searcher = new IndexSearcher(reader);
		analyzer = new SourceCodeAnalyzer();

		Version v = Version.LUCENE_35;
		String[] fields = {"data"};
		parser = new MultiFieldQueryParser(v, fields, analyzer);

        BooleanQuery.setMaxClauseCount(8192);
		
		// Open the query directory, and run every query in the directory!
        String queryDirName = inDir + "/bugs";
		File queryDir = new File(queryDirName);
        if (!queryDir.isDirectory()){
            System.err.println("Error: " + queryDir.toString() + " is not a directory.");
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
				String outFile = outDir + "/" + f.getName();
				FileWriter fwriter = new FileWriter(outFile);
				PrintWriter out    = new PrintWriter(fwriter);
				
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
	}
}
