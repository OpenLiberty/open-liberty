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

import java.io.File;
import java.util.StringTokenizer;

public class NameMangler {
	// defect 196156 begin
    public static String[] keywords;
    public static String [] keywordsSlash;
    static {
        String [] _keywords = {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class",
            "const", "continue", "default", "do",
            "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto",
            "if", "implements", "import",
            "instanceof", "int", "interface",
            "long", "native", "new", "package",
            "private", "protected", "public",
            "return", "short", "static", "super",
            "switch", "synchronized", "this",
            "throw", "throws", "transient",
            "try", "void", "volatile", "while"
        };
        keywords = _keywords;

        keywordsSlash = new String [_keywords.length];
        for (int i=0; i < _keywords.length; i++)
            keywordsSlash [i] = "/" + _keywords[i];

    }
	// defect 196156 end
	
    public static final String mangleName(String name) {
        String convertedClassName;
        int iSep = name.lastIndexOf('/') + 1;
        int iEnd = name.length();
        convertedClassName = name.substring(iSep, iEnd);

        if (name.endsWith(".jsp")) {
            convertedClassName = convertedClassName.substring(0, convertedClassName.length() - 4);
        }
        else if (name.indexOf(".") > -1) {
            convertedClassName = convertedClassName.replace('.', '_');
        }

        return mangleString(convertedClassName);
    }

    public static final String mangleClassName(String jspFileName) {
        return "_" + mangleName(jspFileName);
    }

	public static final String mangleChar ( char ch){
		return mangleChar (ch, false);

	}

    public static final String mangleChar(char ch, boolean shouldMangleLetterDigit) {
        if ( ch == File.separatorChar ) {
            ch = '/';
        }
        if (!shouldMangleLetterDigit) {
            if ( Character.isLetterOrDigit(ch) == true ) {
                return "" + ch;
            }
        }
        return "_" + Integer.toHexString(ch).toUpperCase() + "_";
    }

    public static String mangleString(String name) {
        StringBuffer modifiedName = new StringBuffer();
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetterOrDigit(name.charAt(i)) == true || name.charAt(i) == '/') {
                modifiedName.append(name.substring(i, i + 1));
            }
            else {
                modifiedName.append(mangleChar(name.charAt(i)));
            }
        }
        return modifiedName.toString();
    }
    
	// defect 196156 begin
    public static String handlePackageName (String mangledDirName){    // note any mangled string needs to be capable of be unmangled by WSAD.
        mangledDirName = mangledDirName.replace ('\\', '/');
        String mangledDirNameBuff = mangleString (mangledDirName);
        mangledDirName = handleReservedWords (mangledDirNameBuff.toString() );
        mangledDirName = removeLeadingSlashes(new StringBuffer(mangledDirName));
        mangledDirName = handlePackageStartingWithDigits(mangledDirName);
        return convertFileSepCharToPackageChar (mangledDirName);
    }

    public static String handlePackageStartingWithDigits (String mangledDirName){
        StringBuffer tmpPackageName = new StringBuffer();
        StringTokenizer st = new StringTokenizer (mangledDirName, "/", true);
        while ( st.hasMoreTokens()) {
            String currToken = st.nextToken();
            if (Character.isDigit(currToken.charAt(0)) ) {
                tmpPackageName.append (mangleChar (currToken.charAt(0), true));
                tmpPackageName.append (currToken.substring (1) );
            } else {
                tmpPackageName.append(currToken);
            }
        }
        return tmpPackageName.toString();
    }

    public static String removeLeadingSlashes(StringBuffer buff){
        int index = 0;
        while (buff.charAt(index) == '/') {
            index++;
        }
        String currString = buff.toString();
        return currString.substring(index);

    }

    public static String convertFileSepCharToPackageChar (String pathName){
        if ( pathName != null) {
            pathName = pathName.replace('.','_');
            pathName = pathName.replace ('\\', '/');
            String packageName = pathName.replace('/', '.');
            pathName = packageName;
        }
        return pathName;
    }

    public static String handleReservedWords( String pathName){
        String packageSep = "/";
        for ( int i = 0; i < keywords.length; i++ ) {
            boolean found = ((pathName.indexOf(keywordsSlash[i]) > -1) || (pathName.startsWith (keywords[i]))) ;
            if ( !found ) {
                continue;
            } else { //at least one occurrence was located.
                StringTokenizer st = new StringTokenizer (pathName, packageSep , true); //keep tokens
                StringBuffer pathNameBuffer = new StringBuffer();
                while ( st.hasMoreTokens() ) {
                    String currToken = st.nextToken ();
                    if ( currToken.equals (keywords[i]) ) {
                        String mgChar = mangleChar (currToken.charAt(0), true );    //mangle first character
                        String balance = currToken.substring (1);   // get balance of string unchanged
                        pathNameBuffer.append ( mgChar + balance);  // add to StringBuffer modified pkgname.
                    } else {
                        pathNameBuffer.append (currToken);
                    }
                }
                pathName = pathNameBuffer.toString();
            }
        }
        return pathName;
    }
	// defect 196156 end
}
