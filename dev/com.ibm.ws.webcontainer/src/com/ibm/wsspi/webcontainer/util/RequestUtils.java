/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
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
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

/**
 *
 * 
 * RequestUtils provides methods for retrieving various data based on the current
 * request such as the parsing the query string and retrieving the current uri
 * depending on dispatch type.
 * 
 * @ibm-private-in-use
 * 
 * @since   WAS7.0
 *
 */
public class RequestUtils {
    private static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.util");
    private static final String CLASS_NAME="com.ibm.wsspi.webcontainer.util.RequestUtils";
    private static final TraceNLS nls = TraceNLS.getTraceNLS(RequestUtils.class, "com.ibm.ws.webcontainer.resources.Messages"); //724365.4

    private static final String SHORT_ENGLISH = "8859_1"; //a shortened ASCII encoding
	
    private static final boolean ignoreInvalidQueryString = WCCustomProperties.IGNORE_INVALID_QUERY_STRING;       //PK75617
    private static final boolean allowQueryParamWithNoEqual = WCCustomProperties.ALLOW_QUERY_PARAM_WITH_NO_EQUAL;       //PM35450
    private static final String EMPTY_STRING = ""; //PM35450
    private static final int maxParamPerRequest = WCCustomProperties.MAX_PARAM_PER_REQUEST; // PM53930 (724365)
    private static final int maxDuplicateHashKeyParams = WCCustomProperties.MAX_DUPLICATE_HASHKEY_PARAMS; // PM58495 (728397)
    private static final boolean decodeParamViaReqEncoding = WCCustomProperties.DECODE_PARAM_VIA_REQ_ENCODING; // PM92940
    private static final boolean printbyteValueandcharParamdata = WCCustomProperties.PRINT_BYTEVALUE_AND_CHARPARAMDATA; //PM92940

   /**
    *
    * Parses a query string passed from the client to the
    * server and builds a <code>HashTable</code> object
    * with key-value pairs.
    * The query string should be in the form of a string
    * packaged by the GET or POST method, that is, it
    * should have key-value pairs in the form <i>key=value</i>,
    * with each pair separated from the next by a & character.
    *
    * <p>A key can appear more than once in the query string
    * with different values. However, the key appears only once in
    * the hashtable, with its value being
    * an array of strings containing the multiple values sent
    * by the query string.
    *
    * <p>The keys and values in the hashtable are stored in their
    * decoded form, so
    * any + characters are converted to spaces, and characters
    * sent in hexadecimal notation (like <i>%xx</i>) are
    * converted to ASCII characters.
    *
    * @param s		a string containing the query to be parsed
    *
    * @return		a <code>HashTable</code> object built
    * 			from the parsed key-value pairs
    *
    * @exception IllegalArgumentException	if the query string
    *						is invalid
    *
    */
    @SuppressWarnings("rawtypes")
    static public Hashtable parseQueryString(String s) {
        return parseQueryString(new StringQueryString(s), SHORT_ENGLISH);
    }

    @SuppressWarnings("rawtypes")
    static public Hashtable parseQueryString(String s, String encoding) {
        return parseQueryString(new StringQueryString(s), encoding);
    }

    private static String lastShortEnglishEncoding = null;

    private static boolean isShortEnglishEncoding(String encoding) {
        if (encoding == lastShortEnglishEncoding
            || encoding == SHORT_ENGLISH
            || encoding == "ISO-8859-1")
            return true;

        if (encoding.endsWith("8859_1")
            || encoding.endsWith("8859-1")
            || encoding.indexOf("8859-1-Windows") != -1) {
            lastShortEnglishEncoding = encoding;
            return true;
        }
        return false;
    }

    private static abstract class QueryString {
        protected int pair_start = 0, equalSign = -1, i = -1;
        protected boolean isKeySingleByteString = true;
        protected boolean isValueSingleByteString = true;
        protected boolean equalsFound = false;

        abstract protected boolean lessThanEqualLength();

        abstract protected boolean isEqualLength();

        abstract protected char getNextChar();

        protected void increment() {
            i++;
        }

        protected void setEqualSign() {
            equalSign = i;
        }

