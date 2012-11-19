/*
####################################################################################
Stephen W. Thomas
sthomas@cs.queensu.ca
Queen's University

LDAHelper.java



####################################################################################
*/

package ca.queensu.cs.sail.lucenelda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;



class LDAHelper implements Serializable {

	private static final long serialVersionUID = -5883161587870956703L;
	private static final Logger logger = Logger.getRootLogger();
	
	// 'scens' (short for scenarios) holds all the LDAK objects: one for each K
	// (The data structure is a simple class defined below)
	public ArrayList<LDAK> scens;
	
	
	// Used when LDA needs to be run on the given inDirName
	LDAHelper(String inDirName) throws IOException{
	}
	
	public void runLDA(int K, String inDirName){
		LDAK ldak = new LDAK();
		ldak.K = K;
		
		//TODO: run MALLET
		//TODO: transform MALLET datastructures into LDAK datastructures
	}

	// Used when LDA has already been run by the user, and we just need to swallow up the data in the files
	LDAHelper(){
		scens = new ArrayList<LDAK>();
	}
	
	// Add a scenario from disk
	public void addScenario(int K, String inDirName) throws IOException{
		
		logger.info("Adding LDA scenario: K="+K+", dir="+inDirName);

		// First, check that the four files are present:
		if (! (new File((inDirName + "/vocab.dat")).exists())){
	        System.err.println("Error: " + inDirName + "/vocab.dat does not exist.");
	        return;
	    }
		if (! (new File((inDirName + "/files.dat")).exists())){
	        System.err.println("Error: " + inDirName + "/files.dat does not exist.");
	        return;
	    }
		if (! (new File((inDirName + "/theta.dat")).exists())){
	        System.err.println("Error: " + inDirName + "/theta.dat does not exist.");
	        return;
	    }
		if (! (new File((inDirName + "/words.dat")).exists())){
	        System.err.println("Error: " + inDirName + "/words.dat does not exist.");
	        return;
	    }
		
		LDAK ldak = new LDAK();
		ldak.K = K;
		
		// Read the term map
		BufferedReader br = new BufferedReader(new FileReader(inDirName +  "/vocab.dat"));
		int counter=0;
		String line;
		while ((line = br.readLine()) != null) {
			ldak.termMap.put(line, counter);
			++counter;
		}
		
		// Read the file map
		br = new BufferedReader(new FileReader(inDirName + "/files.dat"));
		counter=0;
		while ((line = br.readLine()) != null) {
			String lineParts[] = line.split("\\s+");
			ldak.fileMap.put(lineParts[1], counter);
			++counter;
		}
		
		// Set the constants, that will be used later.
		ldak.D = ldak.fileMap.size();
		ldak.W = ldak.termMap.size();
		
		// Read the theta and phi matrices
		ldak.theta = readFileIntoMatrix(inDirName + "/theta.dat", ldak.D, ldak.K);
		ldak.phi   = readFileIntoMatrix(inDirName + "/words.dat", ldak.K, ldak.W);
		
		scens.add(ldak);
	}
	
	
	/**
	 * 
	 * @param fileName
	 * @param numRows
	 * @param numCols
	 * @return
	 */
	private float[][] readFileIntoMatrix(String fileName, int numRows, int numCols) {
		BufferedReader br = null;
		float[][] matrix = new float[numRows][numCols];

		try {
			br = new BufferedReader(new FileReader(fileName));
			String line = null;
			int x=0;
			int y=0;
			while ((line = br.readLine()) != null) {
				String[] values = line.split("\\s+");
				y = 0;

                // Special case: MALLET returns nans, for example if there were more topics
                // than documents. In this case, just make a row of 0s
                if (values[0].equals("nan")){
				    for (String str : values) {
					    matrix[x][y++]=0;
				    }
                } else {
                    // Normal case: everything went as expected (real valued numbers
                    // in file)
				    for (String str : values) {
					    float str_double = Float.parseFloat(str);
					    matrix[x][y++]=str_double;
				    }
                }
				++x;
			}
		} catch (Exception e){
			e.printStackTrace();
			return null;
		}

		return matrix;
	}


