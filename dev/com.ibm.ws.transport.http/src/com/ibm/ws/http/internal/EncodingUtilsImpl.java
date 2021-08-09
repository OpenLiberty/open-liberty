/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.http.EncodingUtils;

/**
 * EncodingUtils provides various methods for manipulating and retrieving
 * information related to charsets, locales, and other encoding data.
 */
@Component(configurationPid = "com.ibm.ws.transport.http.encoding", property = { "service.vendor=IBM" })
public class EncodingUtilsImpl implements EncodingUtils {
    /** Debug variable */
    private static final TraceComponent tc = Tr.register(EncodingUtilsImpl.class);

    /** Default HTTP ASCII encoding charset */
    private static final String DEFAULT_ENCODING = "ISO-8859-1";

    private static final byte[] TEST_CHAR = { 'a' };

    /** Mapping of Locale strings to encodings, en=ISO-8859-1 */
    private final Map<String, String> localeMap = new HashMap<String, String>();
    /** Mapping encodings to converters, EUC-JP=Cp33722C */
    private final Map<String, String> converterMap = new HashMap<String, String>();
    /** Cache on whether this JVM supports particular encodings */
    private final Map<String, Boolean> supportedEncodingsCache = new HashMap<String, Boolean>();
    /** Cache of accept-language header values to lists of locales */
    private final Map<String, List<Locale>> localesCache = new HashMap<String, List<Locale>>();
    /** Most recently queried locale */
    private Locale cachedLocale = null;
    /** Most recently queried locale encoding */
    private String cachedEncoding = null;

    /**
     * Constructor.
     */
    public EncodingUtilsImpl() {
    // do nothing
    }

