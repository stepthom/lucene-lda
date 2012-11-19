/*
####################################################################################
Stephen W. Thomas
sthomas@cs.queensu.ca
Queen's University

SimpleAnalyzer.java

This class has default Analyzer behavior, except for one field: "topicspayload". Here,
it just encodes the field to have the form pK$X, where K is the topic ID, and X is the topic
membership. 

####################################################################################
*/

package ca.queensu.cs.sail.lucenelda;

import java.io.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.payloads.DelimitedPayloadTokenFilter;
import org.apache.lucene.analysis.payloads.FloatEncoder;
import org.apache.lucene.util.Version;

public final class SimpleAnalyzer extends Analyzer {
	
	private static final Analyzer STANDARD = new WhitespaceAnalyzer(Version.LUCENE_35);

	public SimpleAnalyzer(){
		super();
	}
	
	public final TokenStream tokenStream(String fieldName, Reader reader) {
		// If this is the topics field, then need to use the notation of "p1$0.45"
		if (fieldName.matches("topicspayload.*")){
			return new DelimitedPayloadTokenFilter(
				      new WhitespaceTokenizer(Version.LUCENE_35, reader),
				      '$', new FloatEncoder());
		}
		// else, this is a "code" field
		return STANDARD.tokenStream(fieldName, reader);
	  }
}
