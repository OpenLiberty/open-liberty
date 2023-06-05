/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.test.featurestart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FileRewriter {

    public static List<String> update(String targetPath,
                                      String[] matchLines,
                                      String[] updatedLines) throws IOException {

        return (new FileRewriter(targetPath)).update(matchLines, updatedLines);
    }

    public static List<String> update(String targetPath,
                                      String[] matchLines,
                                      String[] updatedLines,
                                      Set<Integer> additions) throws IOException {

        return (new FileRewriter(targetPath)).update(matchLines, updatedLines, additions);
    }

    //

    public FileRewriter(String targetPath) {
        this.targetPath = targetPath;
        this.targetFile = new File(targetPath);
    }

    private final String targetPath;

    public String getTargetPath() {
        return targetPath;
    }

    private final File targetFile;

    public File getTargetFile() {
        return targetFile;
    }

    public List<String> update(String[] matchLines, String[] updatedLines) throws IOException {
        return update(matchLines, updatedLines, Collections.emptySet());
    }

    public List<String> update(String[] matchLines, String[] updatedLines,
                               Set<Integer> additions) throws IOException {

        return write(update(read(), matchLines, updatedLines, additions));
    }

    public List<String> read() throws IOException {
        try (FileReader fileReader = new FileReader(getTargetFile());
                        BufferedReader reader = new BufferedReader(fileReader)) {

            List<String> readLines = new ArrayList<>();
            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                readLines.add(nextLine);
            }
            return readLines;
        }
    }

    public List<String> write(List<String> lines) throws IOException {
        try (OutputStream output = new FileOutputStream(getTargetFile());
                        PrintStream printer = new PrintStream(output)) {
            lines.forEach((line) -> printer.println(line));
        }
        return lines;
    }

    public List<String> update(List<String> lines, String matchLine, String updatedLine) {
        return update(lines, matchLine, updatedLine, Collections.emptySet());
    }

    public List<String> update(List<String> lines,
                               String matchLine, String updatedLine,
                               Set<Integer> additions) {
        return update(lines,
                      new String[] { matchLine },
                      new String[] { updatedLine },
                      additions);
    }

    public List<String> update(List<String> lines,
                               String[] matchLines,
                               String[] updatedLines) {
        return update(lines, matchLines, updatedLines, Collections.emptySet());
    }

    public List<String> update(List<String> lines,
                               String[] matchLines,
                               String[] updatedLines,
                               Set<Integer> additions) {

        int numLines = lines.size();
        int numMatches = matchLines.length;

        for (int lineNo = 0; lineNo < numLines; lineNo++) {
            String nextLine = lines.get(lineNo);

            for (int matchNo = 0; matchNo < numMatches; matchNo++) {
                String matchLine = matchLines[matchNo];

                int matchPos = nextLine.indexOf(matchLine);
                if (matchPos == -1) {
                    continue;
                }

                String description;

                String updatedLine = updatedLines[matchNo];

                if (additions.contains(Integer.valueOf(matchNo))) {
                    lineNo++; // Don't match against the added line; add after the match.
                    numLines++; // Grow the lines.
                    description = "Line [ " + lineNo + " ]: Add [ " + updatedLine + " ] after [ " + matchLine + " ]";
                    lines.add(lineNo, updatedLine);

                } else if (updatedLine == null) {
                    description = "Line [ " + lineNo + " ]: Remove [ " + matchLine + " ]";
                    lines.remove(lineNo);
                    lineNo--; // Resume matching at the same place.
                    numLines--; // Shrink the lines.

                } else {
                    String replacement = nextLine.substring(0, matchPos) +
                                         updatedLine +
                                         nextLine.substring(matchPos + matchLine.length());
                    description = "Line [ " + lineNo + " ]: Replace [ " + nextLine + " ] with [ " + replacement + " ]";
                    lines.set(lineNo, replacement);
                    // Resume matching on the next line.
                    // The lines are the same size.
                }

                System.out.println(description);
            }
        }

        return lines;
    }
}
