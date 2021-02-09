/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.srt;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TruncatableThrowable;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.genericbnf.PasswordNullifier;
import com.ibm.ws.http.channel.outstream.HttpOutputStreamConnectWeb;
import com.ibm.ws.http.channel.outstream.HttpOutputStreamObserver;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.ws.webcontainer.core.Response;
import com.ibm.ws.webcontainer.servlet.IServletWrapperInternal;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.WebContainerConstants;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.servlet.IOutputMethodListener;
import com.ibm.wsspi.webcontainer.servlet.IServletResponse;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.util.BufferedWriter;
import com.ibm.wsspi.webcontainer.util.EncodingUtils;
import com.ibm.wsspi.webcontainer.util.IOutputStreamObserver;
import com.ibm.wsspi.webcontainer.util.IResponseOutput;
import com.ibm.wsspi.webcontainer.util.WrappingEnumeration;
import com.ibm.ws.webcontainer.osgi.response.WCOutputStream;
/**
 * The Servlet Runtime Response object
 * 
 * This class handles response object functions that involve the input and output streams. This class
 * contains no WebApp level information, and should not be hacked to include any. A
 * WebAppDispatcherResponse object will proxy this response and handle method calls that need 
 * path or webapp information. 
 * 
 * @author The Unknown Programmer
 * 
 *         104771 - 6/18/01 - DPJ
 */
public class SRTServletResponse implements HttpServletResponse, IResponseOutput, IExtendedResponse, IServletResponse, Response, IOutputStreamObserver, HttpOutputStreamObserver  {
    private static boolean skipInputStreamRead = false;
    private boolean isCharEncodingExplicit;
    private static final String REASON_OK = "OK";
    private static final String CONTENT_LANGUAGE_HEADER = "Content-Language";
    private static final byte[] CONTENT_LANGUAGE_HEADER_BYTES = CONTENT_LANGUAGE_HEADER.getBytes();
    private static final String HEADER_CONTENT_TYPE = WebContainerConstants.HEADER_CONTENT_TYPE;
    private static final byte[] HEADER_CONTENT_TYPE_BYTES = HEADER_CONTENT_TYPE.getBytes();
    protected static final String HEADER_CONTENT_LENGTH = "Content-Length";
    private static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    private static final boolean keepContentLength = (Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("keepcontentlength"))).booleanValue();
    private static final boolean skipHeaderFlush = (Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.skipheaderflush"))).booleanValue();
    private static final String contentTypeCompatibility = WCCustomProperties.CONTENT_TYPE_COMPATIBILITY;
    private static final boolean getSessionCompatibility = (Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.getsession2_4compatibility"))).booleanValue();

    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.srt");
    private static final String CLASS_NAME="com.ibm.ws.webcontainer.srt.SRTServletResponse";
    protected static final TraceNLS nls = TraceNLS.getTraceNLS(SRTServletResponse.class, "com.ibm.ws.webcontainer.resources.Messages");
    public static final int DEFAULT_BUFFER_SIZE = 4 * 1024;
    public static final Locale _defaultLocale = Locale.getDefault();
    public static final String _defaultEncoding = "ISO-8859-1";

    protected IResponse _response = null;
    // 104771 - begin
    protected boolean writerClosed = false;
    // 104771 - end
    protected SRTOutputStream _rawOut = new SRTOutputStream();
    // LIBERTY protected ResponseBuffer _responseBuffer = null;
    // protected BufferedServletOutputStream _bufferedOut = new
    // BufferedServletOutputStream(DEFAULT_BUFFER_SIZE);

    // LIBERTY protected WSServletOutputStream _bufferedOut;
    protected ServletOutputStream _bufferedOut;

    protected BufferedWriter _bufferedWriter = new BufferedWriter(0);
    protected PrintWriter _pwriter;

    protected boolean _firstWrite = false;
    protected boolean _firstWriteToCurrentBuffer = false;

    protected int _bufferSize = 0;

    protected boolean _gotWriter = false;
    protected boolean _firstWriterRetrieval = true;
    protected boolean _gotOutputStream = false;
    protected boolean _firstOutputStreamRetrieval = true;

    // 104034 - begin
    protected OutputStreamWriter _outWriter = null;
    protected String _outWriterEncoding;
    // 104034 - end

    protected boolean writerException = false;
    protected boolean _ignoreStateErrors;

    // begin 134537: part 1: duplicate headers added when jsp compilation fails.
    protected boolean _headersWritten = false;
    // end 134537: part 1

    protected Locale _locale;
    protected String _encoding;
    protected long _contentLength = -1L;
    protected int _statusCode = 200;
    private SRTConnectionContext _connContext;
    private IOutputMethodListener outputMethodListener = null;
    private String _contentType;

    // Custom property to revert to HttpDate
    // Deprecated since WAS 7.0
    //Do not give out this custom property without consultation with WebContainer Authorities. This has not 
    // been released to customers or is planned to be released.
    // PM25931 is removing this custom property.
    //private static final boolean oldDateFormatter = (Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.olddateformatter"))).booleanValue();
    
    private static final boolean localeDependentDateFormatter = WCCustomProperties.LOCALE_DEPENDENT_DATE_FORMATTER; //PM25931

    private static ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>();
    private static String formatStr = "EEE, dd MMM yyyy HH:mm:ss z";
    private static TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");
    
    /**
     * 
     */
    public SRTServletResponse() {
        super();
        // LIBERTY Moving this call to create the output stream to the
        // initForNextResponse method
        // LIBERTY this._bufferedOut = createOutputStream(DEFAULT_BUFFER_SIZE);
        _bufferedWriter.setObserver(this);
        // LIBERTY _bufferedOut.setObserver(this);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"SRTServletResponse", "outputstream is of type --> " + this._bufferedOut);
        }

    }

    public SRTServletResponse(SRTConnectionContext context) {
        this();
        this._connContext = context;
    }

    /**
     * Alert message that the outputstream has been closed.
     */
    public void alertClose() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"alertClose", " _outWriter: --> " + _outWriter);
        // 104771
        writerClosed = true;

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"alertClose", "exit");
    }

    /**
     * Alert message that the outputstream has been written to.
     */
    public void alertFirstWrite() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"alertFirstWrite", "entry");
        _firstWrite = true;
        _firstWriteToCurrentBuffer = true;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"alertFirstWrite", "exit");
    }

    /**
     * Alert message that the outputstream has been written to.
     */
    public void alertFirstFlush() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"alertFirstFlush", "entry");
        if (!isCommitted()) {
            commit();
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"alertFirstFlush", "exit");
    }

    /**
     * Alert message that there has been an exception in the outputstream.
     */
    public void alertException() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"alertException", "entry  _outWriter: --> " + _outWriter);
        writerException = true;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"alertException", "exit");
    }

    /**
     * Used to check whether the writer has been obtained.
     * 
     * @return Logical true if the writer has been previously obtained, false otherwise.
     */
    public boolean writerObtained() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"writerObtained"," " + String.valueOf(_gotWriter),"["+this+"]");
        return _gotWriter;
    }

    /**
     * Used to check whether the output stream has been obtained.
     * 
     * @return Logical true if the stream has been previously obtained, false otherwise.
     */
    public boolean outputStreamObtained() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"outputStreamObtained"," " + String.valueOf(_gotOutputStream),"["+this+"]");
        return _gotOutputStream;
    }

    /**
     * Close this response.
     */
    public void finish() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.entering(CLASS_NAME,"finish","["+this+"]");

        if (!isCommitted()) {
            commit();
        }

        // flush the OutputStream and Writer
        try {
            flushBuffer(false);
            _rawOut.close();
        }
        catch (IOException e) {
            // Don't want to report errors on early browser closes
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"finish", "Servlet.Exception:.Error.while.finishing.response", e);
            }
        }
        finally {
            _rawOut.reset();
            // PK29451 - remove call to clearHeaders(). Implementation of clearHeaders() was empty,
            // which this method relies on, but has been implemented in PK29451 to make reset() work.
            // _response.clearHeaders();
            
            cleanupFromFinish();
            // LIBERTY _bufferedOut.reset();
            _bufferedWriter.reset();
            // LIBERTY this._responseBuffer = null;
        }

        resetState();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.exiting(CLASS_NAME,"finish");
    }
    
    //shared cleanup from finish & finishKeepConnection
    protected void cleanupFromFinish() {
        _response = null;
        _bufferSize = 0;
        _encoding = null;
        // LIBERTY _responseBuffer = null;
        // _outWriterEncoding = null;
        _gotOutputStream = false;
        _gotWriter = false;
        this._pwriter = null;
    }

    protected void resetState() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"resetState", "entry");
        // begin 134537: part 4
        _headersWritten = false;
        _firstWrite = false;
        _firstWriteToCurrentBuffer = false;
        _ignoreStateErrors = false;
        _contentLength = -1L;
        _contentType = null;
        _locale = _defaultLocale;
        writerClosed = false;
        _firstWriterRetrieval = true;
        _firstOutputStreamRetrieval = true;
        isCharEncodingExplicit = false;

        if (com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31) {
            _gotOutputStream = false;
            _gotWriter = false;
        }

        // begin 277717    SVT:Mixed information shown on the Admin Console    WAS.webcontainer    
        // writerException = false;
        // end 277717    SVT:Mixed information shown on the Admin Console    WAS.webcontainer    

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"resetState", "exit");
    }

    public void addField(String key, String value) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"addField", "key --> " + key + " value --> " + value);
        }
        _response.addHeader(key, value);
    }

    public void addDateField(String key, long value) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"addDateField", "key --> " + key + " value --> " + value);
        }
        _response.addDateHeader(key, value);
    }

    public void addIntField(String key, int value) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"addIntField", "key --> " + key + " value --> " + value);
        }
        _response.addIntHeader(key, value);
    }

    public String getHeader(String key) {
        String header = _response.getHeader(key);
        //311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"getHeader", " name --> " + key + " response --> " + header,"["+this+"]");
        }
        return header;
    }

    public IResponse getIResponse(){
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"getIResponse", " response --> " + _response,"["+this+"]");
        }
        return _response;
    }

    protected String getHeader(byte[] key) {
        //311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"getHeader", " name --> " + (key!=null?new String(key):"") + " response --> " + _response.getHeader(key),"["+this+"]");
        }
        return _response.getHeader(key);
    }

    public boolean containsHeader(String name) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"containsHeader", " name --> " + name + " response --> " + String.valueOf(_response.containsHeader(name)));
        }
        return _response.containsHeader(name);
    }

    public boolean containsHeader(byte[] name) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"containsHeader", " name --> " + (name!=null?new String(name):"") + " response --> " + String.valueOf(_response.containsHeader(name)));
        }
        return _response.containsHeader(name);
    }

    protected void removeHeader(String key) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"removeHeader", " name --> " + key);
        }
        _response.removeHeader(key);
    }

    protected void removeHeader(byte[] key) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"removeHeader", " name --> " + (key!=null?new String(key):""));
        }
        _response.removeHeader(key);
    }

    // LIDB1234.3 - added method below
    /**
     * Clears the content of the underlying buffer in the response without clearing
     * headers or status code.
     * 
     * @throws IllegalStateException if the response has already been committed
     * 
     * @since 2.3
     */
    public void resetBuffer() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.entering(CLASS_NAME,"resetBuffer","["+this+"]");

        //is committed check done in underlying layer in tWAS
        if (isCommitted())
            throw new IllegalStateException();
        // LIBERTY
        if (_gotOutputStream)
        {
            _response.resetBuffer();
        }

        // LIBERTY if (_responseBuffer != null) {
        // LIBERTY _responseBuffer.clearBuffer();
        // LIBERTY }

        // begin 156186
        if (_bufferedWriter != null) {
            _bufferedWriter.clearBuffer();
        }
        // end 156186
        _firstWriteToCurrentBuffer = false;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.exiting(CLASS_NAME,"resetBuffer");
    }

    public void closeResponseOutput() {
        closeResponseOutput(false);
    }

    // 111953 - added method below
    //closeResponseOutput with releaseChannel=true can be called from webservices as well
    // when the want to speed up the response
    public void closeResponseOutput(boolean releaseChannel) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.entering(CLASS_NAME, "closeResponseOutput","["+this+"]");
            logger.logp (Level.FINE, CLASS_NAME, "closeResponseOutput", "releaseChannel->"+releaseChannel+", writerClosed->"+writerClosed);
        }
        // if user hasn't closed stream or writer on a forward, close it for them
        if (writerClosed == false) {
            // PK63328 starts - last flush to OutputStream

            boolean isArdEnabled = false;
            if (WebContainer.getWebContainer().getWebContainerConfig().isArdEnabled()) {
                if (((WebAppDispatcherContext) _connContext.getRequest().getWebAppDispatcherContext()).getWebApp().getWebAppConfig().isArdEnabled()) {
                    isArdEnabled = true;
                    //PK89810 Start                     
                    WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);
                    reqState.setAttribute("com.ibm.ws.webcontainer.appIsArdEnabled", true);                     
                    //PK89810 End
                }
            }

            //The goal of this logic is to close the response, release the channel, and send as Content-Length instead of chunked.
            //Previously we tried to just call close after calling setLastBuffer, but that doesn't work because if there is anything
            //already in the Channel's byte buffer queue you don't want to indicate its the last buffer before you're really ready and truncate the response.
            //Therefore, we must call flush with flushMode false so we can get all the data to the channel without it trying to
            //send everything out to the client before its really the last buffer. We gate the checks with if (!isArdEnabled) so
            //we don't indicate that the response is finished until we exit back out through the stack. Otherwise, it could try to close
            //the connection and invalidate the request before we're even out of the Servlet container/servlet call stack

            //All of this next if block is totally new since PK63328
            if (!isArdEnabled){
                _response.setFlushMode(false);
                _response.setIsClosing(true);
                try{
                    if (_gotOutputStream == true){
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                            logger.logp (Level.FINE, CLASS_NAME, "closeResponseOutput", "flush output stream");
                        }
                        if ( !(_bufferedOut instanceof WCOutputStream) || !((WCOutputStream)_bufferedOut).isClosed())
                            _bufferedOut.flush();
                    }
                    else if (_gotWriter == true){
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                            logger.logp (Level.FINE, CLASS_NAME, "closeResponseOutput", "flush writer");
                        }
                        _pwriter.flush();
                    }
                }
                catch (Throwable th){
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.SEVERE)) {
                        logger.logp (Level.SEVERE, CLASS_NAME, "closeResponseOutput", "Error.while.flushing.last.response");
                    }
                }

                _response.setFlushMode(true);
            }

            //This code was always here minus the check for isArdEnabled and setLastBuffer(true)
            //so we should continue calling close even for ard
            //
            try {
                if (_gotOutputStream == true) {
                    if (!isArdEnabled){
                        //PK89810
                        // PM18543 - Add test for WCCustomProperties.COMPLETE_DATA_RESPONSE
                        if (WCCustomProperties.COMPLETE_DATA_RESPONSE && !WCCustomProperties.FINISH_RESPONSE_ON_CLOSE){
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                                logger.logp (Level.FINE, CLASS_NAME, "closeResponseOutput", "setLastBuffer to true ");
                            }
                            _response.setLastBuffer(true); //PK89810,  setLastBuffer in  finish of BufferedWriter if CP is set
                        }
                    }

                    _bufferedOut.close();
                    _rawOut.close();
                }
                else if (_gotWriter == true) {
                    if (!isArdEnabled){
                        //PK89810
                        // PM18543 - Add test for WCCustomProperties.COMPLETE_DATA_RESPONSE
                        if (WCCustomProperties.COMPLETE_DATA_RESPONSE && !WCCustomProperties.FINISH_RESPONSE_ON_CLOSE){
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                                logger.logp (Level.FINE, CLASS_NAME, "closeResponseOutput", "setLastBuffer to true ");
                            }
                            _response.setLastBuffer(true); //PK89810 ,  setLastBuffer in finish of BufferedServletOutputStream if CP is set
                        }
                    }
                    // PK63328 ends
                    _pwriter.close();
                    _rawOut.close();
                }
