/*
####################################################################################
Stephen W. Thomas
sthomas@cs.queensu.ca
Queen's University

IndexDirectory.java

Simple wrapper class for SimpleIndexer.

####################################################################################
*/

package ca.queensu.cs.sail.lucenelda;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

public class IndexDirectoryRunLDA {

	public static void main(String[] args) throws Exception {

		
		// Use the JSAP library to intelligently set up and parse our command line options
		 JSAP jsap = new JSAP();
		 
		 UnflaggedOption opt1 = new UnflaggedOption("inDir")
         .setStringParser(JSAP.STRING_PARSER)
         .setRequired(true);
		 opt1.setHelp("The input directory containing files on which to build the index");
		 
		 UnflaggedOption opt2 = new UnflaggedOption("outDir")
         .setStringParser(JSAP.STRING_PARSER)
         .setRequired(true);
		 opt2.setHelp("The output directory for the resultant Lucene index");
		 
		 FlaggedOption opt3 = new FlaggedOption("fileCodes")
		 .setStringParser(JSAP.STRING_PARSER)
         .setRequired(false)
         .setLongFlag("fileCodes");
		 opt2.setHelp("A file containing a mapping between filenames and some key. " +
				 	  "If specified, the query results will list the key instead of the filename.");
		 
		 Switch sw0 = new Switch("help")
         .setDefault("false")
         .setLongFlag("help");
		 sw0.setHelp("Prints this message.");
		 
		 FlaggedOption opt4 = new FlaggedOption("numK")
		 .setStringParser(JSAP.INTEGER_PARSER)
         .setRequired(false)
         .setDefault("50")
         .setLongFlag("numK");
		opt4.setHelp("Number of topics to run.");
		 
		jsap.registerParameter(sw0);
		jsap.registerParameter(opt1);
		jsap.registerParameter(opt2);
		jsap.registerParameter(opt3);
		jsap.registerParameter(opt4);

        // check whether the command line was valid, and if it wasn't,
        // display usage information and exit.
		JSAPResult config = jsap.parse(args);  
        if (!config.success()) {
        	for (java.util.Iterator errs = config.getErrorMessageIterator(); errs.hasNext();) {
        		System.err.println("Error: " + errs.next());
        	}
        	displayHelp(config, jsap);
            return;
        }
        
        if (config.getBoolean("help")){
        	displayHelp(config, jsap);
        	return;
        }
	        
		 String inDirName = config.getString("inDir");
		 String outDirName = config.getString("outDir");
			
	    // Make sure the code, bugs, and lda files exist
	    if (! (new File((inDirName)).exists())){
	        System.err.println("Error: " + inDirName + " does not exist.");
	        return;
	    }
	        
	    // If the output directory already exists, remove it (or else Lucene will complain)
		File indexDir   = new File(outDirName + "/index");
        if (indexDir.exists()){
            FileUtils.deleteDirectory(indexDir);
        } 
        
        // Read the fileCodes, if there is one
        if (config.getString("fileCodes") != null){
        	// TODO
        }


        // TODO: Run LDA, and build an LDAHelper object
        LDAHelper ldaHelper = new LDAHelper();
        ldaHelper.runLDA(config.getInt("numK"), inDirName);
		
		// Build the index, with the options specified.
		SimpleIndexer.indexDirectory(inDirName, outDirName, config.getString("fileCodes"), ldaHelper);
	
		
	}

	/* Use JSAP to display command-line usage information */
	private static void displayHelp(JSAPResult config, JSAP jsap) {
		System.err.println();
        System.err.println("Usage: java "+ IndexDirectory.class.getName());
        System.err.println("                " + jsap.getUsage());
        System.err.println();
        System.err.println(jsap.getHelp());
        System.err.println();
	}
}

