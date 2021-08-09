/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.utils;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents the line and file mappings associated with a JSR-045
 * "stratum".
 *
 * @author Jayson Falkner
 * @author Shawn Bayern
 */
public class SmapStratum {

    //*********************************************************************
    // Class for storing LineInfo data

    /**
     * Represents a single LineSection in an SMAP, associated with
     * a particular stratum.
     */
    public static class LineInfo {
        private int inputStartLine = -1;
        private int outputStartLine = -1;
        private int lineFileID = 0;
        private int inputLineCount = 1;
        private int outputLineIncrement = 1;
        private boolean lineFileIDSet = false;

        /** Sets InputStartLine. */
        public void setInputStartLine(int inputStartLine) {
            if (inputStartLine < 0)
                throw new IllegalArgumentException("" + inputStartLine);
            this.inputStartLine = inputStartLine;
        }

        /** Sets OutputStartLine. */
        public void setOutputStartLine(int outputStartLine) {
            if (outputStartLine < 0)
                throw new IllegalArgumentException("" + outputStartLine);
            this.outputStartLine = outputStartLine;
        }

        /**
             * Sets lineFileID.  Should be called only when different from
             * that of prior LineInfo object (in any given context) or 0
             * if the current LineInfo has no (logical) predecessor.
             * <tt>LineInfo</tt> will print this file number no matter what.
             */
        public void setLineFileID(int lineFileID) {
            if (lineFileID < 0)
                throw new IllegalArgumentException("" + lineFileID);
            this.lineFileID = lineFileID;
            this.lineFileIDSet = true;
        }

        /** Sets InputLineCount. */
        public void setInputLineCount(int inputLineCount) {
            if (inputLineCount < 0)
                throw new IllegalArgumentException("" + inputLineCount);
            this.inputLineCount = inputLineCount;
        }

        /** Sets OutputLineIncrement. */
        public void setOutputLineIncrement(int outputLineIncrement) {
            if (outputLineIncrement < 0)
                throw new IllegalArgumentException("" + outputLineIncrement);
            this.outputLineIncrement = outputLineIncrement;
        }

        /**
         * Retrieves the current LineInfo as a String, print all values
         * only when appropriate (but LineInfoID if and only if it's been
         * specified, as its necessity is sensitive to context).
         */
        public String getString() {
            if (inputStartLine == -1 || outputStartLine == -1)
                throw new IllegalStateException();
            StringBuffer out = new StringBuffer();
            out.append(inputStartLine);
            if (lineFileIDSet)
                out.append("#" + lineFileID);
            if (inputLineCount != 1)
                out.append("," + inputLineCount);
            out.append(":" + outputStartLine);
            if (outputLineIncrement != 1)
                out.append("," + outputLineIncrement);
            out.append('\n');
            return out.toString();
        }

        public String toString() {
            return getString();
        }
    }

    //*********************************************************************
    // Private state

    private String stratumName;
    private List fileNameList;
    private List filePathList;
    private List lineData;
    private int lastFileID;

    //*********************************************************************
    // Constructor

    /**
     * Constructs a new SmapStratum object for the given stratum name
     * (e.g., JSP).
     *
     * @param stratumName the name of the stratum (e.g., JSP)
     */
    public SmapStratum(String stratumName) {
        this.stratumName = stratumName;
        fileNameList = new ArrayList();
        filePathList = new ArrayList();
        lineData = new ArrayList();
        lastFileID = 0;
    }

    //*********************************************************************
    // Methods to add mapping information

    /**
     * Adds record of a new file, by filename.
     *
     * @param fileName the filename to add, unqualified by path.
     */
    public void addFile(String filename) {
        addFile(filename, null);
    }

    /**
     * Adds record of a new file, by filename and path.  The path
     * may be relative to a source compilation path.
     *
     * @param fileName the filename to add, unqualified by path
     * @param filePath the path for the filename, potentially relative
     *                 to a source compilation path
     */
    public synchronized void addFile(String filename, String filePath) {
        // fix this to check if duplicate name exists.
        int fileIndex = fileNameList.indexOf(filename);
        if (fileIndex == -1) {
            fileNameList.add(filename);
            filePathList.add(filePath);
        }
    }

    /**
     * Adds complete information about a simple line mapping.  Specify
     * all the fields in this method; the back-end machinery takes care
     * of printing only those that are necessary in the final SMAP.
     * (My view is that fields are optional primarily for spatial efficiency,
     * not for programmer convenience.  Could always add utility methods
     * later.)
     *
     * @param inputStartLine starting line in the source file
     *        (SMAP <tt>InputStartLine</tt>)
     * @param inputFileName the filepath (or name) from which the input comes
     *        (yields SMAP <tt>LineFileID</tt>)  Use unqualified names
     *        carefully, and only when they uniquely identify a file.
     * @param inputLineCount the number of lines in the input to map
     *        (SMAP <tt>LineFileCount</tt>)
     * @param outputStartLine starting line in the output file 
     *        (SMAP <tt>OutputStartLine</tt>)
     * @param outputLineIncrement number of output lines to map to each
     *        input line (SMAP <tt>OutputLineIncrement</tt>).  <i>Given the
     *        fact that the name starts with "output", I continuously have
     *        the subconscious urge to call this field
     *        <tt>OutputLineExcrement</tt>.</i>
     */
    public synchronized void addLineData(
        int inputStartLine,
        String inputFileName,
        int inputLineCount,
        int outputStartLine,
        int outputLineIncrement) {
        // check the input - what are you doing here??
        //	int fileIndex = filePathList.indexOf(inputFileName);
        //	if (fileIndex == -1)
        //        fileNameList.indexOf(inputFileName);
        int fileIndex = fileNameList.indexOf(inputFileName);
        if (fileIndex == -1) // still
            throw new IllegalArgumentException("inputFileName: " + inputFileName);

        // build the LineInfo
        LineInfo li = new LineInfo();
        li.setInputStartLine(inputStartLine);
        li.setInputLineCount(inputLineCount);
        li.setOutputStartLine(outputStartLine);
        li.setOutputLineIncrement(outputLineIncrement);
        if (fileIndex != lastFileID)
            li.setLineFileID(fileIndex);
        lastFileID = fileIndex;

        // save it
        lineData.add(li);
    }

    //*********************************************************************
    // Methods to retrieve information

    /**
     * Returns the name of the stratum.
     */
    public String getStratumName() {
        return stratumName;
    }

    /**
     * Returns the given stratum as a String:  a StratumSection,
     * followed by at least one FileSection and at least one LineSection.
     */
    public synchronized String getString() {
        // check state and initialize buffer
        if (fileNameList.size() == 0 || lineData.size() == 0)
            throw new IllegalStateException();
        StringBuffer out = new StringBuffer();

        // print StratumSection
        out.append("*S " + stratumName + "\n");

        // print FileSection
        out.append("*F\n");
        int bound = fileNameList.size();
        for (int i = 0; i < bound; i++) {
            if (filePathList.get(i) != null) {
                out.append("+ " + i + " " + fileNameList.get(i) + "\n");
                out.append(filePathList.get(i) + "\n");
            }
            else {
                out.append(i + " " + fileNameList.get(i) + "\n");
            }
        }

        // print LineSection
        out.append("*L\n");
        bound = lineData.size();
        for (int i = 0; i < bound; i++) {
            LineInfo li = (LineInfo) lineData.get(i);
            out.append(li.getString());
        }

        return out.toString();
    }

    public String toString() {
        return getString();
    }

}
