/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer61.osgi.webapp;

import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer40.osgi.webapp.WebApp40;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

import io.openliberty.webcontainer61.facade.ServletContextFacade61;
import jakarta.servlet.ServletContext;

public class WebApp61 extends WebApp40 implements ServletContext {
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("io.openliberty.webcontainer61.osgi.webapp");
    protected static final String CLASS_NAME = WebApp61.class.getName();

    public WebApp61(WebAppConfiguration webAppConfig,
                    ClassLoader moduleLoader,
                    ReferenceContext referenceContext,
                    MetaDataService metaDataService,
                    J2EENameFactory j2eeNameFactory,
                    ManagedObjectService managedObjectService) {
        super(webAppConfig, moduleLoader, referenceContext, metaDataService, j2eeNameFactory, managedObjectService);

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "constructor", "this [" + this + "]");
        }
    }

    @Override
    public WebAppDispatcherContext createDispatchContext() {
        return new io.openliberty.webcontainer61.osgi.webapp.WebAppDispatcherContext61(this);
    }

    @Override
    public ServletContext getFacade() {
        if (this.facade == null)
            this.facade = new ServletContextFacade61(this);
        return this.facade;
    }

    @Override
    public int getMajorVersion() {
        return 6;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    /*
     * @see jakarta.servlet.ServletContext#setRequestCharacterEncoding(Charset encoding)
     *
     * @since Servlet 6.1
     */
    @Override
    public void setRequestCharacterEncoding(Charset charset) {
        String encoding = charset.name();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "setRequestCharacterEncoding, Charset name [" + encoding + "]");
        }

        if (initialized) {
            throw new IllegalStateException(nls.getString("programmatic.sessions.already.been.initialized"));
        }

        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                                                                         nls.getString("Unsupported.op.from.servlet.context.listener"),
                                                                         new Object[] { "setRequestCharacterEncoding", lastProgAddListenerInitialized, getApplicationName() }));
        }

        if (encoding != null) {
            this.config.setModuleRequestEncoding(encoding);
        } else {
            String msg = nls.getFormattedMessage("unsupported.request.encoding.[{0}]", new Object[] { charset }, "Unsupported Charset specified --> " + charset);
            logger.logp(Level.SEVERE, CLASS_NAME, "setRequestCharacterEncoding", msg);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "setRequestCharacterEncoding");
        }
    }

    /*
     * @see jakarta.servlet.ServletContext#setResponseCharacterEncoding(Charset encoding)
     *
     * @since Servlet 6.1
     */
    @Override
    public void setResponseCharacterEncoding(Charset charset) {
        String encoding = charset.name();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "setResponseCharacterEncoding, Charset name [" + encoding + "]");
        }

        if (initialized) {
            throw new IllegalStateException(nls.getString("programmatic.sessions.already.been.initialized"));
        }

        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                                                                         nls.getString("Unsupported.op.from.servlet.context.listener"),
                                                                         new Object[] { "setResponseCharacterEncoding", lastProgAddListenerInitialized, getApplicationName() }));
        }

        if (encoding != null) {
            this.config.setModuleResponseEncoding(encoding);
        } else {
            String msg = servlet40NLS.getFormattedMessage("unsupported.response.encoding.[{0}]", new Object[] { charset }, "Unsupported encoding specified --> " + charset);
            logger.logp(Level.SEVERE, CLASS_NAME, "setResponseCharacterEncoding", msg);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "setResponseCharacterEncoding");
        }
    }
}
