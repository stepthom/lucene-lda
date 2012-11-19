import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import junit.framework.*;
import ca.queensu.cs.sail.lucenelda.*;

public class Test002 extends TestCase {
	
	//private static final Logger logger = Logger.getRootLogger();

	// Builds the index that other tests will rely on; this itself is a test
    public final void testIndex() throws Exception{

    	// Since this is the first test case executed, set up the logger
    	//BasicConfigurator.configure();
		//logger.setLevel(Level.INFO);
    	
    	//logger.info("Building index");
        String[] args = {"t/t002/docs","t/t002/index"};
        IndexDirectory.main(args);
    }
    
    public final void testVSM1() throws Exception{

    }


}

