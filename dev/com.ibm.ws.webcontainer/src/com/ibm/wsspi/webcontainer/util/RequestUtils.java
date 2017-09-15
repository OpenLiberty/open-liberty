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
	protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.util");
	private static final String CLASS_NAME="com.ibm.wsspi.webcontainer.util.RequestUtils";
	private static TraceNLS nls = TraceNLS.getTraceNLS(RequestUtils.class, "com.ibm.ws.webcontainer.resources.Messages"); //724365.4

	private static final String SHORT_ENGLISH = "8859_1"; //a shortened ASCII encoding
	
	private static boolean ignoreInvalidQueryString = WCCustomProperties.IGNORE_INVALID_QUERY_STRING;       //PK75617
    private static boolean allowQueryParamWithNoEqual = WCCustomProperties.ALLOW_QUERY_PARAM_WITH_NO_EQUAL;       //PM35450
    private static final String EMPTY_STRING = ""; //PM35450
    private static int maxParamPerRequest = WCCustomProperties.MAX_PARAM_PER_REQUEST; // PM53930 (724365)
    private static final int maxDuplicateHashKeyParams = WCCustomProperties.MAX_DUPLICATE_HASHKEY_PARAMS; // PM58495 (728397)
    private static boolean decodeParamViaReqEncoding = WCCustomProperties.DECODE_PARAM_VIA_REQ_ENCODING; // PM92940
    private static boolean printbyteValueandcharParamdata = WCCustomProperties.PRINT_BYTEVALUE_AND_CHARPARAMDATA; //PM92940


    
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
    @SuppressWarnings("unchecked")
   static public Hashtable parseQueryString(String s)
   {
        return parseQueryString(s, SHORT_ENGLISH);
   }
    
   @SuppressWarnings("unchecked")
   static public Hashtable parseQueryString(String s,String encoding)
   {
        char[][] cha = new char[1][];
        cha[0]=s.toCharArray();
        return parseQueryString(cha, encoding);
   }
      
   @SuppressWarnings("unchecked") 
   static public Hashtable parseQueryString(char[][] cha, String encoding)
   {
       if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){	//PK75617
           logger.entering(CLASS_NAME, "parseQueryString( query , encoding --> [" +  encoding +"])"); //PM35450.1
       }																							//PK75617
       String valArray[] = null;
       int totalSize = 0; //PM53930
       int dupSize = 0; // 728397
       if (cha == null || cha.length==0)
       {
           throw new IllegalArgumentException("query string or post data is null");
       }
       Hashtable ht = new Hashtable();
       HashSet<Integer> key_hset = new HashSet<Integer>(); // 728397
       // @MD17415 Start 1 of 3: New Loop for finding key and value           @RWS1
       char [] ch;
       int lgth;
       boolean encoding_is_ShortEnglish = true;                  // @RWS9
       // PK23256 begin
       /*if (encoding.indexOf("8859-1")==-1 && encoding.indexOf(SHORT_ENGLISH)==-1  ) // @RWS9
            encoding_is_ShortEnglish = false;                  // @RWS9*/

       if (!encoding.endsWith("8859_1")   
                       && !encoding.endsWith("8859-1") 
                       && (encoding.indexOf("8859-1-Windows") == -1) 
                       ) { 
           encoding_is_ShortEnglish = false; 
       } 


       // PK23256 end
       int pair_start=0,pair_start_index=0,pair_end=0,pair_end_index=0;
       int i = 0, j=0, k=0, equalSign=0,equalSign_index=0;

       for (k=0; k < cha.length ; k++) {

           //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "process buffer " + k + " : " + Arrays.toString(cha[k]) );


           lgth = cha[k].length;

           for (i=0; i<lgth; i++) {
               if (cha[k][i] == '&') {

                   //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "found an &  - pair start =" + pair_start + ", pair_start_index="+pair_start_index);

                   boolean equalFound=false;
                   for (int count = pair_start_index ; count<= k; count++) {                  
                       int start = 0, end = cha[k].length;
                       equalSign_index=count;
                       if (count == pair_start_index) start=pair_start;
                       if (count==k) end=i;
                       for (equalSign=start; equalSign<end; equalSign++) {
                           if (cha[count][equalSign] == '=') {
                               equalFound=true;
                               break;
                           }
                       } 
                       if (equalFound) break;
                   }

                   //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "equalSign =" + equalSign + ", equalSign_index="+equalSign_index);

                   if ((equalSign < i || equalSign_index < k) || (allowQueryParamWithNoEqual && equalSign == i && equalSign_index ==k )) //PM35450 , parameter is blah and not blah=
                   {   // equal sign found at offset equalSign
                       String key = null; //PM92940 Start
                       String value = null;

                       char[] nameChars,valueChars;
                       int nameStart=0,nameLen=0;
                       int valueStart=0,valueLen=0;

                       if (pair_start_index != equalSign_index) { 
                           nameChars = getSingleBuffer(cha,pair_start,pair_start_index,equalSign, equalSign_index);
                           nameStart = 0;
                           nameLen = nameChars.length;
                       } else {
                           nameChars=cha[pair_start_index];
                           nameStart = pair_start;
                           nameLen = equalSign-pair_start;
                       }

                       //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "nameStart = " + nameStart + ", nameLen = " + nameLen + ", from buffer :" + Arrays.toString(nameChars));

                       if (equalSign_index != k) { 
                           valueChars = getSingleBuffer(cha,equalSign+1,equalSign_index,i,k);
                           valueStart = 0;
                           valueLen = valueChars.length;
                       } else {
                           valueChars=cha[k];
                           valueStart = equalSign+1;
                           valueLen = i-valueStart;
                       }

                       //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "valueStart = " + valueStart + ", valueLen = " + valueLen + ", from buffer :" + Arrays.toString(valueChars));

                       if(decodeParamViaReqEncoding && (!encoding_is_ShortEnglish)){

                           // data, start, (end-start),encoding, string
                           key = parse_decode_Parameter(nameChars,nameStart,nameLen, encoding,"paramKey");

                           if (equalSign == i && equalSign_index == k){
                               if(key != null) 
                                   value = EMPTY_STRING;
                               else value = null;
                           }
                           else {


                               value = parse_decode_Parameter(valueChars,valueStart,valueLen,encoding ,"paramValue");
                           }

                           valueChars=null;
                           nameChars=null;

                           if (ignoreInvalidQueryString && ((value == null) || (key ==null))){             
                               pair_start = i+1;
                               pair_start_index = k;
                               continue;
                           }

                       }
                       else{
                           key = parseName(nameChars,nameStart,nameStart+nameLen);
                           //PM35450 Start
                           //String value = null;
                           if (equalSign == i && equalSign_index == k){
                               if(key != null) value = EMPTY_STRING;
                               else value = null;
                           }
                           else value = parseName(valueChars,valueStart,valueStart+valueLen);
                           //PM35450 End

                           valueChars=null;
                           nameChars=null;

                           if (ignoreInvalidQueryString && ((value == null) || (key ==null))){      //PK75617
                               pair_start = i +1;
                               pair_start_index = k;
                               continue;
                           }                                                                                                                                                //PK75617
                           if ( !encoding_is_ShortEnglish) {
                               try {
                                   key = new String(key.getBytes(SHORT_ENGLISH),encoding);
                                   value = new String(value.getBytes(SHORT_ENGLISH),encoding);
                               } catch ( UnsupportedEncodingException uee ) {
                                   //No need to nls. SHORT_ENGLISH will always be supported
                                   logger.logp(Level.SEVERE, CLASS_NAME,"parseQueryString", "unsupported exception", uee);
                                   throw new IllegalArgumentException();
                               }
                           }
                       }//PM92940 End

                       //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "key="+key+", value="+value);

                       if (ht.containsKey(key)) {
                           String oldVals[] = (String []) ht.get(key);
                           valArray = new String[oldVals.length + 1];
                           for (j = 0; j < oldVals.length; j++)
                               valArray[j] = oldVals[j];
                           valArray[oldVals.length] = value;
                       } else {
                           // 728397 Start                        
                           if(!(key_hset.add(key.hashCode()))){ 
                               dupSize++;// if false then count as duplicate hashcodes for unique keys
                               if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                                   logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "duplicate hashCode generated by key --> " + key);
                               }
                               if( dupSize > maxDuplicateHashKeyParams){							 
                                   logger.logp(Level.SEVERE, CLASS_NAME,"parseQueryString", MessageFormat.format(nls.getString("Exceeding.maximum.hash.collisions"), new Object[]{maxDuplicateHashKeyParams}));

                                   throw new IllegalArgumentException();
                               }
                           } // 728397 End 
                           valArray = new String[1];
                           valArray[0] = value;
                       }
                       // 724365(PM53930) Start
                       totalSize++;
                       if((maxParamPerRequest == -1) || ( totalSize < maxParamPerRequest)){
                           ht.put(key, valArray);
                       }
                       else{
                           // possibly 10000 big enough, will never be here 
                           logger.logp(Level.SEVERE, CLASS_NAME,"parseQueryString", MessageFormat.format(nls.getString("Exceeding.maximum.parameters"), new Object[]{maxParamPerRequest, totalSize}));

                           throw new IllegalArgumentException();
                       }// 724365 End
                   }
                   pair_start = i+1;
                   pair_start_index = k;
                   //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "new pair start =" + pair_start + ", pair_start_index="+pair_start_index);
               }
               pair_end = i;
               pair_end_index = k;
               //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "new pair end =" + pair_end + ", pair_end_index="+pair_end_index);
           }
       }

       boolean equalFound=false;

       //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "k=" + k + ", pair_start_index="+pair_start_index+", cha[pair_start_index].length="+cha[pair_start_index].length);
       //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "pair_start =" +pair_start+ " ,cha[pair_start_index] = "+ Arrays.toString(cha[pair_start_index]));
       for (int count = pair_start_index ; count < cha.length; count++) {  
           //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "loop count=" + count);
           int start = 0, end = cha[count].length;
           if (count == pair_start_index) start=pair_start;
           equalSign_index=count;
           for (equalSign=start; equalSign<end; equalSign++) {
               //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "equalSign="+equalSign+", cha[count][equalSign]=" + cha[count][equalSign]);
               if (cha[count][equalSign] == '=') {
                   equalSign_index=count;
                   equalFound=true;
                   break;
               }
           } 
           if (equalFound) break;
       }


       //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "pair_start="+pair_start+", pair_start_index="+pair_start_index+", pair_end="+pair_end+", pair_end_index="+pair_end_index+", equal_sign="+equalSign+", equalSign_Index="+equalSign_index);

       if (pair_start < equalSign || pair_start_index < equalSign_index) { 
           if ((equalSign <= pair_end || equalSign_index < pair_end_index) || (allowQueryParamWithNoEqual && equalSign > pair_end && equalSign_index == pair_end_index )) //PM35450 , parameter is blah and not blah=
           {   // equal sign found at offset equalSign
               String key = null; //PM92940 Start
               String value = null;
               char[] nameChars,valueChars;
               int nameStart=0,nameLen=0;
               int valueStart=0,valueLen=0;

               if (pair_start_index != equalSign_index) { 
                   nameChars = getSingleBuffer(cha,pair_start,pair_start_index,equalSign, equalSign_index);
                   nameStart = 0;
                   nameLen = nameChars.length;
               } else {
                   nameChars=cha[pair_start_index];
                   nameStart = pair_start;
                   nameLen = equalSign-pair_start;
               }

               if (equalSign_index != pair_end_index) { 
                   valueChars = getSingleBuffer(cha,equalSign+1,equalSign_index,pair_end+1,pair_end_index);
                   valueStart = 0;
                   valueLen = valueChars.length;
               } else {
                   valueChars=cha[pair_end_index];
                   valueStart = equalSign+1;
                   valueLen = pair_end-equalSign;
               }

               if(decodeParamViaReqEncoding && (!encoding_is_ShortEnglish)){

                   key = parse_decode_Parameter(nameChars,nameStart,nameLen, encoding,"paramKey");
                   if (equalSign > pair_end && equalSign_index == pair_end_index){
                       if(key != null) 
                           value = EMPTY_STRING;
                       else value = null;
                   }
                   else
                       value = parse_decode_Parameter(valueChars,valueStart,valueLen,encoding ,"paramValue");

                   valueChars=null;
                   nameChars=null;

                   if (ignoreInvalidQueryString && ((value == null) || (key ==null))){                                             
                       if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){      
                           logger.exiting(CLASS_NAME, "parseQueryString(String, String)");
                       }
                       return ht;
                   }          
               }
               else{
                   key = parseName(nameChars,nameStart,nameStart+nameLen);
                   //PM35450 Start
                   //String value = null;
                   if (equalSign > pair_end && equalSign_index == pair_end_index){
                       if(key != null) value = EMPTY_STRING;
                       else value = null;
                   }
                   else value = parseName(valueChars,valueStart,valueStart+valueLen);
                   //PM35450 End

                   valueChars=null;
                   nameChars=null;

                   if (ignoreInvalidQueryString && ((value == null) || (key ==null))){                                      //PK75617 - start
                       if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){   
                           logger.exiting(CLASS_NAME, "parseQueryString(String, String)");
                       }                                                                                                                                                                            
                       return ht;
                   }                                                                                                                                                                                //PK75617 -end
                   if ( !encoding_is_ShortEnglish ) {
                       try {
                           key = new String(key.getBytes(SHORT_ENGLISH),encoding);
                           value = new String(value.getBytes(SHORT_ENGLISH),encoding);
                       } catch ( UnsupportedEncodingException uee ) {
                           logger.logp(Level.SEVERE, CLASS_NAME,"parseQueryString", "unsupported exception", uee);
                           throw new IllegalArgumentException();
                       }
                   }
               } //PM92940 End

               //logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "key="+key+", value="+value);

               if (ht.containsKey(key)) {
                   String oldVals[] = (String []) ht.get(key);
                   valArray = new String[oldVals.length + 1];
                   for (j = 0; j < oldVals.length; j++)
                       valArray[j] = oldVals[j];
                   valArray[oldVals.length] = value;
               } else {
                   // 728397 Start               
                   if(!(key_hset.add(key.hashCode()))){
                       dupSize++;	// if false then count as duplicate hashcodes for unique keys
                       if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                           logger.logp(Level.FINE, CLASS_NAME,"parseQueryString", "duplicate hashCode generated by key --> " + key);
                       }
                       if( dupSize > maxDuplicateHashKeyParams){							 
                           logger.logp(Level.SEVERE, CLASS_NAME,"parseQueryString", MessageFormat.format(nls.getString("Exceeding.maximum.hash.collisions"), new Object[]{maxDuplicateHashKeyParams}));

                           throw new IllegalArgumentException();
                       }
                   } // 728397 End 
                   valArray = new String[1];
                   valArray[0] = value;
               }
               // 724365(PM53930) Start
               totalSize++;
               if((maxParamPerRequest == -1) || ( totalSize < maxParamPerRequest)){
                   ht.put(key, valArray);
               }
               else{
                   // possibly 10000 big enough, will never be here 
                   logger.logp(Level.SEVERE, CLASS_NAME,"parseQueryString", MessageFormat.format(nls.getString("Exceeding.maximum.parameters"), new Object[]{maxParamPerRequest, totalSize}));

                   throw new IllegalArgumentException();
               }// 724365 End
           }
       }
       // @MD17415 End 1 of 3: New Loop for finding key and value           @RWS1