// code below LIBERTY only
                if (releaseChannel)
                {
                    _response.releaseChannel();
                    WebContainerRequestState.getInstance(true).setCompleted(true);
                }
// code above LIBERTY only
            }
            catch (Throwable th) {
                logger.logp(Level.SEVERE, CLASS_NAME,"closeResponseOutput", "Error.while.closing.response.output",th);
            }
        }
        //Not sure if we should be doing this outside of writeClosed
        //This is not called by default. Only by WebServices and AsyncServlet completion
        if (releaseChannel){
            //check to avoid double release
            WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);
            if (! reqState.isCompleted()) {
                _response.releaseChannel();
                if (reqState.getCurrentThreadsIExtendedRequest()==this.getRequest())
                    WebContainerRequestState.getInstance(true).setCompleted(true);
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.exiting(CLASS_NAME, "closeResponseOutput");
        }
    }

    /**
     * This method is used to prevent IllegalStateExceptions that may be thrown
     * because the engine attempts to report the error, but has no idea what state
     * the currently executing servlet has put this response into.  The error reporting
     * will attempt to set the status code and will use getWriter() which may not be
     * legal in the current response state.  If we are attempting to report an error,
     * we don't care what the state is, so this method will force the response to
     * ignoreStateErrors.
     * 
     * @param b The logical value to set the ignore state errors flag to.
     */
    public void setIgnoreStateErrors(boolean b) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"setIgnoreStateErrors", " " + String.valueOf(b),"["+this+"]");
        _ignoreStateErrors = b;
    }

    public void setBufferSize(int size) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.entering(CLASS_NAME,"setBufferSize ", String.valueOf(size) +" ["+this+"]");
        _bufferSize = size;

        // LIBERTY if (_responseBuffer != null) {
        if (_gotOutputStream || _gotWriter)
        {
            if (!_firstWrite)
            {
                // LIBERTY _responseBuffer.setBufferSize(size);
                _response.setBufferSize(size);
                if (_gotWriter) {
                    _bufferedWriter.setBufferSize(size);
                }
            }
            else {
                logger.logp(Level.SEVERE, CLASS_NAME,"setBufferSize", "setbuffer.size.called.after.write");
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
                    logger.exiting(CLASS_NAME,"setBufferSize", "throw IllegalStateException");
                throw new IllegalStateException("setBufferSize() called after first write to Output Stream/Writer");
            }
        } else {
            // if setBuffer is called before a writer or outputStream is retrieved
            _response.setBufferSize(size);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.exiting(CLASS_NAME,"setBufferSize");
    }

    public int getBufferSize()
    {
        // LIBERTY if (_responseBuffer == null) {
        if (!_gotOutputStream)
        {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"getBufferSize", "size --> " + _bufferSize,"["+this+"]");

            return _bufferSize;
        }
        else
        {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"getBufferSize", "getBufferSize --> " + _response.getBufferSize(),"["+this+"]");

            // LIBERTY return _responseBuffer.getBufferSize();
            return _response.getBufferSize();
        }
    }

    public void flushBuffer() throws IOException {
        flushBuffer(true);
    }

    public void flushBuffer(boolean flushToWire) throws IOException {
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn&&logger.isLoggable (Level.FINE))
            logger.entering(CLASS_NAME, "flushBuffer","flushToWire="+String.valueOf(flushToWire)+" ["+this+"]");

        if (!flushToWire) {
            _response.setFlushMode(false);
        }
        try {
            if (isTraceOn&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "flushBuffer" , "_firstWriteToCurrentBuffer->"+this._firstWriteToCurrentBuffer+", skipHeaderFlush->"+skipHeaderFlush);
            if (this._firstWriteToCurrentBuffer || skipHeaderFlush || flushToWire){
                if (isTraceOn&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "flushBuffer" , "we've written output so flush");

                if (_pwriter != null && writerClosed == false) {
                    if (isTraceOn&&logger.isLoggable (Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME, "flushBuffer" , "flush the printWriter");                   
                    _pwriter.flush();
                }

                if (_gotOutputStream)
                {
                    // LIBERTY _responseBuffer.flushBuffer();
                    // !!MJS-Flush _response.flushBuffer();
                    //if (!_response.isCommitted()) {
                    //    _response.flushBuffer();
                    //}
                    //commit();
                    if ( !(_bufferedOut instanceof WCOutputStream) || !((WCOutputStream)_bufferedOut).isClosed()) {
                        if (isTraceOn&&logger.isLoggable (Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME, "flushBuffer" , "flush the outputStream, isCommitted =" + _response.isCommitted());                                         
                        _bufferedOut.flush();
                    }   
                }
                
            }

            if (!isCommitted()) {
                if (isTraceOn&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "flushBuffer" , "Not committed, so write headers");
                commit();
                // !!MJS-Flush _response.writeHeaders();
                //PI70184
                if (this._firstWriteToCurrentBuffer || skipHeaderFlush || flushToWire){
                    _response.writeHeaders();               
                }
            }
        }
        finally {
            if (!flushToWire) {
                _response.setFlushMode(true);
            }
            if (isTraceOn&&logger.isLoggable (Level.FINE))
                logger.exiting(CLASS_NAME, "flushBuffer");
        }
    }

    // public void sendHeaders(){
    // if (!isCommitted()){
//              if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
//                logger.logp(Level.FINE, CLASS_NAME, "sendHeaders" , "force sending intermediate headers, can happen more than once");
    // _response.writeHeaders();
    // }
    // }

    public boolean isCommitted() {
        // 182383
        if (_headersWritten) {

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "isCommitted" , "headersWritten=true","["+this+"]");

            return true;
        }
        // 182383

        // LIBERTY if (_gotWriter || _gotOutputStream) {
        if (_gotOutputStream)
        {
            // LIBERTY if (_responseBuffer == null)
            // LIBERTY return false;
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "isCommitted" , "responseBuffer isCommitted="+_response.isCommitted(),"["+this+"]");

            // LIBERTY return _responseBuffer.isCommitted();
            return _response.isCommitted();
        }
        if (_gotWriter && _bufferedWriter != null)
        {
            return _bufferedWriter.isCommitted();
        }
        else
        {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "isCommitted" , "false","["+this+"]");
            return false;
        }
    }

    /**
     * @return ServletRequest
     */
    public IExtendedRequest getRequest() {

        if (_connContext==null){
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "getRequest" , "null","["+this+"]");
            return null;
        }
        else {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "getRequest" , "request="+this._connContext.getRequest(),"["+this+"]");
            return this._connContext.getRequest();
        }

    }

    /**
     * Returns an output stream for writing binary response data.
     * 
     * @see reinitStreamState
     */
    public ServletOutputStream getOutputStream() {
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn&&logger.isLoggable (Level.FINE))  //306998.15
            logger.entering(CLASS_NAME,"getOutputStream","gotWriter="+String.valueOf(_gotWriter)+" ["+this+"]");

        if (!_ignoreStateErrors && _gotWriter) {

            if (isTraceOn&&logger.isLoggable (Level.FINE))  //306998.15
                logger.exiting(CLASS_NAME,"getOutputStream","throw IllegalStateException");

            throw new IllegalStateException(nls.getString("Writer.already.obtained", "Writer already obtained"));
        }

        //PK89810 Start
        if(!(WCCustomProperties.FINISH_RESPONSE_ON_CLOSE) || ! _gotOutputStream)                
        {
            _gotOutputStream = true;

            // LIBERTY _bufferedOut.init(_rawOut, getBufferSize());
            // LIBERTY _bufferedOut.setLimit(_contentLength);
            // LIBERTY _bufferedOut.setResponse(_response);
            // LIBERTY _responseBuffer = _bufferedOut;
        } //PK89810 End
        this.fireOutputStreamRetrievedEvent(_bufferedOut);

        if (isTraceOn&&logger.isLoggable (Level.FINE))  //306998.15
            logger.exiting(CLASS_NAME,"getOutputStream");

        return _bufferedOut;
    }

    /**
     * Returns a PrintWriter for writing formatted text output.
     * 
     * @return The PrintWriter to use for writing formatted text output.
     */
    public PrintWriter getWriter() throws IOException, UnsupportedEncodingException {
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn&&logger.isLoggable (Level.FINE))  //306998.15
            logger.entering(CLASS_NAME,"getWriter","gotOutputStream="+String.valueOf(_gotOutputStream)+" ["+this+"]");

        if (!_ignoreStateErrors && _gotOutputStream) {

            if (isTraceOn&&logger.isLoggable (Level.FINE))  //306998.15
                logger.exiting(CLASS_NAME,"getWriter","throw IllegalStateException");

            throw new IllegalStateException(nls.getString("OutputStream.already.obtained", "OutputStream already obtained"));
        }
        if (!_gotWriter) {
            String charEncoding = getCharacterEncoding();

            // begin 249506, part 1 Set content-type charset if not set already
            // begin PK27527  DEFAULT CONTENT-TYPE SETTING ON WEBSPHERE V6.0.X IS "TEXT/PLAN"
            // only add charset if we are in v6 mode.
            if (contentTypeCompatibility==null || (!contentTypeCompatibility.equalsIgnoreCase("V4")&&
                            !contentTypeCompatibility.equalsIgnoreCase("V5"))){
                String contentType = getContentType();
                //PK33871 - added check to make sure the content type started with 'text'.
                if (contentType!=null && contentType.indexOf("charset")==-1 && contentType.startsWith(WebContainerConstants.TEXT))  
                {
                    if (contentType.endsWith(";"))
                        setContentType(contentType + "charset=" + charEncoding);
                    else
                        setContentType(contentType + ";charset=" + charEncoding);
                }
            }
            // end PK27527  DEFAULT CONTENT-TYPE SETTING ON WEBSPHERE V6.0.X IS "TEXT/PLAN"  
            // end 249506, part 1

            //              if ((_outWriter == null) || (writerClosed == true) || (!charEncoding.equals(_outWriterEncoding)))
            if ((_outWriter == null) || (writerException == true) || (!charEncoding.equals(_outWriterEncoding))) {
                if (isTraceOn&&logger.isLoggable (Level.FINE) && writerException){  //306998.15
                    logger.logp(Level.FINE, CLASS_NAME,"getWriter", "writerException --> " + writerException + "--> creating new OutputStreamWriter");
                }
                _outWriter = new OutputStreamWriter(_rawOut, EncodingUtils.getJvmConverter(charEncoding));
                _outWriterEncoding = charEncoding;
                writerException = false;
            }
            else{
                if (isTraceOn&&logger.isLoggable (Level.FINE)){  //306998.15
                    logger.logp(Level.FINE, CLASS_NAME,"getWriter", "reusing _outWriter: --> " + _outWriter);
                }
            }

            if (isTraceOn&&logger.isLoggable (Level.FINE))  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"getWriter", "created writer with encoding: " + _outWriter.getEncoding());

            _bufferedWriter.init(_outWriter, getBufferSize());
            _bufferedWriter.setLimitLong(_contentLength);
            _bufferedWriter.setResponse(_response);

            _pwriter = new PrintWriter(_bufferedWriter, false);
            // LIBERTY _responseBuffer = _bufferedWriter;
            _gotWriter = true;

            writerClosed = false;
        }
        this.fireWriterRetrievedEvent(_pwriter);
        if (isTraceOn&&logger.isLoggable (Level.FINE))  //306998.15
            logger.exiting(CLASS_NAME,"getWriter");
        return _pwriter;
    }

    public void start() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"start","["+this+"]");
        setDefaultResponseEncoding();
    }

    //  //  d151464 - create a thread local object to represent the response context on this thread
    // private static ThreadLocal _responseContext = new ThreadLocal();
    //
    // public static SRTServletResponseContext getResponseContext()
    // {
    // return (SRTServletResponseContext) _responseContext.get();
    // }
    //
    // public static void setResponseContext(SRTServletResponseContext rspCon)
    // {
    // _responseContext.set(rspCon);
    // }

    /**
     * @return SRTConnectionContext
     */
    protected SRTConnectionContext getConnectionContext() {
        return _connContext;
    }

    public Object clone() throws CloneNotSupportedException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"clone","["+this+"]");
        return super.clone();
    }

    /**
     * Uses the given HTTP response message to assign a new value to
     * a browser-specific cookie.  Updates the response to restrict
     * caching of sensitive cookie-related data.
     * 
     * @param cookie The cookie to add.
     */
    public void addCookie(Cookie cookie) {
        
        //
        // Note: this method is overwritten in servlet 4.0
        //
        String cookieName = cookie.getName();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"addCookie", "Adding cookie --> " + cookieName,"["+this+"]");
        }
        // d151464 - check the include flag
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
                logger.logp(Level.FINE, CLASS_NAME,"addCookie", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "addCookie cookie --> " + cookieName);  //311717
        }
        else {
            if (!_ignoreStateErrors && isCommitted()) {
                // log a warning (only the first time)...ignore headers set after response is committed
                IServletWrapper wrapper = dispatchContext.getCurrentServletReference();
                if (logWarningActionNow(wrapper)) {
                    logAlreadyCommittedWarning(new Throwable(), "addCookie");
                } else {
                    logger.logp(Level.FINE, CLASS_NAME,"addCookie", "Cannot set header.  Response already committed.");
                }
            }
            else {
                _response.addCookie(cookie);
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.exiting(CLASS_NAME,"addCookie");
        }
    }

    /**
     * Commits the response by sending response codes and headers.  A response may only be commited once.
     */
    synchronized protected void commit() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.entering(CLASS_NAME,"commit","["+this+"] ,_headersWritten -->" + _headersWritten );
        }
        // begin 134537: part 2
        // (!isCommitted())
        if (!_headersWritten && !isCommitted()) // add check if headersWritten
        {
            // end 134537: part 2

            addLocaleHeader();

            // PQ59244 - disallow content length header if content is encoded
            // LIBERTY
            if (containsHeader(HEADER_CONTENT_ENCODING) && containsHeader(HEADER_CONTENT_LENGTH)) {

                if (keepContentLength){
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME,"commit", "WebContainer custom property set to keep Content-Length header w/presence of Content-Encoding header");
                    }
                }
                else{   
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                        logger.logp(Level.FINE, CLASS_NAME,"commit", "Content-Length header disallowed w/presence of Content-Encoding header");
                    }

                    removeHeader(HEADER_CONTENT_LENGTH);
                }
            }

            WebContainer.notifyHttpServletResponseListenersPreHeaderCommit((HttpServletRequest) _connContext.getRequest(), this);

            String scheme = this.getRequest().getScheme();            
            if(scheme!=null && scheme.equalsIgnoreCase("https")){
                addSTSHeader();
            }

            _response.prepareHeadersForWrite();

            // begin 134537: part 3
            _headersWritten = true;
            // end 134537: part 3
            
            if(this._bufferedOut!= null && this._bufferedOut instanceof WCOutputStream){
                HttpOutputStreamConnectWeb output = ((WCOutputStream) this._bufferedOut).getOutput();
                output.setWebC_headersWritten(true);
                
                String remoteUser = _connContext.getRequest().getRemoteUser();
                
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.logp(Level.FINE, CLASS_NAME, "commit", "Setting remote user : " + remoteUser);
                }
                //Set the remote user to the channel. This will first try to get the security remote user and then fall back on the channel
                output.setWC_remoteUser(remoteUser);
                (((WCOutputStream) this._bufferedOut).getOutput()).setWebC_headersWritten(true);
             }

        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"commit");
        }
    }

    private void addLocaleHeader() {
        // 115097 - convert any underscores to dashes in the locale
        _response.setContentLanguage(_locale.toString().replace('_', '-'));
    }
    
    /**
     * 
     */
    private void addSTSHeader() {

        String value  = this.getRequest().getWebAppDispatcherContext().getWebApp().getConfiguration().getSTSHeaderValue(); 
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"addSTSHeader", " value -->" + value);
        }
        if(value.equalsIgnoreCase("NoValue")){
            return;
        }
        //what if application has already set it programmatically
        if(this.getHeader("Strict-Transport-Security") == null ){
            this.setHeader("Strict-Transport-Security", value); 
        }
    }

    public String encodeRedirectUrl(String URL) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"encodeRedirectUrl","["+this+"]");
        }
        return encodeURL(URL);
    }

    public String encodeRedirectURL(String url) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"encodeRedirectURL","["+this+"]");
        }
        return encodeURL(url);
    }

    public String encodeUrl(String URL) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"encodeUrl","["+this+"]");
        }
        return encodeURL(URL);
    }

    public String encodeURL(String url) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"encodeURL", " url --> " + PasswordNullifier.nullifyParams(url), "["+this+"]");
        }
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        return dispatchContext.encodeURL(url);
    }

    protected void setDefaultResponseEncoding() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"setDefaultResponseEncoding","["+this+"]");
        }

        IExtendedRequest extReq = getRequest();
        WebApp webApp = null;
        boolean useAuto = false;
        if (extReq != null) {
            webApp = ((WebAppDispatcherContext) extReq.getWebAppDispatcherContext()).getWebApp();
        }
        
        // check auto encoding
        if (webApp != null){
            //WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) extReq.getWebAppDispatcherContext();
            //WebApp webApp = dispatchContext.getWebApp();
            if (webApp.getConfiguration().isAutoResponseEncoding() == true) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) { 
                    logger.logp(Level.FINE, CLASS_NAME,"setDefaultResponseEncoding", "auto response encoding is true");
                }
                useAuto = true;
                // it's on...set a default content type and attempt to determine response locale and encoding

                _locale = getRequest().getLocale();
                if (_locale == null) {
                    _locale = _defaultLocale;
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                        logger.logp(Level.FINE, CLASS_NAME,"setDefaultResponseEncoding", "_locale is null: default to " + _locale.toString());
                    }
                }

                // begin PQ89614
                _encoding = getRequest().getCharacterEncoding();
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.logp(Level.FINE, CLASS_NAME,"setDefaultResponseEncoding", "Encoding from request: " + _encoding );
                }
                // end PQ89614

                // PK21127 start
                if (_encoding != null && !EncodingUtils.isCharsetSupported(_encoding))
                {
                    // charset not supported, continue with the logic to determine the encoding
                    _encoding = null;
                }
                // PK21127 end

                if(_encoding == null){
                    /**
                     * Check the DD locale-endoding mappings to see if there is a specified mapping
                     * @since Servlet 2.4
                     */
                    _encoding = webApp.getConfiguration().getLocaleEncoding(_locale);

                    if (_encoding == null) {
                        _encoding = EncodingUtils.getEncodingFromLocale(_locale);
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                            logger.logp(Level.FINE, CLASS_NAME,"setDefaultResponseEncoding", "Encoding from locale: " + _encoding );
                        }
                        if (_encoding == null) {
                            _encoding = _defaultEncoding;
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                                logger.logp(Level.FINE, CLASS_NAME,"setDefaultResponseEncoding", "_encoding is null: default to " + _encoding);
                            }
                        }
                    }
                }

                if (EncodingUtils.setContentTypeBySetHeader){
                    setHeader(WebContainerConstants.HEADER_CONTENT_TYPE,"text/html; charset=" + _encoding);
                }
                else
                {
                    setContentType("text/html; charset=" + _encoding);
                }
            }
        }
        if (!useAuto) {
            // it's off...use defaults
            _locale = _defaultLocale;
            _encoding = getSpecLevelEncoding(_defaultEncoding,webApp);
                        
            //begin PK27527  DEFAULT CONTENT-TYPE SETTING ON WEBSPHERE V6.0.X IS "TEXT/PLAN"
            // set the default content type based on whether we are V4=text/html or V5=text/html;charset=defaultEncoding
            if (contentTypeCompatibility!=null){
                if (contentTypeCompatibility.equalsIgnoreCase("V4")){
                    setHeader(WebContainerConstants.HEADER_CONTENT_TYPE,"text/html");
                    if (EncodingUtils.setContentTypeBySetHeader){
                        setHeader(WebContainerConstants.HEADER_CONTENT_TYPE,"text/html");
                    }
                    else{
                        setContentType("text/html");
                    }
                }
                else if (contentTypeCompatibility.equalsIgnoreCase("V5")){
                    if (EncodingUtils.setContentTypeBySetHeader){
                        setHeader(WebContainerConstants.HEADER_CONTENT_TYPE,"text/html; charset=" + _encoding);
                    }
                    else{
                        setContentType("text/html; charset=" + _encoding);
                    }
                }
            }
            //end PK27527  DEFAULT CONTENT-TYPE SETTING ON WEBSPHERE V6.0.X IS "TEXT/PLAN"  
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"setDefaultResponseEncoding", "_locale: " + _locale.toString()+", _encoding: " + _encoding);
        }
    }
    
    protected String getSpecLevelEncoding(String encoding, WebApp webApp) {
        return encoding;
    }

    public void reset() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"reset","["+this+"]");
        }
        _response.setReason(REASON_OK);
        _response.setStatusCode(200);

        // LIBERTY if (_responseBuffer != null) {
        // begin 156186
        // _responseBuffer = null;
        // LIBERTY _responseBuffer.clearBuffer();
        // end 156186
        // LIBERTY }

        // begin 156186
        if (_bufferedWriter != null) {
            _bufferedWriter.clearBuffer();
        }
        // end 156186
        
        if (isCommitted())
          throw new IllegalStateException();
        
        _response.resetBuffer();

        _response.clearHeaders();

        resetState();
        setDefaultResponseEncoding();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"reset");
        }
    }

    /**
     * Returns the character encoding used for writing text to the
     * body of this response.
     */
    public String getCharacterEncoding() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"getCharacterEncoding","["+this+"]");
        }
        if (_encoding == null) {
            setDefaultResponseEncoding();
        }
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"getCharacterEncoding", " encoding --> " + _encoding);
        }

        return _encoding;
    }

    public String getContentType() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"getContentType","["+this+"]");
        }
        if (_contentType == null) {
           setContentType(getHeader(HEADER_CONTENT_TYPE));
        }
        
        return _contentType;
    }

    public void setCharacterEncoding(String encoding) { 
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"setCharacterEncoding", " encoding --> " + encoding + " ["+this+"]");
        }
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (getRequest()!=null&&dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.exiting(CLASS_NAME,"setCharacterEncoding", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"));  //311717
            }
            return;
        }

        if (_gotWriter || this._headersWritten) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.exiting(CLASS_NAME,"setCharacterEncoding","_gotWriter="+String.valueOf(_gotWriter)+", _headersWritten="+String.valueOf(_headersWritten));  //311717
            }
            return;
        }
        if (encoding != null) {
            String apos = "\'";
            String qoute = "\"";
            if (encoding.startsWith(apos) || encoding.startsWith(qoute)) {
                encoding = encoding.substring(1);
            }
            if (encoding.endsWith(apos) || encoding.endsWith(qoute)) {
                encoding = encoding.substring(0, encoding.length() - 1);
            }
            _encoding = encoding;
            isCharEncodingExplicit = true;
        }
        if (_contentType!=null) {
            int index = _contentType.indexOf("charset=");
            if (index != -1)
                _contentType = _contentType.substring(0, index) + "charset=" + _encoding;
            else {
                _contentType = _contentType + "; charset=" + _encoding;
            }

            if (_response != null)
                _response.setContentType(_contentType);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.exiting(CLASS_NAME,"setCharacterEncoding", "encoding: " + _encoding + " ,contentType: " + _contentType);
    }

    /**
     * Get the Cookies that have been set in this response.
     */
    public Cookie[] getCookies() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"getCookies","["+this+"]");
        }
        return (_response.getCookies());
    }

    /**
     * Sends an error response to the client using the specified status
     * code and no default message.
     * 
     * @param sc The status code.
     * 
     * @exception IOException
     *              If an I/O error has occurred.
     */
    public void sendError(int status) throws IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"sendError", "error --> "+ String.valueOf(status),"["+this+"]");
        }

        Object[] args = { new Integer(status) };
        sendError(status, MessageFormat.format(nls.getString("Error.reported.{0}", "Error reported: {0}"), args));
    }

    /**
     * Sends an error response to the client using the specified status code and detail message.
     * 
     * @param status the status code.
     * @param msg the detail message.
     * 
     * @exception IOException
     *              If an I/O error has occurred.
     */
    public void sendError(int status, String msg) throws IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"sendError", "status --> " + status + " " + msg + " ["+this+"]");
        }

        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (!WCCustomProperties.ALLOW_INCLUDE_SEND_ERROR&&dispatchContext.isInclude()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"sendError", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "sendError --> " + status + " with message --> " + msg);  //311717
            }
        }
        else {
            dispatchContext.sendError(status, msg);

            this.closeResponseOutput();
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"sendError");
        }        
    }

    /**
     * Sends an error response directly to the client using the specified status code and 
     * detail message.
     * 
     * @param status the status code.
     * @param msg the detail message.
     * 
     * @exception IOException
     *              If an I/O error has occurred.
     */
    public void setReason(String msg) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"setReason", " message --> " + msg,"["+this+"]");
        }
        _response.setReason(msg);
    }

    /**
     * Sends a redirect response to the client using the specified redirect location URL.
     * 
     * @param location The redirect location URL.
     * 
     * @exception IOException
     *              If an I/O error has occurred.
     */
    public void sendRedirect(String location) throws IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"sendRedirect", "location --> " + PasswordNullifier.nullifyParams(location) + " ["+this+"]" );
        }

        // d151464 - check the include flag
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"sendRedirect", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "sendRedirect location --> " + PasswordNullifier.nullifyParams(location));  //311717
            }
        }
        else {
            dispatchContext.sendRedirect(location);
        }

        this.closeResponseOutput();

        // PK79143 Start
        if (!isCommitted() && !dispatchContext.isInclude()) {                                          // PM04383
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"sendRedirect"," : Not committed, so write headers");
            commit();
            _response.setLastBuffer(true);
            _response.writeHeaders();
        }
        // PK79143 End

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"sendRedirect");
        }        
    }

    // PQ97429
    public void sendRedirect303(String location) throws IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"sendRedirect303", "location --> " + location+ " ["+this+"]");
        }

        // d151464 - check the include flag
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"sendRedirect303", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "sendRedirect303 location -->" + location);  //311717
            }
        }
        else {
            dispatchContext.sendRedirect303(location);
        }

        this.closeResponseOutput();

        // PK79143 Start
        if (!isCommitted()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"sendRedirect"," : Not committed, so write headers");
            commit();
            _response.setLastBuffer(true);
            _response.writeHeaders();
        }
        // PK79143 End

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"sendRedirect303");
        }
    } // PQ97429

    /**
     * Defines the content length for this response.  This call must be made only once.
     * Not setting the content length may cause significant performance degradation.
     * 
     * @param len The content length.
     */
    public void setContentLength(int len) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"setContentLength", "length --> " + String.valueOf(len) + " ["+this+"]");
        }

        // d151464 - check the include flag
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"setContentLength", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "setContentLength length --> " + String.valueOf(len));  //311717
            }
        }
        else
        {
            if (_response!=null) {
                if (!_response.isCommitted()) {
                    _response.setContentLength(len);
                }
            }
            _contentLength = len;
            // LIBERTY _bufferedOut.setLimit(_contentLength = len);
            _bufferedWriter.setLimitLong(_contentLength);
            //still want to try to set the header even if the response is committed to throw the warning
            setIntHeader(HEADER_CONTENT_LENGTH, len);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"setContentLength");
        }
    }

    /**
     * Defines the content type for this response.  This call must only be made once.
     * 
     * @param type The content type.
     */
    public void setContentType(String type) {
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        // 311717
        if (isTraceOn&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"setContentType", " type --> " + type+ " ["+this+"]");
        }
        //PM50313
        if (type == null){
            if (isTraceOn&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.exiting(CLASS_NAME,"setContentType","not set - type is NULL");
            }  
            return;
        }
        //PM50313

        // d151464 - check the include flag
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (isTraceOn&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.exiting(CLASS_NAME,"setContentType", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"));  //311717
            }
            return;
        }


        if (isCommitted()){
            if (isTraceOn&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.exiting(CLASS_NAME,"setContentType","not set - response isCommitted");  //311717
            }
            return;
        }

        int equalsIndex = type.indexOf('=');
        boolean containsCharset = false;
        int beginCharsetStrIndex = equalsIndex - 7;
        int startCharsetValIndex = equalsIndex + 1;
        boolean isPreV7 = false;
        if (contentTypeCompatibility!=null && (contentTypeCompatibility.equalsIgnoreCase("V4")||
                        contentTypeCompatibility.equalsIgnoreCase("V5")||contentTypeCompatibility.equalsIgnoreCase("V6"))){
            isPreV7 = true;
        }

        boolean addEncoding = false;

        if (beginCharsetStrIndex > -1){
            containsCharset = type.regionMatches(true, beginCharsetStrIndex, "charset", 0, 7);
        }
        if (containsCharset){
            if (isTraceOn&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"setContentType", " isPreV7 --> " + isPreV7);
            }
            if (!_gotWriter) {  // 249506, part 2
                // Example:
                // setContentType("text/html;charset=UTF-8")
                // getWriter();
                // result = text/html;charset=UTF-8
                // we can change the character encoding because we haven't gotten the writer or committed
                // Begin 287829, Fix setContentType when qoutes are a part of the charset

                int endIndex = type.length() - 1;
                if (startCharsetValIndex<=endIndex){
                    isCharEncodingExplicit = true;
                    boolean startsWithQoute = type.charAt(startCharsetValIndex) == '"' || type.charAt(startCharsetValIndex) == '\'';
                    boolean endsWithQoute = type.charAt(endIndex) == '"' || type.charAt(endIndex) == '\'';
                    if (startsWithQoute && endsWithQoute){
                        _encoding = type.substring(startCharsetValIndex + 1, endIndex);
                        addEncoding = true;
                        // remove the qoutes from the encoding and add later
                    }
                    else {
                        _encoding = type.substring(startCharsetValIndex);
                        // type is perfectly formed and can be set as is.
                    }

                }
                // End 287829, Fix setContentType when qoutes are a part of the charset
            }
            else {
                if (isPreV7){
                    // Before v7, strip the charset
                    // Example:
                    // setContentType("text/html");
                    // getWriter();
                    // setContentType("text/xml;charset=UTF-8");
                    // result = text/xml
                    if (isTraceOn&&logger.isLoggable (Level.FINE)) {  //306998.15
                        logger.logp(Level.FINE, CLASS_NAME,"setContentType", " isPreV7, strip charset ");
                    }
                    type = type.substring(0, beginCharsetStrIndex).trim();
                    // if (type.endsWith(";")) { //wasteful??? It has to end with ;
                    type = type.substring(0, type.length() - 1);
                    // }
                }
            }
        }
        if (!isPreV7){
            if (_gotWriter){
                //we've gotten the writer, but not committed, so we can change the mime type,
                //but not the charset. Still we have to add the default encoding or the previous
                // charset
                // setContentType("text/html;charset=ISO-8859-7");
                // getWriter();
                // setContentType("text/xml;charset=UTF-8");
                // result = text/xml;charset=ISO-8859-7
                if (isTraceOn&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.logp(Level.FINE, CLASS_NAME,"setContentType", " add encoding because we retrieved a writer");
                }
                addEncoding = true;
            }
            else {
                //we haven't gotten the writer, and the current contentType argument has no
                //charset included. Therefore check to see if the old content type has a charset
                // and append it *** if the new type is some form of text ***
                // setContentType("text/html;charset=UTF-8");
                // setContentType("text/xml");
                // result = text/xml;charset=UTF-8
                if (isTraceOn&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.logp(Level.FINE, CLASS_NAME,"setContentType", " did not get writer, check to see if charset already exists");
                }
                if (isCharEncodingExplicit&&type.startsWith(WebContainerConstants.TEXT)){ //506208.1
                    if (isTraceOn&&logger.isLoggable (Level.FINE)) {  //306998.15
                        logger.logp(Level.FINE, CLASS_NAME,"setContentType", " add encoding because the content type already had a charset");
                    }
                    addEncoding = true;
                }
            }
        }

        if (addEncoding)
        {
            if (containsCharset){
                type = type.substring(0, startCharsetValIndex) + _encoding;
            }
            else
            {
                type = type + ";" + WebContainerConstants.CHARSET_EQUALS + _encoding;
            }
        }
        /* 252465: We do not want to do the autoResponseEncoding check here
         * because this sets the charset for binary data also.
         * Only use in the case of getWriter.
         */
        _contentType = type;
        if (_response != null)
            _response.setContentType(type);

        if (isTraceOn&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"setContentType","type="+_contentType);  //311717
        }

    }

    /**
     * Adds a date header to the current time. If this is called more than once,
     * the current value will replace the previous value.
     * 
     * @param name the header name.
     */
    public void setDateHeader(String name) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.entering(CLASS_NAME,"setDateHeader", "name --> " + name + " ["+this+"]");
        // d151464 - check the include flag +
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE,CLASS_NAME,"setDateHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"));  //311717
            }
        }
        else {
            // d128646 - if we're not ignoring state errors and response is committed or closed, throw an exception
            if (!_ignoreStateErrors && isCommitted()) {
                // log a warning (only the first time)...ignore headers set after response is committed
                IServletWrapper wrapper = dispatchContext.getCurrentServletReference();
                if (logWarningActionNow(wrapper)) {
                    logAlreadyCommittedWarning(new Throwable(), "setDateHeader");
                } else {
                    logger.logp(Level.FINE, CLASS_NAME,"setDateHeader", "Cannot set header.  Response already committed.");
                }

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.exiting(CLASS_NAME,"setDateHeader","Response already committed");  //311717
                }
                return;
            }

            setHeader(name, createCompliantHttpDateString(System.currentTimeMillis()));
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"setDateHeader");  //311717
        }
    }
    /**
     * Adds a date header with the specified time.  If this is called more than once,
     * the current value will replace the previous value.
     * 
     * @param name The header field name.
     * @param t The time in milliseconds since the epoch.
     */
    public void setDateHeader(String name, long t) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"setDateHeader", " name --> " + name + " value --> " + String.valueOf(t)+ " ["+this+"]");
        }
        // d151464 - check the include flag
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE,CLASS_NAME,"setDateHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"));  //311717
            }
        }
        else {
            // d128646 - if we're not ignoring state errors and response is committed or closed, throw an exception
            if (!_ignoreStateErrors && isCommitted()) {
                // log a warning (only the first time)...ignore headers set after response is committed
                IServletWrapper wrapper = dispatchContext.getCurrentServletReference();
                if (logWarningActionNow(wrapper)) {
                    logAlreadyCommittedWarning(new Throwable(), "setDateHeader");
                } else {
                    logger.logp(Level.FINE, CLASS_NAME,"setDateHeader", "Cannot set header.  Response already committed.");
                }
                
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.exiting(CLASS_NAME,"setDateHeader","Response already committed");
                }
                return;
            }

            // _header.setDateField(name, t);
            _response.setDateHeader(name, t);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"setDateHeader");
        }
    }

    /**
     * Adds a header field with the specified string value.  If this is
     * called more than once, the current value will replace the previous value.
     * 
     * @param name The header field name.
     * @param s The field's string value.
     */
    public void setHeader(String name, String s) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"setHeader", " name --> " + name + " value --> " + PasswordNullifier.nullifyParams(s), "["+this+"]");
        }
        // Begin:248739
        // Add methods for DRS-Hot failover to set internal headers without checking
        // if the request is an include.
        setHeader(name, s, true);
    }

    /**
     * Adds a header field with the specified string value.  If this is
     * called more than once, the current value will replace the previous value.
     * 
     * @param name The header field name.
     * @param s The field's string value.
     * @param checkInclude Whether to check if the request is include before setting header
     */
    public void setHeader(String name, String s, boolean checkInclude) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"setHeader", " name --> " + name + " value --> " + PasswordNullifier.nullifyParams(s) + " checkInclude --> " + checkInclude + " ["+this+"]");
        }

        // d151464 - check the include flag
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (checkInclude&&dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"setHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "setHeader name --> " + name + " value --> " + PasswordNullifier.nullifyParams(s) + " checkInclude --> " + checkInclude);  //311717
            }
        }
        else {
            // make sure we don't have a null name...
            if (name == null) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.exiting(CLASS_NAME,"setHeader","name is null");
                }
                return;
            }

            // d128646 - if we're not ignoring state errors and response is committed or closed, throw an exception
            if (!_ignoreStateErrors && isCommitted()) {

                // log a warning (only the first time)...ignore headers set after response is committed
                IServletWrapper wrapper = dispatchContext.getCurrentServletReference();
                if (logWarningActionNow(wrapper)) {
                    Throwable t = new Throwable();
                    logAlreadyCommittedWarning(t, "setHeader");
                }
                
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.exiting(CLASS_NAME,"setHeader","Response already committed");
                }
                return;
            }

            if (s == null) {
                removeHeader(name);
            }
            else {
                if (name.equalsIgnoreCase(WebContainerConstants.HEADER_CONTENT_TYPE)) {
                    // need to specially handle the content-type header
                    String value = s.toLowerCase();
                    int index = value.indexOf("charset=");
                    if (index != -1) {
                        _encoding = s.substring(index + 8);
                        s = s.substring(0, index) + "charset=" + _encoding;
                    }
                    else {
                        if (dispatchContext.isAutoRequestEncoding()) {  //306998.15
                            // only set default charset if auto response encoding is true.
                            // otherwise cts test will fail.
                            if (s.endsWith(";")) {
                                s = s + "charset=" + getCharacterEncoding();
                            }
                            else {
                                s = s + ";charset=" + getCharacterEncoding();
                            }
                        }
                    }
                    _contentType = s;
                }

                _response.setHeader(name, s);
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"setHeader", " name --> " + name + " value --> " + PasswordNullifier.nullifyParams(s));
        }
    }

    // Do not sync for performance reasons.  It is ok to print this warning more than once, just trying to limit it for the most part.
    protected boolean logWarningActionNow(IServletWrapper w) {
        if (w instanceof IServletWrapperInternal) {
            boolean firstTime = ((IServletWrapperInternal) w).hitWarningStatus();
            return firstTime;
        }  else {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) { 
                logger.logp(Level.FINE, CLASS_NAME,"logWarningActionNow","Not dealing with internal wrapper");
            }
            return true;
        }
        
    }
    
    protected void logAlreadyCommittedWarning(Throwable t, String methodName) {
        String userStackTrace = null;
        TruncatableThrowable tt = new TruncatableThrowable(t);
        StringBuilder sb = new StringBuilder();

        // get the trace trimming off internal entries.
        StackTraceElement[] elements = tt.getStackTrace();
        for (int i = 0; (i < elements.length) && (i < 64); i++) {
            sb.append("\n        at ").append(elements[i].toString());
        }
            
        sb.append("\n");
        userStackTrace = sb.toString();

        // log a warning...ignore headers set after response is committed
        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cannot.set.header.Response.already.committed", new Object[] {userStackTrace} );

    }
    
    
    /**
     * Adds a header field with the specified string value.
     * Does not check to see if this is an include.  If this is
     * called more than once, the current value will replace the previous value.
     * 
     * @param name The header field name.
     * @param s The field's string value.
     */
    public void setInternalHeader(String name, String s) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"setInternalHeader", " name --> " + name + " value --> " + s,"["+this+"]");
        }
        setHeader(name, s, false);
    }
    //  End:248739


    /**
     * Adds a header field with the specified string value.  If this is
     * called more than once, the current value will replace the previous value.
     * 
     * @param name The header field name.
     * @param s The field's string value.
     */
    protected void setHeader(byte[] name, String s) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"setHeader", " name --> " + name + " value --> " + s + " ["+this+"]");
        }

        // d151464 - check the include flag
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"setHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "setHeader name --> " + name + " value --> " + s);  //311717
            }
        }
        else {
            // make sure we don't have a null name...
            if (name == null) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.exiting(CLASS_NAME,"setHeader","name is null");
                }
                return;
            }

            // d128646 - if we're not ignoring state errors and response is committed or closed, throw an exception
            if (!_ignoreStateErrors && isCommitted()) {
                
                // log a warning (only the first time)...ignore headers set after response is committed
                IServletWrapper wrapper = dispatchContext.getCurrentServletReference();
                if (logWarningActionNow(wrapper)) {
                    logAlreadyCommittedWarning(new Throwable(), "setHeader");
                } else {
                    logger.logp(Level.FINE, CLASS_NAME,"setHeader", "Cannot set header.  Response already committed.");
                }

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.exiting(CLASS_NAME,"setHeader","Response already committed");
                }
                return;
            }

            if (s == null) {
                removeHeader(name);
            }
            else {
                if (Arrays.equals(name, HEADER_CONTENT_TYPE_BYTES)) {
                    // need to specially handle the content-type header
                    String value = s.toLowerCase();
                    int index = value.indexOf("charset=");
                    if (index != -1) {
                        _encoding = s.substring(index + 8);
                        s = s.substring(0, index) + "charset=" + _encoding;
                    }
                    else {
                        if (dispatchContext.isAutoRequestEncoding()) {  //306998.15
                            // only set default charset if auto response encoding is true.
                            // otherwise cts test will fail.
                            if (value.endsWith(";")) {
                                s = s + "charset=" + getCharacterEncoding();
                            }
                            else {
                                s = s + ";charset=" + getCharacterEncoding();
                            }
                        }
                    }
                    _contentType = s;
                }

                _response.setHeader(name, s.getBytes());
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"setHeader");
        }
    }

    /**
     * Adds a header field with the specified integer value.  If this is
     * called more than once, the current value will replace the previous value.
     * 
     * @param name The header field name.
     * @param i The field's integer value.
     */
    public void setIntHeader(String name, int i) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"setIntHeader", " name --> " + name + " value --> " + String.valueOf(i)+ " ["+this+"]");
        }
        // d151464 - check the include flag
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"setIntHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "setIntHeader name --> " + name + " value --> " + String.valueOf(i));  //311717
            }
        }
        else {
            // d128646 - if we're not ignoring state errors and response is committed or closed, throw an exception
            if (!_ignoreStateErrors && isCommitted()) {
                
                // log a warning (only the first time)...ignore headers set after response is committed
                IServletWrapper wrapper = dispatchContext.getCurrentServletReference();
                if (logWarningActionNow(wrapper)) {
                    logAlreadyCommittedWarning(new Throwable(), "setIntHeader");
                } else {
                    logger.logp(Level.FINE, CLASS_NAME,"setIntHeader", "Cannot set header.  Response already committed.");
                }

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.exiting(CLASS_NAME,"setIntHeader : Response already committed.");
                }
                return;
            }

            _response.setIntHeader(name, i);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"setIntHeader");
        }
    }

    /**
     * Sets the status code and a default message for this response.
     * 
     * @param status The status code.
     */
    public void setStatus(int status) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"setStatus", " status --> " + String.valueOf(status)+ " ["+this+"]");
        }
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"setStatus", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "setStatus status --> " + String.valueOf(status));  //311717
            }
        }
        else {
            if (!_ignoreStateErrors && isCommitted()) {
                logger.logp(Level.WARNING, CLASS_NAME,"setStatus", "Cannot.set.status.Response.already.committed");
            }
            else {
                if (WCCustomProperties.RESET_BUFFER_ON_SET_STATUS)
                    resetBuffer();
                _response.setStatusCode(status);
                _statusCode = status;
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"setStatus");
        }
    }

    /**
     * Sets the status code and message for this response.
     * 
     * @param status The status code.
     * @param reason The status message.
     */
    public void setStatus(int status, String reason) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"setStatus", " status --> " + String.valueOf(status) + " reason --> " + reason + " ["+this+"]");
        }
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"setStatus", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "setStatus status --> " + String.valueOf(status) + " reason --> " + reason);  //311717
            }
        }
        else {
            if (!_ignoreStateErrors && isCommitted()) {
                logger.logp(Level.WARNING, CLASS_NAME,"setStatus", "Cannot.set.status.Response.already.committed");
            }
            else {
                if (WCCustomProperties.RESET_BUFFER_ON_SET_STATUS)
                    resetBuffer();
                _response.setStatusCode(status);
                _response.setReason(reason);
                _statusCode = status;
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"setStatus");
        }
    }

    public int getStatusCode() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"getStatusCode","statusCode = "+_statusCode,"["+this+"]");
        }
        return _statusCode;
    }

    public void setLocale(Locale loc) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"setLocale", " locale --> " + (loc!=null?loc.toString():"") + " ["+this+"]");  //321485
        }
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.exiting(CLASS_NAME,"setLocale", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"));  //311717
            }
            return;
        }

        _locale = loc;

        if (isCharEncodingSet() || _gotWriter || _headersWritten) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.exiting(CLASS_NAME,"setLocale","_gotWriter="+String.valueOf(_gotWriter)+", _headersWritten="+String.valueOf(_headersWritten));  //311717
            }
            return;
        }

        // check if it has been explicitly set using the setCharacterEncoding() or setContentType()

        /**
         * Check the DD locale-endoding mappings to see if there is a specified mapping
         * @since Servlet 2.4
         */

        isCharEncodingExplicit = false;
        _encoding = dispatchContext.getWebApp().getConfiguration().getLocaleEncoding(_locale);

        if (_encoding == null) {
            _encoding = EncodingUtils.getEncodingFromLocale(_locale);
            if (_encoding == null) {
                _encoding = _defaultEncoding;
            }
        }

        String contentType = getHeader(WebContainerConstants.HEADER_CONTENT_TYPE);

        if (contentType!=null) {
            int index = contentType.indexOf("charset=");
            if (index != -1)
                contentType = contentType.substring(0, index) + "charset=" + _encoding;
            else {
                contentType = contentType + "; charset=" + _encoding;
            }
            _response.setContentType(contentType);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"setLocale", "contentType = " + this._contentType);  //321485
        }
    }

    /**
     * @return
     */
    private boolean isCharEncodingSet()
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"isCharEncodingSet", "_locale = " + _locale+ " ["+this+"]");
        
        String contentType = getHeader(WebContainerConstants.HEADER_CONTENT_TYPE);
        if (contentType!=null) {
            int index = contentType.indexOf("charset=");
            if (index != -1) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
                    logger.logp(Level.FINE, CLASS_NAME,"isCharEncodingSet", String.valueOf(isCharEncodingExplicit));
                return isCharEncodingExplicit;
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"isCharEncodingSet", "false");
        return false;

    }

    public Locale getLocale() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"getLocale", "_locale = " + _locale+ " ["+this+"]");
        if (_locale == null) {
            setDefaultResponseEncoding();
        }
        return _locale;
    }

    public void addDateHeader(String name, long date) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.entering(CLASS_NAME,"addDateHeader", "name --> " + name + " date --> " + date+ " ["+this+"]");

        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"addDateHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "addDateHeader name --> " + name + " value --> " + String.valueOf(date));  //311717
            }
        }
        else {
            if (!_ignoreStateErrors && isCommitted()) {

                // log a warning (only the first time)...ignore headers set after response is committed
                IServletWrapper wrapper = dispatchContext.getCurrentServletReference();
                if (logWarningActionNow(wrapper)) {
                    logAlreadyCommittedWarning(new Throwable(), "addDateHeader");
                } else {
                    logger.logp(Level.FINE, CLASS_NAME,"addDateHeader", "Cannot set header.  Response already committed.");
                }
                
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
                    logger.exiting(CLASS_NAME,"addDateHeader","Response already committed");        
                return;
            }

            if (name.equalsIgnoreCase(WebContainerConstants.HEADER_CONTENT_TYPE)) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
                    logger.exiting(CLASS_NAME,"addDateHeader","throw IllegalArgumentException");        
                throw new IllegalArgumentException("Cannot Set Content-Type to a Date value");
            }
            else {
                setHeader(name, (createCompliantHttpDateString(date)));
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.exiting(CLASS_NAME,"addDateHeader");        
    }

    public void addHeader(String name, String value) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"addHeader", "name --> " + name + " with value --> " + value+ " ["+this+"]");
        }
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"addHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "addHeader --> " + name + " with value --> " + value);  //311717
            }
        }
        else {
            if (!_ignoreStateErrors && isCommitted()) {
                
                // log a warning (only the first time)...ignore headers set after response is committed
                IServletWrapper wrapper = dispatchContext.getCurrentServletReference();
                if (logWarningActionNow(wrapper)) {
                    logAlreadyCommittedWarning(new Throwable(), "addHeader");
                } else {
                    logger.logp(Level.FINE, CLASS_NAME,"addHeader", "Cannot set header.  Response already committed.");
                }

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
                    logger.exiting(CLASS_NAME,"addHeader","Response already committed");        
                return;
            }

            if (name.equalsIgnoreCase(WebContainerConstants.HEADER_CONTENT_TYPE)) {
                _response.setContentType(value);
            }
            else {
                addField(name, value);
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.exiting(CLASS_NAME,"addHeader");        
    }

    public void addHeader(byte[] name, byte[] value) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"addHeader", "name(byte) --> " + (name!=null?new String(name):"") + " with value --> " + (value!=null?new String(value):"")+ " ["+this+"]");
        }
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"addHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "addHeader(byte) --> " + (name!=null?new String(name):"") + " with value --> " + (value!=null?new String(value):""));  //311717
            }
        }
        else {
            if (!_ignoreStateErrors && isCommitted()) {
                // log a warning (only the first time)...ignore headers set after response is committed
                IServletWrapper wrapper = dispatchContext.getCurrentServletReference();
                if (logWarningActionNow(wrapper)) {
                    logAlreadyCommittedWarning(new Throwable(), "addHeader");
                } else {
                    logger.logp(Level.FINE, CLASS_NAME,"addHeader", "Cannot set header.  Response already committed.");
                }

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
                    logger.exiting(CLASS_NAME,"addHeader","Response already committed");        
                return;
            }

            _response.addHeader(name, value);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.exiting(CLASS_NAME,"addHeader");        
    }

    public void addIntHeader(String name, int value) {
        // 311717
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"addIntHeader", " name --> " + name + " value --> " + String.valueOf(value)+" ["+this+"]");
        }
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"addIntHeader", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "addIntHeader name --> " + name + " value --> " + String.valueOf(value));  //311717
            }
        }
        else {

            // d128646 - if we're not ignoring state errors and response is committed or closed, throw an exception
            if (!_ignoreStateErrors && isCommitted()) {
                // log a warning (only the first time)...ignore headers set after response is committed
                IServletWrapper wrapper = dispatchContext.getCurrentServletReference();
                if (logWarningActionNow(wrapper)) {
                    logAlreadyCommittedWarning(new Throwable(), "addIntHeader");
                } else {
                    logger.logp(Level.FINE, CLASS_NAME,"addIntHeader", "Cannot set header.  Response already committed.");
                }

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
                    logger.exiting(CLASS_NAME,"addIntHeader","Response already committed");        
                return;
            }

            if (name.equalsIgnoreCase(WebContainerConstants.HEADER_CONTENT_TYPE)) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
                    logger.exiting(CLASS_NAME,"addIntHeader","throw IllegalArgumentException");        
                throw new IllegalArgumentException("Cannot Set Content-Type to an Int value");
            }
            else {
                addIntField(name, value);
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.exiting(CLASS_NAME,"addIntHeader");        
    }

    public void addSessionCookie(Cookie cookie) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.entering(CLASS_NAME,"addSessionCookie", "cookie --> " + cookie.getName() + " ["+this+"]");

        if (isCommitted()) {
            // log a warning...ignore headers set after response is committed
            // Servlet 2.5
            WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
            if (!getSessionCompatibility&&dispatchContext.isInclude()&&dispatchContext.getWebApp().getVersionID()>=25) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
                    logger.exiting(CLASS_NAME,"addSessionCookie","throw IllegalArgumentException");        
                throw new IllegalStateException();
            }
            else {
                logger.logp(Level.WARNING, CLASS_NAME,"addSessionCookie", "Cannot.set.session.cookie.Response.already.committed");
            }
        }
        else {
            _response.addCookie(cookie);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.exiting(CLASS_NAME,"addSessionCookie");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.core.Response#initForNextResponse(com.ibm.ws.webcontainer.channel.IWCCResponse)
     */
    public void initForNextResponse(IResponse resp) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.entering(CLASS_NAME,"initForNextResponse", "resp = " + resp + " ["+this+"]");

        if (resp == null) {
            _rawOut.init(null);
            _bufferedWriter.clean();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
                logger.exiting(CLASS_NAME,"initForNextResponse");
            return;
        }

        // reset the state before initializing it again and before calling setCommonHeaders.
        resetState();

        resp.setStatusCode(200);
        resp.setReason(REASON_OK);
        _statusCode = 200;

        // _responseContext.set(new SRTServletResponseContext());
        this._response = resp;
        
        setCommonHeaders();
        
        try
        {
            _rawOut.init(_response.getOutputStream());
        }
        catch (IOException e) {
            logger.logp(Level.SEVERE, CLASS_NAME,"initForNextResponse", "error.initializing.output.stream", e);  /*@283348.1*/
        }
        // LIBERTY _bufferedOut.reset();
        _bufferedWriter.reset();
        // LIBERTY _responseBuffer = null;
        // PK53885 start
        _bufferSize = DEFAULT_BUFFER_SIZE;       
        this._bufferedOut = createOutputStream(DEFAULT_BUFFER_SIZE); // LIBERTY
        if(this._bufferedOut instanceof WCOutputStream){
            (((WCOutputStream) this._bufferedOut).getOutput()).setObserver(this);
         }
        _encoding = null;
        _gotOutputStream = false;
        _gotWriter = false;
        this._pwriter = null;

        // Pk53885 end

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.exiting(CLASS_NAME,"initForNextResponse");

    }