        boolean findNextPair() {
            increment();
            pair_start = i;
            isKeySingleByteString = true;
            isValueSingleByteString = true;
            equalsFound = false;
            for (; lessThanEqualLength(); increment()) {
                if (isEqualLength()) {
                    if (!equalsFound) {
                        setEqualSign();
                    }
                    return true;
                }
                char c = getNextChar();
                if (c == '&') {
                    if (!equalsFound) {
                        setEqualSign();
                    }
                    return true;
                }
                // Only record the first = after pair_start so we handle if a value has an = in it.
                if (c == '=' && !equalsFound) {
                    setEqualSign();
                    equalsFound = true;
                }
                if (!decodeParamViaReqEncoding && c > '\u007F') {
                    if (equalsFound) {
                        isValueSingleByteString = false;
                    } else {
                        isKeySingleByteString = false;
                    }
                }
            }
            return false;
        }

        final boolean isKeySingleByteString() {
            return isKeySingleByteString;
        }

        final boolean isValueSingleByteString() {
            return isValueSingleByteString;
        }

        final boolean hasEquals() {
            return equalsFound;
        }

        abstract String getKey();

        abstract String getValue();

        abstract String parseKey();

        abstract String parseValue();

        /*
         * Parse a name in the query string.
         */
        // @MD17415 begin part 3 of 3 New Loop for finding key and value
        protected String parseName(final char[] ch, final int startOffset, final int endOffset, boolean isKey) {
            int j = 0;
            char[] c = null;
            for (int offset = startOffset; offset < endOffset; offset++) {
                switch (ch[offset]) {
                    case '+':
                        if (c == null) {
                            c = new char[endOffset - startOffset];
                            j = offset - startOffset;
                            if (j != 0) {
                                System.arraycopy(ch, startOffset, c, 0, j);
                            }
                        }
                        c[j++] = ' ';
                        break;
                    case '%':
                        if (offset + 2 < endOffset) { // @RWS2
                            if (c == null) {
                                c = new char[endOffset - startOffset];
                                j = offset - startOffset;
                                if (j != 0) {
                                    System.arraycopy(ch, startOffset, c, 0, j);
                                }
                            }
                            int num1 = Character.digit(ch[++offset], 16); //@RWS7
                            int num2 = Character.digit(ch[++offset], 16); //@RWS7
                            if (num1 == -1 || num2 == -1) //@RWS5
                            { //PK75617 starts
                                if (ignoreInvalidQueryString) {
                                    logger.logp(Level.WARNING, CLASS_NAME, "parseName", "invalid.query.string");
                                    return null;
                                } //PK75617 ends
                                throw new IllegalArgumentException(); //@RWS5
                            } //PK75617
                            // c[j++] = (char)(num1*16 + num2);       //@RWS5
                            char newChar = (char) ((num1 << 4) | num2); //@RWS8
                            if (!decodeParamViaReqEncoding && newChar > '\u007F') {
                                if (isKey) {
                                    isKeySingleByteString = false;
                                } else {
                                    isValueSingleByteString = false;
                                }
                            }
                            c[j++] = newChar;
                        } else { // allow '%' at end of value or second to last character (as original code does)
                            if (c != null) {
                                for (; offset < endOffset; offset++) // @RWS2
                                    c[j++] = ch[offset]; // @RWS7
                            } else {
                                offset = endOffset;
                            }
                        }
                        break;
                    default:
                        if (c != null) {
                            c[j++] = ch[offset];
                        }
                        break;
                }
            }
            String returnValue = c != null ? new String(c, 0, j) : new String(ch, startOffset, endOffset - startOffset);
            if (printbyteValueandcharParamdata && com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                printValues(returnValue, "parseNameOUT");
            return returnValue;
        }
        // @MD17415 end part 3 of 3 New Loop for finding key and value
    }

    private static class StringQueryString extends QueryString {
        private final String queryString;
        private final int lgth;

        StringQueryString(String queryString) {
            this.queryString = queryString;
            lgth = queryString.length();
        }

        protected boolean lessThanEqualLength() {
            return i <= lgth;
        }

        protected boolean isEqualLength() {
            return i == lgth;
        }

        protected char getNextChar() {
            return queryString.charAt(i);
        }

        String getKey() {
            return queryString.substring(pair_start, equalSign);
        }

        String getValue() {
            return queryString.substring(equalSign + 1, i);
        }

        String parseKey() {
            return parseName(pair_start, equalSign, true);
        }