//@MD17415 begin part 2 of 3 New Loop for finding key and value
//@MD17415 
//@MD17415         StringBuffer sb = new StringBuffer();
//@MD17415         StringTokenizer st = new StringTokenizer(s, "&");
//@MD17415         while (st.hasMoreTokens())
//@MD17415         {
//@MD17415             String pair = (String) st.nextToken();
//@MD17415             int pos = pair.indexOf('=');
//@MD17415             if (pos == -1)
//@MD17415             {
//@MD17415                 // XXX
//@MD17415                 // should give more detail about the illegal argument
//@MD17415                 // ignore invalid parameter value (eg) http://localhost/servlet/snoop?name&age=9 (ignore name)
//@MD17415                 //don't throw new IllegalArgumentException();
//@MD17415             }
//@MD17415             else
//@MD17415             {
//@MD17415                 String key = parseName(pair.substring(0, pos), sb);
//@MD17415                 String val = parseName(pair.substring(pos + 1, pair.length()), sb);
//@MD17415                 /**
//@MD17415                  * ajg
//@MD17415                  * convert post data to right format
//@MD17415                  * skip if client is ASCII (english)
//@MD17415                 *
//@MD17415                 * Performance enhancement:
//@MD17415                 *   added 1st condition, if !encoding.equals("ISO-8859-1"), to avoid
//@MD17415                 *   the costly conversion.
//@MD17415                 *   (Keith Smith)
//@MD17415                  */
//@MD17415                 if ((!encoding.equals("ISO-8859-1")) && (encoding.indexOf(SHORT_ENGLISH) == -1))
//@MD17415                 {
//@MD17415                     try
//@MD17415                     {
//@MD17415                         key = new String(key.getBytes(SHORT_ENGLISH), encoding);
//@MD17415                         val = new String(val.getBytes(SHORT_ENGLISH), encoding);
//@MD17415                     }
//@MD17415                     catch (UnsupportedEncodingException uee)
//@MD17415                     {
//      @MD17415                         com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(uee, "com.ibm.ws.webcontainer.servlet.RequestUtils.parseQueryString", "289");

