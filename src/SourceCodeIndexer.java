package ca.queensu.cs.sail.doofuslarge;

import java.io.*;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
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

public class SourceCodeIndexer {

	private static LDAHelper lda;
	//public static HashMap<String, docBugs> docBugCounts = new HashMap<String, docBugs>(); 
	public static HashMap<String, Integer> fileCodes    = new HashMap<String, Integer>(); 

	public SourceCodeIndexer() {
		super();
	}
	
	/**
	 * @author sthomas
	 * @throws IOException
	 * 
	 * Nothing is returned, but the index is written to disk (indexDirName). 
	 */
	public static void indexDirectory(String inDirName, String fileCodeFileName, String[] ldaKs)
			throws IOException {

        // If the index already exists, remove it
		File indexDir   = new File(inDirName + "/index");
        if (indexDir.exists()){
            FileUtils.deleteDirectory(indexDir);
        } 

        // Make sure the code, bugs, and lda files exist
        if (! (new File((inDirName + "/code")).exists())){
            System.err.println("Error: " + inDirName + "/code does not exist.");
            return;
        }
        if (! (new File((inDirName + "/bugs")).exists())){
            System.err.println("Error: " + inDirName + "/bug does not exist.");
            return;
        }
        if (! (new File((inDirName + "/lda")).exists())){
            System.err.println("Error: " + inDirName + "/bug does not exist.");
            return;
        }
        
		File docDir     = new File(inDirName + "/code");
		Directory fsDir = FSDirectory.open(indexDir);
		lda 			= new LDAHelper(inDirName+"/lda", ldaKs);
		
		SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer();
		
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_35, analyzer);
		IndexWriter writer = new IndexWriter(fsDir, indexWriterConfig);
		//IndexWriter writer = new IndexWriter(fsDir, analyzer, MaxFieldLength.UNLIMITED);
		
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

		indexDirectory(writer, docDir);
		writer.close();
		
		// Serialize the LDAHelper object
		try{
			FileOutputStream fos    = new FileOutputStream(inDirName + "/ldaHelper.obj");
			ObjectOutputStream out = new ObjectOutputStream(fos);
			out.writeObject(lda);
			out.close();
		}
		catch(IOException ex){
			ex.printStackTrace();
			return;
		}
	}

	/**
	 * @author sthomas
	 * @param writer
	 * @param dir
	 * @throws IOException
	 */
	public static void indexDirectory(IndexWriter writer, File dir) throws IOException {
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (f.isDirectory())
				indexDirectory(writer, f);
			else if (f.getName().endsWith(".java") || f.getName().endsWith(".c") || f.getName().endsWith(".cpp"))
				indexFile(writer, f);
		}
	}

	/**
	 * @author sthomas
	 * @param writer
	 * @param f
	 * Don't call this one directly; only from recursive starter
	 * @throws IOException 
	 */
	public static void indexFile(IndexWriter writer, File f) throws IOException {
		if (f.isHidden() || !f.exists() || !f.canRead()){
			return;
		}
		
		//System.out.printf("Indexing file %s\n", f.getName());
		
		Document doc = new Document();
		
		//doc.add(new Field("file",    f.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("file", fileCodes.get(f.getName()).toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		//int cumBugs = 0, newBugs = 0;
		//if (docBugCounts.containsKey(f.getName())){
			//cumBugs = docBugCounts.get(f.getName()).cumBugs;
			//newBugs = docBugCounts.get(f.getName()).newBugs;
		//}
		//NumericField nf1 = new NumericField("cumBugs", Field.Store.YES, true);
		//nf1.setIntValue(cumBugs);
		//NumericField nf2 = new NumericField("newBugs", Field.Store.YES, true);
		//nf2.setIntValue(newBugs);
		//doc.add(nf1);
		//doc.add(nf2);
		
		// Add content of file
		String data = FileUtils.readFileToString(f);
		data = data.replaceAll("\\n", " ");
		doc.add(new Field("data", data, Field.Store.YES, Field.Index.ANALYZED));

		// Add topic memberships as one big string
		// the row of this document in the theta matrix
		for (int i = 0; i < lda.scens.length; ++i){
			int docId = lda.scens[i].fileMap.get(f.getName());
			doc.add(new Field("topics" + lda.scens[i].K, lda.encodeTopics(docId, lda.scens[i].K), Store.YES, Index.NOT_ANALYZED));
			doc.add(new Field("topicspayload" + lda.scens[i].K, lda.encodeTopicsPayLoad(docId, lda.scens[i].K), Store.YES, Index.ANALYZED));
		}

		try {
			writer.addDocument(doc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//public static class docBugs{
		//int cumBugs=0;
		//int newBugs=0;
	//}
}
