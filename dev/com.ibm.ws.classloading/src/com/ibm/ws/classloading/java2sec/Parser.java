/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.classloading.java2sec;

/**
 *  Utility class that reads the policy file, and indicates if eof is reached or if a particular character
 *  matches an expectation.     Aids in parsing the policy file as well as ensuring the syntax is as expected.
 */
import java.io.*;

public class Parser {
    public Parser(Reader rdr) {
        init(rdr);
    }

    public String toString() {
        return getClass().getName();
    }

    public boolean eof() {
        return lookahead == StreamTokenizer.TT_EOF;
    }

    public int nextToken() throws IOException {
        lookahead = st.nextToken();
        return lookahead;
    }

    public String getStringValue() {
        return st.sval;
    }

    public int getLineNumber() {
        return st.lineno();
    }

    public boolean peek(String str) {
        boolean flag = false;
        switch (lookahead) {
        case StreamTokenizer.TT_WORD:
            flag = str.equalsIgnoreCase(st.sval);
            break;
    
        case '"':  // '"' - 34
            flag = str.equalsIgnoreCase("\"");
            break;
    
        case ',':  // ',' - 44
            flag = str.equalsIgnoreCase(",");
            break;
    
        case '{': // '{' - 123
            flag = str.equalsIgnoreCase("{");
            break;
    
        case '}': // '}' - 125
            flag = str.equalsIgnoreCase("}");
            break;
    
        default:
            break;
        }
    
        return flag;
    }

    public String match_p(String str ) throws IOException, ParserException {
        String s1 = null;
    
        switch (lookahead) {
        case StreamTokenizer.TT_NUMBER:  // Do not expect number
            throw new ParserException(st.lineno(), str, "number " + st.nval);
    
        case StreamTokenizer.TT_EOL:  // Do not expect EOL
            throw new ParserException(st.lineno(), "expected '" + str + "', read end of file");
    
        case StreamTokenizer.TT_WORD:  // Expecting a String
            if (str.equalsIgnoreCase(st.sval)) {
                lookahead = st.nextToken();
                break;
            }
            if (str.equalsIgnoreCase("permission type")) {
                s1 = st.sval;
                lookahead = st.nextToken();
                break;
            }
            throw new ParserException(st.lineno(), str, st.sval);
    
        case '"':  // '"' - 34 and expecting a quoted String
            if (str.equalsIgnoreCase("quoted string")) {
                s1 = st.sval;
                lookahead = st.nextToken();
                // If the next char is not a separator',' 
                // skip it as a nested quated string
                if ( !(lookahead==',') && !(lookahead==';') ) {
                    if ( lookahead == StreamTokenizer.TT_WORD) { 
                        s1=s1+"\"" + st.sval;   
                        lookahead = st.nextToken();
                    } else {
                        s1=s1+"\"" + new String(new char[] { (char) (lookahead)});      
                        lookahead = st.nextToken();
                    }
                    if ( lookahead=='"') {
                        s1=s1+"\"" ;
                        lookahead = st.nextToken();
                    } else {
                        throw new ParserException(st.lineno(), "\"", new String(new char[] {(char) (lookahead)}));
                    }
                }
                break;
            }
            if (str.equalsIgnoreCase("permission type")) {
                s1 = st.sval;
                lookahead = st.nextToken();
                break;
            }
            throw new ParserException(st.lineno(), str, st.sval);
    
        case ',':  // ',' - 44
            if (str.equalsIgnoreCase(",")) {
                lookahead = st.nextToken();
                break;
            }
            throw new ParserException(st.lineno(), str, ",");
    
        case ';':  // ";" -59
            if (str.equalsIgnoreCase(";")) {
                lookahead = st.nextToken();
                break;
            }
            throw new ParserException(st.lineno(), str, ";");
    
        case '{': // '{' - 123
            if (str.equalsIgnoreCase("{")) {
                lookahead = st.nextToken();
                break;
            }
            throw new ParserException(st.lineno(), str, "{");
    
        case '}': // '}' - 125
            if (str.equalsIgnoreCase("}")) {
                lookahead = st.nextToken();
                break;
            }
            throw new ParserException(st.lineno(), str, "}");
    
        default:
            throw new ParserException(st.lineno(), s1, new String(new char[] { (char) (lookahead)}));
        }
    
        return s1;
    }

