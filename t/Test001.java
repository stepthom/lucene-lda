/*
####################################################################################
Stephen W. Thomas
sthomas@cs.queensu.ca
Queen's University

Test001.java

This test suite deals with the data in t/t001, which is a subset of the Mozilla data
used in our TSE paper. It has about 330 files to index, and 6 queries to execute.

LDA has already been executed for this data, and resides in t/001/lda/*

This suite builds the index, and then executes queries using VSM (with various 
parameter combinations) and LDA (again, with various combinations).

####################################################################################
*/

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import junit.framework.*;
import ca.queensu.cs.sail.lucenelda.*;

public class Test001 extends TestCase {
	
	private static final Logger logger = Logger.getRootLogger();

	// Builds the index that other tests will rely on; this itself is a test
    public final void testIndex() throws Exception{
    	
		// Set up the Apache log4j logger.
		if (!logger.getAllAppenders().hasMoreElements()) {
			BasicConfigurator.configure();
			logger.setLevel(Level.INFO);
		}

    	
        String[] args = {"t/t001/code","t/t001/index", "t/t001/lda/ldaHelper.obj",
                         "--fileCodes", "t/t001/fileCodes.csv",
                         "--ldaConfig", "32,t/t001/lda/32",
                         "--ldaConfig", "64,t/t001/lda/64"};
        IndexDirectory.main(args);
    }
    
    public final void testVSM1() throws Exception{
        String[] args = {"t/t001/index","t/t001/bugs", "t/t001/results/vsm.1.1",
                         "--weightingCode", "1",
                         "--scoringCode", "1",};
        VSMQueryAllInDirectory.main(args);
        
        // TODO: ensure that results are kosher
    }
    
    public final void testVSM2() throws Exception{
        String[] args = {"t/t001/index","t/t001/bugs", "t/t001/results/vsm.2.1",
                         "--weightingCode", "2",
                         "--scoringCode", "1",};
        VSMQueryAllInDirectory.main(args);
        
        // TODO: ensure that results are kosher
    }
    
    public final void testVSM3() throws Exception{
        String[] args = {"t/t001/index","t/t001/bugs", "t/t001/results/vsm.1.2",
                         "--weightingCode", "1",
                         "--scoringCode", "2",};
        VSMQueryAllInDirectory.main(args);
        
        // TODO: ensure that results are kosher
    }
    
    public final void testVSM4() throws Exception{
        String[] args = {"t/t001/index","t/t001/bugs", "t/t001/results/vsm.3.1",
                         "--weightingCode", "3",
                         "--scoringCode", "1",};
        VSMQueryAllInDirectory.main(args);
        
        // TODO: ensure that results are kosher
    }
    
    public final void testLDA1() throws Exception{
        String[] args = {"t/t001/index","t/t001/lda/ldaHelper.obj", 
        				 "t/t001/bugs", "t/t001/results/lda.32.1",
                         "--K", "32",
                         "--scoringCode", "1",};
        LDAQueryAllInDirectory.main(args);
        
        // TODO: ensure that results are kosher
    }
    
    public final void testLDA2() throws Exception{
        String[] args = {"t/t001/index","t/t001/lda/ldaHelper.obj", 
        				 "t/t001/bugs", "t/t001/results/lda.64.1",
                         "--K", "64",
                         "--scoringCode", "1",};
        LDAQueryAllInDirectory.main(args);
        
        // TODO: ensure that results are kosher
    }


}

