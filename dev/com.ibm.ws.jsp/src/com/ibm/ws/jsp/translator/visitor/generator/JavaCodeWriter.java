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

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.IOException;

public class JavaCodeWriter {
    PrintWriter writer;

    public JavaCodeWriter(PrintWriter writer) {
        this.writer = writer;
    }

    protected JavaCodeWriter() {}
    
    protected void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    public void close() throws IOException {
        writer.close();
    }
    
    public String quoteString(String s) {
        // Turn null string into quoted empty strings:
        if (s == null)
            return "null";
        // Hard work:
        if (s.indexOf('"') < 0 && s.indexOf('\\') < 0 && s.indexOf('\n') < 0 && s.indexOf('\r') < 0)
            return "\"" + s + "\"";
        StringBuffer sb = new StringBuffer();
        int len = s.length();
        sb.append('"');
        for (int i = 0; i < len; i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < len) {
                sb.append('\\');
                sb.append('\\');
                sb.append(s.charAt(++i));
            }
            else if (ch == '"') {
                sb.append('\\');
                sb.append('"');
            }
            else if (ch == '\n') {
                sb.append("\\n");
            }
            else if (ch == '\r') {
                sb.append("\\r");
            }
            else {
                sb.append(ch);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public void println(String line) {
        writer.println(line);
    }

    public void println() {
        writer.println("");
    }

    public void print(String s) {
        writer.print(s);
    }

    public void printMultiLn(String multiline) {
        // Try to be smart (i.e. indent properly) at generating the code:
        BufferedReader reader = new BufferedReader(new StringReader(multiline));
        try {
            for (String line = null;(line = reader.readLine()) != null;) {
                //		println(SPACES.substring(0, indent)+line);
                println(line);
            }
        }
        catch (IOException ex) {
            // Unlikely to happen, since we're acting on strings
        }
    }
    
    public void write(char[] buff, int off, int len) {
        writer.write(buff, off, len);
    }
}
