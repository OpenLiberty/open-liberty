/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.util;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import wlp.lib.extract.platform.PlatformUtils;

/**
 * File utilities for server installation testing.
 */
public class FileUtils {
    public static final String CLASS_NAME = FileUtils.class.getSimpleName();

    public static void log(String message) {
        System.out.println(message);
    }

    //
    
    public static final String OS_NAME = System.getProperty("os.name");
    public static final boolean IS_WINDOWS = ( (OS_NAME != null) && OS_NAME.toLowerCase().contains("windows") );

    //

    public static final String TEST_OUTPUT_PATH = "build/test";

    public static final String TEST_OUTPUT_PATH_ABS = normalize( (new File(TEST_OUTPUT_PATH)).getAbsolutePath() );
    public static final File TEST_OUTPUT = new File(TEST_OUTPUT_PATH_ABS);

    public static long getOutputTotalSpace() {
        return TEST_OUTPUT.getTotalSpace();
    }

    public static long getOutputFreeSpace() {
        return TEST_OUTPUT.getFreeSpace();
    }
    
    public static long getOutputUsableSpace() {
        return TEST_OUTPUT.getUsableSpace();
    }
    
    public static void logOutput() {
        log("Test output path [ " + TEST_OUTPUT_PATH + " ]");
        log("  Total  [ " + getOutputTotalSpace() + " ]");
        log("  Free   [ " + getOutputFreeSpace() + " ]");
        log("  Usable [ " + getOutputUsableSpace() + " ]");        
    }

    static {
        log("OS name [ os.name ]: [ " + OS_NAME + " ]");
        log("Windows [ " + IS_WINDOWS + " ]");

        logOutput();
    }

    public static void verifySpace(long required) {
        long usable = getOutputUsableSpace();

        if ( usable < required ) {
            fail("Usable space [ " + usable + " ] is less than the required space [ " + required + " ]");
        }

        log("Verified usable space [ " + usable + " ] is less than the required space [ " + required + " ]");        
    }
    
    //
    
    //

    public static String describe(String[] parts) {
        StringBuilder description = new StringBuilder();

        boolean onFirst = true;
        for ( String part : parts ) {
            if ( part == null ) {
                part = "*";
            }
            if ( onFirst ) {
                onFirst = false;
            } else {
                description.append('-');
            }
            description.append(part);
        }
        return description.toString();
    }
    
    public static boolean match(String[] parts, String name) {
        int numParts = parts.length;
        int nameLen = name.length();

        boolean partsMatch = true;
        int partNo = 0;

        int lastPos = 0;

        while ( partsMatch && (partNo < numParts) && (lastPos < nameLen) ) {
            int nextPos = name.indexOf(lastPos, '-');
            if ( nextPos == -1 ) {
                nextPos = nameLen;
            }

            String nextPart = parts[partNo];
            if ( nextPart != null ) {
                int partLen = nextPart.length();
                if ( ((nextPos - lastPos) != partLen) ||
                     !name.regionMatches(lastPos, nextPart, 0, partLen) ) {
                    partsMatch = false;
                }
            }

            lastPos = nextPos + 1;
            partNo++;
        }
        
        return ( partsMatch && (partNo == numParts) );
    }
        
    //

    public static String normalize(String path) {
        if ( File.separatorChar == '/' ) {
            return path;
        } else {
            return path.replace('\\', '/');
        }
    }

    public static void normalize(String[] paths) {
        if ( File.separatorChar == '/' ) {
            return;
        } else {
            for ( int pathNo = 0; pathNo < paths.length; pathNo++ ) {
                paths[pathNo] = paths[pathNo].replace('\\',  '/');
            }
        }
    }
    
    //

    public static String removeEnds(String value, String prefix, String suffix) {
        return value.substring(
                prefix.length(),
                value.length() - suffix.length() );
    }

    //

