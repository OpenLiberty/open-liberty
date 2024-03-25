/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer61.srt;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.genericbnf.PasswordNullifier;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

import io.openliberty.webcontainer60.srt.SRTServletResponse60;
import io.openliberty.webcontainer61.osgi.srt.SRTConnectionContext61;
import io.openliberty.webcontainer61.osgi.webapp.WebAppDispatcherContext61;
import jakarta.servlet.http.HttpServletResponse;

public class SRTServletResponse61 extends SRTServletResponse60 implements HttpServletResponse {
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("io.openliberty.webcontainer61.srt");
    private static final String CLASS_NAME = SRTServletResponse61.class.getName();

    public SRTServletResponse61(SRTConnectionContext61 context) {
        super(context);
    }

    @Override
    public void initForNextResponse(IResponse resp) {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "initForNextRequest", "this [" + this + "] , resp [" + resp + "]");
        }
        super.initForNextResponse(resp);
    }

    /**
     * This method has no effect if {@code null} is passed for the {@code name} parameter.
     */
    @Override
    public void addDateHeader(String name, long date) {
        if (name == null) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "addDateHeader", "name is null; return [" + this + "]");
            }
            return;
        }
        super.addDateHeader(name, date);
    }

    /**
     * This method has no effect if {@code null} is passed for the {@code name} parameter.
     */
    @Override
    public void setDateHeader(String name, long date) {
        if (name == null) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "setDateHeader", "name is null; return. [" + this + "]");
            }
            return;
        }
        super.setDateHeader(name, date);
    }

    /**
     * This method has no effect if {@code null} is passed for either the {@code name} or {@code value} parameters.
     */
    @Override
    public void addHeader(String name, String value) {
        if (name == null || value == null) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "addHeader", "name or value is null; return. [" + this + "]");
            }
            return;
        }
        super.addHeader(name, value);
    }

    /**
     * This method has no effect if {@code null} is passed for the {@code name} parameter.
     * <p>
     * Passing {@code null} as the value removes all headers with the given name.
     *
     */
    @Override
    public void setHeader(String name, String value) {
        if (name == null) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "setHeader", "name is null; return. [" + this + "]");
            }
            return;
        }
        super.setHeader(name, value);
    }

    /**
     * This method has no effect if {@code null} is passed for the {@code name} parameter.
     */
    @Override
    public void addIntHeader(String name, int value) {
        if (name == null) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "addIntHeader", "name is null; return. [" + this + "]");
            }
            return;
        }
        super.addIntHeader(name, value);
    }

    /**
     * This method has no effect if {@code null} is passed for the {@code name} parameter.
     */
    @Override
    public void setIntHeader(String name, int value) {
        if (name == null) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "setIntHeader", "name is null; return. [" + this + "]");
            }
            return;
        }
        super.setIntHeader(name, value);
    }

    /**
     * @see jakarta.servlet.ServletResponse#setCharacterEncoding(java.nio.charset.Charset)
     */
    @Override
    public void setCharacterEncoding(Charset charset) {
        String encoding = charset.name();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
            logger.entering(CLASS_NAME, "setCharacterEncoding", "Charset encoding [" + encoding + "] [" + this + "]");
        }
        super.setCharacterEncoding(encoding);
    }

    /**
     * Sends a redirect response to the client using the specified redirect location URL with the status code
     * {@link #SC_FOUND} 302 (Found), clears the response buffer and commits the response. The response buffer will be
     * replaced with a short hypertext note as per RFC 9110.
     *
     * <p>
     * This method has no effect if called from an include.
     *
     * @param location the redirect location URL (may be absolute or relative)
     *
     * @exception IOException              If an input or output exception occurs
     * @exception IllegalArgumentException If a relative URL is given and cannot be converted into an absolute URL
     * @exception IllegalStateException    If the response was already committed when this method was called
     *
     * @see #sendRedirect(String, int, boolean)
     * 
     * TODO: Update official API doc link when it is available.
     */
    @Override
    public void sendRedirect(String location) throws IOException {
        sendRedirect(location, HttpServletResponse.SC_FOUND, true);
    }

    /**
     * Sends a redirect response to the client using the specified redirect location URL with the status code
     * {@link #SC_FOUND} 302 (Found), optionally clears the response buffer and commits the response. If the response buffer
     * is cleared, it will be replaced with a short hypertext note as per RFC 9110.
     *
     * <p>
     * This method has no effect if called from an include.
     *
     * @param location    the redirect location URL (may be absolute or relative)
     * @param clearBuffer if {@code true}, clear the buffer and replace it with the data set by this method otherwise retain
     *                        the existing buffer
     *
     * @exception IOException              If an input or output exception occurs
     * @exception IllegalArgumentException If a relative URL is given and cannot be converted into an absolute URL
     * @exception IllegalStateException    If the response was already committed when this method was called
     *
     * @see #sendRedirect(String, int, boolean)
     *
     * @since Servlet 6.1
     * 
     * TODO: Update official API doc link when it is available.
     */
    @Override
    public void sendRedirect(String location, boolean clearBuffer) throws IOException {
        sendRedirect(location, HttpServletResponse.SC_FOUND, clearBuffer);
    }

    /**
     * Sends a redirect response to the client using the specified redirect location URL and status code, clears the
     * response buffer and commits the response. The response buffer will be replaced with a short hypertext note as per RFC
     * 9110.
     *
     * <p>
     * This method has no effect if called from an include.
     *
     * @param location the redirect location URL (may be absolute or relative)
     * @param sc       the status code to use for the redirect
     *
     * @exception IOException              If an input or output exception occurs
     * @exception IllegalArgumentException If a relative URL is given and cannot be converted into an absolute URL
     * @exception IllegalStateException    If the response was already committed when this method was called
     *
     * @see #sendRedirect(String, int, boolean)
     *
     * @since Servlet 6.1
     * 
     * TODO: Update official API doc link when it is available.
     */
    @Override
    public void sendRedirect(String location, int sc) throws IOException {
        sendRedirect(location, sc, true);
    }

    /**
     * Sends a redirect response to the client using the specified redirect location URL and status code, optionally clears
     * the response buffer and commits the response. If the response buffer is cleared, it will be replaced with a short
     * hypertext note as per RFC 9110.
     *
     * <p>
     * This method has no effect if called from an include.
     *
     * <p>
     * This method accepts both relative and absolute URLs. Absolute URLs passed to this method are used as provided as the
     * redirect location URL. Relative URLs are converted to absolute URLs unless a container specific feature/option is
     * provided that controls whether relative URLs passed to this method are converted to absolute URLs or used as provided
     * for the redirect location URL. If converting a relative URL to an absolute URL then:
     * <ul>
     * <li>If the location is relative without a leading '/' the container interprets it as relative to the current request
     * URI.</li>
     * <li>If the location is relative with a leading '/' the container interprets it as relative to the servlet container
     * root.</li>
     * <li>If the location is relative with two leading '/' the container interprets it as a network-path reference (see
     * <a href="http://www.ietf.org/rfc/rfc3986.txt"> RFC 3986: Uniform Resource Identifier (URI): Generic Syntax</a>,
     * section 4.2 &quot;Relative Reference&quot;).</li>
     * </ul>
     *
     * <p>
     * If the response has already been committed, this method throws an IllegalStateException. After using this method, the
     * response should be considered to be committed and should not be written to.
     *
     * @param location    the redirect location URL (may be absolute or relative)
     * @param sc          the status code to use for the redirect
     * @param clearBuffer if {@code true}, clear the buffer and replace it with the data set by this method otherwise retain
     *                        the existing buffer
     *
     * @exception IOException              If an input or output exception occurs
     * @exception IllegalArgumentException If a relative URL is given and cannot be converted into an absolute URL
     * @exception IllegalStateException    If the response was already committed when this method was called
     *
     * @since Servlet 6.1
     * 
     * TODO: Update official API doc link when it is available.
     */
    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "sendRedirect",
                        "location [" + PasswordNullifier.nullifyParams(location) + "], sc [" + sc + "] , clearBuffer [" + clearBuffer + "] ;this [" + this + "]");
        }

        if (location == null) {
            throw new IllegalArgumentException(nls.getString("redirect.location.cannot.be.null"));
        }

        WebAppDispatcherContext61 dispatchContext = (WebAppDispatcherContext61) getRequest().getWebAppDispatcherContext();

        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "sendRedirect", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"),
                            "sendRedirect location --> " + PasswordNullifier.nullifyParams(location));
            }
        } else {
            dispatchContext.sendRedirect(location, sc, clearBuffer);
        }

        this.closeResponseOutput();

        if (!isCommitted() && !dispatchContext.isInclude()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "sendRedirect", " : Not committed, so write headers");

            commit();
            _response.setLastBuffer(true);
            _response.writeHeaders();
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "sendRedirect");
        }
    }
}
