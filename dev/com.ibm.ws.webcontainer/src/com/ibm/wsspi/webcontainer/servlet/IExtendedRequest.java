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
package com.ibm.wsspi.webcontainer.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

/**
 * 
 * 
 * IExtendedRequest is an spi for websphere additions to the standard
 * ServletRequest methods
 * 
 * @ibm-private-in-use
 * 
 * @since WAS7.0
 * 
 */
public interface IExtendedRequest extends HttpServletRequest {
    // public IProtocolHeader getRawHeaders();
    // public Hashtable getRawParameters();
    // public void setRawHeaders(IProtocolHeader h);
    // public void setRawParameters(Hashtable h);
    // public void setMethod(String method);

    // Httpsession helper methods
    /**
     * Returns incoming SSL session id of the request. Applicable only for
     * requests over ssl
     */
    public byte[] getSSLId();

    /**
     * Returns a cookie value as bytes
     */
    byte[] getCookieValueAsBytes(String cookieName);

    /**
     * Get the values for the cookie specified.
     * 
     * @param name
     *            the cookie name
     * @return List of values associated with this cookie name.
     */
    @SuppressWarnings("unchecked")
    List getAllCookieValues(String cookieName);

    /**
     * Sets sessionId that is being generated for this request
     */
    void setSessionId(String id);

    /**
     * returns sessionId that is being generated for this request
     */
    String getUpdatedSessionId();

    /**
     * Sets SessionAffinityContext for this request
     * 
     * @param SessionAffinityContext
     *            object
     */
    void setSessionAffinityContext(Object sac); // cmd LIDB4395

    /**
     * Get the SessionAffinityContext for this request
     * 
     * @return SessionAffinityContext object
     */
    Object getSessionAffinityContext(); // cmd LIDB4395

    /**
     * returns url with encoded session information of the incoming request
     */
    String getEncodedRequestURI();

    public void pushParameterStack();

    public void aggregateQueryStringParams(String additionalQueryString, boolean setQS);

    public void removeQSFromList();

    /**
     * @return
     */
    public String getQueryString();

    public void setQueryString(String qs);

    /**
     * Sets boolean used to indicate to session manager if collaborators are
     * running.
     */
    void setRunningCollaborators(boolean runningCollaborators); // PK01801

    /**
     * Returns boolean that indicates if collaborators are running. Used by
     * session manager when session security integration is enabled.
     */
    boolean getRunningCollaborators(); // PK01801

    public String getReaderEncoding();

    public IRequest getIRequest();

    public void attributeAdded(String key, Object newVal);

    public void attributeRemoved(String key, Object oldVal);

    public void attributeReplaced(String key, Object oldVal);

    // PQ94384
    public void addParameter(String name, String[] values);

    // PQ94384

    public void setMethod(String method);

    public void setWebAppDispatcherContext(IWebAppDispatcherContext ctx);

    public IWebAppDispatcherContext getWebAppDispatcherContext();

    public IExtendedResponse getResponse();

    public void setResponse(IExtendedResponse extResp);

    public void initForNextRequest(IRequest req);

    public void start();

    public void finish() throws ServletException;

    public void destroy();

    public String getPathInfo();

    public String getRequestURI();

    public void removeHeader(String header);

    public AsyncContext getAsyncContext();

    public void closeResponseOutput();

    public void setAsyncSupported(boolean asyncSupported);

    void finishAndDestroyConnectionContext();

    public void setDispatcherType(DispatcherType dispatcherType);

    public void setAsyncStarted(boolean b);

    //used by the security component to get at information on the request
    public HashMap getInputStreamData() throws IOException;
    public void setInputStreamData(HashMap inStreamInfo) throws IOException;

    // the following methods are used by the security component to serialize/deserialize the input stream data.
    /**
     * Serialize the Map object of InputStreamData.
     * The format is as follows:
     * byte[0][]    : byte array of long value of INPUT_STREAM_CONTENT_DATA_LENGTH
     * byte[1][]    : the length of INPUT_STREAM_CONTENT_TYPE
     * byte[2][]    : the byte array of the value of INPUT_STREAM_CONTENT_TYPE if the length is zero, it only contains one byte data of which value is zero.
     * byte[3...] : byte array of INPUT_STREAM_CONTENT_DATA (it could be multiple tWAS v9) byte[3] doesn't exist if the length is zero.
     */
    @SuppressWarnings("rawtypes")
    public byte[][] serializeInputStreamData(Map isd) throws IOException, UnsupportedEncodingException, IllegalStateException;
    @SuppressWarnings("rawtypes")
    public HashMap deserializeInputStreamData(byte[][] input) throws UnsupportedEncodingException, IllegalStateException;
    /** 
     * returns estimated size of serialized InputStreamData
     * this code does not consider that the length in long overwraps. 
     */
    @SuppressWarnings("rawtypes")
    public long sizeInputStreamData(Map isd) throws UnsupportedEncodingException, IllegalStateException;

    //used to set MultiRead custom property
    public void setValuesIfMultiReadofPostdataEnabled();
}
