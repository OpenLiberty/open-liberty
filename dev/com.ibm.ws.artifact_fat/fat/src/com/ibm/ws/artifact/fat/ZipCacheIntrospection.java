/*******************************************************************************
 * Copyright (c) 2019,2023 IBM Corporation and others.
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
package com.ibm.ws.artifact.fat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Marshal values from zip cache introspection output.
 */
public class ZipCacheIntrospection {
    private static void logInfo(String methodName, String text) {
        FATLogging.info(ZipCacheIntrospection.class, methodName, text);
    }

    private static String readLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        // logInfo("readLine", " [ " + line + " ]");
        return line;
    }
    
    /**
     * Tell if a specified line matches any of specified values.
     * 
     * @param line The line which is to be tested.
     * @param headers Values to test the line against.
     * 
     * @return True if the line equals any of the values.  Otherwise,
     *     false.
     */
    private static boolean matchAny(String line, String... headers) {
        for ( String header : headers ) {
            if ( line.equals(header) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Read lines from a reader until a line is read which matches
     * any of specified values, or until no additional lines can
     * be read.
     * 
     * Accumulate lines, except for a line matching the specified
     * values, putting line end characters after each line.
     * 
     * Clear the builder before and after retrieving the accumulated
     * text.  Answer the accumulated text.
     * 
     * @param builder A string builder into which to place read lines.
     * @param reader A reader from which lines are read.
     * @param headers Values which will terminate reading.
     * 
     * @return Accumulated text, up to but not including a terminating
     *     values.
     *     
     * @throws IOException Thrown if a read fails.
     */
    private static String readSection(
        StringBuilder builder, BufferedReader reader, String... headers)
            throws IOException {

        builder.setLength(0);

        String nextLine;
        while ( ((nextLine = readLine(reader)) != null) &&
                !matchAny(nextLine, headers) ) {
            builder.append(nextLine);
            builder.append("\n");
        }

        String result = builder.toString();
        builder.setLength(0);
        return result;
    }

    /**
     * Answer either null or a list of values, depending on whether
     * any of the values matches a target value.
     * 
     * @param lines Lines which are tested against the target value.
     * @param condition The target value.
     * 
     * @return The original lines, if none match the target value.
     *     Null if any of the lines matches the target value.
     */
    private static List<String> maybeNull(List<String> lines, String condition) {
        for ( String line : lines ) {
            if ( line.contains(condition) ) {
                return null;
            }
        }
        return lines;
    }
    
    /**
     * A simple reference type.  Used to return multiple values
     * from a method.
     *
     * @param <T> The referenced type.
     */
    private static class Ref<T> {
        public T referant;
        
        @SuppressWarnings("unused")
        public Ref() {
            this.referant = null;
        }
        
        public Ref(T referant) {
            this.referant = referant;
        }
    }

    /**
     * Read lines until a specified value is read, or until no lines
     * remain to be read.
     * 
     * @param reader A reader supplying lines.
     * @param headers Values which will terminate reading.
     * @return Lines up to a terminal value, or until no more lines
     *     are available.
     *     
     * @throws IOException Thrown if a read fails.
     */
    private static List<String> readLines(BufferedReader reader, String... headers)
        throws IOException {

        return readLines(reader, null, headers);
    }

    /**
     * Read lines until a specified value is read, or until no lines
     * remain to be read.  Place the last read line into the reference
     * parameter. (Store null if reading is terminated because no lines
     * remain to be read.)
     * 
     * @param reader A reader supplying lines.
     * @param lastLineRef A reference to hold the last line read.
     * @param headers Values which will terminate reading.
     * @return Lines up to a terminal value, or until no more lines
     *     are available.
     *     
     * @throws IOException Thrown if a read fails.
     */
    private static List<String> readLines(
        BufferedReader reader, Ref<String> lastLineRef, String... headers)
            throws IOException {

        List<String> lines = null;

        String nextLine;
        while ( ((nextLine = readLine(reader)) != null) &&
                !matchAny(nextLine, headers) ) {
            if ( lines == null ) {
                lines = new ArrayList<>();
            }
            lines.add(nextLine);
        }

        if ( lastLineRef != null ) {
            lastLineRef.referant = nextLine;
        }

        return ( (lines == null) ? Collections.emptyList() : lines );
    }    

    //

    /** Text which terminates zip introspector output. */
    private static final String EOF =
        "------------------------------------------------------------";

    @FunctionalInterface
    public interface InputStreamSource {
        InputStream open() throws IOException;
    }

    // [08/10/2023 12:28:08:538 EDT] 001 ZipCacheIntrospection          readLine                       
    // I  [ Active and Cached ZipFile Handles: ]
    // I  [   ZipFileHandle@0xd6369f0f(C:\\dev\\repos-pub\\ol-baw\\dev\\build.image\\wlp\\usr\\servers\\com.ibm.ws.artifact.zipIntrospections\\apps\\app1.war, 0) ]
    // I  [     [ WEB-INF/classes/com/ibm/ws/artifact/fat/zip/HoldingServlet.class:::3678503633:::1691684782621 ] [ 3833 bytes ] ]
    // I  [   ZipFileHandle@0x56bc5b7c(C:\\dev\\repos-pub\\ol-baw\\dev\\build.image\\wlp\\usr\\servers\\com.ibm.ws.artifact.zipIntrospections\\apps\\app2.war, 0) ]
    // I  [     [ WEB-INF/classes/com/ibm/ws/artifact/fat/zip/HoldingServlet.class:::3678503633:::1691684782621 ] [ 3833 bytes ] ]
    // I  [   ZipFileHandle@0x835c199e(C:\\dev\\repos-pub\\ol-baw\\dev\\build.image\\wlp\\usr\\servers\\com.ibm.ws.artifact.zipIntrospections\\workarea\\org.eclipse.osgi\\58\\data\\cache\\app2\\.cache\\WEB-INF\\lib\\TestJar.jar, 0) ]
    // I  [     [ META-INF/MANIFEST.MF:::2121629107:::1691684708000 ] [ 66 bytes ] ]
    // I  [     [ TestClass.class:::327105450:::1691684708000 ] [ 381 bytes ] ]
    // I  [   ZipFileHandle@0xdc9b834d(C:\\dev\\repos-pub\\ol-baw\\dev\\build.image\\wlp\\usr\\servers\\com.ibm.ws.artifact.zipIntrospections\\workarea\\org.eclipse.osgi\\58\\data\\cache\\app1\\.cache\\WEB-INF\\lib\\TestJar.jar, 0) ]
    // I  [     [ META-INF/MANIFEST.MF:::2121629107:::1691684708000 ] [ 66 bytes ] ]
    // I  [     [ TestClass.class:::327105450:::1691684708000 ] [ 381 bytes ] ]
    // I  [   ZipFileHandle@0xfdeb7340(/C:/dev/repos-pub/ol-baw/dev/build.image/wlp/usr/servers/com.ibm.ws.artifact.zipIntrospections/workarea/org.eclipse.osgi/58/data/cache/app1/.cache/WEB-INF/lib/TestJar.jar, 0) ]
    // I  [  ]
                    
    public ZipCacheIntrospection(InputStreamSource inputSource) throws IOException {
        this.zipFileData = null; // Computed on demand.

        // logInfo("<init>", "Reading ...");
        
        try ( InputStream input = inputSource.open();
              InputStreamReader inputReader = new InputStreamReader(input);
              BufferedReader reader = new BufferedReader(inputReader); ) {

            StringBuilder sectionBuilder = new StringBuilder();

            @SuppressWarnings("unused")
            String firstLine = readLine(reader);

            // "Liberty zip file caching diagnostics"            
            this.description = readLine(reader);

            readSection(sectionBuilder, reader, "Zip Caching Service:");

            this.zipCachingTime = readSection(sectionBuilder, reader, "Format:");
            this.outputFormat = readSection(sectionBuilder, reader, "Entry Cache Settings:");
            this.entryCacheSettings = readSection(sectionBuilder, reader, "Zip Reaper Settings:");
            this.zipReaperSettings = readSection(sectionBuilder, reader, "Active and Cached ZipFile Handles:");

            // "Active and Cached ZipFile Handles:" => "Zip Reaper:"
            this.activeAndCached =
                maybeNull( readLines(reader, "Zip Reaper:"), "  ** NONE **");

            // "Zip Reaper:" => "Active and Pending Data:" || EOF
            this.zipReaperValues =            
                maybeNull( readLines(reader, "Active and Pending Data:"), "  ** DISABLED **");

            if ( this.zipReaperValues == null ) {
                this.activeAndPending = null;                
                this.pendingQuick = null;
                this.pendingSlow = null;            
                this.completed = null;            

                // logInfo("<init>", "Reading ... complete");
                return;
            }

            // "Active and Pending Data:" => "Zip File Data [ pendingQuick ]"
            this.activeAndPending =
                maybeNull( readLines(reader, "Zip File Data [ pendingQuick ]"), "  ** NONE **");

            // "Zip File Data [ pendingQuick ]" => "Zip File Data [ pendingSlow ]"
            this.pendingQuick =
                maybeNull( readLines(reader, "Zip File Data [ pendingSlow ]"), "  ** NONE **");

            Ref<String> lastLineRef = new Ref<>(null);

            // "Zip File Data [ pendingSlow]" => "Zip File Data [ completed ]" | ...
            this.pendingSlow =
                maybeNull( readLines(reader, lastLineRef,
                            "Zip File Data [ completed ]",
                            "Completed zip file data is not being tracked",
                            EOF),
                            "  ** NONE **"); 

            // check the current line if it stopped at the start of completed
            // storage or the non tracked line

            String lastLine = lastLineRef.referant;

            if ( lastLine == null ) {
                throw new IOException("Unexpected end of input");

            } else if ( lastLine.equals("Zip File Data [ completed ]") ) {
                this.completed = maybeNull( readLines(reader, EOF), "  ** NONE **"); 

            } else if ( lastLine.equals("Completed zip file data is not being tracked") ) {
                this.completed = null;

            } else if ( lastLine.equals(EOF) ) {
                throw new IOException("Unexpected Zip Introspector EOF");
            } else {
                throw new IOException("Malformed Zip Introspector Output");
            }
        }
        
        // logInfo("<init>", "Reading ... complete");        
    }
    
    //
    
    private final String description;
    
    private final String zipCachingTime;
    private final String outputFormat;
    private final String entryCacheSettings;
    private final String zipReaperSettings;
    
    private final List<String> activeAndCached;
    
    private final List<String> zipReaperValues;
    private final List<String> activeAndPending;
    private final List<String> pendingQuick;
    private final List<String> pendingSlow;
    private final List<String> completed;

    public String getDescription() {
        return description;
    }

    public String getZipCachingTime() {
        return zipCachingTime;
    }
    
    public String getOutputFormat() {    
        return outputFormat;
    }

    public String getEntryCacheSettings() {
        return entryCacheSettings;
    }

    public String getZipReaperSettings() {
        return zipReaperSettings;
    }

    public List<String> getZipReaperValues() {
        return zipReaperValues;
    }

    public List<String> getActiveAndCached() {
        return activeAndCached;
    }
    
    public List<String> getActiveAndPending() {
        return activeAndPending;
    }

    public List<String> getPendingQuick() {
        return pendingQuick;
    }

    public List<String> getPendingSlow() {
        return pendingSlow;
    }

    public List<String> getCompleted() {
        return completed;
    }

    //

    // The pattern captures the file name at the end of the path,
    // including the comma and directory separator:
    // 
    // "open-liberty/dev/build.image/wlp/usr/servers/com.ibm.ws.artifact.zipReaper/apps" +
    //   "/testServlet1.war,"
    //
    // For each line, match for the archive name and remove the comma and
    // directory separator

    private static final Pattern fileHandlesPattern =
        Pattern.compile("[/\\\\][^/:\\*\\?\\\"<>\\|\\\\]+\\.[ewj]ar,");
    
    public List<String> getZipHandleArchiveNames() {
        List<String> useZipFileHandles = getActiveAndCached();
        if ( useZipFileHandles == null ) {
            return null;
        }

        List<String> handles = new ArrayList<String>();

        useZipFileHandles.forEach( (String line) -> {
            if ( line.contains("ZipFileHandle@0x") ) {
                String handle = findGroup(line, fileHandlesPattern, "\\/,"); 
                if ( handle != null ) {
                    handles.add(handle);
                }
            }
        } );

        return handles;
    }

    private static final Pattern statePattern = Pattern.compile("\\[.+\\]");
    
    public String getZipReaperThreadState() {
        List<String> values = getZipReaperValues();
        if ( values == null ) {
            return null;
        }
        
        for ( String valuesLine : values ) {
            if ( !valuesLine.contains("State") ) {
                continue;
            }

            String state = findGroup(valuesLine, statePattern, "[]");
            if ( state != null ) {
                return state;
            }
        }

        return null;
    }

    // Next Delay [ INDEFINITE (s) ]
    // Next Delay [ ######## (s) ]
    
    private static final Pattern delayPattern = Pattern.compile("\\[ .+ \\]");

    public String getZipReaperRunnerDelay() {
        List<String> values = getZipReaperValues();
        if ( values == null ) {
            return null;
        }
        
        for ( String valueLine : values ) {
            if ( !valueLine.contains("Next Delay") ) {
                continue;
            }
            
            String delay = findGroup(valueLine, delayPattern, "[]()s");
            if ( delay != null ) {
                return delay;
            }
        }

        return null;
    }

    private static final Pattern namePattern =
        Pattern.compile("[^/\\\\]+\\.[ewj]ar");

    public List<String> getOpenAndActiveArchiveNames() {
        List<String> introspections = getActiveAndPending();

        if ( introspections == null ) {
            return Collections.emptyList();
        }

        List<String> names = new ArrayList<String>();
        
        introspections.forEach( (String introspection) -> {
            if ( !introspection.contains("ZipFile") ) {
                return;
            }
            String name = findGroup(introspection, namePattern);
            if ( name != null ) {
                names.add(name);
            }
        } );
        
        return names;
    }

    // pendingQuick and pendingSlow introspections are sparse so the full
    // output will only be in active/pending and completed
    
    private List<String> zipFileData;
    
    public List<String> getAllZipFileData() {
        if ( zipFileData == null ) {
            List<String> allZipFileData = new ArrayList<String>();

            collectZipFiles( allZipFileData, getActiveAndPending() );
            collectZipFiles( allZipFileData, getCompleted() );

            zipFileData = allZipFileData;
        }
        
        return zipFileData;
    }

    // pattern for the first line of the ZipFileData.introspect() output    
    private static final Pattern zipFilePattern = Pattern.compile("ZipFile\\s\\[\\s.+\\s\\]");
    
    private static void collectZipFiles(List<String> sink, List<String> source) {
        if ( source == null ) {
            return;
        }

        StringBuilder builder = new StringBuilder();

        for ( String sourceLine : source ) {
            if ( sourceLine.isEmpty() ) {
                continue;
            }

            if ( find(sourceLine, zipFilePattern) ) {
                maybeTransfer(builder, sink);
            }

            builder.append(sourceLine);
            builder.append('\n');
        }

        maybeTransfer(builder, sink);
    }
    
    private static void maybeTransfer(StringBuilder builder, List<String> sink) {
        if ( builder.length() != 0 ) {
            sink.add( builder.toString() );
            builder.setLength(0);
        }        
    }

    /**
     * Tell if text has a specified pattern.
     * 
     * @param text Text which is to be tested.
     * @param pattern The target pattern.
     * 
     * @return True or false telling if the text matches the pattern.
     */
    private static boolean find(String text, Pattern pattern) {
        Matcher match = pattern.matcher(text);
        return match.find();
    }

    /**
     * Answer the first group from text which matches a pattern.
     * 
     * @param text Text which is to be tested.
     * @param pattern The target pattern.
     * 
     * @return The first group of the pattern, if the text matches
     *    pattern.  Null if the text does not match the pattern.
     */
    private static String findGroup(String text, Pattern pattern) {
        Matcher match = pattern.matcher(text);
        return ( match.find() ? match.group() : null );
    }

    /**
     * Answer the first group from text which matches a pattern.
     * Remove specified characters from the group.
     * 
     * @param text Text which is to be tested.
     * @param pattern The target pattern.
     * @param toRemove Character which are to be removed from the
     *     match group.
     *     
     * @return The first group of the pattern, if the text matches
     *    pattern, with the specific characters removed.  Null if
     *    the text does not match the pattern.  Trim white space
     *    from the returned group.
     */
    private static String findGroup(String text, Pattern pattern, CharSequence toRemove) {
        String group = findGroup(text, pattern);
        if ( group == null ) {
            return null;
        }

        for ( int charNo = 0; charNo < toRemove.length(); charNo++ ) {
            group = group.replace( toRemove.subSequence(charNo, charNo + 1), "" );
        }
        return group.trim();
    }
}