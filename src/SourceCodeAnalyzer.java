package ca.queensu.cs.sail.doofuslarge;

import java.io.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.payloads.DelimitedPayloadTokenFilter;
import org.apache.lucene.analysis.payloads.FloatEncoder;
import org.apache.lucene.util.Version;


/**
 * @author sthomas
 *
 */
public class SourceCodeAnalyzer extends Analyzer {
	
	private static final Analyzer STANDARD = new WhitespaceAnalyzer(Version.LUCENE_35);

	public SourceCodeAnalyzer(){
		super();
	}
	
	/**
	 * @author sthomas
	 * This method performs the Analysis (i.e., calls tokenizers and filters)
	 * on all fields.
	 */
	public TokenStream tokenStream(String fieldName, Reader reader) {
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
