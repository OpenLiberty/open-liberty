/*******************************************************************************
 * Copyright (c) 2009, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

public class FileLogger {
    public static final String CLASS_NAME = FileLogger.class.getSimpleName();

    public static final String PATTERN_PROPERTY_NAME = "LTI_TRACE_PATTERN";

    public static final String pattern = System.getProperty(PATTERN_PROPERTY_NAME);

    static {
    	System.out.println("LTI: Logging pattern [ " + pattern + " ]");
    }

    public static boolean isLoggable(String className) {
        return true;
  //  	className = className.replace('/', '.');
//    	return ( (pattern != null) && !pattern.isEmpty() && className.contains(pattern) );
    }

    //
    
    public static byte[] read(InputStream inputStream) throws IOException {
    	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    	
    	byte[] buffer = new byte[32 * 1024];

    	int read;
    	while ( (read = inputStream.read(buffer)) != -1 ) {
    		outputStream.write(buffer, 0, read);
    	}

    	return outputStream.toByteArray();
    }
    
    //
    
    public static String getTmpPath() {
        return System.getProperty("libertyInstallPath")+"/usr";
    }

    //

    public static long getTime() {
        return System.currentTimeMillis();
    }

    private static final SimpleDateFormat formatter =
    	new SimpleDateFormat("MM/dd/yy HH:mm:ss:SSS z");  // 03/22/16 12:01:13:654 ESD

    private static final Date current = new Date( getTime() );
    private static String currentFormatted = formatter.format(current);

    public static String getFormattedTime() {
    	long currentMs = System.currentTimeMillis();
    	if ((currentMs - current.getTime()) > 10)  {
    		current.setTime(currentMs);
    		currentFormatted = formatter.format(current);
    	}
    	return currentFormatted;
    }

    //

	public static final FileLogger fileLogger = FileLogger.create();
	
	static {
		if ( fileLogger == null ) {
	    	System.out.println("LTI: Logging is disabled");
		} else {
	    	System.out.println("LTI: Logging is enabled");
		}
	}

	public static PrintWriter fileWriter() {
		return ( (fileLogger == null) ? null : new PrintWriter( fileLogger.outputPrinter ) );
	}
	
	public static void fileLog(String className, String methodName, String text) {
		if ( fileLogger != null ) {
			fileLogger.log(className, methodName, text);
		}
	}
	
	public static void fileLog(String className, String methodName, String text, Object value) {
		if ( fileLogger != null ) {
			fileLogger.log(className, methodName, text, value);
		}
	}
	
	public static void fileDump(String className, String methodName, String text, byte[] bytes) {
		if ( fileLogger != null ) {
			fileLogger.dump(className, methodName, text, bytes);
		}
	}

	public static void fileStack(String className, String methodName, String text, Throwable th) {
		if ( fileLogger != null ) {
			fileLogger.logStack(className, methodName, text, th);
		}
	}
	
    //

    public static final String ENABLED_PROPERTY_NAME     = "LTI_TRACE_ENABLED";
    public static final String DIR_PROPERTY_NAME         = "LTI_TRACE_DIR";
    public static final String FILE_PROPERTY_NAME        = "LTI_TRACE_FILE";
    public static final String FILE_EXT_PROPERTY_NAME    = "LTI_TRACE_EXT"; 
    public static final String PREFIX_PROPERTY_NAME      = "LTI_TRACE_PREFIX";
    public static final String AUTOFLUSH_PROPERTY_NAME   = "LTI_TRACE_AUTOFLUSH";
    
    public static void transfer(Properties properties, String ... propertyNames) {
    	for ( String propertyName : propertyNames ) {
    		String propertyValue = System.getProperty(propertyName);
    		if ( propertyValue != null ) {
    			properties.setProperty(propertyName, propertyValue);
    		}
    	}
    }

    public static FileLogger create() {
    	Properties properties = new Properties();

    	transfer(properties,
    			 ENABLED_PROPERTY_NAME,
    			 DIR_PROPERTY_NAME,
    			 FILE_PROPERTY_NAME,
    			 FILE_EXT_PROPERTY_NAME,
    			 PREFIX_PROPERTY_NAME,
    			 AUTOFLUSH_PROPERTY_NAME);

    	return create(properties);
    }

    public static FileLogger create(File logFile, String prefix, boolean autoflush) {
    	Properties properties = new Properties();
    	
    	File parent = logFile.getParentFile();
    	String parentPath = ((parent == null) ? "." : parent.getPath());

    	String logName = logFile.getName();
    	int extOffset = logName.lastIndexOf('.');

    	String baseLogName;
    	String logExt;
    	if ( extOffset == -1 ) {
    		baseLogName = logName;
    		logExt = null;
    	} else {
    		baseLogName = logName.substring(0, extOffset);
    		logExt = logName.substring(extOffset); // Include the "."
    	}

    	properties.setProperty(ENABLED_PROPERTY_NAME, Boolean.TRUE.toString());
    	properties.setProperty(DIR_PROPERTY_NAME, parentPath);
    	properties.setProperty(FILE_PROPERTY_NAME, baseLogName);
    	if ( logExt != null ) {
    		properties.setProperty(FILE_EXT_PROPERTY_NAME, logExt);
    	}

		properties.setProperty(PREFIX_PROPERTY_NAME, prefix);
		properties.setProperty(AUTOFLUSH_PROPERTY_NAME, Boolean.valueOf(autoflush).toString());

    	return create(properties);
    }

    public static FileLogger create(Properties properties) {    
        String enabledValue = properties.getProperty(ENABLED_PROPERTY_NAME);
        boolean enabled = ( (enabledValue != null) && enabledValue.equalsIgnoreCase("true") );
        if ( !enabled ) {
        	return null;
        }

        String dirName = properties.getProperty(DIR_PROPERTY_NAME);
        if ( dirName == null ) {
        	dirName = FileLogger.getTmpPath();
        }

        String fileName = properties.getProperty(FILE_PROPERTY_NAME);
        if ( fileName == null ) {
        	fileName = "LTInject";
        }
            
        String fileExt = properties.getProperty(FILE_EXT_PROPERTY_NAME);
        if ( fileExt == null ) {
        	fileExt = ".log";
        }

        String prefix;
        String prefixPropertyValue = properties.getProperty(PREFIX_PROPERTY_NAME);
        if ( prefixPropertyValue != null ) {
        	prefix = prefixPropertyValue;
        } else {
        	prefix = "LTI: ";
        }
        
        boolean autoflush;
        String autoflushPropertyValue = properties.getProperty(AUTOFLUSH_PROPERTY_NAME);
        if ( autoflushPropertyValue != null ) {
        	autoflush = Boolean.valueOf(autoflushPropertyValue);
        } else {
        	autoflush = false;
        }

        return new FileLogger(dirName, fileName, fileExt, prefix, autoflush);
    }

    //
    
    public static final boolean AUTOFLUSH = true;
    public static final boolean DO_APPEND = true;        

    public FileLogger(
    	String outputDirPath, String outputPrefix, String outputSuffix,
    	String debugPrefix,
    	boolean autoflush) {

        String methodName = "init";

        this.textPrefix = debugPrefix;
        this.autoflush = autoflush;

        File useOutputFile = null;
        
        if ( (outputDirPath != null) || (outputPrefix != null) ) {
        	if ( outputPrefix == null ) {
        		outputPrefix = "LTI: ";
        	}
        	if ( outputSuffix == null ) {
        		outputSuffix = ".log";
        	}

            File outputDir;
            String actualOutputDirPath = null;

        	if ( outputDirPath == null ) {
        		outputDir = new File(".");
        		actualOutputDirPath = outputDir.getAbsolutePath();
        		System.out.println("LTI: Logging [ " + outputPrefix + " ] [ " + outputSuffix + " ] to current directory [ " + actualOutputDirPath + " ]");

        	} else {
                outputDir = new File(outputDirPath);
        		actualOutputDirPath = outputDir.getAbsolutePath();

                if ( !outputDir.exists() ) {
                	outputDir.mkdirs();
                	if ( !outputDir.exists() ) {
                		System.out.println("LTI: ERROR: Logging [ " + outputPrefix + " ] [ " + outputSuffix + " ] failed to create directory [ " + actualOutputDirPath + " ]");
                		outputDir = null;
                	} else {
                    	System.out.println("LTI: Logging [ " + outputPrefix + " ] [ " + outputSuffix + " ] to new directory [ " + actualOutputDirPath + " ]");
                	}
                } else {
                	System.out.println("LTI: Logging [ " + outputPrefix + " ] [ " + outputSuffix + " ] to existing directory [ " + actualOutputDirPath + " ]");
                }
        	}

        	if ( outputDir != null ) {
        		try {
        			useOutputFile = File.createTempFile(outputPrefix, outputSuffix, outputDir);
        		} catch ( IOException e ) {
        			System.out.println("LTI: ERROR: Failed to create [ " + outputPrefix + " ] [ " + outputSuffix + " ] [ " + actualOutputDirPath + " ]");
        			e.printStackTrace(System.out);
        		}
        	}
        }

    	this.outputFile = useOutputFile;
    	
    	String useOutputPath;
        OutputStream useOutputStream;
        PrintStream useOutputPrinter;

        if ( this.outputFile == null ) {
        	useOutputPath = null;
            useOutputStream = null;
            useOutputPrinter = System.out;
			System.out.println("LTI: Logging to Standard Output");

        } else {
        	useOutputPath = this.outputFile.getAbsolutePath();
        	
            try {
                useOutputStream = new FileOutputStream(this.outputFile, DO_APPEND);
                useOutputPrinter = new PrintStream(useOutputStream, this.autoflush);
    			System.out.println("LTI: Logging to [ " + useOutputPath + " ]");        			

            } catch ( IOException e ) {
    			System.out.println("LTI: ERROR: Unable to write to output file [ " + useOutputPath + " ]");
            	e.printStackTrace(System.out);

            	useOutputPath = null;
                useOutputStream = null;
                useOutputPrinter = System.out;
    			System.out.println("LTI: Logging to Standard Output");                
            }
        }

        this.outputPath = useOutputPath;
        this.outputStream = useOutputStream;
        this.outputPrinter = useOutputPrinter;

        //

        Object output = ( (this.outputFile != null) ? this.outputFile.getAbsolutePath() : "[ System.out ]");
        this.log(CLASS_NAME, methodName, "Output to", output); 
    }

    public final boolean autoflush;

    public final File outputFile;
    public final String outputPath;

    public final OutputStream outputStream;
    public final PrintStream outputPrinter;

    protected void rawLog(String text) {
    	outputPrinter.println(text);
    }
    
    public final String textPrefix;

    public synchronized void log(String text) {
        rawLog(head() + text);
    }        
    
    public synchronized void log(String className, String text) {
        rawLog(head() + className + ": " + text);
    }        
        
    public synchronized void log(String className, String methodName, String text) {
        rawLog(head() + className + ": " + methodName + ": " + text);
    }        

    public synchronized void log(String className, String methodName, String text, Object value) {
        rawLog(head() + className + ": " + methodName + ": " + text + " [ " + value + " ]");
    }        

    //
    
    public synchronized void logStack(String text) {
        (new Throwable(head() + text)).printStackTrace(outputPrinter);
    }

    public synchronized void logStack(String className, String text) {
        (new Throwable(head() + className + ": " + text)).printStackTrace(outputPrinter);
    }

    public synchronized void logStack(String className, String methodName, String text) {
        (new Throwable(head() + className + ": " + methodName + ": " + text)).printStackTrace(outputPrinter);
    }

    public synchronized void logStack(String text, Throwable th) {
    	log(text);
        th.printStackTrace(outputPrinter);
    }

    public synchronized void logStack(String className, String text, Throwable th) {
    	log(className, text);
        th.printStackTrace(outputPrinter);
    }

    public synchronized void logStack(String className, String methodName, String text, Throwable th) {
    	log(className, methodName, text);
        th.printStackTrace(outputPrinter);
    }

    //

    public synchronized void dump(String text, byte[] bytes) {
    	String header = head() + text;
    	rawDump(header, bytes);
    }

    public synchronized void dump(String className, String text, byte[] bytes) {
        String header = head() + className + ": " + text;
        rawDump(header, bytes);
    }            
    
    public synchronized void dump(String className, String methodName, String text, byte[] bytes) {
        String header = head() + className + ": " + methodName + ": " + text;
        rawDump(header, bytes);
    }                

    //

    public Thread getCurrentThread() {
    	return Thread.currentThread();
    }
    
    public static String getLongId(Thread thread) {
    	return thread.toString();
    }
    
    private final Map<Thread, String> shortIds = new WeakHashMap<>();
    private int lastId = 0;

    private static final String ZERO_FILL = "000000"; 
    private static final int ZERO_FILL_LENGTH = 6;
    
    private String computeShortId(Thread thread) {
		String longId = getLongId(thread);
		
		String rawId = Integer.toHexString(++lastId);
		int rawIdLen = rawId.length();
		
		String shortId = "Thread-";
		if ( rawIdLen < ZERO_FILL_LENGTH ) {
			shortId += ZERO_FILL.substring(rawIdLen );
		}
		shortId += rawId;

    	String head = head(shortId);
		rawLog( head + "Assigned thread ID [ " + longId + " ] [ " + shortId + " ]" );

		if ( lastId % 20 == 0 ) {
			displayThreadIds(head);
		}

		return shortId;
    }

    private static final String SPACES = "                                        ";
    private static final int NUM_SPACES = 40;

    private void displayThreadIds(String head) {
		rawLog( head + "Thread assignments:");
		
		final int LONG_ID = 0;
		final int SHORT_ID = 1;

		List<String[]> assignments = new ArrayList<>( shortIds.size() );

		int maxLong = 0;

		for ( Map.Entry<Thread, String> idEntry : shortIds.entrySet() ) {
			Thread t = idEntry.getKey();
			String shortId = idEntry.getValue();
			
			String longId = getLongId(t);
			int longLen = longId.length();
			if ( longLen > maxLong ) {
				maxLong = longLen;
			}

			assignments.add( new String[] { longId, shortId } );
		}

		if ( maxLong > NUM_SPACES ) {
			maxLong = NUM_SPACES;
		}

		assignments.sort( (String[] a1, String[] a2) -> {
			return ( a1[SHORT_ID].compareTo(a2[SHORT_ID]) );
		});

		int useMaxLong = maxLong; // Java needs this to be final.

		assignments.forEach( (assignment) -> {
			String longId = assignment[LONG_ID];

			String spaces;
			int longIdLen = longId.length();
			if ( longIdLen > useMaxLong ) {
				longIdLen = useMaxLong;
			}
			spaces = SPACES.substring(longIdLen, useMaxLong);

			String shortId = assignment[SHORT_ID];
			
			rawLog( head + ":  [ " + longId + spaces + " ] [ " + shortId + " ]");			
		});
    }
    
    private String getShortId(Thread thread) {
    	return shortIds.computeIfAbsent(thread, this::computeShortId );
    }
    
    private String head() {
    	return head( getShortId( getCurrentThread() ) );
    }

    private String head(String threadId) {
    	return "[ " + getFormattedTime() + " ] [ " + threadId + " ] " + textPrefix;
    }    

    private void rawDump(String header, byte[] bytes) { 
    	String tail = " [ " + bytes.length + " ]";
        rawLog(header + tail + ": BEGIN");
        dump(bytes);
        rawLog(header + tail + ": END");
    }   

    public static final int BYTES_PER_ROW = 16;
    public static final int BYTE_INDENT = 4;
    public static final String INDENT = "    ";

    private void dump(byte[] bytes) {
    	int len = bytes.length;
    	if ( len == 0 ) {
    		return;
    	}

    	int rows = len / BYTES_PER_ROW;
    	int rem = len % BYTES_PER_ROW;
    	int partialRow = ((rem == 0) ? 0 : 1);
    	
    	StringBuilder builder = new StringBuilder(BYTE_INDENT + ((BYTES_PER_ROW - 1) * 3) + 2);
    	
    	for ( int rowNo = 0; rowNo < rows + partialRow; rowNo++ ) {
    		int start = rowNo * BYTES_PER_ROW;
    		int end = start + ((rowNo == rows) ? rem : BYTES_PER_ROW);
    		
    		builder.append(INDENT);

    		for ( int byteNo = start; byteNo < end; byteNo++ ) {
    			if ( byteNo > start ) {
    				builder.append(' ');
    			}
    			String nextHex = Integer.toHexString(((int) bytes[byteNo]) & 0xFF);
    			if ( nextHex.length() < 2 ) {
    				builder.append('0');
    			}
    			builder.append(nextHex);
    		}

    		String output = builder.toString();
    		builder.setLength(0);

    		rawLog(output);
    	}
    }
}