        String parseValue() {
            return parseName(equalSign + 1, i, false);
        }

        private String parseName(final int startOffset, final int endOffset, boolean isKey) {
            StringBuilder sb = null;
            for (int offset = startOffset; offset < endOffset; offset++) {
                char c = queryString.charAt(offset);
                switch (c) {
                    case '+':
                        if (sb == null) {
                            sb = new StringBuilder(endOffset - startOffset);
                            if (offset != startOffset) {
                                sb.append(queryString, startOffset, offset);
                            }
                        }
                        sb.append(' ');
                        break;
                    case '%':
                        if (offset + 2 < endOffset) { // @RWS2
                            if (sb == null) {
                                sb = new StringBuilder(endOffset - startOffset);
                                if (offset != startOffset) {
                                    sb.append(queryString, startOffset, offset);
                                }
                            }
                            int num1 = Character.digit(queryString.charAt(++offset), 16); //@RWS7
                            int num2 = Character.digit(queryString.charAt(++offset), 16); //@RWS7
                            if (num1 == -1 || num2 == -1) //@RWS5
                            { //PK75617 starts
                                if (ignoreInvalidQueryString) {
                                    logger.logp(Level.WARNING, CLASS_NAME, "parseName", "invalid.query.string");
                                    return null;
                                } //PK75617 ends
                                throw new IllegalArgumentException(); //@RWS5
                            } //PK75617
                              // c[j++] = (char)(num1*16 + num2);       //@RWS5
                            char newChar = (char) ((num1 << 4) | num2); //@RWS8
                            if (!decodeParamViaReqEncoding && newChar > '\u007F') {
                                if (isKey) {
                                    isKeySingleByteString = false;
                                } else {
                                    isValueSingleByteString = false;
                                }
                            }
                            sb.append(newChar);
                        } else { // allow '%' at end of value or second to last character (as original code does)
                            if (sb != null) {
                                if (offset < endOffset) {
                                    sb.append(queryString, offset, endOffset);
                                }
                            } else {
                                offset = endOffset;
                            }
                        }
                        break;
                    default:
                        if (sb != null) {
                            sb.append(c);
                        }
                        break;
                }
            }
            String returnValue = sb != null ? sb.toString() : queryString.substring(startOffset, endOffset);
            if (printbyteValueandcharParamdata && com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                printValues(returnValue, "parseNameOUT");
            return returnValue;
        }
    }

    private static class CharArrayQueryString extends QueryString {
        private final char[] queryString;
        private final int lgth;

        CharArrayQueryString(char[] queryString) {
            this.queryString = queryString;
            lgth = queryString.length;
        }

        protected boolean lessThanEqualLength() {
            return i <= lgth;
        }

        protected boolean isEqualLength() {
            return i == lgth;
        }

        protected char getNextChar() {
            return queryString[i];
        }

        String getKey() {
            return new String(queryString, pair_start, equalSign - pair_start);
        }

        String getValue() {
            return new String(queryString, equalSign + 1, i - equalSign - 1);
        }

        String parseKey() {
            return parseName(queryString, pair_start, equalSign, true);
        }

        String parseValue() {
            return parseName(queryString, equalSign + 1, i, false);
        }
    }

    private static class CharArrayArrayQueryString extends QueryString {

        private final char[][] queryString;
        private int pair_start_index = 0;
        private int k = 0, equalSign_index = 0;

        CharArrayArrayQueryString(char[][] cha) {
            this.queryString = cha;
        }

        protected boolean lessThanEqualLength() {
            return k < queryString.length && i <= queryString[k].length;
        }

        protected boolean isEqualLength() {
            return k == queryString.length - 1 && i == queryString[queryString.length - 1].length;
        }

        protected char getNextChar() {
            return queryString[k][i];
        }

        @Override
        protected void increment() {
            i++;
            while (k < queryString.length - 1 && i >= queryString[k].length) {
                i = 0;
                k++;
            }
        }

        @Override
        protected void setEqualSign() {
            super.setEqualSign();
            equalSign_index = k;
        }

        @Override
        boolean findNextPair() {
            while (k < queryString.length && i + 1 >= queryString[k].length) {
                queryString[k] = null;
                i = -1;
                k++;
            }

            // release buffers for garbage collection that may not have gotten consumed
            if (pair_start_index < k && queryString[pair_start_index] != null) {
                for (int j = pair_start_index; j < k; ++j) {
                    queryString[j] = null;
                }
            }

            pair_start_index = k;

            return super.findNextPair();
        }

