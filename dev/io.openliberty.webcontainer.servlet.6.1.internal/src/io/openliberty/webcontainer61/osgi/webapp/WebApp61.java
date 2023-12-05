/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer61.osgi.webapp;

import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer40.facade.ServletContextFacade40;
import com.ibm.ws.webcontainer40.osgi.webapp.WebApp40;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

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
        return new com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40(this);
    }

    @Override
    public ServletContext getFacade() {
        if (this.facade == null)
            this.facade = new ServletContextFacade40(this);
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
     * @since Servlet 6.1
     */
    @Override
    public void setRequestCharacterEncoding(Charset encoding) {
        //to be implemented
    }

    /*
     * @since Servlet 6.1
     */
    @Override
    public void setResponseCharacterEncoding(Charset encoding) {
        //to be implemented
    }

}
