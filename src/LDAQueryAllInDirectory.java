package ca.queensu.cs.sail.doofuslarge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.PrintWriter;

import java.util.HashMap;

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

public class LDAQueryAllInDirectory {
	
	private static int maxHits = 500;
	private static IndexReader reader = null;
	private static IndexSearcher searcher = null;
	static LDAHelper lda = null;
	
	public static void main(String[] args) throws Exception {
		if (args.length != 4){
			System.err.println("Usage: LDAQueryAllInDirectory inDirName outDirName K scoringCode");
			System.exit(1);
		}
		
		String inDir  = args[0];
		String outDir = args[1];
		int K             = Integer.parseInt(args[2]);
		int scoringCode   = Integer.parseInt(args[3]);

        // Make sure output directory exists
        File outDirF = new File(outDir);
        if (!outDirF.exists()){
            outDirF.mkdir();
        }
		
		String queryDirName = inDir + "/bugs";
		
		// Open the serialized LDAHelper object
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try{
			fis = new FileInputStream(inDir + "/ldaHelper.obj");
			in = new ObjectInputStream(fis);
			lda = (LDAHelper)in.readObject();
			in.close();
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
		
		int idx = lda.which(K);
		
		// Open the index
		File indexDir = new File(inDir + "/index");
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
				// Need to find all the topics in the query: for each term, find out all topics that contain this term;
				// Then, take the union of all the terms
				// Then, build the Boolean query below with multiple PayloadTermQuery()s
				float queryOpt[]    = new float[lda.scens[idx].K];
				BooleanQuery bquery = new BooleanQuery();
							
				// For each topic, sum up the scores of the words in the query
				String[] querySplitParts = query.split("\\s+");
				for (int k = 0; k < lda.scens[idx].K; ++k){
					float score = 0.0f;
					for (int j = 0; j<querySplitParts.length; ++j){
						if (lda.scens[idx].termMap.containsKey(querySplitParts[j])){ // Make sure the key exists in the map (it might not, due to vocab mismatch)
							int idx2 = lda.scens[idx].termMap.get(querySplitParts[j]);
							score = score + lda.scens[idx].phi[k][idx2];
						}
					}
					
					// Now, add this topic to the score
					if (score > 0.01f){
						//System.out.printf("Adding topic %d with score %f\n", k, score);
						PayloadTermQuery fsq1 = new PayloadTermQuery(new Term("topicspayload"+lda.scens[idx].K, "p"+k), new AveragePayloadFunction(), false);
						bquery.add(fsq1, Occur.SHOULD);						
						queryOpt[k] = score;
					}
				}
				
				//System.out.println(bquery);

				TopDocs hits = searcher.search(bquery, maxHits);
				ScoreDoc[] scoreDocs = hits.scoreDocs;
				//System.out.printf("Found %d hits\n", hits.totalHits);
				
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
				HashMap<String, Float> sorted = lda.reRank(searcher, hits, queryOpt, lda.scens[idx].K);
				
				// Open the output file for results
				String outFile = outDir + "/" + f.getName();
				FileWriter fwriter = new FileWriter(outFile);
				PrintWriter out = new PrintWriter(fwriter);
				
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
					//int cumBugs      = Integer.parseInt(d.get("cumBugs"));
					//int newBugs      = Integer.parseInt(d.get("newBugs"));
					
					//if (cumBugs >= minBugsFilter){
						out.printf("%s,%4.3f\n", fileName, score);
						++counter;
					//}
				}
				out.close();
			}
		}
	}
}