        String getKey() {
            char[] nameChars;
            int nameStart;
            int nameLen;
            if (pair_start_index != equalSign_index) {
                nameChars = getSingleBuffer(queryString, pair_start, pair_start_index, equalSign, equalSign_index);
                nameStart = 0;
                nameLen = nameChars.length;
            } else {
                nameChars = queryString[pair_start_index];
                nameStart = pair_start;
                nameLen = equalSign - pair_start;
            }
            return new String(nameChars, nameStart, nameLen);
        }

        String getValue() {
            char[] valueChars;
            int valueStart;
            int valueLen;
            if (equalSign_index != k) {
                valueChars = getSingleBuffer(queryString, equalSign + 1, equalSign_index, i, k);
                valueStart = 0;
                valueLen = valueChars.length;
            } else {
                valueChars = queryString[k];
                valueStart = equalSign + 1;
                valueLen = i - valueStart;
            }
            return new String(valueChars, valueStart, valueLen);
        }

        String parseKey() {
            char[] nameChars;
            int nameStart;
            int nameEnd;
            if (pair_start_index != equalSign_index) {
                nameChars = getSingleBuffer(queryString, pair_start, pair_start_index, equalSign, equalSign_index);
                nameStart = 0;
                nameEnd = nameChars.length;
            } else {
                nameChars = queryString[pair_start_index];
                nameStart = pair_start;
                nameEnd = equalSign;
            }
            return parseName(nameChars, nameStart, nameEnd, true);
        }

        String parseValue() {
            char[] valueChars;
            int valueStart;
            int valueEnd;
            if (equalSign_index != k) {
                valueChars = getSingleBuffer(queryString, equalSign + 1, equalSign_index, i, k);
                valueStart = 0;
                valueEnd = valueChars.length;
            } else {
                valueChars = queryString[k];
                valueStart = equalSign + 1;
                valueEnd = i;
            }
            return parseName(valueChars, valueStart, valueEnd, false);
        }
    }

    @SuppressWarnings("rawtypes")
    static private Hashtable parseQueryString(QueryString qs, String encoding) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //PK75617
            logger.entering(CLASS_NAME, "parseQueryString( QueryString , encoding --> [" + encoding + "])"); //PM35450.1
        } //PK75617
        int totalSize = 0; //PM53930
        int dupSize = 0; // 728397
        Hashtable<String, String[]> ht = new Hashtable<>();
        HashSet<Integer> key_hset = new HashSet<>(); // 728397
        // PK23256 begin
        boolean encoding_is_ShortEnglish = isShortEnglishEncoding(encoding);
        // PK23256 end