    /**
     * DS activation method for this component.
     * 
     * @param context
     */
    @Activate
    protected void activate(Map<String, Object> config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Activating " + this);
        }
        modified(config);
    }

    /**
     * DS deactivation method for this component.
     * 
     * @param context
     */
    @Deactivate
    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Deactivating " + this, "reason=" + reason);
        }
        // purge cached data from config
        this.cachedEncoding = null;
        this.cachedLocale = null;
        this.localeMap.clear();
        this.converterMap.clear();
        this.supportedEncodingsCache.clear();
        this.localesCache.clear();
    }

    /**
     * DS method for runtime updates to configuration without stopping and
     * restarting the component.
     * 
     * @param config
     */
    @Modified
    protected void modified(Map<String, Object> config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Config modified: " + this);
        }

        if (null == config) {
            return;
        }
        final String ENC = "encoding.";
        final String CONV = "converter.";
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            int len = key.length();
            try {
                if (len > ENC.length() && key.startsWith(ENC)) {
                    String value = (String) entry.getValue();
                    localeMap.put(key.substring(ENC.length()), value);
                } else if (len > CONV.length() && key.startsWith(CONV)) {
                    String value = (String) entry.getValue();
                    converterMap.put(key.substring(CONV.length()).toLowerCase(),
                                     value.toLowerCase());
                }
            } catch (Throwable t) {
                FFDCFilter.processException(t,
                                            "EncodingUtils.processConfig", "1",
                                            new Object[] { key, entry.getValue() });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Invalid property: [" + key
                                 + "]=[" + config.get(key) + "]");
                }
            }
        } // loop on the entries
    }

    /*
     * @see com.ibm.websphere.http.EncodingUtils#getDefaultEncoding()
     */
    @Override
    public String getDefaultEncoding() {
        return DEFAULT_ENCODING;
    }

    /*
     * @see com.ibm.websphere.http.EncodingUtils#getCharsetFromContentType(java.lang.String)
     */
    @Override
    public String getCharsetFromContentType(String type) {
        if (null != type) {
            int index = type.indexOf(';');
            if (-1 != index) {
                index = type.indexOf("charset=", (index + 1));
                if (-1 != index) {
                    String charset = type.substring(index + 8).trim();
                    int end = charset.length() - 1;
                    if (-1 == end) {
                        // empty value following charset=
                        return null;
                    }
                    if (end > 0 && '"' == charset.charAt(0) && '"' == charset.charAt(end)) {
                        // strip off quotes if both exist
                        charset = charset.substring(1, end);
                    }
                    return charset;
                }
            }
        }
        return null;
    }

    /*
     * @see com.ibm.websphere.http.EncodingUtils#getLocales(java.lang.String)
     */
    @Override
    public List<Locale> getLocales(String acceptLangHdr) {
        // Short circuit with an empty enumeration if null header
        if (null == acceptLangHdr) {
            List<Locale> def = new ArrayList<Locale>(1);
            def.add(Locale.getDefault());
            return def;
        }

        // Check cache
        List<Locale> langList = this.localesCache.get(acceptLangHdr);
        if (null == langList) {
            // Create and add to cache
            List<List<String>> rc = processAcceptLanguage(acceptLangHdr);
            if (null != rc) {
                langList = extractLocales(rc);
                this.localesCache.put(acceptLangHdr, langList);
            } else {
                // this can happen with only q=0 values in the header
                List<Locale> def = new ArrayList<Locale>(1);
                def.add(Locale.getDefault());
                return def;
            }
        }

        return langList;
    }

    /**
     * Processes the accept language header into a sublists based on the
     * qvalue. Each sublist is a list of string values for a given qvalue
     * and the overall list is ordered with preferred languages first.
     * 
     * @param acceptLanguage to process.
     * @return List<List<String>>
     */
    private List<List<String>> processAcceptLanguage(String acceptLanguage) {
        StringTokenizer languageTokenizer = new StringTokenizer(acceptLanguage, ",");
        TreeMap<Double, List<String>> map = new TreeMap<Double, List<String>>(Collections.reverseOrder());
        List<String> list;

        while (languageTokenizer.hasMoreTokens()) {
            String language = languageTokenizer.nextToken().trim();
            if (language.length() == 0) {
                continue;
            }

            int semicolonIndex = language.indexOf(';');
            Double qValue = Double.valueOf(1);

            if (semicolonIndex > -1) {
                int qIndex = language.indexOf("q=");
                String qValueStr = language.substring(qIndex + 2);
                try {
                    qValue = Double.valueOf(qValueStr.trim());
                } catch (NumberFormatException nfe) {
                    FFDCFilter.processException(nfe,
                                                "EncodingUtils.processAcceptLanguage", "215");
                }
                language = language.substring(0, semicolonIndex);
            }
            if (language.length() > 0) {
                if ((qValue.doubleValue() > 0) && (language.charAt(0) != '*')) {
                    list = map.get(qValue);
                    if (null == list) {
                        list = new ArrayList<String>(1);
                    }
                    list.add(language);
                    map.put(qValue, list);
                }
            }
        }

        List<List<String>> rc = null;
        if (!map.isEmpty()) {
            rc = new ArrayList<List<String>>(map.values());
        }
        return rc;
    }

    /**
     * Extract the locales from a passed in language list.
     * 
     * @param allLangs
     * @return List<Locale>
     */
    private List<Locale> extractLocales(List<List<String>> allLangs) {
        List<Locale> rc = new ArrayList<Locale>();
        for (List<String> langList : allLangs) {
            for (String language : langList) {
                String country = "";
                String variant = "";
                int countryIndex = language.indexOf('-');
                if (countryIndex > -1) {
                    int variantIndex = language.indexOf('-', (countryIndex + 1));
                    if (variantIndex > -1) {
                        variant = language.substring(variantIndex + 1).trim();
                        country = language.substring(countryIndex, variantIndex).trim();
                    } else {
                        country = language.substring(countryIndex + 1).trim();
                    }
                    language = language.substring(0, countryIndex).trim();
                }

                rc.add(new Locale(language, country, variant));
            }
        }

        return rc;
    }

    /*
     * @see com.ibm.websphere.http.EncodingUtils#getEncodingFromLocale(java.util.Locale)
     */
    @Override
    public String getEncodingFromLocale(Locale locale) {
        if (null == locale) {
            return null;
        }
        if (locale.equals(this.cachedLocale)) {
            return this.cachedEncoding;
        }

        String encoding = localeMap.get(locale.toString());
        if (null == encoding) {
            final String lang = locale.getLanguage();
            encoding = localeMap.get(lang + '_' + locale.getCountry());
            if (null == encoding) {
                encoding = localeMap.get(lang);
            }
        }

        this.cachedEncoding = encoding;
        this.cachedLocale = locale;

        return encoding;
    }

    /*
     * @see com.ibm.websphere.http.EncodingUtils#getJvmConverter(java.lang.String)
     */
    @Override
    public String getJvmConverter(String encoding) {
        if (null != encoding) {
            String converter = this.converterMap.get(encoding.toLowerCase());
            if (null != converter) {
                return converter;
            }
        }
        return encoding;
    }

    /*
     * @see com.ibm.websphere.http.EncodingUtils#isCharsetSupported(java.lang.String)
     */
    @Override
    public boolean isCharsetSupported(String charset) {
        if (null == charset) {
            return false;
        }
        Boolean supported = this.supportedEncodingsCache.get(charset);
        if (supported != null) {
            return supported.booleanValue();
        }
        try {
            new String(TEST_CHAR, charset);
            supported = Boolean.TRUE;
        } catch (UnsupportedEncodingException e) {
            supported = Boolean.FALSE;
        }
        this.supportedEncodingsCache.put(charset, supported);
        return supported.booleanValue();
    }

    /*
     * @see com.ibm.websphere.http.EncodingUtils#stripQuotes(java.lang.String)
     */
    @Override
    public String stripQuotes(String value) {
        if (null == value) {
            return null;
        }
        String modvalue = value.trim();
        if (0 == modvalue.length()) {
            return modvalue;
        }
        boolean needTrimming = false;
        int start = 0;
        if ('"' == modvalue.charAt(0) || '\'' == modvalue.charAt(0)) {
            start = 1;
            needTrimming = true;
        }
        int end = modvalue.length() - 1;
        if ('"' == modvalue.charAt(end) || '\'' == modvalue.charAt(end)) {
            needTrimming = true;
        } else {
            end++;
        }
        if (needTrimming) {
            return modvalue.substring(start, end);
        }
        return modvalue;
    }
}