    public static void setPermissions(String path, String permissions) throws IOException {
        PlatformUtils.chmod( new String[] { path }, permissions );

        File[] children = (new File(path)).listFiles();
        if ( children == null ) {
            return;
        }
        
        String[] childPaths = new String[ children.length ];
        for ( int childNo = 0; childNo < children.length; childNo++ ) {
            childPaths[childNo] = children[childNo].getPath();
        }

        PlatformUtils.chmod(childPaths, permissions);
    }
    
    public static final String IGNORE_EXTENSION = null;
    public static final boolean FLATTEN = true;

    public static String extract(String sourcePath, String targetPath) throws IOException { 
        return extract(sourcePath, targetPath, IGNORE_EXTENSION, !FLATTEN);
    }

    public static String extract(String sourcePath, String targetPath, String extension, boolean flatten) throws IOException {
        extract( new File(sourcePath), new File(targetPath), extension, flatten);
        return targetPath;
    }

    public static void extract(File source, File target, String extension, boolean flatten) throws IOException {
        target.mkdirs();

        byte[] buffer = new byte[1024 * 32];

        try ( InputStream inputStream = new FileInputStream(source);
              ZipInputStream zipStream = new ZipInputStream(inputStream) ) {

            ZipEntry nextEntry;
            while ( (nextEntry = zipStream.getNextEntry()) != null ) {
                if ( nextEntry.isDirectory() ) {
                    continue;
                }                

                String entryName = nextEntry.getName();

                if ( (extension != null) && !entryName.toLowerCase().endsWith(extension) ) {                
                    continue;
                }
                
                if ( flatten ) {
                    int slashLoc = entryName.lastIndexOf('/');
                    if ( slashLoc != -1 ) {
                        entryName = entryName.substring(slashLoc + 1);
                    }
                }

                File targetChild = new File(target, entryName);
                
                if ( !flatten ) {
                    targetChild.getParentFile().mkdirs();
                }

                transfer(zipStream, targetChild, buffer);
            }
        }
    }

    private static void transfer(InputStream input, File target, byte[] buffer) throws IOException {
        try ( OutputStream output = new FileOutputStream(target) ) {
            int b;
            while ( (b = input.read(buffer)) != -1 ) {
                output.write(buffer, 0, b);
            }
        }
    }

    public static void compare(
            Set<String> set1, Set<String> set2,
            List<String> in1Only, List<String> in2Only, List<String> inBoth) {

        compare(set1, set2, in1Only, inBoth);
        compare(set2, set1, in2Only, null);        
    }

    public static void compare(
            Set<String> set1, Set<String> set2,
            List<String> in1Only, List<String> in1And2,
            List<String> in2Only, List<String> in2And1) {

        compare(set1, set2, in1Only, in1And2);
        compare(set2, set1, in2Only, in2And1);
    }

    public static void compare(
            Set<String> set1, Set<String> set2,
            List<String> in1Only, List<String> in1And2) {

        for ( String e1 : set1 ) {
            if ( set2.contains(e1) ) {
                if ( in1And2 != null ) {
                    in1And2.add(e1);
                }
            } else {
                if ( in1Only != null ) {
                    in1Only.add(e1);
                }
            }
        }
    }
    
    public static void verify(String tag, List<String> expected, List<String> actual) {
        Set<String> expectedSet = new HashSet<>(expected);
        Set<String> actualSet = new HashSet<>(actual);
        verify(tag, expectedSet, actualSet);
    }

    public static void verify(String tag, Set<String> expected, Set<String> actual) {
        List<String> missing = new ArrayList<>();
        List<String> extra = new ArrayList<>();
        List<String> common = new ArrayList<>();
    
        compare(expected, actual, missing, extra, common);
    
        log("Compare [ " + tag + " ]");

        if ( common.isEmpty() ) {
            log("No common were detected!");
        } else {
            log("Common:");
            for ( String element : common ) {
                log("[ " + element + " ]");
            }
        }
        if ( !missing.isEmpty() ) {
            log("Missing:");
            for ( String element : missing ) {
                log("[ " + element + " ]");
            }
        }

        if ( !extra.isEmpty() ) { 
            log("Extra:");
            for ( String element : extra ) {
                log("[ " + element + " ]");
            }
        }
    
        if ( !missing.isEmpty() || !extra.isEmpty() ) {
            fail("Compare of [ " + tag + " ]: Actual does not match expected");
        }
    }
    
