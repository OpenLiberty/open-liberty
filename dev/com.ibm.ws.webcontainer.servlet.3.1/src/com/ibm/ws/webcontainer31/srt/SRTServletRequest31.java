/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.srt;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.commons.fileupload.disk.DiskFileItem;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet31.request.IRequest31;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.webcontainer.servlet.RequestUtils;
import com.ibm.ws.webcontainer.srt.SRTInputStream;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.srt.SRTServletRequestThreadData;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.osgi.srt.SRTConnectionContext31;
import com.ibm.ws.webcontainer31.osgi.webapp.WebApp31;
import com.ibm.wsspi.http.ee7.HttpInputStreamEE7Extended;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.util.WSServletInputStream;
import com.ibm.ws.transport.access.TransportConnectionUpgrade;

@SuppressWarnings("deprecation")
public class SRTServletRequest31 extends SRTServletRequest implements HttpServletRequest
{
    // Class level objects
    // =========================
    private final static TraceComponent tc = Tr.register(SRTServletRequest31.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);
    protected static final TraceNLS nls = TraceNLS.getTraceNLS(SRTServletRequest31.class, WebContainerConstants.NLS_PROPS);
    private static final String CLASS_NAME="com.ibm.ws.webcontainer31.srt.SRTServletRequest31";
    
    private boolean upgradeCalledAndSuccessful = false;
    private HttpUpgradeHandler httpUpgradeHandlerObject;
    