//@MD17415                         throw new IllegalArgumentException();
//@MD17415                     }
//@MD17415                 }
//@MD17415                 if (ht.containsKey(key))
//@MD17415                 {
//@MD17415                     String oldVals[] = (String[]) ht.get(key);
//@MD17415                     valArray = new String[oldVals.length + 1];
//@MD17415                     for (int i = 0; i < oldVals.length; i++)
//@MD17415                         valArray[i] = oldVals[i];
//@MD17415                     valArray[oldVals.length] = val;
//@MD17415                 }
//@MD17415                 else
//@MD17415                 {
//@MD17415                     valArray = new String[1];
//@MD17415                     valArray[0] = val;
//@MD17415                 }
//@MD17415                 ht.put(key, valArray);
//@MD17415             }
//@MD17415         }
//@MD17415 end part 2 of 3 New Loop for finding key and value
       if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){		//PK75617
           logger.exiting(CLASS_NAME, "parseQueryString(String, String)");
       }																								//PK75617
       return ht;
   }
   /*
        * Parse a name in the query string.
        */
   // @MD17415 begin part 3 of 3 New Loop for finding key and value
   static private String parseName(char [] ch, int startOffset, int endOffset) {
       int j = 0;
       int startOffsetLocal = startOffset;   // local variable  -  @RWS2 
       int endOffsetLocal = endOffset;       // local variable  -  @RWS2 
       char [] chLocal = ch;                 // local variable  -  @RWS7
       char [] c = new char [endOffsetLocal-startOffsetLocal];     // @RWS2
       for (int i = startOffsetLocal; i < endOffsetLocal; i++) {  // @RWS2
           switch (chLocal[i]) {                                   // @RWS7
           case '+' :
               c[j++] = ' ';
               break;
           case '%' :
               if (i+2 < endOffsetLocal) {   // @RWS2
                   int num1 = Character.digit(chLocal[++i],16);   //@RWS7
                   int num2 = Character.digit(chLocal[++i],16);   //@RWS7
                   if (num1 == -1 || num2 == -1)             //@RWS5
                   {																	//PK75617 starts
                	   if (ignoreInvalidQueryString)									
                	   {
                		   logger.logp(Level.WARNING, CLASS_NAME,"parseName", "invalid.query.string");
                		   return null;
                	   }																//PK75617 ends
                       throw new IllegalArgumentException(); //@RWS5
                   }																	//PK75617
                   // c[j++] = (char)(num1*16 + num2);       //@RWS5
                   c[j++] = (char)((num1<<4) | num2);       //@RWS8
               } else {   // allow '%' at end of value or second to last character (as original code does)
                   for (i=i; i<endOffsetLocal; i++)   // @RWS2
                       c[j++] = chLocal[i];           // @RWS7
               }
               break;
           default :
               c[j++] = chLocal[i];
               break;
           } 
       } 
       if (printbyteValueandcharParamdata && com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
           printValues(new String(c,0,j), "parseNameOUT");
       return new String(c,0,j);
   }
   // @MD17415 end part 3 of 3 New Loop for finding key and value
   
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
   static private String parse_decode_Parameter(char[] data, int start, int length, String encoding, String val) {

       String paramData = null;     
       paramData = new String(data, start, length);
       
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
