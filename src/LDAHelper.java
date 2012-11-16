/*
####################################################################################
Stephen W. Thomas
sthomas@cs.queensu.ca
Queen's University

LDAHelper.java



####################################################################################
*/

package ca.queensu.cs.sail.doofuslarge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;



class LDAHelper implements Serializable {

	private static final long serialVersionUID = -5883161587870956703L;
	
	// 'scens' (short for scenarios) holds all the LDAK objects: one for each K
	// (Data structure is a simple class below)
	public LDAK[] scens;

	
	LDAHelper(String inDirName, String[] ldaKs) throws IOException{

        // If ldaKs is empty, what are we doing here? Throw an error.
		if (ldaKs == null){
            System.err.println("Error: no list of LDA Ks provided.");
			return;
		}

        // For each given K value, we need to read the associated files into
        // our datastructures.	
		scens = new LDAK[ldaKs.length];
		for (int i = 0; i < ldaKs.length; ++i){
			scens[i] = new LDAK();
			scens[i].K = Integer.parseInt(ldaKs[i]);
			
			// Read the term map
			BufferedReader br = new BufferedReader(new FileReader(inDirName +  "/" + scens[i].K + "/vocab.dat"));
			int counter=0;
			String line;
			while ((line = br.readLine()) != null) {
				scens[i].termMap.put(line, counter);
				++counter;
			}
			
			// Read the file map
			br = new BufferedReader(new FileReader(inDirName +  "/" + scens[i].K + "/files.dat"));
			counter=0;
			while ((line = br.readLine()) != null) {
				String lineParts[] = line.split("\\s+");
				scens[i].fileMap.put(lineParts[1], counter);
				++counter;
			}
			
			// Set the constants, that will be used later.
			scens[i].D = scens[i].fileMap.size();
			scens[i].W = scens[i].termMap.size();
			
			// Read the theta and phi matrices
			scens[i].theta = readFileIntoMatrix(inDirName + "/"  + scens[i].K + "/theta.dat", scens[i].D, scens[i].K);
			scens[i].phi   = readFileIntoMatrix(inDirName +  "/" + scens[i].K + "/words.dat", scens[i].K, scens[i].W);
		}
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
		
		for (int i=0;i<scens[idx].K;++i){
			out += ("," + scens[idx].theta[docId][i]);
		}
		return out;
	}
	
	public int which(int k) {
		for (int i = 0; i < scens.length; ++i){
			if (k == scens[i].K){
				return i;
			}
		}
		System.err.println("Warning: cannot find K="+k+" in LDAHelper().");
		System.exit(2);
		return -1;
	}


	public float[] decodeTopics(String encodedTopicString, int K) {
		String[] parts = encodedTopicString.split(",");
		int idx = which(K);
		float result[] = new float[scens[idx].K];
		for (int i=1;i<parts.length;++i){
			result[i-1] = Float.parseFloat(parts[i]);
		}
		return result;
	}


	public String encodeTopicsPayLoad(int docId, int K) {
		String out = "";
		int idx = which(K);
		for (int i=0;i<scens[idx].K;++i){
			if (scens[idx].theta[docId][i] > 0.05){
				out += (" p" + i + "$"  + scens[idx].theta[docId][i]);
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
	/**
	 * 
	 */
	private static final long serialVersionUID = 2161745883533541761L;
	public float phi[][]; // holds all the topics
	public float theta[][]; // holds all the topic vectors
	public int K = 0;   // The number of LDA topics
	public int W = 0; // The number of terms
	public int D = 0;   // number of documents
	public HashMap<String, Integer> termMap = new HashMap<String, Integer>(); // Contains the ids of each term (for the topics)
	public HashMap<String, Integer> fileMap = new HashMap<String, Integer>(); // Contains the ids of each term (for the topics)
}

}
