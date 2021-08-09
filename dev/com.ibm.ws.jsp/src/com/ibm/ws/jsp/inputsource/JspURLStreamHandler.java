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
package com.ibm.ws.jsp.inputsource;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import com.ibm.ws.webcontainer.util.DocumentRootUtils;


public class JspURLStreamHandler extends URLStreamHandler {
    static protected Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.inputsource.JspUrlStreamHandler";
    static{
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
	
	private String relativeUrl = null;
	private DocumentRootUtils dru = null;
	private boolean searchOnClasspath = false;
	private ClassLoader classloader = null;
	private String docRoot;
	private ServletContext servletContext;

	public JspURLStreamHandler(String docRoot, String relativeUrl,
			DocumentRootUtils dru,
			boolean searchOnClasspath,
			ClassLoader classloader,
			ServletContext servletContext) {
		this.docRoot = docRoot;
		this.relativeUrl = relativeUrl;
		this.dru = dru;
		this.searchOnClasspath = searchOnClasspath;
		this.classloader = classloader;
		this.servletContext = servletContext;
	}

	protected URLConnection openConnection(URL url) throws IOException {
		return new JspURLConnection(docRoot, url, relativeUrl, dru, searchOnClasspath, classloader,servletContext);
	}

	protected void parseURL(URL u, String spec, int start, int limit) {
		String encodedChars = replaceEncodedChars(spec);
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
			if(spec.equals(encodedChars) == false){
				logger.logp(Level.FINEST, CLASS_NAME, "parseURL", "parseURL spec ["+ spec +"]");
				logger.logp(Level.FINEST, CLASS_NAME, "parseURL", "parseURL spec encoded ["+ encodedChars +"]");
			}
		}
		int length = encodedChars.length(); // new encoded length limit
		super.parseURL(u, encodedChars, start, start + length);

	}

	private static Map ENCODED_CHARACTER_MAP = new HashMap();

	private static Pattern CHARS_REQUIRING_ENCODING = null;
	static {
		StringBuffer tmpBuffer = new StringBuffer();
		String reservedCharacterString = "!\"#$%&'()*+,:;<=>?@[]^`{|}~"; // list of punctuation characters obtained from http://java.sun.com/j2se/1.4.2/docs/api/java/util/regex/Pattern.html
																		// removed _ - . / \ since these do not need to be encoded.
		char reservedCharacters[] = reservedCharacterString.toCharArray();
		for (int i = 0; i < reservedCharacters.length; i++) {
			String reservedChar = String.valueOf(reservedCharacters[i]);
			tmpBuffer.append("(\\" + reservedChar + ")");
			if (i != (reservedCharacters.length - 1)) {
				tmpBuffer.append("|");
			}
			try {
				String encoded = URLEncoder.encode(reservedChar, "UTF-8");
				ENCODED_CHARACTER_MAP.put(reservedChar, encoded);
			} catch (Exception e) {
				// should not happen
				logger.logp(Level.FINER, CLASS_NAME, "staticInit", "failed	to add encoding "+ reservedChar, e);
			}
		}
		CHARS_REQUIRING_ENCODING = Pattern.compile(tmpBuffer.toString());
	}

	public static String replaceEncodedChars(String replaceString) {
		Matcher m = CHARS_REQUIRING_ENCODING.matcher(replaceString);
		StringBuffer sb = new StringBuffer();
		boolean matchFound = false;
		while (m.find()) {
			matchFound = true;
			m.appendReplacement(sb, (String) ENCODED_CHARACTER_MAP.get(m
					.group()));
		}
		if (matchFound) {
			sb = m.appendTail(sb);
			return sb.toString();
		} else {
			return replaceString;
		}
	}

}
