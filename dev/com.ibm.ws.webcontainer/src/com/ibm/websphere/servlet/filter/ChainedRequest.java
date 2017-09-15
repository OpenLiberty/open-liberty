/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.servlet.request.ServletInputStreamAdapter;
import com.ibm.ws.webcontainer.servlet.RequestUtils;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.WCCustomProperties; //PM35450

/**
 *
 * This class adapts a response from a previous servlet/jsp into a request that may
 * be passed on for processing into another servlet/jsp.
 * 
 * The request object passed into the constructor should be the original request so
 * that state information can be preserved correctly (Deprecated since WebSphere 6.0). 
 * 
 * @deprecated Application developers requiring this functionality
 *  should implement this using javax.servlet.filter classes.
 *  
 * @ibm-api  
 */
public class ChainedRequest extends HttpServletRequestWrapper
{
protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.websphere.servlet.filter");
	private static final String CLASS_NAME="com.ibm.websphere.servlet.filter.ChainedRequest";
    
    private ChainedResponse _resp;
    private HttpServletRequest _req;
    private ServletInputStream _in;
    private BufferedReader _reader;
    private Hashtable _parameters = new Hashtable();
    private Hashtable _headers = new Hashtable(); //@bkm
    
    private static TraceNLS nls = TraceNLS.getTraceNLS(ChainedRequest.class, "com.ibm.ws.webcontainer.resources.Messages");

    private static boolean allowQueryParamWithNoEqual = WCCustomProperties.ALLOW_QUERY_PARAM_WITH_NO_EQUAL;       //PK35450

    ChainedRequest(ChainedResponse resp,
                   HttpServletRequest req) throws IOException
    {
    	super(req);
        _resp = resp;
        _req = req;
        ByteArrayInputStream bin = new ByteArrayInputStream(resp.getOutputBuffer());
        //System.out.println("Creating chained Request with buffer:\n"+new String(resp.getOutputBuffer())); //PK23010 
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))	                                                                    //PK23010
            logger.logp(Level.FINE, CLASS_NAME,"onServletUnloaded", "Creating chained Request with buffer:\n"+new String(resp.getOutputBuffer()));     //PK23010
        
        _in = new ServletInputStreamAdapter(bin);
    }

    public Cookie[] getCookies()
    {
        return _resp.getCookies();
    }
    
    public ServletInputStream getInputStream() throws IOException{
        return _in;
    }

    public BufferedReader getReader() throws IOException{
        if (_reader == null)
        {
            _reader = new BufferedReader(new InputStreamReader(_in, _resp.getCharacterEncoding()));
        }
        return _reader;
    }

    public HttpServletRequest getProxiedHttpServletRequest()
    {
        return _req;
    }

    //@bkma - added method
    protected void setHeader(String name,String value)
    {
        // make sure we don't have a null name...
        if (name == null)
        {
            return;
        }

        // if we have a null value, then remove entry from table
        if (value == null)
        {
            _headers.remove(name);
        }
        else
        {
            _headers.put(name,value);
        }
    }

    public Enumeration getHeaderNames()
    {
        return _headers.keys();  //@bkmc
    }

    public String getHeader(String name)
    {
        return(String)_headers.get(name); //@bkmc
    }

    public int getIntHeader(String name) throws NumberFormatException {
        String val = (String)_headers.get(name); //@bkma
        if (val==null) return -1;                //@bkma
        return Integer.valueOf(val).intValue();  //@bkmc
    }

    public long getDateHeader(String name)
    {
        String val = (String)_headers.get(name); //@bkma
        if (val==null) return -1;                //@bkma
        return Long.valueOf(val).longValue();    //@bkmc
    }

    public int getContentLength()
    {
        return getIntHeader("content-length");
    }

    public String getContentType()
    {
        return getHeader("content-type");
    }

    public String getParameter(String name)
    {
        if (_parameters == null)
        {
            parseParameters();
        }
        String[] values = (String[])_parameters.get(name);
        if (values != null && values.length > 0)
        {
            return values[0];
        }
        else
        {
            return null;
        }
    }

    public Enumeration getParameterNames()
    {
        if (_parameters == null)
        {
            parseParameters();
        }
        return _parameters.keys();
    }

    public String[] getParameterValues(String name)
    {
        if (_parameters == null)
        {
            parseParameters();
        }
        return(String[])_parameters.get(name);
    }

    synchronized private void parseParameters()
    {
        if (_parameters == null)
        {
            try
            {
                String method = getMethod().toLowerCase();
                if (method.equals("get"))
                {
                    String queryString = getQueryString();
                    if (queryString != null && ((queryString.indexOf("=") != -1)|| allowQueryParamWithNoEqual))//PM35450
                    {
                        _parameters = RequestUtils.parseQueryString(getQueryString());
                    }
                }
                else if (method.equals("post"))
                {
                    String contentType = getContentType();
                    if (contentType != null && contentType.equals("application/x-www-form-urlencoded"))
                    {
                        _parameters = parsePostData(getContentLength(), getInputStream());
                    }
                }
            }
            catch (IOException e)
            {
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.websphere.servlet.filter.ChainedResponse.parseParameters", "286", this);
            }
            if (_parameters == null)
            {
                _parameters = new Hashtable();
            }
        }
    }

    private Hashtable parsePostData(int len, ServletInputStream in)
    {
        int inputLen, offset;
        byte[] postedBytes = null;
        String postedBody;

        if (len <=0)
            return null;

        try
        {
            //
            // Make sure we read the entire POSTed body.
            //
            postedBytes = new byte [len];
            offset = 0;
            do
            {
                inputLen = in.read (postedBytes, offset, len - offset);
                if (inputLen <= 0)
                {
                    String msg = nls.getString("post.body.contains.less.bytes.than.specified","post body contains less bytes than specified by content-length");
                    throw new IOException (msg);
                }
                offset += inputLen;
            } while ((len - offset) > 0);

        }
        catch (IOException e)
        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.websphere.servlet.filter.ChainedResponse.parsePostData", "326", this);
            return new Hashtable();
        }

        // XXX we shouldn't assume that the only kind of POST body
        // is FORM data encoded using ASCII or ISO Latin/1 ... or
        // that the body should always be treated as FORM data.
        //

        try
        {
            postedBody = new String(postedBytes, _resp.getCharacterEncoding());
        }
        catch (UnsupportedEncodingException e)
        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.websphere.servlet.filter.ChainedResponse.parsePostData", "341", this);
            postedBody = new String(postedBytes);
        }
        return RequestUtils.parseQueryString(postedBody);
    }
}