        while (qs.findNextPair()) {

            //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "equalSign =" + equalSign);

            if (qs.hasEquals() || allowQueryParamWithNoEqual) //PM35450 , parameter is blah and not blah=
            { // equal sign found at offset equalSign
                String key = null; //PM92940 Start
                String value = null;

                if (decodeParamViaReqEncoding && (!encoding_is_ShortEnglish)) {

                    // data, start, (end-start),encoding, string
                    key = parse_decode_Parameter(qs.getKey(), encoding, "paramKey");

                    if (!qs.hasEquals()) {
                        value = key != null ? EMPTY_STRING : null;
                    } else {
                        value = parse_decode_Parameter(qs.getValue(), encoding, "paramValue");
                    }

                    if (ignoreInvalidQueryString && ((value == null) || (key == null))) {
                        continue;
                    }

                } else {
                    key = qs.parseKey();
                    //PM35450 Start
                    //String value = null;
                    if (!qs.hasEquals()) {
                        value = key != null ? EMPTY_STRING : null;
                    } else {
                        value = qs.parseValue();
                    }
                    //PM35450 End

                    if (ignoreInvalidQueryString && ((value == null) || (key == null))) { //PK75617
                        continue;
                    } //PK75617
                    if (!encoding_is_ShortEnglish) {
                        try {
                            if (!qs.isKeySingleByteString()) {
                                key = new String(key.getBytes(SHORT_ENGLISH), encoding);
                            }
                            if (!qs.isValueSingleByteString()) {
                                value = new String(value.getBytes(SHORT_ENGLISH), encoding);
                            }
                        } catch (UnsupportedEncodingException uee) {
                            //No need to nls. SHORT_ENGLISH will always be supported
                            logger.logp(Level.SEVERE, CLASS_NAME, "parseQueryString", "unsupported exception", uee);
                            throw new IllegalArgumentException();
                        }
                    }
                } //PM92940 End

                //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "key="+key+", value="+value);
                String valArray[] = new String[] { value };
                String[] oldVals = (String[]) ht.put(key, valArray);
                if (oldVals != null) {
                    valArray = new String[oldVals.length + 1];
                    System.arraycopy(oldVals, 0, valArray, 0, oldVals.length);
                    valArray[oldVals.length] = value;
                    ht.put(key, valArray);
                } else {
                    // 728397 Start 
                    if (!(key_hset.add(key.hashCode()))) {
                        dupSize++;// if false then count as duplicate hashcodes for unique keys
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, CLASS_NAME, "parseQueryString", "duplicate hashCode generated by key --> " + key);
                        }
                        if (dupSize > maxDuplicateHashKeyParams) {
                            logger.logp(Level.SEVERE, CLASS_NAME, "parseQueryString",
                                        MessageFormat.format(nls.getString("Exceeding.maximum.hash.collisions"), new Object[] { maxDuplicateHashKeyParams }));

                            throw new IllegalArgumentException();
                        }
                    } // 728397 End 
                }
            }
            // 724365(PM53930) Start
            if (maxParamPerRequest != -1 && ++totalSize >= maxParamPerRequest) {
                // possibly 10000 big enough, will never be here 
                logger.logp(Level.SEVERE, CLASS_NAME, "parseQueryString",
                            MessageFormat.format(nls.getString("Exceeding.maximum.parameters"), new Object[] { maxParamPerRequest, totalSize }));
                throw new IllegalArgumentException();
            } // 724365 End
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //PK75617
            logger.exiting(CLASS_NAME, "parseQueryString(QueryString, String)");
        } //PK75617
        return ht;
    }

    @SuppressWarnings("rawtypes")
    static public Hashtable parseQueryString(char[][] cha, String encoding) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //PK75617
            logger.entering(CLASS_NAME, "parseQueryString( query , encoding --> [" + encoding + "])"); //PM35450.1
        }                                                                                            //PK75617

        if (cha == null || cha.length == 0) {
            throw new IllegalArgumentException("query string or post data is null");
        }

        // Call optimized version if there is only 1 char[]
        if (cha.length == 1) {
            Hashtable returnValue = parseQueryString(new CharArrayQueryString(cha[0]), encoding);
            cha[0] = null;
            return returnValue;
        }

        return parseQueryString(new CharArrayArrayQueryString(cha), encoding);
    }

   /**
    * Used to retrive the "true" uri that represents the current request.
    * If include request_uri attribute is set, it returns that value.
    * Otherwise, it returns the default of req.getRequestUri
    * @param req
    * @return
    */
   public static String getURIForCurrentDispatch (HttpServletRequest req){
          String includeURI = (String) req.getAttribute(WebAppRequestDispatcher.REQUEST_URI_INCLUDE_ATTR);
          if (includeURI == null)
                 return req.getRequestURI();
          else 
                 return includeURI;
   }
   
   //PM92940 adds this method   
   /**
    * @param data
    * @param start
    * @param length
    * @param encoding
    * @param val
    * @return
    */
   static private String parse_decode_Parameter(String paramData, String encoding, String val) {

       if (printbyteValueandcharParamdata && com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){  
           printValues(paramData, "parsed " +val);                  
       } 
       try {                   
           paramData = URLDecoder.decode(paramData, encoding);
           if (printbyteValueandcharParamdata && com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
               printValues(paramData, "decoded " + val);                        
       }catch ( UnsupportedEncodingException uee ) {
           logger.logp(Level.SEVERE, CLASS_NAME,"parse_decode_Parameter", "unsupported exception--> ", uee);
           throw new IllegalArgumentException();
       }catch ( IllegalArgumentException ie ) {
           if (ignoreInvalidQueryString)                                                                        
           {
               logger.logp(Level.WARNING, CLASS_NAME,"parse_decode_Parameter", "invalid.query.string");
               return null;
           }               
           throw ie;
       }

       return paramData;
   }

  //PM92940 adds this method        
   /**
    * @param value
    * @param loc
    */
   static private void printValues(String value, String loc){
       //for debug printing      
       char[] buffervalue = value.toCharArray();
       byte[] bytevalue = new byte[buffervalue.length];
       for (int ivalue = 0; ivalue < bytevalue.length; ivalue++) {
           bytevalue[ivalue] = (byte) buffervalue[ivalue];
           if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
               logger.logp(Level.FINE, CLASS_NAME,"printValues", ""+loc +" byteValue-->"+ bytevalue[ivalue] +" ,charValue-->" + buffervalue[ivalue]);              
           }
       }
       if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
           logger.logp(Level.FINE, CLASS_NAME,"printValues", ""+loc+" -->[" + value + "]");
       //for printing
   }
   
   
   static private char[] getSingleBuffer(char[][] buffers, int start_pos, int start_index, int end_pos, int end_index) throws  IllegalArgumentException {
       if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){   //PK75617
       logger.entering(CLASS_NAME, "getSingleBuffer", "start_pos="+start_pos+", start_index="+start_index+", end_pos="+end_pos+", end_index="+end_index);
       }                                                                                                                                                                                    //PK75617
       
       long bufflen = buffers[start_index].length - start_pos;
       if (start_index != end_index) {
           for (int i=start_index+1 ; i <= end_index ; i++) {
               if (i==end_index) {
                   bufflen+=end_pos;
               } else {
                   bufflen+=buffers[i].length;
               }
           }
       }
       if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){   //PK75617
       logger.logp(Level.FINE,CLASS_NAME, "getSingleBuffer", "calculated required buffer length ="+bufflen);
       }                                                                                                                                                                                    //PK75617
       
       if (bufflen >= Integer.MAX_VALUE) {
           throw new IllegalArgumentException();
       }
                       
       char[] buff = new char[(int)bufflen];
       
       if (start_index != end_index) { 
           
           int bufferPos=0;
           for (int count = start_index ; count <= end_index ; count++ ) {

               // pairs spans arrays so create a single array containing the name
               if (count == start_index) {
                   if (start_pos < buffers[start_index].length) {
                       // copy everything left in the first buffer
                       if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){   //PK75617
                           logger.logp(Level.FINE,CLASS_NAME, "getSingleBuffer", "copy "+ (buffers[start_index].length - start_pos) + " bytes from buffer " + count);
                       }                                                                                                                                                                                    //PK75617
                       System.arraycopy(buffers[start_index], start_pos, buff, bufferPos, buffers[start_index].length - start_pos);
                   } 
                    bufferPos = buffers[start_index].length - start_pos;
                    // release the original buffer for garbage collection
                    buffers[count]=null;
               } else if (count == end_index) {
                   // add up to the equal sign in the last buffer
                    long len = bufferPos + end_pos;
                    if (len > Integer.MAX_VALUE)
                           throw new IllegalArgumentException();
                    else {
                           if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){   //PK75617
                               logger.logp(Level.FINE,CLASS_NAME, "getSingleBuffer", "copy "+ end_pos + " bytes from buffer " + count);
                           }                                                                                                                                                                                    //PK75617
                           System.arraycopy(buffers[count], 0, buff, bufferPos, end_pos);  
                    }    
               } else {
                   // add the entire intermediate buffer
                   long len = bufferPos + buffers[count].length;
                   if (len > Integer.MAX_VALUE)
                        throw new IllegalArgumentException();
                   else { 
                       if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){   //PK75617
                           logger.logp(Level.FINE,CLASS_NAME, "getSingleBuffer", "copy "+ buffers[count].length + " bytes from buffer " + count);
                       }                                                                                                                                                                                    //PK75617
                       System.arraycopy(buffers[count], 0, buff, bufferPos, buffers[count].length);
                       bufferPos+=buffers[count].length;
                       // release the original buffer for garbage collection
                       buffers[count]=null;
                   }
               }    
           }
            
       } else {
           System.arraycopy(buffers[start_index], start_pos, buff, 0, buffers[start_index].length - start_pos);
       }
       if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){   //PK75617
       logger.exiting(CLASS_NAME, "getSingleBuffer");
       }                                                                                                                                                                                    //PK75617       
       return buff;
   }   
   
}
