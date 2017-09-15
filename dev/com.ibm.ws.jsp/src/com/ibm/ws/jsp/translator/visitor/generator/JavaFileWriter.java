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
package com.ibm.ws.jsp.translator.visitor.generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Map;

import org.w3c.dom.Element;

import com.ibm.ws.jsp.Constants;

public class JavaFileWriter extends JavaCodeWriter {
    private static final int TAB_WIDTH = 2;
    private static final int NUMBER_OF_SPACES = 128;
    private char[] spaces = null;
    
    private int indent = 0;
    private int lineNum = 1;
    private int lineCount = 0;
    private int syntaxLineNum = 0;
    private Map jspElementMap = null;
    private Map cdataJspIdMap = null;
    private Map customTagMethodJspIdMap = null; //232818
    private boolean newLine = true;

    public JavaFileWriter(String filePath, Map jspElementMap, Map cdataJspIdMap, Map customTagMethodJspIdMap, String encoding) throws IOException { //232818
        super(new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), encoding))));
        this.jspElementMap = jspElementMap;
        this.cdataJspIdMap = cdataJspIdMap;
        this.customTagMethodJspIdMap = customTagMethodJspIdMap; //232818
        spaces = new char[NUMBER_OF_SPACES];
        for (int i = 0; i < NUMBER_OF_SPACES; i++) {
            spaces[i] = ' ';
        }
    }
    public JavaFileWriter(PrintWriter writer, Map jspElementMap, Map cdataJspIdMap, Map customTagMethodJspIdMap) throws IOException { //232818
        super(writer);
        this.jspElementMap = jspElementMap;
        this.cdataJspIdMap = cdataJspIdMap;
        this.customTagMethodJspIdMap = customTagMethodJspIdMap; //232818
        spaces = new char[NUMBER_OF_SPACES];
        for (int i = 0; i < NUMBER_OF_SPACES; i++) {
            spaces[i] = ' ';
        }
    }

    public void println(String line) {
        newLine = true;
        int index = 0;
        while ((index = line.indexOf('\n', index)) > -1) {
            lineNum++;
            lineCount++;
            index++;
        }
        if (line.indexOf("/* ElementId[") != -1) {
            Integer elementId = Integer.valueOf(line.substring(line.indexOf("/* ElementId[")+13, line.indexOf("]")));
            Element e = (Element)jspElementMap.get(elementId);
            String jspId = e.getAttributeNS(Constants.JSP_NAMESPACE, "id");
            if (line.indexOf("sb") != -1) {
                lineCount = 0;
                syntaxLineNum = lineNum;
            }
            else if (line.indexOf("se") != -1) {
                jspId = jspId + ":["+ syntaxLineNum + "," + lineCount + "]";
                e.setAttributeNS(Constants.JSP_NAMESPACE, "jsp:id", jspId);
            }
            else if (line.indexOf("eb") != -1) {
                lineCount = 0;
                syntaxLineNum = lineNum;
            }
            else if (line.indexOf("ee") != -1) {
                jspId = jspId + "["+ syntaxLineNum + "," + lineCount + "]";
                e.setAttributeNS(Constants.JSP_NAMESPACE, "jsp:id", jspId);
            } //232818 Start
            else if (line.indexOf("ctmb") != -1) {
                lineCount = 0;
                syntaxLineNum = lineNum;
            }
            else if (line.indexOf("ctme") != -1) {
            	customTagMethodJspIdMap.put(e, syntaxLineNum + "," + lineCount);
            } //232818 End
        }
        else if (line.indexOf("/* CDATAId[") != -1) {
            Integer cdataId = Integer.valueOf(line.substring(line.indexOf("/* CDATAId[")+11, line.indexOf("]")));
            String jspId = (String)cdataJspIdMap.get(cdataId);
            if (jspId != null) {
                if (line.indexOf("sb") != -1) {
                    lineCount = 0;
                    syntaxLineNum = lineNum;
                }
                else if (line.indexOf("se") != -1) {
                    jspId = jspId + ":["+syntaxLineNum + "," + lineCount + "]";
                    cdataJspIdMap.put(cdataId, jspId);
                }
            }
        }
        else {
            lineCount++;
            lineNum++;
            int charCount = 0;
             
            if ((charCount = lookFor('}', line)) > 0) {
                indent = indent - charCount;
            }
            
            int numberOfSpaces = indent * TAB_WIDTH;
            if (numberOfSpaces > 0) {
                if (numberOfSpaces > spaces.length) {
                    reallocateSpaces();
                }
                write(spaces, 0, numberOfSpaces);
            }
            super.println(line);
            
            if ((charCount = lookFor('{', line)) > 0) {
                indent = indent + charCount;
            }
        }
    }

    public void println() {
        newLine = true;
        lineCount++;
        lineNum++;
        super.println();
    }

    public void print(String s) {
        int index = 0;

        // look for hidden newlines inside strings
        if (s != null) { // 113979 IBM Jasper Change
            while ((index = s.indexOf('\n', index)) > -1) {
                lineNum++;
                lineCount++;
                index++;
            }
        }

        int charCount = 0;
             
        if ((charCount = lookFor('}', s)) > 0) {
            indent = indent - charCount;
        }
        if (newLine) {
            int numberOfSpaces = indent * TAB_WIDTH;
            if (numberOfSpaces > 0) {
                if (numberOfSpaces > spaces.length) {
                    reallocateSpaces();
                }
                write(spaces, 0, numberOfSpaces);
            }
            newLine = false;
        }
        super.print(s);
        if ((charCount = lookFor('{', s)) > 0) {
            indent = indent + charCount;
        }
    }

    public void printMultiLn(String multiline) {
        // Try to be smart (i.e. indent properly) at generating the code:
        BufferedReader reader = new BufferedReader(new StringReader(multiline));
        try {
            for (String line = null;(line = reader.readLine()) != null;) {
                //      println(SPACES.substring(0, indent)+line);
                this.println(line);
            }
        }
        catch (IOException ex) {
            // Unlikely to happen, since we're acting on strings
        }
    }
    
    private int lookFor(char character, String s) {
        int count = 0;
        boolean inQuotes = false;
        
        if (s!=null) {
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (ch == '\"') {
                    if (inQuotes)
                        inQuotes = false;
                    else 
                        inQuotes = true;                    
                }
                else if (ch == character && !inQuotes) {
                    count++;
                }
            }
        }
        
        return count;
    }

    public int getCurrentLineNumber() {
    	return lineNum;
    }
    
    private void reallocateSpaces() {
        int newSize = spaces.length + NUMBER_OF_SPACES;
        spaces = new char[newSize];
        for (int i = 0; i < newSize; i++) {
            spaces[i] = ' ';
        }
    }
}
