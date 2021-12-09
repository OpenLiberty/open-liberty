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
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.UnresolvedPermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;


/**
 *  Processes the java.policy file
 */
public class ParseJavaPolicy {
    /**
     * The trace component
     */
    private static final TraceComponent tc = Tr.register(ParseJavaPolicy.class);

    String file = null;
    FileReader fr = null;
    Parser parser = null;
    boolean expandProp = false;

    String keyStoreUrlString;
    String keyStoreType;
    static List<GrantEntry> grants = new ArrayList<GrantEntry>();
    
    String javaHome = AccessController.doPrivileged(new PrivilegedAction<String>() {
        public String run() {
            return System.getProperty("java.home");
        }
    });
    String javaVersion = AccessController.doPrivileged(new PrivilegedAction<String>() {
        public String run() {
            return System.getProperty("java.version");
        }
    });
    String javaSecurityPolicy = AccessController.doPrivileged(new PrivilegedAction<String>() {
        public String run() {
            return System.getProperty("java.security.policy");
        }
    });
    String javaVendor = AccessController.doPrivileged(new PrivilegedAction<String>() {
        public String run() {
            return System.getProperty("java.vendor");
        }
    });




    public ParseJavaPolicy(boolean expandProp) throws FileNotFoundException, IOException, ParserException {

        // let's find the java.policy file
        // if using Open JDK 9 or higher, find it under java.home/conf/security
        // if not there, look for it under java.home/jre/lib/security
        // if not there, look for it under java.home/lib/security
        
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "java.home: " + javaHome + " javaVendor: " + javaVendor + " javaSecurityPolicy: " + javaSecurityPolicy + " javaVersion: " + javaVersion);
        }
        
        try {
            file = javaSecurityPolicy;
            
            if (file == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Did not find java.policy under the location specified by java.security.policy");
                }
                
                // first we look at whatever java.security.policy is pointing at
                // if java.security.policy isn't defined, then we look under java.home
                
                if (javaHome != null) {
                    Integer version = 0;
                    
                    // first let's check if this is version 9 or up, where the java.policy file would 
                    // be under java.home/conf/security
                    
                    
                    String[] versionElements = javaVersion.split("\\D"); // split on non-digits

                    // Pre-JDK 9 the java.version is 1.MAJOR.MINOR
                    // Post-JDK 9 the java.version is MAJOR.MINOR
                    int i = Integer.valueOf(versionElements[0]) == 1 ? 1 : 0;
                    Integer MAJOR = Integer.valueOf(versionElements[i++]);

                    Integer MINOR;
                    if (i < versionElements.length)
                        MINOR = Integer.valueOf(versionElements[i++]);
                    else
                        MINOR = 0;

                    Integer MICRO;
                    if (i < versionElements.length)
                        MICRO = Integer.valueOf(versionElements[i]);
                    else
                        MICRO = 0;

                    
                    if (9 <= MAJOR.intValue()) {
                            file = javaHome.concat("/conf/security/java.policy");
                            File fileToCheck = new File(file);
                            if (!fileToCheck.exists()) {
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "JDK version installed: " + version.intValue() + " but did not find java.home/conf/security/java.policy");
                                }
                                // if not there, then let's just check the usual old places
                                file = wheresJavaPolicy();
                            }
                    } else {
                        // not using 9 or higher, so let's check the usual old places
                        file = wheresJavaPolicy();
                    }

                }
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "File location of java.policy file: " + file);
            }
            
            if (file != null && file.charAt(0) == '=') {
                // skip '=' for case where "==" is specified
                file = file.substring(1);
            }
            
            if (file == null) {
                // no java.policy file found, just return
                return;
            }

            File fileToCheck = new File(file);
            if (!fileToCheck.exists()) {
                // no java.policy file exists, return
                return;
            }
            
            fr = new FileReader(file);
            
            init(fr, expandProp);

            // walk the java.policy file, pulling out what permissions were granted to what codebase and 
            // what permissions are granted to all codebases
            
            parse();

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
    
    /*
     * In the fashion of Where's Waldo, let's find our java.policy file
     * With open jdk version 9 and up, it should be under java.home/conf/security
     * With earlier versions of the non-open JDK and OpenJDK, it should be under java.home/security 
     * if java.home points to the jre directory; else it should be under java.home/jre/security
     */
    public String wheresJavaPolicy() {
        String policyLocation = null;
        if (javaHome.endsWith("jre")) {
            // let's look under java.home/jre/lib/security
            policyLocation = javaHome.concat("/lib/security/java.policy");
            File fileToCheck = new File(policyLocation);
            if (!fileToCheck.exists()) {
                // if not under java.home/jre/lib/security, let's look under lib/security
                String javaHomeWithoutJre = javaHome.substring(0, javaHome.length() - 3);
                policyLocation = javaHomeWithoutJre.concat("/lib/security/java.policy");
                if (!fileToCheck.exists()) {
                    // if not under lib/security, then return
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "javaHome: " + javaHome + "Could not find java.policy file under either java.home/jre/lib/security or java.home/lib/security");
                    }
                } else {
                    return policyLocation;
                }
            } else {
                return policyLocation;                
            }
        } else {
            // let's look under java.home/lib/security
            policyLocation = javaHome.concat("/lib/security.java.policy");
            File fileToCheck = new File(policyLocation);
            if (!fileToCheck.exists()) {
                // if not under java.home/lib/security,  look under java.home/jre/lib/security
                policyLocation = javaHome.concat("/jre/lib/security/java.policy");
                fileToCheck = new File(policyLocation);
                if (!fileToCheck.exists()) {
                    // if not there, return
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "javaHome: " + javaHome + "Could not find java.policy file under either java.home/jre/lib/security or java.home/lib/security");
                    }
                } else {
                    return policyLocation;
                }
            } else {
                return policyLocation;
            }
        }                        
        return policyLocation;
    }
    
    /*
     * Return the arrayList of grants that were defined in the java.policy file.
     * The grants variable will consist of an ArrayList of GrantEntry types
     */
    public static List getJavaPolicyGrants() {
        return grants;
    }

    /*
     * Initialize the parser to point to the implementing class of the Parser class, and
     * whether we will expand the properties.
     */
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

    /*
     * Traverse the java.policy file, looking for two things:  either a grant keyword or a keystore keyword.
     * If we find a grant keyword, then we call parseGrantEntry to parse through the grant for the codeBase,
     * signedBy, and list of permissions.
     * If we find a keystore keyword, then we parse the keystore entry.  If the keystore type is not specified,
     * we default to use JKS per the Java 2 spec.
     */
    void parse() throws IOException, ParserException {
        for (parser.nextToken(); !parser.eof(); parser.match(";")) {
            if (parser.peek(Constants.GRANT_KEYWORD)) {
                // parse grant
                GrantEntry g = parseGrantEntry();
                if (g != null) {
                    grants.add(g);
                }
            } else if (parser.peek(Constants.KEYSTORE_KEYWORD)) {
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

    /*
     * A keystore is a database of private keys and their associated digital certificates such as X.509 certificate chains 
     * authenticating the corresponding public keys. The keytool utility (for Solaris/Linux) (for Windows) is used to create 
     * and administer keystores. The keystore specified in a policy configuration file is used to look up the public keys of 
     * the signers specified in the grant entries of the file. A keystore entry must appear in a policy configuration file 
     * if any grant entries specify signer aliases, or if any grant entries specify principal aliases (see below).  At this time, 
     * there can be only one keystore/keystorePasswordURL entry in the policy file (other entries following the first one are ignored). 
     * This entry can appear anywhere outside the file's grant entries. It has the following syntax:
     * keystore "some_keystore_url", "keystore_type", "keystore_provider";
     * keystorePasswordURL "some_password_url";
     * some_keystore_url specifies the URL location of the keystore, some_password_url specifies the URL location of the 
     * keystore password, keystore_type specifies the keystore type, and keystore_provider specifies the keystore provider. 
     * Note that the input stream from some_keystore_url is passed to the KeyStore.load method. If NONE is specified as the URL, 
     * then a null stream is passed to the KeyStore.load method. NONE should be specified if the KeyStore is not file-based, 
     * for example, if it resides on a hardware token device.  The URL is relative to the policy file location. Thus if the 
     * policy file is specified in the security properties file as:
     *     policy.url.1=http://foo.example.com/fum/some.policy
     *     and that policy file has an entry:
     *         keystore ".keystore";
     *         then the keystore will be loaded from:
     *             http://foo.example.com/fum/.keystore
     *                 The URL can also be absolute.
     * A keystore type defines the storage and data format of the keystore information, and the algorithms used to protect private 
     * keys in the keystore and the integrity of the keystore itself. The default type supported by Sun Microsystems is a 
     * proprietary keystore type named "JKS". Thus, if the keystore type is "JKS", it does not need to be specified 
     * in the keystore entry.
     */
    
    void parseKeystoreEntry() throws IOException, ParserException {
        parser.match(Constants.KEYSTORE_KEYWORD);
        keyStoreUrlString = parser.match(Constants.QUOTED_STRING);
        if(!parser.peek(","))
            return;
        parser.match(",");
        if(parser.peek("\""))
            keyStoreType = parser.match(Constants.QUOTED_STRING);
        else
            throw new ParserException(parser.getLineNumber(), "expected keystore type");
    }
    
    /*
     * See GrantEntry class for a description of the GrantEntry syntax
     * This method parses for the keywords: grant, codeBase, signedBy and permission
     */

    GrantEntry parseGrantEntry() throws IOException, ParserException {
        
        String filePrefix = "file:";
        
        GrantEntry g = new GrantEntry();
        parser.match(Constants.GRANT_KEYWORD);
        while (!parser.peek("{")) {
            if (parser.peek(Constants.CODEBASE_KEYWORD)) {
                parser.match(Constants.CODEBASE_KEYWORD);
                g.codeBase = parser.match(Constants.QUOTED_STRING);
                
                
                if ((g.codeBase).startsWith(filePrefix)) {
                    g.codeBase = (g.codeBase).substring(filePrefix.length());
                }                
              
                if (parser.peek(",")) {
                    parser.match(",");
                }
                
            } else if (parser.peek(Constants.SIGNEDBY_KEYWORD)) {
                parser.match(Constants.SIGNEDBY_KEYWORD);
                g.signedBy = parser.match(Constants.QUOTED_STRING);
                if (parser.peek(",")) {
                    parser.match(",");
                }
            } else {
                throw new ParserException(parser.getLineNumber(), "expected " + Constants.CODEBASE_KEYWORD + " or " + Constants.SIGNEDBY_KEYWORD);
            }
        }
        parser.match("{");

        while (!parser.peek("}")) {
            if (parser.peek(Constants.PERMISSION_KEYWORD)) {
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
    
    /*
     * See the PermissionEntry class for a description of the PermissionEntry syntax
     * This method parses for the keywords: permission, signedBy, name, action
     */

    PermissionEntry parsePermissionEntry() throws IOException, ParserException {
        
        PermissionEntry p = new PermissionEntry();
        parser.match(Constants.PERMISSION_KEYWORD);
        p.permissionType = parser.match(Constants.PERMISSION_TYPE);
        if (parser.peek("\"")) {
            p.name = expand(parser.match_p(Constants.QUOTED_STRING)).trim(); //JDK BUG 177028
        }
        if (!parser.peek(",")) {
            return p;
        }

        parser.match(",");
        if (parser.peek("\"")) {
            p.action = expand(parser.match(Constants.QUOTED_STRING));

            if (!parser.peek(",")) {
                return p;
            }
            parser.match(",");
        }

        try {
            if (parser.peek(Constants.SIGNEDBY_KEYWORD)) {
                parser.match(Constants.SIGNEDBY_KEYWORD);
                p.signedBy = expand(parser.match(Constants.QUOTED_STRING));
            }
        } catch (ParserException e) {
            return (null); 
        }

        return p;
    }

    String expand(String str) throws ParserException {
        return expand(str, false);
    }

    /*
     * Expands any variables if the encodeValue is set to true
     */
    String expand(String str, boolean encodeValue) throws ParserException {
        
        if (expandProp == true) {
            int strLen = 0;
            if ((str == null) || ((strLen = str.length()) == 0)) {
                return str;
            }

            StringBuffer buf = new StringBuffer(strLen + 25);
            for (int index = 0, last = 0; last < strLen; ) {
                // look for file:${{ first
                index = str.indexOf("${{", last);
                if (index == -1) {
                    // we didn't find a {{, so look for a {
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
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "key: " + key);
                    }
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
                } else {
                    buf.append(str.substring(last, index));
                    last = str.indexOf("}", index);
                    if (last == -1) {
                        buf.append(str.substring(index));
                        break;
                    }
                    String key = str.substring(index + 3, last);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "key: " + key);
                    }

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
 
                }
                

                last += 1;
            }
            return buf.toString();
        } else {
            return str;
        }
    }




}

