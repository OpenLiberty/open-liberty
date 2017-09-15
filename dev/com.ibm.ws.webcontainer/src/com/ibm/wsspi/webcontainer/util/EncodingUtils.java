/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.util;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.WCCustomProperties; 
import com.ibm.wsspi.webcontainer.WebContainer;
import com.ibm.wsspi.webcontainer.WebContainerConstants;

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

    private static Object lock = new Object();
    //private static Properties _localeProps = null;
    //private static Properties _converterProps = null;
    //private static HashMap _localeMap = new HashMap();
    //private static HashMap _converterMap = new HashMap();
    private static boolean inited = false;
    private static Hashtable supportedEncodingsCache = new Hashtable();
    private final static byte[] TEST_CHAR = {'a'};
    public static boolean setContentTypeBySetHeader;

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
    
    public static void init(){
    	if (inited==true) return;
    	//com.ibm.wsspi.http.EncodingUtils encodingUtils = com.ibm.ws.webcontainer.osgi.WebContainer.getEncodingUtils();
    	
    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
    		logger.logp(Level.FINE, CLASS_NAME,"init","initing EncodingUtils");
		synchronized(lock){
			//296095    Web Services Uncaught exception in service method    WASCC.web.webcontainer    
			if (inited==true) return;
			//296095    Web Services Uncaught exception in service method    WASCC.web.webcontainer

			//WebContainer wc = WebContainer.getWebContainer();
	    	//WebContainerConfig wcConfig = wc.getWebContainerConfig();
	    	//if (wcConfig !=null ){
		    	//_localeProps = wcConfig.getLocaleProps();
		    	//_converterProps = wcConfig.getConverterProps();
	    	//}
	    	
	    	/*if (_localeProps == null){
	    		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
	        		logger.logp(Level.FINE, CLASS_NAME,"init","locale properties using default location");
	    		_localeProps = new Properties ();
		        try {
		                AccessController.doPrivileged(new PrivilegedExceptionAction() {
		                    public Object run() throws IOException {
		                        for (Enumeration enumeration = EncodingUtils.class.getClassLoader().getResources("encoding.properties"); enumeration.hasMoreElements();) {
		                            final URL url = (URL) enumeration.nextElement();
		                            InputStream is = url.openStream();
		                            _localeProps.load(is);
		                            is.close();
		                            //System.out.println(" _localeProps "+_localeProps);
		                        }
		                        return null;
		                    }
		                });
		        }
		        catch (Throwable ex) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ex, "com.ibm.ws.webcontainer.srt.SRTRequestUtils", "56");
		            logger.logp(Level.SEVERE, CLASS_NAME,"init", "failed.to.load.encoding.properties", ex);
		        }
	    	}
	    	else
	    	{
	    		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
	        		logger.logp(Level.FINE, CLASS_NAME,"init","locale properties specified by webcontainer shell");
	    	}
	    	if (_converterProps == null){
	    		_converterProps = new Properties ();
	    		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
	        		logger.logp(Level.FINE, CLASS_NAME,"init","converter properties using default location");
		        // get the converter properties
		        try {
		                AccessController.doPrivileged(new PrivilegedExceptionAction() {
		                    public Object run() throws IOException {
		                        for (Enumeration enumeration = EncodingUtils.class.getClassLoader().getResources("converter.properties"); enumeration.hasMoreElements();) {
		                            final URL url = (URL) enumeration.nextElement();
		                            InputStream is = url.openStream();
		                            _converterProps.load(is);
		                            is.close();
		                            //System.out.println(" _jvmProps "+_jvmProps);
		                        }
		                        return null;
		                    }
		                });
		
		                // lowercase the jvm props
		                Properties newProps = new Properties();
		
		                for (Enumeration e = _converterProps.propertyNames(); e.hasMoreElements();) {
		                    String key = (String) e.nextElement();
		                    String value = (String) _converterProps.get(key);
		
		                    newProps.put(key.toLowerCase(), value);
		                    //System.out.println(" _jvmProps key, value "+key.toLowerCase()+" "+value);
		                }
		                _converterProps = newProps;
		        }
		        catch (Throwable ex) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ex, "com.ibm.ws.webcontainer.srt.SRTRequestUtils", "74");
		            logger.logp(Level.SEVERE, CLASS_NAME,"init", "failed.to.load.converter.properties", ex);
		        }
	    	}
	    	else
 	    	{
 	    		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
 	        		logger.logp(Level.FINE, CLASS_NAME,"init","converter properties specified by webcontainer shell");
 	    	}
                */
	        // PK75844 Start
	    	// set the hash maps
	        //_localeMap.putAll(_localeProps);
	        //_converterMap.putAll(_converterProps);
	        // PK75844 End
	    	inited = true;
		}
    	// set the hash maps
        // _localeMap.putAll(_localeProps);         PK75844
        // _converterMap.putAll(_converterProps);   PK75844
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
    	init();
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
    	init();
        String acceptLanguage = req.getHeader("Accept-Language");
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
    	init();
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
    	init();
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
    	init();
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
    	init();
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
    
	 /**
     * Tests whether the specified charset is supported on the server
     *
     * @param String The charset we want to test
     *
     * @return boolean indicating if supported
     */
    // rewritten as part of PK13492
    public static boolean isCharsetSupported (String charset){
        Boolean supported = (Boolean) supportedEncodingsCache.get(charset);
        if(supported != null){
            return supported.booleanValue();
        }
        try{
            new String (TEST_CHAR, charset);
            supportedEncodingsCache.put(charset, Boolean.TRUE);
        }catch (UnsupportedEncodingException e){
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME,"isCharsetSupported", "Encountered UnsupportedEncoding charset [" + charset +"]");
            }
            supportedEncodingsCache.put(charset, Boolean.FALSE);
            return false;
        }
        return true;
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