    public static List<String> load(String path) throws IOException {
        return load( new File(path) );
    }

    public static List<String> load(File target) throws IOException {
        List<String> lines = new ArrayList<String>();

        try ( FileReader fr = new FileReader(target);
              BufferedReader br = new BufferedReader(fr) ) {
            String line;            
            while ( (line = br.readLine()) != null ) {
                lines.add(line);
            }
        }
        
        return lines;
    }
    
    public static void save(String path, List<String> lines) throws IOException {
        save( new File(path), lines );
    }

    public static void save(File target, List<String> lines) throws IOException {
        try ( FileWriter fw = new FileWriter(target);
              BufferedWriter bw = new BufferedWriter(fw) ) {
            for ( String line : lines ) {
                bw.write(line);
            }
        }
    }    
    
    public static final boolean REQUIRED = true;
    public static final boolean FORBIDDEN = false;

    public static List<String> selectMissing(List<String> lines, String[] expressions) {
        return verify(lines, expressions, REQUIRED);
    }
    
    public static List<String> selectExtra(List<String> lines, String[] expressions) {
        return verify(lines, expressions, FORBIDDEN);        
    }

    public static List<String> verify(List<String> lines, String[] expressions, boolean isRequired) {
        Map<String, Pattern> patterns = new HashMap<>(expressions.length);
        for ( String expression : expressions ) {
            patterns.put( expression, Pattern.compile(expression) );
        }            

        List<String> selected = null;

        for ( Map.Entry<String, Pattern> patternEntry : patterns.entrySet() ) {
            String firstLine = null;
            for ( String line : lines ) {
                if ( patternEntry.getValue().matcher(line).matches() ) {
                    firstLine = line;
                    break;
                }
            }

            if ( (firstLine == null) == isRequired ) {
                if ( selected == null ) {
                    selected = new ArrayList<>();
                }
                selected.add( patternEntry.getKey() );
            }
        }

        return selected;
    }

    public static List<String> select(File target, String regexInclude) throws IOException {
        return selectLines(target, regexInclude, EXCLUDE_NONE);
    }
    
    public static List<String> reject(File target, String regexExclude) throws IOException {
        return selectLines(target, INCLUDE_ALL, regexExclude);
    }
    
    public static List<String> selectLines(File target, String regexInclude, String regexExclude) throws IOException {
        List<String> matches = new ArrayList<String>();

        try ( FileReader fr = new FileReader(target);
              BufferedReader br = new BufferedReader(fr) ) {

            Pattern include = ( (regexInclude == null) ? null : Pattern.compile(regexInclude) );
            Pattern exclude = ( (regexExclude == null) ? null : Pattern.compile(regexExclude) );
            
            String line;            
            while ( (line = br.readLine()) != null ) {
                if ( (include != null) && !include.matcher(line).matches() ) {
                    continue;
                }
                if ( (exclude != null) && exclude.matcher(line).matches() ) {
                    continue;
                }
                matches.add(line);
            }
        }

        return matches;
    }

    public static List<String> select(List<String> lines, String regexInclude) {
        return selectLines(lines, regexInclude, EXCLUDE_NONE);
    }
    
    public static List<String> reject(List<String> lines, String regexExclude) {
        return selectLines(lines, INCLUDE_ALL, regexExclude);
    }

    public static final String INCLUDE_ALL = null;
    public static final String EXCLUDE_NONE = null;
        
