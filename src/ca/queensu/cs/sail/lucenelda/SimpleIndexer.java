package ca.queensu.cs.sail.lucenelda;

import java.io.*;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class SimpleIndexer {

	public static HashMap<String, Integer> fileCodes    = new HashMap<String, Integer>(); 
	private static final Logger logger = Logger.getRootLogger();

	public SimpleIndexer() {
		super();
	}
	
	/**
	 * @author sthomas
	 * @throws IOException
	 * 
	 * Nothing is returned, but the index is written to disk (indexDirName). 
	 */
	public static void indexDirectory(String inDirName, String indexDirName, String fileCodeFileName, LDAHelper lda)
			throws IOException {
      
		File inDir         = new File(inDirName);
		Directory indexDir = FSDirectory.open(new File(indexDirName));
		
		SimpleAnalyzer analyzer = new SimpleAnalyzer();
		
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_35, analyzer);
		IndexWriter writer = new IndexWriter(indexDir, indexWriterConfig);
		
		// TODO: is this the best place for this?
		// Read file code files into the hashmap
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileCodeFileName));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",\\s*");
				fileCodes.put(values[1], Integer.parseInt(values[0]));
			}
		} catch (Exception e){
			e.printStackTrace();
			return;
		}

		indexDirectory(writer, inDir, lda);
		writer.close();
	}

	/**
	 * @author sthomas
	 * @param writer
	 * @param dir
	 * @throws IOException
	 */
	public static void indexDirectory(IndexWriter writer, File dir, LDAHelper lda) throws IOException {
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (f.isDirectory()){
				indexDirectory(writer, f, lda);
			}
			//else if (f.getName().endsWith(".java") || f.getName().endsWith(".c") || f.getName().endsWith(".cpp"))
			else {
				indexFile(writer, f, lda);
			}
		}
	}

	/**
	 * @author sthomas
	 * @param writer
	 * @param f
	 * Don't call this one directly; only from recursive starter
	 * @throws IOException 
	 */
	public static void indexFile(IndexWriter writer, File f, LDAHelper lda) throws IOException {
		if (f.isHidden() || !f.exists() || !f.canRead()){
			return;
		}
		logger.debug("Indexing file " + f.getName());
		
		Document doc = new Document();
		doc.add(new Field("file", fileCodes.get(f.getName()).toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));

		// Add content of file
		String data = FileUtils.readFileToString(f);
		data = data.replaceAll("\\n", " ");
		doc.add(new Field("data", data, Field.Store.YES, Field.Index.ANALYZED));

		// Add LDA topic memberships as one big string: the row of this document in the theta matrix
		for (int i = 0; i < lda.scens.size(); ++i){
			int docId = lda.scens.get(i).fileMap.get(f.getName());
			doc.add(new Field("topics" + lda.scens.get(i).K, lda.encodeTopics(docId, lda.scens.get(i).K), Store.YES, Index.NOT_ANALYZED));
			doc.add(new Field("topicspayload" + lda.scens.get(i).K, lda.encodeTopicsPayLoad(docId, lda.scens.get(i).K), Store.YES, Index.ANALYZED));
		}

		try {
			writer.addDocument(doc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
