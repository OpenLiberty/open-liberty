/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.webcontainer60.srt;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer40.srt.SRTServletResponse40;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

import io.openliberty.webcontainer60.osgi.srt.SRTConnectionContext60;
import jakarta.servlet.http.HttpServletResponse;

public class SRTServletResponse60 extends SRTServletResponse40 implements HttpServletResponse {

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("io.openliberty.webcontainer60.srt");
    private static final String CLASS_NAME = SRTServletResponse60.class.getName();

    public SRTServletResponse60(SRTConnectionContext60 context) {
        super(context);
    }

    @Override
    public void initForNextResponse(IResponse resp) {
        super.initForNextResponse(resp);
    }

    @Override
    public void setCharacterEncoding(String encoding) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "setCharacterEncoding", "6.0 , encoding --> " + encoding + " [" + this + "]");
        }

        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (getRequest() != null && dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.exiting(CLASS_NAME, "setCharacterEncoding", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"));
            }
            return;
        }

        if (_gotWriter || this._headersWritten) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.exiting(CLASS_NAME, "setCharacterEncoding", "_gotWriter=" + String.valueOf(_gotWriter) + ", _headersWritten=" + String.valueOf(_headersWritten));
            }
            return;
        }

        if (encoding == null) {
            if (super.isCharEncodingSet() || isCharEncodingExplicitViaSetLocale) {
                _encoding = null;

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.exiting(CLASS_NAME, "setCharacterEncoding", "set encoding to null");
                }
                return;
            }
        }

        super.setCharacterEncoding(encoding);
    }

    @Override
    public void setContentType(String type) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "setContentType", "6.0 , type --> " + type + " [" + this + "]");
        }

        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.exiting(CLASS_NAME, "setContentType", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"));
            }
            return;
        }

        if (isCommitted()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.exiting(CLASS_NAME, "setContentType", "not set - response isCommitted");
            }
            return;
        }

        if (type == null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "setContentType", "null type");
            }

            _contentType = type;

            if (!writerObtained()) { //clear encoding if call before getWriter && _encoding has set explicitly via either setContentType or setLocale
                if (super.isCharEncodingSet() || isCharEncodingExplicitViaSetLocale) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, "setContentType", "set encoding to null");
                    }
                    _encoding = null;
                }
            }

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.exiting(CLASS_NAME, "setContentType", "null type");
            }

            return;
        }

        super.setContentType(type);
    }

    @Override
    public void setLocale(Locale loc) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "setLocale", "6.0,  locale --> " + (loc != null ? loc.toString() : "[null]") + " [" + this + "]");
        }

        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) getRequest().getWebAppDispatcherContext();
        if (dispatchContext.isInclude() == true) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.exiting(CLASS_NAME, "setLocale", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"));
            }
            return;
        }

        if (isCommitted()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.exiting(CLASS_NAME, "setLocale", "not set - response isCommitted");
            }
            return;
        }

        if (loc == null) {
            _locale = getRequest().getLocale();
            if (_locale == null) {
                _locale = _defaultLocale;
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "setLocale", "_locale is null: default to " + _locale.toString());
                }
            }

            /*
             * It does not set the response's character encoding if it is called after
             * {@link #setContentType} has been called with a charset specification, after {@link #setCharacterEncoding} has been
             * called, after <code>getWriter</code> has been called
             */
            if (writerObtained() || super.isCharEncodingSet()) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "setLocale", "not set encoding");
                }
            } else if (isCharEncodingExplicitViaSetLocale) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "setLocale", "set encoding to null");
                }
                _encoding = null;
            }

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.exiting(CLASS_NAME, "setLocale", " 6.0 null locale");
            }

            return;
        }

        super.setLocale(loc);
    }
}
