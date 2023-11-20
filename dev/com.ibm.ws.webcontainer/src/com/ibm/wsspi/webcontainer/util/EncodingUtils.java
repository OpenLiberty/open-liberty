/*******************************************************************************
 * Copyright (c) 1997, 2023 IBM Corporation and others.
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
package com.ibm.wsspi.webcontainer.util;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.srt.ISRTServletRequest;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.WebContainer;
import com.ibm.wsspi.webcontainer.WebContainerConstants;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

/**
 *
 * 
 * EncodingUtils provides various methods for manipulating and retrieving
 * information related to charsets, locales, and other encoding data.
 * 
 * @ibm-private-in-use
 * 
 * @since   WAS6.1
 * 
 */
/*
 * This class is a wrapper around the Liberty - Declarative Services way of declaring encodings.
 * This should just call in to that service to get the desired encodings.
 * It is kept for comparison sake to traditional WAS.
 */

@SuppressWarnings("unchecked")
public class EncodingUtils {
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.util");
    private static final String CLASS_NAME="com.ibm.wsspi.webcontainer.util.EncodingUtils";

    private static final Map<String, Charset> supportedEncodingsCache = new ConcurrentHashMap<>();

    /**
     * This Class is just used as a place holder for an invalid Charset so we don't have to look it up again and found
     * that it isn't valid again.
     */
    private static final Charset NOT_FOUND = new Charset("not_found", new String[0]) {
        @Override
        public CharsetEncoder newEncoder() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public CharsetDecoder newDecoder() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean contains(Charset cs) {
            throw new UnsupportedOperationException();
        }
    };

    public static final boolean setContentTypeBySetHeader;