    public static List<String> selectLines(List<String> lines, String regexInclude, String regexExclude) {
        List<String> matches = new ArrayList<String>();

        Pattern include = ( (regexInclude == null) ? null : Pattern.compile(regexInclude) );
        Pattern exclude = ( (regexExclude == null) ? null : Pattern.compile(regexExclude) );
        
        for ( String line : lines ) {            
            if ( (include != null) && !include.matcher(line).matches() ) {
                continue;
            }
            if ( (exclude != null) && exclude.matcher(line).matches() ) {
                continue;
            }
            matches.add(line);
        }

        return matches;
    }

    //
    
    public static void ensureNonexistence(String targetPath) {
        delete( new File(targetPath) );
    }

    public static void ensureNonexistence(File target) {
        String targetPath = target.getPath();

        if ( !target.exists() ) {
            log("Target [ " + targetPath + " ] does not exist");

        } else {
            log("Target [ " + targetPath + " ] will be deleted");

            String failurePath = delete(target);
            if ( failurePath != null ) {
                String message = "Failed to delete [ " + targetPath + " ]";
                if ( !targetPath.equals(failurePath) ) {
                    message += ": Failed on [ " + failurePath + " ]";
                }
                fail(message);
            }
        }
    }

    public static String delete(File target) {        
        if ( target.isDirectory() ) {
            File[] children = target.listFiles();
            if ( children == null ) {
                log("Failed to delete [ " + target.getAbsolutePath() + " ]: Directory could not be listed");
            } else {
                String childFailure = null;

                for ( File child : children ) {
                    String nextFailure = delete(child);
                    if ( (nextFailure != null) && (childFailure == null) ) {
                        childFailure = nextFailure;
                        log("Failed to delete [ " + target.getAbsolutePath() + " ]: Could not delete [ " + childFailure + " ]");                                                
                    }
                }

                if ( childFailure != null ) {
                    return childFailure;
                }
            }
        }

        target.delete();

        if ( target.exists() ) {
            String targetPath = target.getAbsolutePath();
            log("Failed to delete [ " + targetPath + " ]");                
            return targetPath;
        }

        return null;
    }

    //
    
    public static void updateProperties(String propertiesPath, Properties newProperties) throws IOException {
        File propertiesFile = new File(propertiesPath);

        Properties oldProperties = new Properties();
        if ( propertiesFile.exists() ) {
            try ( InputStream input = new FileInputStream(propertiesFile) ) {
                oldProperties.load(input);
            }
        }

        oldProperties.putAll(newProperties);

        try ( OutputStream output = new FileOutputStream(propertiesFile) ) { 
            oldProperties.store(output, null);
        }
    }
    
    //
    
    public static int update(List<String> lines, Function<String, String> lineProcessor) {
        int numLines = lines.size();
        int numChanged = 0;
        for ( int lineNo = 0; lineNo < numLines; lineNo++ ) {
            String oldLine = lines.get(lineNo);
            String newLine = lineProcessor.apply(oldLine);
            if ( oldLine == null ) {
                lines.remove(lineNo);
                numChanged++;
            } else if ( oldLine == newLine ) {
                // Do nothing; the line was not modified.
            } else {
                lines.set(lineNo, newLine);
                numChanged++;
            }
        }
        return numChanged;
    }
    
    public static int update(String inputPath, Function<String, String> lineProcessor)
        throws IOException {

        return update(inputPath, UPDATE_INPUT, lineProcessor);
    }

    public static final String UPDATE_INPUT = null;

    public static int update(String inputPath, String outputPath, Function<String, String> lineProcessor)
        throws IOException {

        List<String> inputLines = load(inputPath);
        int numChanged = update(inputLines, lineProcessor);
        if ( numChanged == 0 ) {
            if ( outputPath == null ) {
                return 0;
            }
        }

        if ( outputPath == null ) {
            outputPath = inputPath;
        }
        
        save(outputPath, inputLines);

        return numChanged;
    }
}
