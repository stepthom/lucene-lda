/*
####################################################################################
Stephen W. Thomas
sthomas@cs.queensu.ca
Queen's University

IndexDirectory.java

Simple wrapper class for SourceCodeIndexer.

####################################################################################
*/

package ca.queensu.cs.sail.doofuslarge;

import java.io.IOException;

public class IndexDirectory {

	public static void main(String[] args) throws IOException {
		if (args.length < 2){
			System.err.println("Usage: inDirName fileCodeName [LDAK1 [LDAK2 [...]]]");
			return;
		}

		int numLDAKs = args.length - 2;
		String[] ldas = null;
		if (numLDAKs > 0){
			ldas = new String[numLDAKs];
			int j = 2;
			for (int i=0;i<numLDAKs;++i,++j){
				ldas[i] = args[j];
			}
		}
		// Build the index, with the options specified.
		SourceCodeIndexer.indexDirectory(args[0], args[1], ldas);
	}
}

