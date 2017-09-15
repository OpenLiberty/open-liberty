/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaas.config.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.security.auth.login.AppConfigurationEntry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;

abstract public class Parser
{
    private static final TraceComponent tc = Tr.register(Parser.class);
    StreamTokenizer st;
    int lookahead;
    Map<String, List<AppConfigurationEntry>> login = new HashMap<String, List<AppConfigurationEntry>>(); // Thread-safe, 2011/04/13

    public Parser()
    {}

    public Map<String, List<AppConfigurationEntry>> parse(Reader rdr) throws IOException, ParserException
    {
        clearFileEntry();
        init(rdr);
        for (lookahead = st.nextToken(); lookahead != StreamTokenizer.TT_EOF; match(";")) {
            while (!peek("}")) {
                // get alias
                String name = getString();
                if (peek("{"))
                    skipChar('{');
                List<AppConfigurationEntry> g = parseAppConfigurationEntry();
                if (g != null && name != null && name.trim().length() > 0) {
                    if (login.containsKey(name))
                        Tr.error(tc, "security.jaas.duplicate.config", name);
                    login.put(name, g);
                }
            }
            if (peek("}"))
                skipChar('}');
        }
        return login;
    }

    public void clearFileEntry()
    {
        login.clear();
    }

    public List<AppConfigurationEntry> parseAppConfigurationEntry()
    {
        Vector<AppConfigurationEntry> appEntry = new Vector<AppConfigurationEntry>();
        try {
            while (!peek("}")) {
                String lmName = getString();
                String flag = getString();
                Map<String, String> opt = new HashMap<String, String>();
                while (!peek(";")) {
                    String opName = getString();
                    skipChar('='); // skip =
                    String opValue = getString();
                    opt.put(opName, opValue);
                }
                appEntry.add(new AppConfigurationEntry(lmName, buildFlag(flag), opt));
                if (peek(";"))
                    skipChar(';'); //skip ";"
            }
        } catch (IOException e) {
            Tr.error(tc, "security.jaas.app.parseIO", e);
            appEntry = null;

        } catch (ParserException e) {
            Tr.error(tc, "security.jaas.app.parse", e);
        }
        return appEntry;
    }

    public String getString() throws IOException, ParserException
    {
        String s1 = null;

        switch (lookahead) {
            case StreamTokenizer.TT_EOL: // Do not expect EOL
                throw new ParserException(st.lineno(), " read end of Line");
            case StreamTokenizer.TT_EOF: // Do not expect EOF
                throw new ParserException(st.lineno(), " getString:read end of file");
            case StreamTokenizer.TT_WORD: // Expecting a String
                s1 = st.sval;
                lookahead = st.nextToken();
                break;
            case '"': // 
                s1 = st.sval;
                lookahead = st.nextToken();
                break;
            default:
                throw new ParserException(st.lineno(), s1, new String(new char[] { (char) lookahead }));
        }
        return s1;
    }

    public String match(String str) throws IOException, ParserException
    {
        String s1 = null;

        switch (lookahead) {
            case StreamTokenizer.TT_NUMBER: // Do not expect number
                throw new ParserException(st.lineno(), "number " + st.nval);
            case StreamTokenizer.TT_EOL: // Do not expect EOL
                throw new ParserException(st.lineno(), " read end of Line");
            case StreamTokenizer.TT_EOF: // Do not expect EOL
                throw new ParserException(st.lineno(), " match;read end of FILE");
            case StreamTokenizer.TT_WORD: // Expecting a String
                s1 = st.sval;
                lookahead = st.nextToken();
                break;
            case ';': // ";" -59
                if (str.equalsIgnoreCase(";")) {
                    lookahead = st.nextToken();
                    break;
                }
                throw new ParserException(st.lineno(), str, ";");
            case '=': // 
                if (str.equalsIgnoreCase("=")) {
                    lookahead = st.nextToken();
                    break;
                }
                throw new ParserException(st.lineno(), str, "=");
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
                throw new ParserException(st.lineno(), s1, new String(new char[] { (char) lookahead }));
        }
        return s1;
    }

    private AppConfigurationEntry.LoginModuleControlFlag buildFlag(String flag)
    {
        if (flag.equalsIgnoreCase("sufficient")) {
            return AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
        } else if (flag.equalsIgnoreCase("optional")) {
            return AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL;
        } else if (flag.equalsIgnoreCase("required")) {
            return AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
        } else if (flag.equalsIgnoreCase("requisite")) {
            return AppConfigurationEntry.LoginModuleControlFlag.REQUISITE;
        }
        return null;
    }

    @Sensitive
    public boolean peek(String str)
    {
        boolean flag = false;
        switch (lookahead) {
            case StreamTokenizer.TT_WORD:
                flag = str.equalsIgnoreCase(st.sval);
                break;
            case ';': // ";" -59
                flag = str.equalsIgnoreCase(";");
                break;
            case '{': // '{' - 123
                flag = str.equalsIgnoreCase("{");
                break;
            case '}': // '}' - 125
                flag = str.equalsIgnoreCase("}");
                break;
            case '=': // 
                flag = str.equalsIgnoreCase("=");
                break;
            case ' ': // 
                flag = str.equalsIgnoreCase(" ");
                break;
            default:
                break;
        }
        return flag;
    }

    public void skipChar(int str) throws IOException, ParserException
    {
        switch (str) {
            case '=':
                lookahead = st.nextToken();
                break;
            case ';':
                lookahead = st.nextToken();
                break;
            case '{':
                lookahead = st.nextToken();
                break;
            case '}':
                lookahead = st.nextToken();
                break;
            default:
                if (lookahead != StreamTokenizer.TT_EOF) {
                    lookahead = st.nextToken();
                }
                break;
        }
    }

    private void init(Reader rdr)
    {
        if ((rdr instanceof BufferedReader)) {
            st = new StreamTokenizer(new BufferedReader(rdr));
        } else {
            st = new StreamTokenizer(rdr);
        }

        st.resetSyntax();
        st.wordChars(97, 122); // 'a' to 'z'
        st.wordChars(65, 90); // 'A' to 'Z'
        st.wordChars(48, 58); // '0' to '\'     $SC1
        st.wordChars(45, 47); //  '-' & '.' '/'  PQ79082 $SC1
        st.wordChars(92, 92); //  ':'           $SC1
        st.wordChars(95, 95); // '_'
        st.wordChars(36, 36); // '$'
        st.wordChars(160, 255); // the rest of characters range
        st.whitespaceChars(0, 32); // 0 to 32 is whitspaces
//              st.commentChar(47);           // '/'    $SC1
        st.quoteChar(39); // '''
        st.quoteChar(34); // '"'
//              st.ordinaryChar(47);          // '/'     $SC1
        st.lowerCaseMode(false); // case sensitive
        st.slashSlashComments(true); // "//"
        st.slashStarComments(true); // "/*" 
    }
}