    public String match(String str) throws IOException, ParserException {
        String s1 = null;
    
        switch (lookahead) {
        case StreamTokenizer.TT_NUMBER:  // Do not expect number
            throw new ParserException(st.lineno(), str, "number " + st.nval);
    
        case StreamTokenizer.TT_EOL:  // Do not expect EOL
            throw new ParserException(st.lineno(), "expected '" + str + "', read end of file");
    
        case StreamTokenizer.TT_WORD:  // Expecting a String
            if (str.equalsIgnoreCase(st.sval)) {
                lookahead = st.nextToken();
                break;
            }
            if (str.equalsIgnoreCase("permission type")) {
                s1 = st.sval;
                lookahead = st.nextToken();
                break;
            }
            throw new ParserException(st.lineno(), str, st.sval);
    
        case '"':  // '"' - 34 and expecting a quoted String
            if (str.equalsIgnoreCase("quoted string")) {
                s1 = st.sval;
                lookahead = st.nextToken();
                break;
            }
            if (str.equalsIgnoreCase("permission type")) {
                s1 = st.sval;
                lookahead = st.nextToken();
                break;
            }
            throw new ParserException(st.lineno(), str, st.sval);
    
        case ',':  // ',' - 44
            if (str.equalsIgnoreCase(",")) {
                lookahead = st.nextToken();
                break;
            }
            throw new ParserException(st.lineno(), str, ",");
    
        case ';':  // ";" -59
            if (str.equalsIgnoreCase(";")) {
                lookahead = st.nextToken();
                break;
            }
            throw new ParserException(st.lineno(), str, ";");
    
        case '{': // '{' - 123
            if (str.equalsIgnoreCase("{")) {
                lookahead = st.nextToken();
                break;
            }
            throw new ParserException(st.lineno(), str, "{");
    
        case '}': // '}' - 125
            if (str.equalsIgnoreCase("}")) {
                lookahead = st.nextToken();
                break;
            }
            throw new ParserException(st.lineno(), str, "}");
    
        default:
            throw new ParserException(st.lineno(), s1, new String(new char[] { (char) lookahead}));
        }
    
        return s1;
    }

    public void skipEntry() throws IOException, ParserException {
        while (lookahead != ';') {  // ";" - 59
            switch (lookahead) {
            case StreamTokenizer.TT_NUMBER: 
                throw new ParserException(st.lineno(), ";", "number " + st.nval);
    
            case StreamTokenizer.TT_EOF:
                throw new ParserException(st.lineno(), "expected ';', read end of file");
    
            default:
                lookahead = st.nextToken();
                break;
            }
        }
    }

    private void init(Reader rdr) {
        if ((rdr instanceof BufferedReader)) {
            st = new StreamTokenizer(new BufferedReader(rdr));
        } else {
            st = new StreamTokenizer(rdr);
        }

        // Initialize StreamTokenizer
        st.resetSyntax();
        st.wordChars(97, 122);        // 'a' to 'z'
        st.wordChars(65, 90);         // 'A' to 'Z'
        st.wordChars(48, 57);         // '0' to '9'
        st.wordChars(46, 46);         // '.'
        st.wordChars(95, 95);         // '_'
        st.wordChars(36, 36);         // '$'
        st.wordChars(160, 255);       // the rest of characters range
        st.whitespaceChars(0, 32);    // 0 to 32 is whitspaces
        st.commentChar(47);           // '/'
        st.quoteChar(39);             // '''
        st.quoteChar(34);             // '"'
        st.ordinaryChar(47);          // '/'
        st.lowerCaseMode(false);      // case sensitive
        st.slashSlashComments(true);  // "//"
        st.slashStarComments(true);   // "/*"
    }

    private StreamTokenizer st;
    private int lookahead;
}

