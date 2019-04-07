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
package com.ibm.ws.webcontainer.servlet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.util.Hashtable;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.wsspi.webcontainer.util.EncodingUtils;

public class RequestUtils extends com.ibm.wsspi.webcontainer.util.RequestUtils
{
    private static final String SYSTEM_CLIENT_ENCODING;
    private static final String SYSTEM_FILE_ENCODING;
    public static final String SYS_PROP_FILE_ENCODING = "file.encoding";
    public static final String SYS_PROP_DFLT_CLIENT_ENCODING = "default.client.encoding";
    private static final TraceNLS nls = TraceNLS.getTraceNLS(RequestUtils.class, "com.ibm.ws.webcontainer.resources.Messages");
    private static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.servlet");
    private static final String CLASS_NAME="com.ibm.ws.webcontainer.servlet.RequestUtils";

    static
    {
        // get the system file encoding
        SYSTEM_FILE_ENCODING = AccessController.doPrivileged(new java.security.PrivilegedAction<String>()
                                                                     {
                                                                         public String run()
                                                                         {
                                                                             return(System.getProperty(SYS_PROP_FILE_ENCODING));
                                                                         }
                                                                     });

        // setup the override for client encoding...if specified, this property will be used as the client encoding.
        SYSTEM_CLIENT_ENCODING = AccessController.doPrivileged(new java.security.PrivilegedAction<String>()
                                                                     {
                                                                         public String run()
                                                                         {
                                                                             return(System.getProperty(SYS_PROP_DFLT_CLIENT_ENCODING));
                                                                         }
                                                                     });
    }

    public static final String ACCEPT_CHARSET_HEADER = "Accept-Charset";
    public static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    public static final String DEFAULT_CLIENT_ENCODING = "iso-8859-1";
    public static final String HEADER_SEPARATOR = ",";
    public static final String LANG_CHINESE_S = "zh-cn";
    public static final String LANG_CHINESE = "zh";
    public static final String LANG_CHINESE_T = "zh-tw";
    public static final String LANG_EN = "en";
    public static final String LANG_KOREA = "ko";
    public static final String LANG_JAPAN1 = "ja";
    public static final String LANG_JAPAN2 = "jp";
    public static final String ENC_CHINESE_S = "Cp1381";
    public static final String ENC_CHINESE_T = "Cp950";
    public static final String ENC_ENGLISH = "iso-8859-1";
    public static final String ENC_KOREA = "KSC5601";
    public static final String ENC_JAPAN = "Shift_JIS";
    private static final String SHORT_ASCII = "8859"; //a shortened ASCII encoding
    private static final String ACCEPT_ALL_VAL = "*";
    //Defect 44789 - JTG:  Add EBCDIC code pages
    private static final String CP1047_ENCODING = "Cp1047";
    private static final String ISO8859_1_ENCODING = "8859_1";
    private static final String CP939_ENCODING = "Cp939";
    private static final String SJIS_ENCODING = "SJIS";
    
    private static final int DEFAULT_BUFFER_SIZE = 2 * 1024;
    
