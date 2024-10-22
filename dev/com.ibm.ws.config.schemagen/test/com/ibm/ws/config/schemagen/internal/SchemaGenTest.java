package com.ibm.ws.config.schemagen.internal;
/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 *  Test wlp/bin/schemaGen.
 *
 *  <ul><li>with no parameters</li>
 *      <li>with -help</li>
 *      <li>with an output file</li>
 *  </ul>
 */
public class SchemaGenTest {

    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        if (os.startsWith("Win")) {
            return true;
        }
        return false;
    }

    public static final boolean IS_WINDOWS = isWindows();

    // Make sure to give the process builder paths with the
    // correct slash for the test environment!

    // 'schemaGen' is a script in the server bin directory.
    public static final String WLP_BIN_DIR_WIN = "..\\build.image\\wlp\\bin";
    public static final String WLP_BIN_DIR_UNIX = "../build.image/wlp/bin";
    public static final String WLP_BIN_DIR = (IS_WINDOWS ? WLP_BIN_DIR_WIN : WLP_BIN_DIR_UNIX);
	public static File WLP_BIN = new File(WLP_BIN_DIR);

	// The script has a windows format (batch script) and a unix format (shell script).
    public static final String SCHEMAGEN_SCRIPT_WIN = ".\\schemaGen.bat";
    public static final String SCHEMAGEN_SCRIPT_UNIX = "./schemaGen";
    public static final String SCHEMAGEN_SCRIPT = (IS_WINDOWS ? SCHEMAGEN_SCRIPT_WIN : SCHEMAGEN_SCRIPT_UNIX);

    public static final File SCHEMAGEN = new File(WLP_BIN, SCHEMAGEN_SCRIPT);

    // The option used to request help from schemaGen.
    public static final String HELP_OPTION = "-help";

    // Output file used by the test.
    public static final String OUTPUT_FILE = "schemaGenOutput.xsd";
    public static final File OUTPUT = new File(WLP_BIN, OUTPUT_FILE);
    public static final String OUTPUT_PATH = OUTPUT.getAbsolutePath();
    
    // Parameters used to bound the script output and the script running time.
    public static final int MAX_OUTPUT_LINES = 500; // Reasonable maximum count of output lines.
    public static final long TIMEOUT_NS = 90_000_000_000L;  // Reasonable maximum time to run (90s).

    public static void displayEnv() {
    	System.out.println("Environment:");
    	System.out.println("OS [ os.name ] [ " + System.getProperty("os.name") + " ] isWindows [ " + IS_WINDOWS + " ]");
    	System.out.println("WLP Bin [ " + WLP_BIN_DIR + " ] [ " + WLP_BIN.getAbsolutePath() + " ] Exists [ " + WLP_BIN.exists() + " ]");
    	System.out.println("  Script [ " + SCHEMAGEN_SCRIPT + " ] [ " + SCHEMAGEN.getAbsolutePath() + " ] Exists [ " + SCHEMAGEN.exists() + " ]");
    	System.out.println("  Output [ " + OUTPUT_FILE + " ] [ " + OUTPUT_PATH + " ] Exists [ " + OUTPUT.exists() + " ]");    	
    }

    public static ProcessBuilder schemaGen() {
    	return schemaGen(null);
    }

    public static ProcessBuilder schemaGen(String option) {
    	ProcessBuilder pb;        	
    	if (IS_WINDOWS) {
    		if ( option == null ) {
    			pb = new ProcessBuilder("cmd", "/c", SCHEMAGEN_SCRIPT_WIN);
    		} else {
    			pb = new ProcessBuilder("cmd", "/c", SCHEMAGEN_SCRIPT_WIN, option);
    		}
    	} else {
    		if ( option == null ) {
    			pb = new ProcessBuilder(SCHEMAGEN_SCRIPT_UNIX);
    		} else {
    			pb = new ProcessBuilder(SCHEMAGEN_SCRIPT_UNIX, option);
    		}
    	}

    	pb.directory(WLP_BIN);
    	
    	// Only one stream is being captured.  If this redirect
    	// is not enabled, error output will not be captured.
    	pb.redirectErrorStream(true);

    	return pb;
    }

    /**
     * Process line processor.
     */
    public static class ProcessActor {
    	public ProcessActor() {
    		this.numLines = 0;
    	}

    	/**
    	 * Process a single line.
    	 * 
    	 * Increment the count of lines.
	     *
    	 * @param line A line which is to be processed.
    	 * 
    	 * @return The count of lines.  (Subclasses are expected
    	 *     to redefine the return value.)
    	 */
    	int process(String line) {
    		numLines++;
    		
    		return numLines;
    	}

    	private int numLines;
    	
    	/**
    	 * Tell how many lines were processed.
    	 *
    	 * @return The count of line which were processed.
    	 */
    	int getLines() {
    		return numLines;
    	}
    }
    
    /**
     * Launch a process and apply a process actor to its lines.
     * 
     * The process is bounded.  No more than {@link #MAX_OUTPUT_LINES}
     * will be accept, and the process will not be allowed to run longer
     * than {@link #TIMEOUT_NS} nano-seconds.  An except is thrown if
     * either bound is exceeded.
     * 
     * @param pBuild A process builder.
     * @param pAct A actor to apply to the lines from the process.
     * @return The return code from the process.
     *
     * @return The process return code.
     *
     * @throws Exception Thrown if the process did not launch, or if process
     *     lines could not be read, or if a process bound was exceeded.  An
     *     exception is not thrown if the process generates a failure return code.
     */
    public static int apply(ProcessBuilder pBuild, ProcessActor pAct) throws Exception {
    	System.out.println("Running [ " + pBuild + " ]:");
    	
        long startTime = System.nanoTime();
        int lines = 0;
            	
        Process p = null;
        try {
            p = pBuild.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = br.readLine()) != null) {
            	lines++;
                System.out.println(line);

                if (lines > MAX_OUTPUT_LINES) {
                    throw new Exception("Exceeded maximum output [ " + MAX_OUTPUT_LINES + " ] (lines)");
                } else if (System.nanoTime() - startTime >  TIMEOUT_NS) {
                    throw new Exception("Exceeded maximum process time [ " + TIMEOUT_NS + " ] (ns)");
                }

                pAct.process(line);
            }

        } finally {
            if (p != null) {
            	if ( !p.waitFor(30, TimeUnit.SECONDS) ) {
            		p.destroyForcibly();
            		fail("Process ran longer than 30 seconds");
            	}
            }
        }
        
        int rc = ((p == null) ? -1 : p.exitValue());
        System.out.println("Return code [ " + rc + " ]");
        return rc;
    }

    /**
     * A process actor used to detect specific values on lines.
     */
    public static class TagActor extends ProcessActor {
    	/**
    	 * Create a tag actor for specified tags.
    	 * 
    	 * See {@link #process(String)} for processing details.
    	 *
    	 * @param tags Values which are to be detected.
    	 */
    	public TagActor(String... tags) {
    		super();

    		Map<String, int[]> useHits =  new HashMap<>(tags.length);

    		for ( String tag : tags ) {
    			useHits.put(tag, new int[] { 0 });
    		}
    		
    		this.tagHits = useHits;
    	}

    	private final Map<String, int[]> tagHits;
    	
    	/**
    	 * Tell how many hits were recorded for a specified
    	 * tag.  Answer zero if the tag is not being detected
    	 * by this actor.
    	 * 
    	 * @param tag A tag value.
    	 * 
    	 * @return The number of hits which were recorded for the tag.
    	 */
    	public int getHits(String tag) {
    		int[] hits = tagHits.get(tag);
    		return ((hits == null) ? 0 : hits[0]);
    	}

    	private int totalHits;

    	/**
    	 * Answer the total number of hits for all tags.
    	 * 
    	 * @return The total number of hits for all tags.
    	 */
		public int getHits() {
			return totalHits;
		}

    	/**
    	 * Subclass extension: Record the tags which appear on the
    	 * line.  Each tag is detected at most once.  Multiple tags
    	 * will be detected on the same line.
    	 * 
    	 * Add the number of new hits to the total number of hits.
    	 * 
    	 * @param line A line which is to be processed.
    	 * 
    	 * @return The number of keys which were detected.
    	 */
    	@Override
    	public int process(String line) {
    		super.process(line); // Increment the number of lines

    		int hits = 0;
    		for ( String key : tagHits.keySet() ) {
    			if ( line.indexOf(key) != -1 ) {
    				(tagHits.get(key))[0]++;
    				hits++;
    			}
    		}
    		
    		totalHits += hits;

    		return hits;
    	}
    }

    /**
     * Test that when no parameters are passed, only basic usage info is displayed.
     */
    @Test
    public void testSchemaGenNoParms() throws Exception {
        System.out.println("==== testSchemaGenNoParms ====");
        displayEnv();
        
        TagActor pActor = new TagActor("Usage", "--encoding");
        int rc = apply( schemaGen(), pActor );

    	assertEquals("Unexpected return code", 0, rc);
    	
        int usageCount = pActor.getHits("Usage");
        int encodingCount = pActor.getHits("--encoding");

        assertTrue("'Usage' should appear", (usageCount > 0));
    	System.out.println("Detected 'Usage'");

        assertTrue("'--encoding' should NOT appear", (encodingCount == 0));
    	System.out.println("Did not detect detect '--encoding'");

        System.out.println("PASSED");
        System.out.println();        
    }

    /**
     * Test that when help parameters are passed, help info is displayed.
     */
    @Test
    public void testSchemaGenHelp() throws Exception {
        System.out.println("==== testSchemaGenHelp ====");
        displayEnv();
        
        TagActor pActor = new TagActor("Usage", "--encoding");
        int rc = apply( schemaGen(HELP_OPTION), pActor );

    	assertEquals("Unexpected return code", 0, rc);
    	
        int usageCount = pActor.getHits("Usage");
        int encodingCount = pActor.getHits("--encoding");

        assertTrue("'Usage' should appear", (usageCount > 0));
    	System.out.println("Detected 'Usage'");
    	
        assertTrue("'--encoding' should appear", (encodingCount > 0));
    	System.out.println("Detected '--encoding'");
    	
        System.out.println("PASSED");
        System.out.println();
    }
    
    /**
     * Test that when an output file is specified as parameter that the output file is created,
     * and that CWWKG0109I "success" message is created.
     */
    @Test
    public void testSchemaGenOutput() throws Exception {
        System.out.println("==== testSchemaGenOutput ====");
        displayEnv();

        // Make sure there isn't stale output from another test.
        OUTPUT.delete();
        assertFalse("Output [" + OUTPUT_PATH + "] does not exist", OUTPUT.exists());

        try {
        	TagActor pActor = new TagActor("CWWKG0109I");

        	int rc = apply( schemaGen(OUTPUT_FILE), pActor);

        	assertEquals("Unexpected return code", 0, rc);
        	
        	int msgCount = pActor.getHits("CWWKG0109I");
        	int numLines = pActor.getLines();
        
        	assertTrue("'CWWKG0109I' should appear once", (msgCount == 1));
        	System.out.println("Detected 'CWWKG0109I'");
            
        	assertTrue("Should have exactly one output line", (numLines == 1));
            assertTrue("Output [ " + OUTPUT_PATH + " ] exists", OUTPUT.exists());
            System.out.println("Detected output [" + OUTPUT_PATH + " ]");

        } finally {
        	// TODO: Would be nice to capture this output file in the build folder.

            // Don't leave the output behind.
            OUTPUT.delete();
            assertFalse("Output [" + OUTPUT_PATH + "] does not exist", OUTPUT.exists());
        }

        System.out.println("PASSED");
        System.out.println();        
    }
}
