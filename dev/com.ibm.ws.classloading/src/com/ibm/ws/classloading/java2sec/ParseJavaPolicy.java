/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.java2sec;

import java.lang.String;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.FileReader;
import java.security.Permission;
import java.security.UnresolvedPermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class ParseJavaPolicy {

    String file = null;
    FileReader fr = null;
    Parser parser = null;
    boolean expandProp = false;

    String keyStoreUrlString;
    String keyStoreType;
    static List<GrantEntry> grants = new ArrayList<GrantEntry>();


    final static String NEW_LINE = System.getProperty("line.separator");
    final static String QUOTED_STRING = "quoted string";
    final static String PERMISSION_TYPE = "permission type";
    final static String GRANT_KEYWORD = "grant";
    final static String KEYSTORE_KEYWORD = "keystore";
    final static String CODEBASE_KEYWORD = "codeBase";
    final static String PERMISSION_KEYWORD = "permission";
    final static String SIGNEDBY_KEYWORD = "signedBy";
    final static String FILTER_KEYWORD = "filterMask";


    public ParseJavaPolicy(boolean expandProp) throws FileNotFoundException, IOException, ParserException {

        try {
            file = System.getProperty("java.security.policy");
            
            if (file == null) {
                String javaHome = System.getProperty("java.home");
                if (javaHome != null) {
                    file = javaHome.concat("/lib/security/java.policy");
                }
            } 
            
            if (file.charAt(0) == '=') {
                // skip '=' for case where "==" is specified
                file = file.substring(1);
            }

            fr = new FileReader(file);
            
            init(fr, expandProp);


            parse();
            /*if (tc.isDebugEnabled())
            {
                StringBuffer buf = new StringBuffer("Contents of ");
                buf.append(file).append(": ").append(NEW_LINE);
                Iterator it = grants.iterator();
                while (it.hasNext())
                {
                    buf.append(NEW_LINE);
                    buf.append(it.next().toString());
                }
                System.out.println(buf.toString());
             }
             */

        }  catch (ParserException e) {
            throw e;
        }  catch (FileNotFoundException e) {
            throw e;
        }  catch (IOException e) {
            throw e;
        }
        finally
        {
            try {
                if (fr != null)
                    fr.close();  
            } catch (IOException e) {
                throw e;
            }

        } 

    }
    
    public static List getJavaPolicyGrants() {
        return grants;
    }

    private void init(Reader rdr, boolean expandProp) {
        this.parser = new com.ibm.ws.classloading.java2sec.Parser(rdr);
        this.expandProp = expandProp;
    }

    public String toString() {
        return getClass().getName();
    }

    Iterator grantEntries() {
        return grants.iterator();
    }

    void parse() throws IOException, ParserException {
        for (parser.nextToken(); !parser.eof(); parser.match(";")) {
            if (parser.peek(GRANT_KEYWORD)) {
                // parse grant
                GrantEntry g = parseGrantEntry();
                if (g != null) {
                    grants.add(g);
                }
            } else if (parser.peek(KEYSTORE_KEYWORD)) {
                if (keyStoreUrlString == null) {
                    parseKeystoreEntry();
                    if (keyStoreType == null) {
                        keyStoreType = "JKS";
                    }
                }
            } else {
                throw new ParserException(parser.getLineNumber(), "Unexpected keyword \"" + parser.getStringValue() + "\"");
            }
        }

    }


    void parseKeystoreEntry() throws IOException, ParserException {
        parser.match(KEYSTORE_KEYWORD);
        keyStoreUrlString = parser.match(QUOTED_STRING);
        if(!parser.peek(","))
            return;
        parser.match(",");
        if(parser.peek("\""))
            keyStoreType = parser.match(QUOTED_STRING);
        else
            throw new ParserException(parser.getLineNumber(), "expected keystore type");
    }

    GrantEntry parseGrantEntry() throws IOException, ParserException {
        
        String filePrefix = "file:";
        
        GrantEntry g = new GrantEntry();
        parser.match(GRANT_KEYWORD);
        while (!parser.peek("{")) {
            if (parser.peek(CODEBASE_KEYWORD)) {
                parser.match(CODEBASE_KEYWORD);
                g.codeBase = parser.match(QUOTED_STRING);
                
                
                if ((g.codeBase).startsWith(filePrefix)) {
                    g.codeBase = (g.codeBase).substring(filePrefix.length());
                }                
              
                if (parser.peek(",")) {
                    parser.match(",");
                }
                
            } else if (parser.peek(SIGNEDBY_KEYWORD)) {
                parser.match(SIGNEDBY_KEYWORD);
                g.signedBy = parser.match(QUOTED_STRING);
                if (parser.peek(",")) {
                    parser.match(",");
                }
            } else {
                throw new ParserException(parser.getLineNumber(), "expected " + CODEBASE_KEYWORD + " or " + SIGNEDBY_KEYWORD);
            }
        }
        parser.match("{");

        while (!parser.peek("}")) {
            if (parser.peek(PERMISSION_KEYWORD)) {
                try {
                    PermissionEntry p = parsePermissionEntry();
                    g.add(p);
                } catch (ParserException e) {
                    parser.skipEntry();
                }
                parser.match(";");
            } else {
                throw new ParserException(parser.getLineNumber(), "expected permission entry");
            }
        }
        parser.match("}");

        try {
            if (g.codeBase != null)
                g.codeBase = expand(g.codeBase, false);
            g.signedBy = expand(g.signedBy);
        } catch(ParserException e) {
            return null;
        }

        return g;
    }

    PermissionEntry parsePermissionEntry() throws IOException, ParserException {
        
        PermissionEntry p = new PermissionEntry();
        parser.match(PERMISSION_KEYWORD);
        p.permissionType = parser.match(PERMISSION_TYPE);
        if (parser.peek("\"")) {
            p.name = expand(parser.match_p(QUOTED_STRING)).trim(); //JDK BUG 177028
        }
        if (!parser.peek(",")) {
            return p;
        }

        parser.match(",");
        if (parser.peek("\"")) {
            p.action = expand(parser.match(QUOTED_STRING));

            if (!parser.peek(",")) {
                return p;
            }
            parser.match(",");
        }

        try {
            if (parser.peek(SIGNEDBY_KEYWORD)) {
                parser.match(SIGNEDBY_KEYWORD);
                p.signedBy = expand(parser.match(QUOTED_STRING));
            }
        } catch (ParserException e) {
            return (null); 
        }

        return p;
    }

    String expand(String str) throws ParserException {
        return expand(str, false);
    }

    String expand(String str, boolean encodeValue) throws ParserException {
        
        if (expandProp == true) {
            int strLen = 0;
            if ((str == null) || ((strLen = str.length()) == 0)) {
                return str;
            }

            StringBuffer buf = new StringBuffer(strLen + 25);
            for (int index = 0, last = 0; last < strLen; ) {
                index = str.indexOf("${", last);
                if (index == -1) {
                    buf.append(str.substring(last));
                    break;
                }
                buf.append(str.substring(last, index));
                last = str.indexOf("}", index);
                if (last == -1) {
                    buf.append(str.substring(index));
                    break;
                }
                String key = str.substring(index + 2, last);
                if (key.equals("/")) {
                    buf.append(File.separator);
                } else {
                    String value = System.getProperty(key);
                    if (value != null) {
                        if (encodeValue == true) {
                            value = FilePathUtil.encodeFilePath(value);
                        }
                        buf.append(value);
                    } else {
                        StringBuffer errBuf = new StringBuffer(32);
                        errBuf.append("line ").append(parser.getLineNumber()).append(": ");
                        errBuf.append("unable to expand \"").append(key).append("\"");
                        String errStr = errBuf.toString();
                        throw new ParserException(errStr);
                    }
                }
                last += 1;
            }
            return buf.toString();
        } else {
            return str;
        }
    }




}