    static {
    	String propStr = WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.setcontenttypebysetheader");
    	if (propStr==null||propStr.equalsIgnoreCase("true")){
    		setContentTypeBySetHeader = true;
    	}
    	else {
    		setContentTypeBySetHeader = false;
    	}
    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
    		logger.logp(Level.FINE, CLASS_NAME,"staticInitializer","setContentTypeBySetHeader="+Boolean.toString(setContentTypeBySetHeader));
    	}
    }
    
    /**
     * Basically returns everything after ";charset=".  If no charset specified, uses
     * the HTTP default (ASCII) character set.
     *
     * @param type The content type to extract the charset from.
     *
     * @return The charset encoding.
     */
    public static String getCharsetFromContentType(String type) {
        if (type == null) {
            return null;
        }

        int semi = type.indexOf(";");

        if (semi == -1) {
            return null;
        }

        String afterSemi = type.substring(semi + 1);

        int charsetLocation = afterSemi.indexOf("charset=");

        if (charsetLocation == -1) {
            return null;
        }

        return afterSemi.substring(charsetLocation + 8).trim();
    }

    // Keep a cache of locales
    private static final Hashtable localesCache = new Hashtable();

    /**
     * Returns a Vector of locales from the passed in request object.
     *
     * @param req The request object to extract the locales from.
     *
     * @return The extracted locales.
     */
    public static Vector getLocales(HttpServletRequest req) {
        String acceptLanguage = ISRTServletRequest.getHeader(req, HttpHeaderKeys.HDR_ACCEPT_LANGUAGE);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"getLocales", "Accept-Language --> " + acceptLanguage);
        }

        // Short circuit with an empty enumeration if null header
        if ((acceptLanguage == null)|| (acceptLanguage.trim().length() ==0)) { 
            Vector def = new Vector();
            def.addElement(Locale.getDefault());
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME,"getLocales", "processed Locales --> ", def);
            }
            return def;
        }

        // Check cache
        Vector langList = null;
        langList = (Vector) localesCache.get(acceptLanguage);

        if (langList == null) {
            // Create and add to cache
            langList = processAcceptLanguage(acceptLanguage);
          
            if(WCCustomProperties.VALIDATE_LOCALE_VALUES){
                langList = extractLocales(langList , true);
            }
            else
                langList = extractLocales(langList , false);
            
            localesCache.put(acceptLanguage, langList);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"getLocales", "processed Locales --> " + langList);
        }

        return langList;
    }

    /**
     * Processes the accept languages in a passed in String into a Vector object.
     *
     * @param acceptLanguage The accept language String to process.
     *
     * @return The processed accept languages.
     */
    public static Vector processAcceptLanguage(String acceptLanguage) {
        StringTokenizer languageTokenizer = new StringTokenizer(acceptLanguage, ",");
        TreeMap map = new TreeMap(Collections.reverseOrder());

        while (languageTokenizer.hasMoreTokens()) {
            String language = languageTokenizer.nextToken().trim();
            /* begin pq57399: part 1 */
            if (language == null || language.length() == 0) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME,"processAcceptLanguage", "Encountered zero length language token without quality index.. skipping token");
                    logger.logp(Level.FINE, CLASS_NAME,"processAcceptLanguage", "acceptLanguage param = [" + acceptLanguage + "]");
                }
                continue;
            }
            /* end pq57399: part 1 */

            int semicolonIndex = language.indexOf(';');
            Double qValue = new Double(1);

            if (semicolonIndex > -1) {
                int qIndex = language.indexOf("q=");
                String qValueStr = language.substring(qIndex + 2);
                try {
                    qValue = new Double(qValueStr.trim());
                }
                catch (NumberFormatException nfe) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(nfe, "com.ibm.ws.webcontainer.srt.SRTRequestUtils.processAcceptLanguage", "215");
                }
                language = language.substring(0, semicolonIndex);
            }
            if (language.length() > 0) { /* added for pq57399: part 2*/
                if ((qValue.doubleValue() > 0) && (language.charAt(0) != '*')) {
                    Vector newVector = new Vector();
                    if (map.containsKey(qValue)) {
                        newVector = (Vector) map.get(qValue);
                    }
                    newVector.addElement(language);
                    map.put(qValue, newVector);
                } /* begin pq57399: part 3 */

            }
            else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME,"processAcceptLanguage", "Encountered zero length language token with quality index.. skipping token");
                    logger.logp(Level.FINE, CLASS_NAME,"processAcceptLanguage", "acceptLanguage param = [" + acceptLanguage + "]");
                }

            } /* end pq57399: part 3 */

        }

        if (map.isEmpty()) {
            Vector v = new Vector();
            v.addElement(Locale.getDefault().toString());
            map.put("1", v);
        }
        return new Vector(map.values());
    }

    /**
     * Extract the locales from a passed in language Vector.
     *
     * @param languages The language Vector to extract the locales from.
     *
     * @return The extracted locales.
     */
    public static Vector extractLocales(Vector languages) {
        return extractLocales(languages , false);
    }
        
    /** 
     * This method will validate the values.
     * 
     * Validate language and country values as alphanumeric.
     * Validate variant value as alphanumeric , '_' ,and  '-'
     * Add appropriate values in Locale upon validation.
     * 
     * Extract the locales from a passed in language Vector.
     *
     * @param languages The language Vector to extract the locales from.
     * @param secure
     * @return The extracted locales.
     */ 
    public static Vector extractLocales(Vector languages, boolean secure) {
        Enumeration e = languages.elements();
        Vector l = new Vector();

        while (e.hasMoreElements()) {
            Vector langVector = (Vector) e.nextElement();
            Enumeration enumeration = langVector.elements();
            while (enumeration.hasMoreElements()) {
                String language = (String) enumeration.nextElement();
                String country = "";
                String variant = "";
                int countryIndex = language.indexOf("-");

                if (countryIndex > -1) {
                    country = language.substring(countryIndex + 1).trim();
                    language = language.substring(0, countryIndex).trim();

                    int variantIndex = country.indexOf("-");
                    if (variantIndex > -1) {
                        variant = country.substring(variantIndex + 1).trim();
                        country = country.substring(0, variantIndex).trim();
                    }

                } 
                if(secure){
                    if ((country.trim().length()!= 0 && !isValueAlphaNumeric(country, "country")) ||
                                    (language.trim().length()!= 0 && !isValueAlphaNumeric(language, "language"))) {
                        language = Locale.getDefault().getLanguage();
                        country = Locale.getDefault().getCountry();
                        variant = "";
                    }                                   
                    if (variant.trim().length()!= 0 && !isValueAlphaNumeric(variant, "variant")) {
                        variant = "";
                    }
                }

                l.addElement(new Locale(language, country, variant));
            }
        }

        return l;
    }
    
    /**
     * @param input
     * @param caller
     * @return
     */
    private static boolean isValueAlphaNumeric(String input, String caller) {   

        for(int i = 0; i < input.length(); i++)
        {
            char ch = input.charAt(i);
            if (!Character.isLetterOrDigit(ch) && !(caller.equalsIgnoreCase("variant") && (( ch == '-') || (ch == '_')))){                    
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME,"isValueAlphaNumeric", "Encountered incorrect value ["+ch+"] .. in this token [" +caller+"]");
                }
                return false;           
            }
        }
        return true;            
    }

    private static Locale cachedLocale = null;
    private static String cachedEncoding = null;


    /**
     * Get the encoding for a passed in locale.
     *
     * @param locale The locale.
     *
     * @return The encoding.
     */
    public static String getEncodingFromLocale(Locale locale) {
        if (locale == cachedLocale) {
            return cachedEncoding;
        }

        String encoding = null;
        /*(String) _localeMap.get(locale.toString());

        if (encoding == null) {
            encoding = (String) _localeMap.get(locale.getLanguage() + "_" + locale.getCountry());

            if (encoding == null) {
                encoding = (String) _localeMap.get(locale.getLanguage());
            }            
        }*/
        if (encoding == null) {
            //check the com.ibm.wsspi.http.EncodingUtils
            com.ibm.wsspi.http.EncodingUtils encodingUtils = com.ibm.ws.webcontainer.osgi.WebContainer.getEncodingUtils();
            if (encodingUtils!=null) {
                encoding = encodingUtils.getEncodingFromLocale(locale);
            }
        }

        cachedEncoding = encoding;
        cachedLocale = locale;

        return encoding;
    }

    /**
     * Get the JVM Converter for the specified encoding.
     *
     * @param encoding The encoding.
     *
     * @return The converter if it exists, otherwise return the encoding.
     */
    public static String getJvmConverter(String encoding) {
        //String converter = (String) _converterMap.get(encoding.toLowerCase());
    	String converter = null;
    	com.ibm.wsspi.http.EncodingUtils encodingUtils = com.ibm.ws.webcontainer.osgi.WebContainer.getEncodingUtils();
        if (encodingUtils!=null) {
            converter = encodingUtils.getJvmConverter(encoding);
        }

        if (converter != null) {
            return converter;
        }
        else {
            return encoding;
        }
    }

    public static boolean isCharsetSupported (String charset){
        return getCharsetForName(charset) != null;
    }

    /**
     * Tests whether the specified charset is supported on the server
     *
     * @param String The charset we want to test
     *
     * @return boolean indicating if supported
     */
    // rewritten as part of PK13492
    public static Charset getCharsetForName(String name) {
        Charset charset = supportedEncodingsCache.get(name);
        if (charset == null) {
            try {
                charset = Charset.forName(name);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                charset = NOT_FOUND;
            }
            supportedEncodingsCache.put(name, charset);
        }
        
        if (charset == NOT_FOUND && com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getCharsetForName", "Encountered UnsupportedEncoding charset [" + name + "]");
        }

        return charset == NOT_FOUND ? null : charset;
    }
    
    public static void setContentTypeByCustomProperty (String type, String matchString, HttpServletResponse resp){
		if (type == null)
		{		
			if (matchString.endsWith(".html") || matchString.endsWith(".htm"))
			{
				// no type specification...set text/html if
				type = "text/html";
			}
			else
			{
				type = "text/plain";
			}
		}
		
		if (setContentTypeBySetHeader){
			resp.setHeader(WebContainerConstants.HEADER_CONTENT_TYPE,type);
		}
		else{
			resp.setContentType(type);
		}
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"setContentTypeByCustomProperty","setContentType --> " +type);
    }
}
