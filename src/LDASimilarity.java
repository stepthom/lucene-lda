package ca.queensu.cs.sail.doofuslarge;

import java.util.Hashtable;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.DefaultSimilarity;

/**
 * @author sthomas
 *
 * Custom similarity class that uses LDA topics for scoring, using the 
 * conditional probability equation:
 * 	TODO
 * 
 * In Lucene, the score between a query and document is the produce of a bunch of 
 * functions: TODO
 * 
 * By overriding all of these functions, and setting all but 1 of them always return 1, 
 * we can have our own arbitrary scoring function (in this case, I choose tf).
 * 
 */
public class LDASimilarity extends DefaultSimilarity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	 public float idf(int docFreq, int numDocs) {
		    return 1;
	 }
	 
	 public float scorePayload(int docId, String fieldName,
			 int start, int end, byte[] payload, int offset, int length) {
		 if (payload != null) {
			 
			 //System.out.printf("%d %s %d %d %s %d %d\n", docId, fieldName, start, end, payload.toString(), offset, length);

			 // The actual LDA similarity comparison calculation
			 return PayloadHelper.decodeFloat(payload, offset); // * queryScores.get(start+1);
		 } else {
			 return 1.0F;
		 }
	 }
	 
	 
	 /**
	  * @author sthomas
	  * 
	  * This is the LDA hack. 
	  */
	 public float tf(int freq) {
		    return 1;
     }
	 
	 public float queryNorm(float sumOfSquaredWeights){
		 return 1;
	 }
	 
	 public float coord(int overlap, int maxOverlap) {
		 	return 1;
	 }
	 
	 public float computeNorm(String field, FieldInvertState state){
		 return 1;
	 }
	 

	 
	 public float sloppyFreq(int distance) {
		 return 0;
	 }

	 public float	decodeNormValue(byte b) {
		 return 0;
	 }


}