// Added by V8 merge:
    private void setCommonHeaders() {
        String methodName = "setCommonHeaders";
        String xPoweredByValue = null;

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, "DISABLE_X_POWERED_BY [ " + WCCustomProperties.DISABLE_X_POWERED_BY + " ]");
        }

        if (!WCCustomProperties.DISABLE_X_POWERED_BY){
            if (WCCustomProperties.X_POWERED_BY==null){
                xPoweredByValue = getXPoweredbyHeader();
            }
            else {
                xPoweredByValue = WCCustomProperties.X_POWERED_BY;
            }
            if (xPoweredByValue!=null)
                setHeader(WebContainerConstants.X_POWERED_BY_KEY,xPoweredByValue);
        }
    }

    /*
     * Return the default "X-Powered-By" header value for Servlet 3.0
     */
    protected String getXPoweredbyHeader() {
       return WebContainerConstants.X_POWERED_BY_DEFAULT_VALUE;
    }
    
    public Vector[] getHeaderTable() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"getHeaderTable","["+this+"]");
        return _response.getHeaderTable();
    }

    public void destroy(){
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE,CLASS_NAME,"destroy","["+this+"]");
        }
        this._connContext = null;
        this._rawOut = null;
        this._bufferedOut = null;
        this._bufferedWriter = null;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"destroy", " exit");
        }
    }

        protected ServletOutputStream createOutputStream(int size)  {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                logger.logp(Level.FINE, CLASS_NAME,"createOutputStream","size : "+ size +" ["+this+"]");
            }
    
            // TODO: Liberty - Changed to CoyoteOutputStream
            // return new CoyoteOutputStream(size);
            // return new BufferedServletOutputStream(size);
            try
            {
                return _response.getOutputStream();
            }
            catch (IOException e)
            {
                FFDCFilter.processException(e, this.getClass().getName() + ".createOutputStream", "2261");
            }
            return null;
        }

    protected String createCompliantHttpDateString(long timestamp)  {
        SimpleDateFormat tmpDateFmt = dateFormat.get();
        if (tmpDateFmt==null){

            //PM25931 Start
            if (!localeDependentDateFormatter ){
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                    logger.logp(Level.FINE, CLASS_NAME,"createCompliantHttpDateString", "use english date formatter");
                }
                Locale locale1 = new Locale("en","US");
                tmpDateFmt = new SimpleDateFormat(formatStr,locale1);
            }
            else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                    logger.logp(Level.FINE, CLASS_NAME,"createCompliantHttpDateString", 
                                "use localeDependentDateFormatter, JVM locale-> " + Locale.getDefault().toString());
                }
                tmpDateFmt = new SimpleDateFormat(formatStr);
            } 
            // PM25931 End
            tmpDateFmt.setTimeZone(gmtTimeZone);
            dateFormat.set(tmpDateFmt);  
        }


        Date myDate = new Date (timestamp);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"createCompliantHttpDateString", "createCompliantHttpDateString result->"+tmpDateFmt.format(myDate));
        }

        return (tmpDateFmt.format(myDate));

    }


    public static void main (String []  args){
        SRTServletResponse srtRes = new SRTServletResponse();
        srtRes.setContentType("text/html;charset='ISO-8859-1'");
        srtRes._gotWriter = true;
        srtRes._headersWritten = true;
        srtRes.setContentType("text/xml;charset=ISO-8859-7");
        System.out.println(srtRes.getContentType());

        srtRes = new SRTServletResponse();
        srtRes.setCharacterEncoding("ISO-8859-1");
        System.out.println(srtRes.getContentType());

        srtRes = new SRTServletResponse();
        srtRes.setCharacterEncoding("UTF-8");
        srtRes.setContentType(("text/html"));

        try {
            srtRes.getWriter();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        srtRes._gotWriter = true;
        srtRes._headersWritten = true;
        srtRes.setContentType(("text/xml"));
        System.out.println(srtRes.getContentType());

        srtRes = new SRTServletResponse();

        try {
            srtRes.getWriter();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        srtRes._gotWriter = true;
        srtRes._headersWritten = true;
        srtRes.setContentType(("text/xml;charset=UTF-8"));
        System.out.println(srtRes.getContentType());

        srtRes = new SRTServletResponse();
        srtRes.setContentType(("text/html"));
        srtRes.setCharacterEncoding("UTF-8");
        System.out.println(srtRes.getContentType());

        srtRes = new SRTServletResponse();
        try {
            srtRes.getWriter();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block

        } catch (IOException e) {
            // TODO Auto-generated catch block

        }
        System.out.println(srtRes.getContentType());
    }

    public void fireOutputStreamRetrievedEvent(ServletOutputStream sos) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"fireOutputStreamRetrievedEvent","_firstOutputStreamRetrieval:"+String.valueOf(_firstOutputStreamRetrieval)+" ,outputMethodListener:"+outputMethodListener,"["+this+"]");
        if (outputMethodListener!=null&&_firstOutputStreamRetrieval){
            _firstOutputStreamRetrieval = false;
            outputMethodListener.notifyOutputStreamRetrieved(sos);
        }
    }

    public void fireWriterRetrievedEvent(PrintWriter pw) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"fireWriterRetrievedEvent","__firstWriterRetrieval:"+String.valueOf(_firstWriterRetrieval)+" ,outputMethodListener:"+outputMethodListener,"["+this+"]");
        if (outputMethodListener!=null&&_firstWriterRetrieval){
            _firstWriterRetrieval = false;
            outputMethodListener.notifyWriterRetrieved(pw);
        }
    }

    public void registerOutputMethodListener(IOutputMethodListener listener) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"registerOutputMethodListener","listener = "+ listener,"["+this+"]");
        this.outputMethodListener = listener;
    }

    public static void setSkipInputStreamRead(boolean b) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"setSkipInputStreamRead","b = "+ String.valueOf(b));
        skipInputStreamRead = b;
    }

    public static boolean isSkipInputStreamRead() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"isSkipInputStreamRead","skipInputStreamRead = "+ String.valueOf(skipInputStreamRead));
        return skipInputStreamRead;
    }

    public boolean isOutputWritten() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"isOutputWritten","_firstWrite = "+ String.valueOf(_firstWrite),"["+this+"]");           
        return _firstWrite;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return ((WrappingEnumeration)_response.getHeaderNames()).getTargetCollection();
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return ((WrappingEnumeration)_response.getHeaders(name)).getTargetCollection();
    }

    @Override
    public int getStatus(){
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"getStatus","statusCode = "+_statusCode,"["+this+"]");
        }    
        return this._statusCode;
    }

    @Override
    public void removeCookie(String cookieName) {
        if (isCommitted()) {
            throw new IllegalStateException("cannot remove cookie if the response is committed");
        }
        else {
            _response.removeCookie(cookieName);
        }

    }

    /* (non-Javadoc)
     * @see com.ibm.ws.http.channel.outstream.HttpOutputStreamObserver#alertOSFirstFlush()
     */
    @Override
    public void alertOSFirstFlush() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
            logger.logp(Level.FINE, CLASS_NAME,"alertOSFirstFlush", "entry");
        
        if (!isCommitted()) {
            commit();
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
            logger.logp(Level.FINE, CLASS_NAME,"alertOSFirstFlush", "exit");
        
    }
}
