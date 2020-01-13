/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Vector;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.cache.DynamicContentProvider;
import com.ibm.websphere.servlet.cache.ServletCacheResponse;
import com.ibm.ws.ffdc.FFDCFilter;                                      
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.util.IResponseOutput;

/**
 * This class is a proxy to the WebSphere response object.
 * It has features added to enable caching.
 */
public class CacheProxyResponse extends HttpServletResponseWrapper implements IResponseOutput, ServletCacheResponse {

    private static TraceComponent tc = Tr.register(CacheProxyResponse.class,
                                                   "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    protected FragmentComposer fragmentComposer = null;
    boolean _gotWriter = false;
    boolean _gotOutputStream = false;
    protected boolean composerActive = true;
    private boolean containsESIContent = false;
    boolean bufferOutput = false;
    CacheProxyWriter bufferedWriter = null;
    CacheProxyOutputStream bufferedOutputStream = null;
    CacheProxyWriter writer = null;
    CacheProxyOutputStream outputStream = null;
    boolean doneFlushBuffer = false;
    boolean ard = false; /* is this an ARD response */
    int statusCode = 0;

    /**
     * Constructor with parameter.
     * 
     * @param proxiedResponse The WebSphere response being proxied.
     */
    public CacheProxyResponse(HttpServletResponse proxiedResponse) {
        super(proxiedResponse);
    }

    /**
     * finished - for easier/faster garbage collection - break links
     * 
     */
    public void finished() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Releasing: " + this + " for GC");
        }
        fragmentComposer = null;
        bufferedWriter = null;
        bufferedOutputStream = null;
        composerActive = false;
    }

    public Vector[] getHeaderTable() {
        return ((IExtendedResponse) getResponse()).getHeaderTable();
    }

    protected void setBufferingOutput(boolean buffer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, " setBufferingOutput=" + buffer + " " + this);
        bufferOutput = buffer;
    }

    protected boolean isBufferingOutput() {
        return bufferOutput;
    }

    protected void flushOutput() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "flushOutput(), bufferedWriter is " + bufferedWriter +
                         ", bufferedOutputStream is " + bufferedOutputStream + " " + this);

        if (bufferedWriter != null && bufferedOutputStream != null) {
            throw new IllegalStateException("cannot buffer both text and bytes");
        }
        if (bufferedWriter != null) {
            CharArrayWriter writer = (CharArrayWriter) bufferedWriter.getWriter();
            writer.writeTo(getResponse().getWriter());
            writer.reset();
            bufferedWriter = null;
        } else if (bufferedOutputStream != null) {
            ByteArrayOutputStream stream = (ByteArrayOutputStream) bufferedOutputStream.getOutputStream();
            stream.writeTo(getResponse().getOutputStream());
            stream.reset();
            bufferedOutputStream = null;
        }
        bufferOutput = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "flushOutput()");
    }

    /**
     * This removes the last bytes from the output stream.
     * It is used by the XSP code.
     * 
     * @param length The number of bytes to remove from the end.
     */

    /**
     * This sets the FragmentComposer for this response object.
     * 
     */
    public void setFragmentComposer(FragmentComposer fragmentComposer) {
        this.fragmentComposer = fragmentComposer;
    }

    /**
     * This gets the FragmentComposer for this response object.
     * 
     * @return The FragmentComposer for this response object
     * 
     */
    public FragmentComposer getFragmentComposer() {
        return fragmentComposer;
    }

    /**
     * Sets whether or not this response object should capture side-effects
     * 
     */
    public void setComposerActive(boolean active) {
        composerActive = active;
    }

    /**
     * Returns whether or not this response object should capture side-effects
     * 
     */
    public boolean getComposerActive() {
        return composerActive;
    }

    @Override
    public void flushBuffer() throws IOException {
        doneFlushBuffer = true;
        if (!bufferOutput) {
            if (getResponse() instanceof IResponseOutput) {
                ((IResponseOutput) getResponse()).flushBuffer(true);
            } else {
                getResponse().flushBuffer();
            }
        }
    }

    public void flushBuffer(boolean flushToWire) throws IOException {
        doneFlushBuffer = true;
        if (!bufferOutput) {
            if (getResponse() instanceof IResponseOutput) {
                ((IResponseOutput) getResponse()).flushBuffer(flushToWire);
            } else {
                getResponse().flushBuffer();
            }
        }
    }

    @Override
    public boolean isCommitted() {
        boolean isCommitted = false;
        HttpServletResponse surrogateResponse = this;
        while (surrogateResponse instanceof HttpServletResponseWrapper) {
            if (surrogateResponse instanceof CacheProxyResponse) {
                CacheProxyResponse tempResponse = (CacheProxyResponse) surrogateResponse;
                if ((tempResponse.writer != null && tempResponse.writer.writerFlushed)
                                                || (tempResponse.outputStream != null && tempResponse.outputStream.outputStreamFlushed)) {
                    isCommitted = true;
                    break;
                }
            }
            surrogateResponse = (HttpServletResponse) ((HttpServletResponseWrapper) surrogateResponse).getResponse();

        }
        if (!isCommitted)
            return getResponse().isCommitted();
        else
            return isCommitted;
    }

    /**
     * This overrides the method in the WebSphere response object. It returns
     * the output stream from the fragmentComposer.
     * 
     * @return The output stream.
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getOutputStream: " + this);
        _gotOutputStream = true;
        outputStream = (CacheProxyOutputStream) fragmentComposer.getOutputStream();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getOutputStream: " + this + " returning: " + outputStream);
        return outputStream;
    }

    /**
     * This overrides the method in the WebSphere response object.
     * It returns the print writer from the fragmentComposer.
     * 
     * @return The print writer.
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        _gotWriter = true;
        if (fragmentComposer == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "fragmentComposer is null: " + this);
        } else {
            writer = (CacheProxyWriter) fragmentComposer.getPrintWriter();
        }
        return writer;
    }

    public ServletOutputStream getBufferedOutputStream() throws IOException {
        if (!bufferOutput)
            throw new IllegalStateException("shouldn't call getBufferedOutputStream() when not buffering output");
        //obtain the underlying outputstream now, to force illegalstateexception if not possible
        getResponse().getOutputStream();

        if (bufferedOutputStream == null) {
            if (ard) {
                bufferedOutputStream = new CacheProxyOutputStream(new ByteArrayOutputStream(), fragmentComposer);
            } else {
                bufferedOutputStream = new CacheProxyOutputStream(new ByteArrayOutputStream());
            }
        }

        return bufferedOutputStream;
    }

    public PrintWriter getBufferedWriter() throws IOException {
        if (!bufferOutput)
            throw new IllegalStateException("shouldn't call getBufferedWriter() when not buffering output");
        //obtain the underlying writer now, to force illegalstateexception if not possible
        getResponse().getWriter();

        if (bufferedWriter == null) {
            if (ard) {
                bufferedWriter = new CacheProxyWriter(new CharArrayWriter(), fragmentComposer);
            } else {
                bufferedWriter = new CacheProxyWriter(new CharArrayWriter()); //no ARD
            }
        }

        return bufferedWriter;
    }

    /**
     * resetBuffer
     * Clears the content of the underlying buffer in the response without
     * clearing headers or status code. If the response has been committed,
     * this method throws an IllegalStateException.
     */
    @Override
    public void resetBuffer() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "resetBuffer", this);

        if (composerActive)
            fragmentComposer.resetBuffer();

        super.resetBuffer();

        if (bufferedOutputStream != null) {
            ((ByteArrayOutputStream) bufferedOutputStream.getOutputStream()).reset();
        }
        if (bufferedWriter != null) {
            ((CharArrayWriter) bufferedWriter.getWriter()).reset();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "resetBuffer");
    }

    @Override
    public void reset() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "reset", this);
        resetBuffer();
        super.reset();
        ard = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "reset");
    }

    public boolean getContainsESIContent() {
        return containsESIContent;
    }

    public void setContainsESIContent(boolean b) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setContainsESIContent(" + b + "), containsESIContent is " + containsESIContent + ", parent is " + fragmentComposer.getParent());
        if (containsESIContent == b)
            return;
        containsESIContent = b;
        FragmentComposer fc = fragmentComposer.getParent();
        if (fc != null)
            fc.getResponse().setContainsESIContent(containsESIContent);
    }

    /**
     * setBufferSize
     * Sets the preferred buffer size for the body of the response. The servlet
     * container will use a buffer at least as large as the size requested. The
     * actual buffer size used can be found using getBufferSize.
     */
    @Override
    public void setBufferSize(int size) {
        if (composerActive)
            fragmentComposer.setBufferSize(size);
        super.setBufferSize(size);
    }

    /**
     * getBufferSize
     * Returns the actual buffer size used for the response. If no buffering
     * is used, this method returns 0.
     * 
     */
    @Override
    public int getBufferSize() {
        if (composerActive)
            return fragmentComposer.getBufferSize();
        return super.getBufferSize();
    }

    /**
     * This adds a header to the list of state that is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param key The header key.
     * @param value The header value.
     */
    @Override
    public void setHeader(String key, String value) {
        if (composerActive)
            fragmentComposer.setHeader(key, value, true);
        super.setHeader(key, value);
    }

    /**
     * This adds a header to the list of state that is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param key The header key.
     * @param value The header value.
     */
    @Override
    public void addHeader(String key, String value) {
        if (composerActive)
            fragmentComposer.setHeader(key, value, false);
        super.addHeader(key, value);
    }

    /**
     * This adds a cookie to the list of state that is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param cookie The cookie.
     */
    @Override
    public void addCookie(Cookie cookie) {
        if (composerActive)
            fragmentComposer.addCookie(cookie);
        super.addCookie(cookie);
    }

    /**
     * This adds a Dynamic Content Provider that will
     * generate dynamic content without executing its JSP.
     * 
     * @param dynamicContentProvider The DynamicContentProvider.
     */
    public void addDynamicContentProvider(DynamicContentProvider dynamicContentProvider) throws IOException {
        if (composerActive)
            fragmentComposer.addDynamicContentProvider(dynamicContentProvider);
    }

    /**
     * This adds a date header to the list of state that is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param name The date header name.
     * @param value The date header value.
     */
    @Override
    public void setDateHeader(String name, long value) {
        if (composerActive)
            fragmentComposer.setDateHeader(name, value, true);
        super.setDateHeader(name, value);
    }

    /**
     * This adds a date header to the list of state that is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param name The date header name.
     * @param value The date header value.
     */
    @Override
    public void addDateHeader(String name, long value) {
        if (composerActive)
            fragmentComposer.setDateHeader(name, value, false);
        super.addDateHeader(name, value);
    }

    /**
     * This adds a int header to the list of state that is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param name The int header name.
     * @param value The int header value.
     */
    @Override
    public void setIntHeader(String name, int value) {
        if (composerActive)
            fragmentComposer.setIntHeader(name, value, true);
        super.setIntHeader(name, value);
    }

    /**
     * This adds a int header to the list of state that is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param name The int header name.
     * @param value The int header value.
     */
    @Override
    public void addIntHeader(String name, int value) {
        if (composerActive)
            fragmentComposer.setIntHeader(name, value, false);
        super.addIntHeader(name, value);
    }

    /**
     * This adds a status code to the list of state that is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param status The status code.
     */
    @Override
    public void setStatus(int statusCode) {
        if (composerActive)
            fragmentComposer.setStatus(statusCode);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setStatus: statusCode=" + statusCode);
        this.statusCode = statusCode;
        super.setStatus(statusCode);
    }

    /**
     * This adds a status code with comment to the list of state that
     * is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param status The status code.
     * @param comment The status comment.
     * @deprecated
     */
    @Deprecated
    @Override
    public void setStatus(int statusCode, String comment) {
        if (composerActive)
            fragmentComposer.setStatus(statusCode, comment);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setStatus: statusCode=" + statusCode);
        this.statusCode = statusCode;
        super.setStatus(statusCode, comment);
    }

    /**
     * This adds a status code with comment to the list of state that
     * is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param status The status code.
     * @param comment The status comment.
     * @deprecated
     */
    @Deprecated
    @Override
    public void sendError(int statusCode) {
        if (composerActive)
            fragmentComposer.setStatus(statusCode);
        this.statusCode = statusCode;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "sendError: statusCode=" + statusCode);
        try {
            super.sendError(statusCode);
        } catch (IOException ex) {
            FFDCFilter.processException(ex, this.getClass().getName() + ".sendError()", "509", this);
        }
    }

    /**
     * This adds a status code with comment to the list of state that
     * is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param status The status code.
     * @param comment The status comment.
     * @deprecated
     */
    @Deprecated
    @Override
    public void sendError(int statusCode, String comment) {
        if (composerActive)
            fragmentComposer.setStatus(statusCode, comment);
        this.statusCode = statusCode;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "sendError: statusCode=" + statusCode);
        try {
            super.sendError(statusCode, comment);
        } catch (IOException ex) {
            FFDCFilter.processException(ex, this.getClass().getName() + ".sendError()", "532", this);
        }
    }

    /**
     * This adds a content length to the list of state that is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param contentLength The content length.
     */
    @Override
    public void setContentLength(int contentLength) {
        if (composerActive)
            fragmentComposer.setContentLength(contentLength);
        super.setContentLength(contentLength);
    }

    @Override
    public void setCharacterEncoding(String charEnc) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setCharacterEncoding: " + charEnc);
        super.setCharacterEncoding(charEnc);
        if (composerActive)
            fragmentComposer.setCharacterEncoding(charEnc);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setCharacterEncoding");
    }

    /**
     * This adds a content type to the list of state that is remembered just
     * prior to the execution of a JSP so that it can be executed
     * again without executing its parent JSP.
     * 
     * @param contentType The content type.
     */
    @Override
    public void setContentType(String contentType) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setContentType: " + contentType);
        super.setContentType(contentType);
        if (composerActive) {
            fragmentComposer.setContentType(contentType);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setContentType");
    }

    @Override
    public void setLocale(Locale locale) {
        super.setLocale(locale);
        if (composerActive) {
            fragmentComposer.setLocale(locale);
        }
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        ESISupport.handleESIPostProcessing(this, fragmentComposer.getFragmentInfo(), false);
        if (composerActive) {
            fragmentComposer.sendRedirect(location);
        }
        super.sendRedirect(location);
    }

    public void setDoNotConsume(boolean doNotConsume) {
        if (composerActive) {
            fragmentComposer.setDoNotConsume(doNotConsume);
        }
    }

    public boolean writerObtained() {
        return _gotWriter;
    }

    public boolean outputStreamObtained() {
        return _gotOutputStream;
    }

    public void setArd(boolean a) {
        this.ard = a;
    }
}