	public String encodeTopics(int docId, int K) {
		String out = "";
		int idx = which(K);
		
		for (int i=0;i<scens.get(idx).K;++i){
			out += ("," + scens.get(idx).theta[docId][i]);
		}
		return out;
	}
	
	
	// Given an K value, this function returns the index of this K in the scens ArrayList.
	public int which(int k) {
		
		// Special case: if k==0, then the command line option was ommitted and we should return the
		// index of the first k.
		if (k==0){
			return 1;
		}
		
		for (int i = 0; i < scens.size(); ++i){
			if (k == scens.get(i).K){
				return i;
			}
		}
		
		// Default: just return the index of the first K.
		return 1;
	}


	public float[] decodeTopics(String encodedTopicString, int K) {
		String[] parts = encodedTopicString.split(",");
		int idx = which(K);
		float result[] = new float[scens.get(idx).K];
		for (int i=1;i<parts.length;++i){
			result[i-1] = Float.parseFloat(parts[i]);
		}
		return result;
	}


	public String encodeTopicsPayLoad(int docId, int K) {
		String out = "";
		int idx = which(K);
		for (int i=0;i<scens.get(idx).K;++i){
			if (scens.get(idx).theta[docId][i] > 0.05){
				out += (" p" + i + "$"  + scens.get(idx).theta[docId][i]);
			}
		}
		return out;
	}
	
	
	/**
	 * 
	 * @param searcher
	 * @param hits
	 * @param queryScore
	 * @param K
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public HashMap<String, Float> reRank(IndexSearcher searcher, TopDocs hits, float queryScore[], int K) throws IOException, Exception{
		ScoreDoc[] scoreDocs = hits.scoreDocs;
		
		HashMap<String, Float> result = new HashMap<String, Float>();

		for (int n = 0; n < scoreDocs.length; ++n) {
			ScoreDoc sd = scoreDocs[n];
			int docId = sd.doc;
			Document d = searcher.doc(docId);
			String fileName = d.get("file");
			
			String encodedTopicString = (d.get("topics" + K));
			float sim = computeSimilarity(encodedTopicString, queryScore, K);

			//System.out.printf("%3d %4.5f %d %s\n", n, sim, docId, fileName);
			result.put(fileName, sim);
		}
		
		return sortHashMap(result);
	}
	
	
	/**
	 * 
	 * @param input
	 * @return
	 */
	private HashMap<String, Float> sortHashMap(HashMap<String, Float> input){
	    Map<String, Float> tempMap = new HashMap<String, Float>();
	    for (String wsState : input.keySet()){
	        tempMap.put(wsState,input.get(wsState));
	    }

	    List<String> mapKeys = new ArrayList<String>(tempMap.keySet());
	    List<Float> mapValues = new ArrayList<Float>(tempMap.values());
	    HashMap<String, Float> sortedMap = new LinkedHashMap<String, Float>();
	    TreeSet<Float> sortedSet = new TreeSet<Float>(mapValues);
	    Object[] sortedArray = sortedSet.descendingSet().toArray();
	    int size = sortedArray.length;
	    for (int i=0; i<size; i++){
	        sortedMap.put(mapKeys.get(mapValues.indexOf(sortedArray[i])), 
	                      (Float)sortedArray[i]);
	    }
	    return sortedMap;
	}
	
	/**
	 * 
	 * @param docId
	 * @param queryScore
	 * @return The conditional probability between the document and the query.
	 */
public float computeSimilarity(String encodedTopicString, float queryScore[], int K){
	float result = 0;
	float[] topicVector = decodeTopics(encodedTopicString, K);
	for (int i=0; i<topicVector.length;++i){
		result += topicVector[i]*queryScore[i];
	}
	return result;
}


// Data containers
public class LDAK implements Serializable {
	private static final long serialVersionUID = 2161745883533541761L;
	public float phi[][]; // holds all the topics
	public float theta[][]; // holds all the topic vectors
	public int K = 0;   // The number of LDA topics
	public int W = 0; // The number of terms
	public int D = 0;   // number of documents
	public HashMap<String, Integer> termMap = new HashMap<String, Integer>(); // Contains the ids of each term (for the topics)
	public HashMap<String, Integer> fileMap = new HashMap<String, Integer>(); // Contains the file map
}

}