    public static String getClientEncoding(HttpServletRequest req)
    {
        String encoding = null;
        if (SYSTEM_CLIENT_ENCODING != null)
        {
            encoding = SYSTEM_CLIENT_ENCODING;
            return encoding;
        }
        String header = req.getHeader(ACCEPT_CHARSET_HEADER);
        if (header != null)
        {
        	if (header.indexOf(SHORT_ASCII)!=-1)
        		encoding = getLanguage(req);
        	else{
	            StringTokenizer st = new StringTokenizer(header, HEADER_SEPARATOR);
	            encoding = st.nextToken().trim();
	            if (encoding.equals(ACCEPT_ALL_VAL))
	            {
	                encoding = DEFAULT_CLIENT_ENCODING;
	            }
	            if (!EncodingUtils.isCharsetSupported(encoding)){
	        		encoding = getLanguage(req);
	        	}
        	}
        }
        else
        {
            encoding = getLanguage(req);
            /**
             * at this point it's guaranteed to be ok,
             * since we create a bytetocharconverted with it
             * so just return it
             */
        }
        return encoding;
    }
    //begin PQ77356
    public static String getEbcdicEncodingIfZos()
    {
        String osName = System.getProperty("os.name");
        String encoding = null;
        
        if ( osName.equalsIgnoreCase("z/OS") || osName.equalsIgnoreCase("OS/390") ) {
            String lang = Locale.getDefault().getLanguage();
            if ( lang.startsWith(LANG_EN) ) 
                encoding = CP1047_ENCODING;
            else if ( lang.equals(LANG_JAPAN1) || lang.equals(LANG_JAPAN2) ) 
                encoding = SJIS_ENCODING;
        }
        return encoding;
    }
    //end PQ77356
    public static String getEncodingFromLanguage(String lang)
    {
        if (lang.startsWith(LANG_EN))
        { //English
            return ENC_ENGLISH;
        }
        else if (lang.equals(LANG_JAPAN1) || lang.equals(LANG_JAPAN2))
        { //Japanese
            return ENC_JAPAN;
        }
        else if (lang.equals(LANG_CHINESE_T))
        { //T-Chinese
            return ENC_CHINESE_T;
        }
        else if (lang.equals(LANG_KOREA))
        { //Korea
            return ENC_KOREA;
        }
        else if (lang.equals(LANG_CHINESE_S) || lang.equals(LANG_CHINESE))
        { //S-Chinese
            return ENC_CHINESE_S;
        }
        else
        {
            return ENC_ENGLISH;
        }
    }
    public static String getLanguage(HttpServletRequest req)
    {
        String header;
        String encoding = null;
        header = req.getHeader(ACCEPT_LANGUAGE_HEADER);
        if (header != null)
        {
            StringTokenizer st = new StringTokenizer(header, HEADER_SEPARATOR);
            String lang = st.nextToken().trim().toLowerCase();
            encoding = getEncodingFromLanguage(lang);
        }
        else
        {
            // 115780 - begin - check the static default file encoding...make sure it isn't null
            /* Defect 44789 - JTG:
               Add checks for EBDCIC English and Japanese.  If server code is
               running on OS/390, default encoding detected is English ASCII.
               Since all clients are ASCII, we must change the EBCDIC encoding
               that is detected to be ASCII, so that the response data is built
               in the client's (ASCII) code page.
            */
            if (SYSTEM_FILE_ENCODING != null)
            {
                if (SYSTEM_FILE_ENCODING.equals(CP1047_ENCODING))
                {
                    encoding = ISO8859_1_ENCODING;
                }
                else if (SYSTEM_FILE_ENCODING.equals(CP939_ENCODING))
                {
                    encoding = SJIS_ENCODING;
                }
                else
                {
                    encoding = DEFAULT_CLIENT_ENCODING;
                }
            }
            else
            {
                encoding = DEFAULT_CLIENT_ENCODING;
            }
            // 115780 - end
        }
       if (!EncodingUtils.isCharsetSupported(encoding)){
       		if (SYSTEM_FILE_ENCODING == null)
            {
                //default to English
                encoding = DEFAULT_CLIENT_ENCODING;
            }
            else
            {
                encoding = SYSTEM_FILE_ENCODING;
            }
            // 115780 - end
        }
        return encoding;
    }
       
