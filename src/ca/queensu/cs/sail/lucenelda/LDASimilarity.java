/*
####################################################################################
Stephen W. Thomas
sthomas@cs.queensu.ca
Queen's University

LDASimilarity.java

This class overrides Lucene's default similarity computation, mostly by making this class obsolete and returning 1.

Our goal is to mimic how latent Dirichlet allocation (LDA) would compute the similarity between
a query and a document. Lucene's default similarity is basic tf-idf, using
the vector space model (VSM). However, we override this behavior to implement 'conditional probability', a popular
document similarity measure when LDA is involved. Specifically, we use the Payload associated with a document 
(which was created at index time, and captures which topics are in the document),
to determine similarity between queries and documents.

So, we don't need or use any of the values from functions in this class, except that of scorePayload(), 
where the value of the Payload is returned. The actual similarity is computed in 
LDAHelper::computeSimilarity (called by LDAQueryAllInDirectory()).

####################################################################################
*/

package ca.queensu.cs.sail.lucenelda;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.DefaultSimilarity;

public class LDASimilarity extends DefaultSimilarity {

	private static final long serialVersionUID = 1L;
	
	 public float idf(int docFreq, int numDocs) {
		    return 1;
	 }
	 
	 public float scorePayload(int docId, String fieldName,
			 int start, int end, byte[] payload, int offset, int length) {
		 if (payload != null) {
			 // We will need this payload to calculate the similarity later.
			 return PayloadHelper.decodeFloat(payload, offset);
		 } else {
			 return 1.0F;
		 }
	 }
	 
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
