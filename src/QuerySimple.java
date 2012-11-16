package ca.queensu.cs.sail.doofuslarge;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


/**
 * 
 * @author sthomas
 *
 * A simple example class that queries a specified index.
 * 
 */

public class QuerySimple {
	private static int maxHits = 200;
	private static IndexReader reader = null;
	private static IndexSearcher searcher = null;
	private static QueryParser parser = null;
	private static SourceCodeAnalyzer analyzer = null;

	public static void main(String[] args) throws IOException, ParseException {

		if (args.length != 1){
			System.err.println("USage: QuerySimple indexDirName");
			System.exit(1);
		}


		// Build the necessary tools
		File indexDir = new File(args[0]);
		Directory dir = FSDirectory.open(indexDir);
		reader = IndexReader.open(dir);
		searcher = new IndexSearcher(reader);
		
		analyzer = new SourceCodeAnalyzer();

		Version v = Version.LUCENE_30;
		String[] fields = {"data"};
		parser = new MultiFieldQueryParser(v, fields, analyzer);
		
		String query = "expression";

		System.out.println(query);
		Query q1 = parser.parse(query);
		System.out.println(q1);

		//searcher.setSimilarity(new LDASimilarity());
		//TopDocs hits = searcher.search(q, maxHits);
		//ScoreDoc[] scoreDocs = hits.scoreDocs;
		
		Query q2 = NumericRangeQuery.newIntRange("cumBugs",
                new Integer(10), new Integer(100),
                true, true);
		
		BooleanQuery bquery = new BooleanQuery();
		
		bquery.add(q1, Occur.SHOULD);
		bquery.add(q2, Occur.MUST);
		
		TopDocs hits = searcher.search(bquery, maxHits);
		ScoreDoc[] scoreDocs = hits.scoreDocs;

		System.out.println("Hits");
		for (int n = 0; n < scoreDocs.length; ++n) {
			ScoreDoc sd = scoreDocs[n];
			float score = sd.score;
			int docId = sd.doc;
			Document d = searcher.doc(docId);
			String fileName = d.get("file");
			Explanation ex = searcher.explain(bquery, docId);

			System.out.printf("%3d %4.5f  %s\n%s\n", n, score, fileName, ex.toString());
		}
	}
}