    private static String getPostBody(int len, ServletInputStream in, String encoding) /* 157338 add throws */ throws IOException
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE,CLASS_NAME,"getPostBody","len = " + len, ", encoding = " + encoding);
        
        int inputLen, offset;
        byte[] postedBytes = null;
        String postedBody;
        if (len <= 0)
            return null;
        if (in == null)
            throw new IllegalArgumentException("post data inputstream is null");
        try
        {
            //
            // Make sure we read the entire POSTed body.
            //
            postedBytes = new byte[len];
            offset = 0;
            do
            {
                inputLen = in.read(postedBytes, offset, len - offset);
                if (inputLen <= 0)
                {
                    String msg = nls.getString("post.body.contains.less.bytes.than.specified", "post body contains less bytes than specified by content-length");
                    throw new IOException(msg);
                }
                logger.logp(Level.FINE, CLASS_NAME, "getPostBody","read of " +  inputLen + " bytes.");
               offset += inputLen;
            }
            while ((len - offset) > 0);            
            
        }
        catch (IOException e)
        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.RequestUtils.parsePostData", "398");
            // begin 157338
            throw e;
            //return new Hashtable();
            // begin 157338
        }
        // XXX we shouldn't assume that the only kind of POST body
        // is FORM data encoded using ASCII or ISO Latin/1 ... or
        // that the body should always be treated as FORM data.
        //
        try
        {
            postedBody = new String(postedBytes, encoding);
        }
        catch (UnsupportedEncodingException e)
        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.RequestUtils.parsePostData", "411");
            postedBody = new String(postedBytes);
        }
        
       return postedBody;
    }
    
    @SuppressWarnings("rawtypes")
    public static Hashtable parsePostData(int len, ServletInputStream in, String encoding, boolean multireadPropertyEnabled) /* 157338 add throws */ throws IOException // MultiRead
    {    
        String postedBody = getPostBody(len, in, encoding);

        // MultiRead Start
        if (multireadPropertyEnabled) {
            in.close();
        }
        // MultiRead End

        return parseQueryString(postedBody, encoding);
    }
    
    @SuppressWarnings("rawtypes")
    public static Hashtable parsePostDataLong(long len, ServletInputStream in, String encoding, boolean multireadPropertyEnabled) throws IOException // MultiRead
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE,CLASS_NAME,"parsePostDataLong","len = " + len, ", encoding = " + encoding);
        
        int MaxBufferSize = WCCustomProperties.SERVLET31_PRIVATE_BUFFERSIZE_FOR_LARGE_POST_DATA;
        
        if (len<=MaxBufferSize) {
            return parsePostData((int)len,in,encoding, multireadPropertyEnabled); // MultiRead
        } 
        
        long remaining = len;
        
        long arraySize = len/MaxBufferSize;
        if (len%MaxBufferSize>0) arraySize++;
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE,CLASS_NAME,"parsePostDataLong","buffer size in use = " + MaxBufferSize + ", arraySize required = " + Long.toString(arraySize));
        
        if(arraySize > Integer.MAX_VALUE) {
             throw new IllegalArgumentException();   
        }
        char[][] paramData = new char[(int)arraySize][];
        int readLen = MaxBufferSize;
        String data=null;
        int index = 0;
        while (remaining>0) {
            
            // Check if we are near the end of the data
            if (remaining>MaxBufferSize) {
                readLen = MaxBufferSize;
            } else {
                readLen = (int)remaining;
            }    
            
            data = getPostBody(readLen,in,encoding);
            
            paramData[index] = data.toCharArray(); 
            index++;
            remaining-=readLen;
        }

        // MultiRead Start
        if (multireadPropertyEnabled) {
            in.close();
        }
        // MultiRead End
        
        return parseQueryString(paramData, encoding);
    }
    
    // begin 231634    Support posts with query parms in chunked body    WAS.webcontainer    
    @SuppressWarnings("rawtypes")
    public static Hashtable parsePostData(ServletInputStream in, String encoding, boolean multireadPropertyEnabled) /* 157338 add throws */ throws IOException
    {
        int inputLen;
        byte[] postedBytes = null;
        String postedBody;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"parsePostData","parsing chunked post data. encoding = " + encoding);
		
        
        if (in == null)
            throw new IllegalArgumentException("post data inputstream is null");
        try
        {
            //
            // Make sure we read the entire POSTed body.
            //
            ByteArrayOutputStream byteOS = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
            do
            {
            	byte [] readInBytes = new byte[DEFAULT_BUFFER_SIZE];
            	inputLen = in.read(readInBytes, 0, DEFAULT_BUFFER_SIZE);
            	if (inputLen > 0){
            		byteOS.write(readInBytes,0,inputLen);
            	}
            }
            while (inputLen != -1);
            
            // MultiRead Start
            if (multireadPropertyEnabled) {
                in.close();
            }
            // MultiRead End
            
            postedBytes = byteOS.toByteArray();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
    			logger.logp(Level.FINE, CLASS_NAME,"parsePostData","finished reading ["+postedBytes.length+"] bytes");
    		
        }
        catch (IOException e)
        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.RequestUtils.parsePostData", "598");
            // begin 157338
            throw e;
            //return new Hashtable();
            // begin 157338
        }
        // XXX we shouldn't assume that the only kind of POST body
        // is FORM data encoded using ASCII or ISO Latin/1 ... or
        // that the body should always be treated as FORM data.
        //
        try
        {
            postedBody = new String(postedBytes, encoding);
        }
        catch (UnsupportedEncodingException e)
        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.servlet.RequestUtils.parsePostData", "618");
            postedBody = new String(postedBytes);
        }
        
        if (WCCustomProperties.PARSE_UTF8_POST_DATA && encoding.equalsIgnoreCase("UTF-8")) {
            for (byte nextByte : postedBytes) {
            	if (nextByte < (byte)0 ) {
            		encoding = "8859_1";            		
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            			logger.logp(Level.FINE, CLASS_NAME,"parsePostData","UTF8 post data, set encoing to 8859_1 to prevent futrther encoding");
        	        break;
            	}    	
            }
        }    

        
        return parseQueryString(postedBody, encoding);
    }
    // end 231634    Support posts with query parms in chunked body    WAS.webcontainer    


    
    
    
}
