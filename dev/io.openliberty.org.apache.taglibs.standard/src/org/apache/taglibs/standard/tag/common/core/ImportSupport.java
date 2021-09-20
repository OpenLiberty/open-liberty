/*
 * Copyright (c) 1997-2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.taglibs.standard.tag.common.core;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.BodyTagSupport;
import jakarta.servlet.jsp.tagext.TryCatchFinally;

import org.apache.taglibs.standard.resources.Resources;

/**
 * <p>Support for tag handlers for &lt;import&gt;, the general-purpose
 * text-importing mechanism for JSTL 1.0.  The rtexprvalue and expression-
 * evaluating libraries each have handlers that extend this class.</p>
 *
 * @author Shawn Bayern
 */

public abstract class ImportSupport extends BodyTagSupport 
        implements TryCatchFinally, ParamParent {

    //*********************************************************************
    // Public constants
    
    /** <p>Valid characters in a scheme.</p>
     *  <p>RFC 1738 says the following:</p>
     *  <blockquote>
     *   Scheme names consist of a sequence of characters. The lower
     *   case letters "a"--"z", digits, and the characters plus ("+"),
     *   period ("."), and hyphen ("-") are allowed. For resiliency,
     *   programs interpreting URLs should treat upper case letters as
     *   equivalent to lower case in scheme names (e.g., allow "HTTP" as
     *   well as "http").
     *  </blockquote>
     * <p>We treat as absolute any URL that begins with such a scheme name,
     * followed by a colon.</p>
     */
    public static final String VALID_SCHEME_CHARS =
	"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+.-";

    /** Default character encoding for response. */
    public static final String DEFAULT_ENCODING = "ISO-8859-1";

    //*********************************************************************
    // Protected state

    protected String url;                         // 'url' attribute
    protected String context;			  // 'context' attribute
    protected String charEncoding;                // 'charEncoding' attrib.

    //*********************************************************************
    // Private state (implementation details)

    private String var;                 // 'var' attribute
    private int scope;			// processed 'scope' attribute
    private String varReader;           // 'varReader' attribute
    private Reader r;	 		// exposed reader, if relevant
    private boolean isAbsoluteUrl;	// is our URL absolute?
    private ParamSupport.ParamManager params;    // parameters
    private String urlWithParams;	// URL with parameters, if applicable

    //*********************************************************************
    // Constructor and initialization

    public ImportSupport() {
	super();
	init();
    }

    private void init() {
	url = var = varReader = context = charEncoding = urlWithParams = null;
	params = null;
        scope = PageContext.PAGE_SCOPE;
    }


    //*********************************************************************
    // Tag logic

    // determines what kind of import and variable exposure to perform 
    public int doStartTag() throws JspException {
	// Sanity check
	if (context != null
	        && (!context.startsWith("/") || !url.startsWith("/"))) {
	    throw new JspTagException(
		Resources.getMessage("IMPORT_BAD_RELATIVE"));
	}

	// reset parameter-related state
	urlWithParams = null;
	params = new ParamSupport.ParamManager();

	// check the URL
	if (url == null || url.equals(""))
	    throw new NullAttributeException("import", "url");

	// Record whether our URL is absolute or relative
	isAbsoluteUrl = isAbsoluteUrl();

	try {
	    // If we need to expose a Reader, we've got to do it right away
	    if  (varReader != null) {
	        r = acquireReader();
	        pageContext.setAttribute(varReader, r);
	    }
	} catch (IOException ex) {
	    throw new JspTagException(ex.toString(), ex);
	}

	return EVAL_BODY_INCLUDE;
    }

    // manages connections as necessary (creating or destroying)
    public int doEndTag() throws JspException {
        try {
	    // If we didn't expose a Reader earlier...
	    if (varReader == null) {
	        // ... store it in 'var', if available ...
	        if (var != null)
	            pageContext.setAttribute(var, acquireString(), scope);
                // ... or simply output it, if we have nowhere to expose it
	        else
	            pageContext.getOut().print(acquireString());
	    }
	    return EVAL_PAGE;
        } catch (IOException ex) {
	    throw new JspTagException(ex.toString(), ex);
        }
    }

    // simply rethrows its exception
    public void doCatch(Throwable t) throws Throwable {
	throw t;
    }

    // cleans up if appropriate
    public void doFinally() { 
        try {
	    // If we exposed a Reader in doStartTag(), close it.
	    if (varReader != null) {
		// 'r' can be null if an exception was thrown...
	        if (r != null)
		    r.close();
		pageContext.removeAttribute(varReader, PageContext.PAGE_SCOPE);
	    }
        } catch (IOException ex) {
	    // ignore it; close() failed, but there's nothing more we can do
        }
    }

    // Releases any resources we may have (or inherit)
    public void release() {
	init();
        super.release();
    }

    //*********************************************************************
    // Tag attributes known at translation time

    public void setVar(String var) {
	this.var = var;
    }

    public void setVarReader(String varReader) {
	this.varReader = varReader;
    }

    public void setScope(String scope) {
	this.scope = Util.getScope(scope);
    }


    //*********************************************************************
    // Collaboration with subtags

    // inherit Javadoc
    public void addParameter(String name, String value) {
	params.addParameter(name, value);
    }

    //*********************************************************************
    // Actual URL importation logic

    /*
     * Overall strategy:  we have two entry points, acquireString() and
     * acquireReader().  The latter passes data through unbuffered if
     * possible (but note that it is not always possible -- specifically
     * for cases where we must use the RequestDispatcher.  The remaining
     * methods handle the common.core logic of loading either a URL or a local
     * resource.
     *
     * We consider the 'natural' form of absolute URLs to be Readers and
     * relative URLs to be Strings.  Thus, to avoid doing extra work,
     * acquireString() and acquireReader() delegate to one another as
     * appropriate.  (Perhaps I could have spelled things out more clearly,
     * but I thought this implementation was instructive, not to mention
     * somewhat cute...)
     */

    private String acquireString() throws IOException, JspException {
        if (isAbsoluteUrl) {
            // for absolute URLs, delegate to our peer
            BufferedReader r = new BufferedReader(acquireReader());
            StringBuffer sb = new StringBuffer();
            int i;
            
            // under JIT, testing seems to show this simple loop is as fast
            // as any of the alternatives
            // 
            // gmurray71 : putting in try/catch/finally block to make sure the
            // reader is closed to fix a bug with file descriptors being left open
            try {
                while ((i = r.read()) != -1)
                    sb.append((char)i);
            } catch (IOException iox) {
              throw iox;
            } finally {
                r.close();
            }
            
            return sb.toString();
        } else { 
            // handle relative URLs ourselves
            
            // URL is relative, so we must be an HTTP request
            if (!(pageContext.getRequest() instanceof HttpServletRequest
                  && pageContext.getResponse() instanceof HttpServletResponse))
                throw new JspTagException(
                                          Resources.getMessage("IMPORT_REL_WITHOUT_HTTP"));
            
            // retrieve an appropriate ServletContext
            ServletContext c = null;
            String targetUrl = targetUrl();
            if (context != null)
                c = pageContext.getServletContext().getContext(context);
            else {
                c = pageContext.getServletContext();
                
                // normalize the URL if we have an HttpServletRequest
                if (!targetUrl.startsWith("/")) {
                    String sp = ((HttpServletRequest) 
                                 pageContext.getRequest()).getServletPath();
                    targetUrl = sp.substring(0, sp.lastIndexOf('/'))
                        + '/' + targetUrl;
                }
            }
            
            if (c == null) {
                throw new JspTagException(
                                          Resources.getMessage(
                                                               "IMPORT_REL_WITHOUT_DISPATCHER", context, targetUrl));
            }
            
            // from this context, get a dispatcher
            RequestDispatcher rd =
                c.getRequestDispatcher(stripSession(targetUrl));
            if (rd == null)
                throw new JspTagException(stripSession(targetUrl));
            
            // include the resource, using our custom wrapper
            ImportResponseWrapper irw = 
                new ImportResponseWrapper(pageContext);
            
            // spec mandates specific error handling form include()
            try {
                rd.include(pageContext.getRequest(), irw);
            } catch (IOException ex) {
                throw new JspException(ex);
            } catch (RuntimeException ex) {
                throw new JspException(ex);
            } catch (ServletException ex) {
                Throwable rc = ex.getRootCause();
                if (rc == null)
                    throw new JspException(ex);
                else
                    throw new JspException(rc);
            }
            
            // disallow inappropriate response codes per JSTL spec
            if (irw.getStatus() < 200 || irw.getStatus() > 299) {
                throw new JspTagException(irw.getStatus() + " " +
                                          stripSession(targetUrl));
            }
            
            // recover the response String from our wrapper
            return irw.getString();
        }
    }

    private Reader acquireReader() throws IOException, JspException {
        if (!isAbsoluteUrl) {
            // for relative URLs, delegate to our peer
            return new StringReader(acquireString());
        } else {
            // absolute URL
            String target = targetUrl();
            try {
                // handle absolute URLs ourselves, using java.net.URL
                URL u = new URL(target);
                URLConnection uc = u.openConnection();
                InputStream i = uc.getInputStream();
                
                // okay, we've got a stream; encode it appropriately
                Reader r = null;
                String charSet; 
                if (charEncoding != null && !charEncoding.equals("")) {
                    charSet = charEncoding;
                } else {
                    // charSet extracted according to RFC 2045, section 5.1
                    String contentType = uc.getContentType();
                    if (contentType != null) {
                        charSet = Util.getContentTypeAttribute(contentType, "charset");
                        if (charSet == null) charSet = DEFAULT_ENCODING;
                    } else {
                        charSet = DEFAULT_ENCODING;
                    }
                }
                try {
                    r = new InputStreamReader(i, charSet);
                } catch (Exception ex) {
                    r = new InputStreamReader(i, DEFAULT_ENCODING);
                }
                
                // check response code for HTTP URLs before returning, per spec,
                // before returning
                if (uc instanceof HttpURLConnection) {
                    int status = ((HttpURLConnection) uc).getResponseCode();
                    if (status < 200 || status > 299)
                        throw new JspTagException(status + " " + target);
                }
                return r;
            } catch (IOException ex) {
                throw new JspException(
                                       Resources.getMessage("IMPORT_ABS_ERROR", target, ex), ex);
            } catch (RuntimeException ex) {  // because the spec makes us
                throw new JspException(
                                       Resources.getMessage("IMPORT_ABS_ERROR", target, ex), ex);
            }
        }
    }

    /** Wraps responses to allow us to retrieve results as Strings. */
    private class ImportResponseWrapper extends HttpServletResponseWrapper {

	//************************************************************
	// Overview

	/*
	 * We provide either a Writer or an OutputStream as requested.
	 * We actually have a true Writer and an OutputStream backing
	 * both, since we don't want to use a character encoding both
	 * ways (Writer -> OutputStream -> Writer).  So we use no
	 * encoding at all (as none is relevant) when the target resource
	 * uses a Writer.  And we decode the OutputStream's bytes
	 * using OUR tag's 'charEncoding' attribute, or ISO-8859-1
	 * as the default.  We thus ignore setLocale() and setContentType()
	 * in this wrapper.
	 *
	 * In other words, the target's asserted encoding is used
	 * to convert from a Writer to an OutputStream, which is typically
	 * the medium through with the target will communicate its
	 * ultimate response.  Since we short-circuit that mechanism
	 * and read the target's characters directly if they're offered
	 * as such, we simply ignore the target's encoding assertion.
	 */

	//************************************************************
	// Data

	/** The Writer we convey. */
	private StringWriter sw = new StringWriter();

	/** A buffer, alternatively, to accumulate bytes. */
	private ByteArrayOutputStream bos = new ByteArrayOutputStream();

	/** A ServletOutputStream we convey, tied to this Writer. */
	private ServletOutputStream sos = new ServletOutputStream() {
        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }

        public void write(int b) throws IOException {
		    bos.write(b);
	    }

        public void flush() throws IOException {
            pageContext.getOut().write(getString());
            bos.reset();
        }
	};

	/** 'True' if getWriter() was called; false otherwise. */
	private boolean isWriterUsed;

	/** 'True if getOutputStream() was called; false otherwise. */
	private boolean isStreamUsed;

	/** The HTTP status set by the target. */
	private int status = 200;

        private PageContext pageContext;
	
	//************************************************************
	// Constructor and methods

	/** Constructs a new ImportResponseWrapper. */
	public ImportResponseWrapper(PageContext pageContext) {
            super((HttpServletResponse)pageContext.getResponse());
            this.pageContext = pageContext;
	}
	
	/** Returns a Writer designed to buffer the output. */
       public PrintWriter getWriter() throws IOException {
	    if (isStreamUsed)
		throw new IllegalStateException(
		    Resources.getMessage("IMPORT_ILLEGAL_STREAM"));
	    isWriterUsed = true;
	    return new PrintWriterWrapper(sw, pageContext.getOut());
	}
	
	/** Returns a ServletOutputStream designed to buffer the output. */
	public ServletOutputStream getOutputStream() {
	    if (isWriterUsed)
		throw new IllegalStateException(
		    Resources.getMessage("IMPORT_ILLEGAL_WRITER"));
	    isStreamUsed = true;
	    return sos;
	}

	/** Has no effect. */
	public void setContentType(String x) {
	    // ignore
	}

	/** Has no effect. */
	public void setLocale(Locale x) {
	    // ignore
	}

	public void setStatus(int status) {
	    this.status = status;
	}

	public int getStatus() {
	    return status;
	}

	/** 
	 * Retrieves the buffered output, using the containing tag's 
	 * 'charEncoding' attribute, or the tag's default encoding,
	 * <b>if necessary</b>.
         */
	// not simply toString() because we need to throw
	// UnsupportedEncodingException
	public String getString() throws UnsupportedEncodingException {
	    if (isWriterUsed)
		return sw.toString();
	    else if (isStreamUsed) {
		if (charEncoding != null && !charEncoding.equals(""))
		    return bos.toString(charEncoding);
		else
		    return bos.toString(DEFAULT_ENCODING);
	    } else
		return "";		// target didn't write anything
	}
    }

    private static class PrintWriterWrapper extends PrintWriter {

        private StringWriter out;
        private Writer parentWriter;

        public PrintWriterWrapper(StringWriter out, Writer parentWriter) {
            super(out);
            this.out = out;
            this.parentWriter = parentWriter;
        }

        public void flush() {
            try {
                parentWriter.write(out.toString());
                StringBuffer sb = out.getBuffer();
                sb.delete(0, sb.length());
            } catch (IOException ex) {
            }
        }
     }

    //*********************************************************************
    // Some private utility methods

    /** Returns our URL (potentially with parameters) */
    private String targetUrl() {
	if (urlWithParams == null)
	    urlWithParams = params.aggregateParams(url);
	return urlWithParams;
    }

    /**
     * Returns <tt>true</tt> if our current URL is absolute,
     * <tt>false</tt> otherwise.
     */
    private boolean isAbsoluteUrl() throws JspTagException {
        return isAbsoluteUrl(url);
    }


    //*********************************************************************
    // Public utility methods

    /**
     * Returns <tt>true</tt> if our current URL is absolute,
     * <tt>false</tt> otherwise.
     */
    public static boolean isAbsoluteUrl(String url) {
	// a null URL is not absolute, by our definition
	if (url == null)
	    return false;

	// do a fast, simple check first
	int colonPos;
	if ((colonPos = url.indexOf(":")) == -1)
	    return false;

	// if we DO have a colon, make sure that every character
	// leading up to it is a valid scheme character
	for (int i = 0; i < colonPos; i++)
	    if (VALID_SCHEME_CHARS.indexOf(url.charAt(i)) == -1)
		return false;

	// if so, we've got an absolute url
	return true;
    }

    /**
     * Strips a servlet session ID from <tt>url</tt>.  The session ID
     * is encoded as a URL "path parameter" beginning with "jsessionid=".
     * We thus remove anything we find between ";jsessionid=" (inclusive)
     * and either EOS or a subsequent ';' (exclusive).
     */
    public static String stripSession(String url) {
	StringBuffer u = new StringBuffer(url);
        int sessionStart;
        while ((sessionStart = u.toString().indexOf(";jsessionid=")) != -1) {
            int sessionEnd = u.toString().indexOf(";", sessionStart + 1);
            if (sessionEnd == -1)
		sessionEnd = u.toString().indexOf("?", sessionStart + 1);
	    if (sessionEnd == -1) 				// still
                sessionEnd = u.length();
            u.delete(sessionStart, sessionEnd);
        }
        return u.toString();
    }
}