    public SRTServletRequest31(SRTConnectionContext31 context)
    {
        this._connContext = context;
        this._requestContext = new SRTRequestContext31(this);
        this._in = createInputStream();
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"SRTServletRequest", "this->"+this+": " + "inputStream is of type --> " + this._in);
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "clone");
        }
        if (WCCustomProperties.CHECK_REQUEST_OBJECT_IN_USE){
            checkRequestObjectInUse();
        }
        SRTServletRequest31 clonedSRTServletRequest = (SRTServletRequest31) super.clone();
        clonedSRTServletRequest.upgradeCalledAndSuccessful = this.upgradeCalledAndSuccessful;
        clonedSRTServletRequest.httpUpgradeHandlerObject = this.httpUpgradeHandlerObject;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "clone", "original -->" + this + " cloned -->" + clonedSRTServletRequest);
        }

        return clonedSRTServletRequest;
    }
    
    @Override
    public void initForNextRequest(IRequest req) {
        // 321485
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"initForNextRequest", "this->"+this+" : " + " req->"+req);
        }
        
        if (WCCustomProperties.CHECK_REQUEST_OBJECT_IN_USE){
                checkRequestObjectInUse();
        }

        try {

            if (req == null) {
                // MultiRead Start
                if(this.multiReadPropertyEnabled) {
                    if(this._in instanceof SRTInputStream) {
                        ((SRTInputStream) this._in).cleanupforMultiRead();
                    }
                    multiReadPropertyEnabled = false;
                    httpUpdatedwMultiReadValues = false;
                }
                _in.init(null);
                return;
            }

            _setInputStreamContentType = null; // PK57679
            _setInputStreamContentLength = -1; // PK57679
            _setInputDataStreamCalled = false; // 516233

            this._request = req;
            _srtRequestHelper = getRequestHelper();
            getRequestData().init(null);
            _in.init(_request.getInputStream());
            // begin 280584.1    SVT: StackOverflowError when installing app larger than 2GB    WAS.webcontainer    
            if( this.getContentLengthLong() > 0 ){            
                _in.setContentLength(this.getContentLengthLong());
            }
            //  end 280584.1    SVT: StackOverflowError when installing app larger than 2GB    WAS.webcontainer
        } catch (IOException e) {
      // shouldn't happen.
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(
                e,
                "com.ibm.ws.webcontainer.srt.SRTServletRequest.initForNextRequest",
                "828",
                this);
            //logger.logp(Level.SEVERE, CLASS_NAME,"initForNextRequest", "Error.Initializing.for.Next.Request", e);
        }

        this.upgradeCalledAndSuccessful = false;
    }


    @Override
    protected void cleanupFromFinish() {
        super.cleanupFromFinish();
        this.upgradeCalledAndSuccessful = false;
        
        // if cdi is enabled and there was an upgrade handler, then remove it from the cdi mapping
        // webapp is null if a request is accepted into WC but fail before any application is located for the request.
        WebApp31 webapp = (WebApp31) ((WebAppDispatcherContext) this.getDispatchContext()).getWebApp();
        if (webapp != null && webapp.isCDIEnabled() && httpUpgradeHandlerObject != null) {
            Map<Object, ManagedObject> contexts = webapp.getCdiContexts();
            if (contexts != null) {
                contexts.remove(httpUpgradeHandlerObject);
            }    
        }

        this.httpUpgradeHandlerObject = null;
    }


    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {

        WebApp31 webapp = (WebApp31) ((WebAppDispatcherContext) this.getDispatchContext()).getWebApp();
        T classInstance = null;

        if(handlerClass!= null){
            classInstance = webapp.createHttpUpgradeHandler(handlerClass);
            upgradeCalledAndSuccessful = true;
            httpUpgradeHandlerObject = classInstance;
        }
        else {
            throw new ServletException( nls.getString("handlerClass.is.null", webapp.getWebAppName()) );
        }
        
        //Check to make sure this is not a WebSockets upgrade
        if(!(httpUpgradeHandlerObject instanceof TransportConnectionUpgrade)){
            //In the upgrade call save off the thread information for use later
            //This is so we can restore at a later time and have JNDI lookup calls in an upgraded request
            
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"upgrade", "Saving of thread context data for later use");
            }
            
            WebContainerRequestState threadContextRequestState = WebContainerRequestState.getInstance(true);
            
            ThreadContextManager tcm = new ThreadContextManager();
            
            threadContextRequestState.setAttribute("ApplicationsOriginalTCM", tcm);
        }

        return classInstance;
    }

    public boolean isUpgradeInProgress() {
        return upgradeCalledAndSuccessful;
    }

    public HttpUpgradeHandler getHttpUpgradeHandler() {
        return httpUpgradeHandlerObject;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#getContentLengthLong()
     */
    @Override
    public long getContentLengthLong() {
        if (WCCustomProperties.CHECK_REQUEST_OBJECT_IN_USE){
            checkRequestObjectInUse();
        }

        long contentLength;
        if (!_setInputDataStreamCalled) {  
            contentLength = ((IRequest31)_request).getContentLengthLong();
        } else {
            contentLength = _setInputStreamContentLength;
        } 
          
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"getContentLengthLong", "this->"+this+": "+" length --> " + String.valueOf(contentLength));
        }
        return contentLength;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#changeSessionId()
     */
    @Override
    public String changeSessionId() {
        WebApp webapp = ((WebAppDispatcherContext) this._dispatchContext).getWebApp();
        
        HttpSession thisSession = ((SRTRequestContext31)_requestContext).generateNewId(webapp);
        
        return thisSession.getId();
    }

    @Override
    protected WSServletInputStream createInputStream()  {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "createInputStream entry");
        }
        if (WCCustomProperties.CHECK_REQUEST_OBJECT_IN_USE){
            checkRequestObjectInUse();
        }

        return new SRTInputStream31(this);
    }
    
    
    /**
     * 
     */
    public void finishKeepConnection() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "finishKeepConnection", "entry ["+this+"]");
        }
        if (WCCustomProperties.CHECK_REQUEST_OBJECT_IN_USE){
            checkRequestObjectInUse();
        }
        cleanupFromFinish();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "finishKeepConnection", " exit ["+this+"]");
        }
    }
    
    @Override
    protected Part morphIntoPart(DiskFileItem commonsFile) {
        if (WCCustomProperties.CHECK_REQUEST_OBJECT_IN_USE){
                checkRequestObjectInUse();
        }
        Part p = new SRTServletRequestPart31(commonsFile);
        return p;
    }
    
    @Override
    protected void prepareMultipart() throws ServletException, IOException {
        if(((SRTInputStream31) _in).getReadListener() != null) {
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
                logger.logp(Level.FINE, CLASS_NAME,"prepareMultipart", "Non-Blocking read already started on this InputStream , cannot parse again->" + _in);
            
            throw new IllegalStateException("An attempt to read or parse the post body failed because a non-blocking read event has already started");
        }
        super.prepareMultipart();
    }

    @Override
    protected Hashtable parsePostData() throws IOException {

        if(((SRTInputStream31) _in).getReadListener() != null) {
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
                logger.logp(Level.FINE, CLASS_NAME,"prepareMultipart", "Non-Blocking read already started on this InputStream , cannot parse again->" + _in);
            return null;
        }

        if( getContentLengthLong() > 0){

            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
                logger.logp(Level.FINE, CLASS_NAME,"parseParameters", "parsing post data based upon content length long");
            return  RequestUtils.parsePostDataLong(getContentLengthLong(), getInputStream(), getReaderEncoding(), this.multiReadPropertyEnabled);  // MultiRead
        } 
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  
            logger.logp(Level.FINE, CLASS_NAME,"parseParameters", "parsing post data based upon input stream (possibly chunked)");
        return RequestUtils.parsePostData(getInputStream(), getReaderEncoding(), this.multiReadPropertyEnabled);   // MultiRead
    }

    @Override
    public void setInputStreamData(HashMap inStreamInfo) throws IOException
    {
        if (WCCustomProperties.CHECK_REQUEST_OBJECT_IN_USE){
            checkRequestObjectInUse();
        }       
        if (getHelperGotReader())
        {
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"setInputStreamData","attempt to setInputStreamData after it has been read");
            throw new IllegalStateException(
                                            liberty_nls.getString(
                                                                  "Reader.already.obtained",
                                                            "Reader already obtained"));
        } else {

            _setInputDataStreamCalled = true; 

            if (inStreamInfo != null)
            {

                Long contentLength = (Long) inStreamInfo.get(INPUT_STREAM_CONTENT_DATA_LENGTH);

                if (contentLength==null) {

                    // security passed in a map that we did not create - should not happen
                    if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME,"setInputStreamData", "No content length in passed map. Throw IOException");
                    throw new IllegalStateException();

                } else {

                    _setInputStreamContentLength = contentLength.longValue();

                    _setInputStreamContentType = (String) inStreamInfo.get(INPUT_STREAM_CONTENT_TYPE);                  

                    byte[][] inStreamContentData = (byte[][])inStreamInfo.get(INPUT_STREAM_CONTENT_DATA);

                    if (inStreamContentData != null)
                    {

                        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME, "setInputStreamData", "SetInputStreamData Content Type = " + _setInputStreamContentType + ", " +
                                            "contentLength = " + _setInputStreamContentLength+ ", data array length = " + inStreamContentData.length+ " : this = " + this );

                        HttpInputStreamEE7Extended inStream = new HttpInputStreamEE7Extended(inStreamContentData); 
                        _in.init(inStream);
                        _in.setContentLength(_setInputStreamContentLength);
                        setHelperParameters(null);  //reset output of parseParameter method.

                    }
                }    
            } else {
                // securty passed didn't provide a map - should not happen
                if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "setInputStreamData","No map passed as input");
                throw new IllegalArgumentException();
            }
        }
    }

    public HashMap getInputStreamData() throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.entering(CLASS_NAME, "getInputStreamData");
            logger.logp(Level.FINE, CLASS_NAME,"getInputStreamData","[" + this + "]");
        }
        if (WCCustomProperties.CHECK_REQUEST_OBJECT_IN_USE){
            checkRequestObjectInUse();
        }

        ServletInputStream in = this.getInputStream();

        HashMap inStreamInfo = new HashMap();
        if (this.getContentType() != null)
        {
            inStreamInfo.put(INPUT_STREAM_CONTENT_TYPE, this.getContentType());
        } else {
            inStreamInfo.put(INPUT_STREAM_CONTENT_TYPE, null);
        }

        int offset = 0, inputLen = 0;
        long len = getContentLengthLong();
        long lenRead = 0;

        inStreamInfo.put(INPUT_STREAM_CONTENT_DATA_LENGTH, len);

        if (len>0) {        
            int bufferLen=0, MaxBufferSize = WCCustomProperties.SERVLET31_PRIVATE_BUFFERSIZE_FOR_LARGE_POST_DATA;
            long arraySize = len/MaxBufferSize;
            if (len%MaxBufferSize>0) arraySize++;

            if(arraySize > Integer.MAX_VALUE) {
                throw new IllegalArgumentException();   
            }

            byte[][] bytes = new byte[(int)arraySize][];
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "getInputStreamData","data length = " + Long.toString(len) + ", MaxBufferSize = " + MaxBufferSize + ", Array size = " + arraySize);

            for (int i= 0; len > lenRead ; i++)
            {

                if (len - lenRead >= MaxBufferSize) {
                    bufferLen = MaxBufferSize;
                } else {
                    bufferLen = (int)(len - lenRead);
                }
                bytes[i]  = new byte[bufferLen];

                if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "getInputStreamData","buffer " + i + " of length " + bufferLen + ", left to read = " + Long.toString(len-lenRead));
                offset = 0;
                do {
                    inputLen = in.read(bytes[i], offset, bufferLen - offset);
                    if (inputLen <= 0) {
                        String msg = nls.getString("post.body.contains.less.bytes.than.specified", "post body contains less bytes than specified by content-length");
                        throw new IOException(msg);
                    }
                    logger.logp(Level.FINE, CLASS_NAME, "getInputStreamData","read of " +  inputLen + " bytes.");
                    offset += inputLen;
                }
                while ((bufferLen - offset) > 0);  
                lenRead += bufferLen;
            }     
            inStreamInfo.put(INPUT_STREAM_CONTENT_DATA, bytes);
        } else {
            inStreamInfo.put(INPUT_STREAM_CONTENT_DATA, null);
        }

        if (in != null) {
            releaseInputStream();
        }

        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"getInputStreamData","ContentType = " + this.getContentType() + ", data length = " + len);
            logger.exiting(CLASS_NAME, "getInputStreamData");
        }

        return inStreamInfo;

    }

    /**
     * Serialize the Map object of InputStreamData.
     * The format is as follows:
     * byte[0][]    : byte array of long value of INPUT_STREAM_CONTENT_DATA_LENGTH
     * byte[1][]    : the length of INPUT_STREAM_CONTENT_TYPE
     * byte[2][]    : the byte array of the value of INPUT_STREAM_CONTENT_TYPE if the length is zero, it only contains one byte data of which value is zero.
     * byte[3...] : byte array of INPUT_STREAM_CONTENT_DATA (it could be multiple for servlet31 on Liberty) byte[3] doesn't exist if the length is zero.
     */
    @SuppressWarnings("rawtypes")
    public byte[][] serializeInputStreamData(Map isd) throws IOException, UnsupportedEncodingException, IllegalStateException {
        validateInputStreamData(isd);

        String type = (String)isd.get(INPUT_STREAM_CONTENT_TYPE);
        Long length = (Long)isd.get(INPUT_STREAM_CONTENT_DATA_LENGTH);
        byte[][] data = (byte [][])isd.get(INPUT_STREAM_CONTENT_DATA);
        int arraySize = OFFSET_CONTENT_DATA;
        if(data != null) {
            arraySize += data.length;
        }

        byte[][] output = new byte[arraySize][];
        output[OFFSET_CONTENT_DATA_LENGTH] = longToBytes((long)length.intValue());
        if (type != null) {
            output[OFFSET_CONTENT_TYPE_LEN] = intToBytes(type.length());
            output[OFFSET_CONTENT_TYPE_DATA] = type.getBytes("UTF-8"); 
        } else {
            output[OFFSET_CONTENT_TYPE_LEN] = intToBytes(0);
            output[OFFSET_CONTENT_TYPE_DATA] = new byte[1];
        }
        if (data != null) {
            for (int i = 0 ; i < data.length; i++) {
                output[OFFSET_CONTENT_DATA + i] = data[i];
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            String message = "total : " + sizeInputStreamData(isd) + " number of items : " + output.length;
            logger.logp(Level.FINE, CLASS_NAME,"serializeInputStreamData", message);
        }
        return output;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public HashMap deserializeInputStreamData(byte[][] input) throws UnsupportedEncodingException, IllegalStateException {
        if (input == null || input.length < 2) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"deseriallizeInputStreamData", "The input data is null or fewer items than the expected. ");
            throw new IllegalStateException("Invalid data.");
        }
        HashMap output = new HashMap();
        long length = bytesToLong(input[OFFSET_CONTENT_DATA_LENGTH]);
        output.put(INPUT_STREAM_CONTENT_DATA_LENGTH, Long.valueOf(length));
        int typeLen = bytesToInt(input[OFFSET_CONTENT_TYPE_LEN]);
        if (typeLen > 0) {
            output.put(INPUT_STREAM_CONTENT_TYPE, new String(input[OFFSET_CONTENT_TYPE_DATA], "UTF-8"));
        } else {
            output.put(INPUT_STREAM_CONTENT_TYPE, null);
        }
        int items = input.length;
        if (length > 0 && items > OFFSET_CONTENT_DATA) {
            byte [][] data = new byte [items - OFFSET_CONTENT_DATA][];
            for (int i = 0; i < (items - OFFSET_CONTENT_DATA); i++) {
                data[i] = input[OFFSET_CONTENT_DATA + i];
            }
            output.put(INPUT_STREAM_CONTENT_DATA, data);
        } else {
            output.put(INPUT_STREAM_CONTENT_DATA, null);
        }
        return output;
    }

    /** 
     * returns estimated size of serialized InputStreamData
     * this code does not consider that the length in long overwraps. 
     */
    @SuppressWarnings("rawtypes")
    public long sizeInputStreamData(Map isd) throws UnsupportedEncodingException, IllegalStateException {
        validateInputStreamData(isd);
        // The length of IMPUT_STREAM_CONTENT_TYPE won't exceed Integer.MAX_VALUE
        long size = LENGTH_INT + LENGTH_LONG;
        String type = (String)isd.get(INPUT_STREAM_CONTENT_TYPE);
        if (type != null) {
            size += type.getBytes("UTF-8").length;
        } else {
            size +=1; // if the size is zero, one byte data will be used for placeholder.
        }
        byte data[][] = (byte [][])isd.get(INPUT_STREAM_CONTENT_DATA);
        if (data != null) {
            for(int i = 0; i < data.length; i ++) {
                size += data[i].length;
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"sizeInputStreamData", Long.toString(size));
        return size;
    }

}
