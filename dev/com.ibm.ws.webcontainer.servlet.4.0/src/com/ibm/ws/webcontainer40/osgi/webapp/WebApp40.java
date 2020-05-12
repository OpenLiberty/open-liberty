/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.osgi.webapp;

import java.text.MessageFormat;
import java.util.logging.Level;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.session.SessionManager;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.osgi.WebContainer;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer40.facade.ServletContextFacade40;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.session.ISessionManagerCustomizer;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.util.EncodingUtils;

public class WebApp40 extends com.ibm.ws.webcontainer31.osgi.webapp.WebApp31 implements ServletContext {
    private final static TraceComponent tc = Tr.register(WebApp40.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);
    protected static final TraceNLS servlet40NLS = TraceNLS.getTraceNLS(WebApp40.class, "com.ibm.ws.webcontainer40.resources.Messages");
    protected static final String CLASS_NAME = "com.ibm.ws.webcontainer40.osgi.webapp.WebApp40";

    /*
     * This value applies to all sessions in this webmodule.
     * Default is 30 minutes, unless changed and set explicitly.
     */
    private static final int defaultSessionTimeout = 30;

    /**
     * Constructor.
     *
     * @param name
     * @param parent
     * @param warDir
     */
    public WebApp40(WebAppConfiguration webAppConfig,
                    ClassLoader moduleLoader,
                    ReferenceContext referenceContext,
                    MetaDataService metaDataService,
                    J2EENameFactory j2eeNameFactory,
                    ManagedObjectService managedObjectService) {
        super(webAppConfig, moduleLoader, referenceContext, metaDataService, j2eeNameFactory, managedObjectService);
    }

    /*
     *
     * For Servlet 4.0 the major version should be 4.
     *
     * @see com.ibm.ws.webcontainer.webapp.WebApp#getMajorVersion()
     */
    @Override
    public int getMajorVersion() {
        return WebContainer.getServletContainerSpecLevel() == WebContainer.SPEC_LEVEL_50 ? 5 : 4;
    }

    /*
     *
     * For Servlet 4.0 the minor version should be 0.
     *
     * @see com.ibm.ws.webcontainer.webapp.WebApp#getMinorVersion()
     */
    @Override
    public int getMinorVersion() {
        return 0;
    }

    /*
     * Throw NPE if name is null
     *
     * @see com.ibm.ws.webcontainer.webapp.WebApp#getAttribute(java.lang.String)
     *
     */
    @Override
    public Object getAttribute(String name) {
        if (name == null) {
            logger.logp(Level.SEVERE, CLASS_NAME, "getAttribute", servlet40NLS.getString("name.is.null"));
            throw new java.lang.NullPointerException(servlet40NLS.getString("name.is.null"));
        }

        return super.getAttribute(name);
    }

    /*
     * Throw NPE if name is null
     *
     * @see com.ibm.ws.webcontainer.webapp.WebApp#getInitParameter(java.lang.String)
     */
    @Override
    public String getInitParameter(String name) {
        if (name == null) {
            logger.logp(Level.SEVERE, CLASS_NAME, "getInitParameter", servlet40NLS.getString("name.is.null"));
            throw new java.lang.NullPointerException(servlet40NLS.getString("name.is.null"));
        }

        return super.getInitParameter(name);
    }

    /*
     * Throw NPE if name is null
     *
     * @see com.ibm.ws.webcontainer.webapp.WebApp#setAttribute(java.lang.String, java.lang.Object)
     */
    @Override
    public void setAttribute(String name, Object value) {
        if (name == null) {
            logger.logp(Level.SEVERE, CLASS_NAME, "setAttribute", servlet40NLS.getString("name.is.null"));
            throw new java.lang.NullPointerException(servlet40NLS.getString("name.is.null"));
        }

        super.setAttribute(name, value);
    }

    /*
     * Throw NPE if name is null
     *
     * @see com.ibm.ws.webcontainer.webapp.WebApp#setInitParameter(java.lang.String, java.lang.String)
     */
    @Override
    public boolean setInitParameter(String name, String value) throws IllegalStateException, IllegalArgumentException {
        if (name == null) {
            logger.logp(Level.SEVERE, CLASS_NAME, "setInitParameter", servlet40NLS.getString("name.is.null"));
            throw new java.lang.NullPointerException(servlet40NLS.getString("name.is.null"));
        }

        return super.setInitParameter(name, value);
    }

    @Override
    public ServletContext getFacade() {
        if (this.facade == null)
            this.facade = new ServletContextFacade40(this);
        return this.facade;
    }

    /**
     * @see javax.servlet.ServletContext#getSessionTimeout()
     */
    @Override
    public int getSessionTimeout() {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                                                                         nls.getString("Unsupported.op.from.servlet.context.listener"),
                                                                         new Object[] { "getSessionTimeout", lastProgAddListenerInitialized, getApplicationName() })); // PI41941
        }
        if (!this.config.isModuleSessionTimeoutSet()) {
            return defaultSessionTimeout;
        }
        return this.config.getSessionTimeout();
    }

    /**
     * @see javax.servlet.ServletContext#setSessionTimeout(int sessionTimeout)
     */
    @Override
    public void setSessionTimeout(int sessionTimeout) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "setSessionTimeout(int)");
        }

        if (initialized) {
            throw new IllegalStateException(nls.getString("programmatic.sessions.already.been.initialized"));
        }

        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                                                                         nls.getString("Unsupported.op.from.servlet.context.listener"),
                                                                         new Object[] { "setSessionTimeout", lastProgAddListenerInitialized, getApplicationName() }));
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setSessionTimeout", "Setting session timeout to: " + sessionTimeout + " for application: " + getApplicationName());
        }

        // If session is less or equal than 0, then it means that session should never timeout.
        if (sessionTimeout <= 0) {
            sessionTimeout = -1;
        }

        // Set new value in the web app config
        this.config.setSessionTimeout(sessionTimeout);
        this.config.setModuleSessionTimeoutSet(true);

        sessionTimeout = sessionTimeout * 60; // The timeout is in minutes

        // Get the session manager
        String id = getVirtualServerName() + getContextPath();
        ISessionManagerCustomizer sessionMgrCustomizer = SessionManager.getSessionManager(id);
        // Set the new session timeout
        sessionMgrCustomizer.setSessionTimeout(sessionTimeout);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "setSessionTimeout(int)");
        }
    }

    /**
     * @see javax.servlet.ServletContext#getRequestCharacterEncoding()
     */
    @Override
    public String getRequestCharacterEncoding() {

        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                                                                         nls.getString("Unsupported.op.from.servlet.context.listener"),
                                                                         new Object[] { "getRequestCharacterEncoding", lastProgAddListenerInitialized, getApplicationName() }));
        }

        return this.config.getModuleRequestEncoding();
    }

    /**
     * @see javax.servlet.ServletContext#setRequestCharacterEncoding(String encoding)
     */
    @Override
    public void setRequestCharacterEncoding(String encoding) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "setRequestCharacterEncoding, encoding [" + encoding + "]");
        }

        if (initialized) {
            throw new IllegalStateException(nls.getString("programmatic.sessions.already.been.initialized"));
        }

        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                                                                         nls.getString("Unsupported.op.from.servlet.context.listener"),
                                                                         new Object[] { "setRequestCharacterEncoding", lastProgAddListenerInitialized, getApplicationName() }));
        }

        if (EncodingUtils.isCharsetSupported(encoding)) {
            this.config.setModuleRequestEncoding(encoding);
        } else {
            String msg = nls.getFormattedMessage("unsupported.request.encoding.[{0}]", new Object[] { encoding }, "Unsupported encoding specified --> " + encoding);
            logger.logp(Level.SEVERE, CLASS_NAME, "setRequestCharacterEncoding", msg);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "setRequestCharacterEncoding");
        }
    }

    /**
     * @see javax.servlet.ServletContext#getResponseCharacterEncoding()
     */
    @Override
    public String getResponseCharacterEncoding() {
        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                                                                         nls.getString("Unsupported.op.from.servlet.context.listener"),
                                                                         new Object[] { "getResponseCharacterEncoding", lastProgAddListenerInitialized, getApplicationName() }));
        }

        return this.config.getModuleResponseEncoding();
    }

    /**
     * @see javax.servlet.ServletContext#setResponseCharacterEncoding(String encoding)
     */
    @Override
    public void setResponseCharacterEncoding(String encoding) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "setResponseCharacterEncoding, encoding [" + encoding + "]");
        }

        if (initialized) {
            throw new IllegalStateException(nls.getString("programmatic.sessions.already.been.initialized"));
        }

        if (withinContextInitOfProgAddListener) {
            throw new UnsupportedOperationException(MessageFormat.format(
                                                                         nls.getString("Unsupported.op.from.servlet.context.listener"),
                                                                         new Object[] { "setResponseCharacterEncoding", lastProgAddListenerInitialized, getApplicationName() }));
        }

        if (EncodingUtils.isCharsetSupported(encoding)) {
            this.config.setModuleResponseEncoding(encoding);
        } else {
            String msg = servlet40NLS.getFormattedMessage("unsupported.response.encoding.[{0}]", new Object[] { encoding }, "Unsupported encoding specified --> " + encoding);
            logger.logp(Level.SEVERE, CLASS_NAME, "setResponseCharacterEncoding", msg);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "setResponseCharacterEncoding");
        }

    }

    @Override
    protected RequestDispatcher getRequestDispatcher(WebApp webApp, RequestProcessor p) {
        return new WebAppRequestDispatcher(webApp, p);
    }

    @Override
    protected RequestDispatcher getRequestDispatcher(WebApp app, String path) {
        return new WebAppRequestDispatcher(app, path);
    }

    @Override
    public WebAppDispatcherContext createDispatchContext() {
        return new com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40(this);
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "addJspFile() : servletName = " + servletName + ", jspFile = " + jspFile);
        }

        if (initialized) {

            throw new IllegalStateException(liberty_nls.getString("Not.in.servletContextCreated"));

        } else if (withinContextInitOfProgAddListener) {

            throw new UnsupportedOperationException(MessageFormat.format(
                                                                         nls.getString("Unsupported.op.from.servlet.context.listener"),
                                                                         new Object[] { "addServlet", lastProgAddListenerInitialized, getApplicationName() })); // PI41941

        } else if (servletName == null || servletName.isEmpty()) {

            throw new IllegalArgumentException();

        }

        // make sure a servlet doesn't already exist
        IServletConfig sconfig = config.getServletInfo(servletName);

        if (sconfig == null) {
            try {

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.fine(CLASS_NAME + "addJspFile() : create a new servet config");
                }

                sconfig = this.webExtensionProcessor.createConfig("DYN_" + servletName + "_" + System.currentTimeMillis());

                sconfig.setServletName(servletName);
                sconfig.setDisplayName(servletName);
                sconfig.setFileName(jspFile);
                sconfig.setIsJsp(true);
                sconfig.setServletContext(this.getFacade());

                // add to the config
                config.addServletInfo(servletName, sconfig);
                config.addDynamicServletRegistration(servletName, sconfig);

                sconfig.setServletWrapper(jspAwareCreateServletWrapper(sconfig, servletName));

            } catch (Exception e) {
                FFDCFilter.processException(e, this.getClass().getName() + ".addJspFile", "14");
            }

        } else {
            if (sconfig.isClassDefined() || sconfig.getFileName() != null) {
                logger.logp(Level.SEVERE, CLASS_NAME, "addJspFile", "servlet.with.same.name.already.exists", new Object[] { servletName });
                sconfig = null;
            } else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.fine("addJspFile() : existing empty servlet config found");
                }
                sconfig.setFileName(jspFile);

                sconfig.setIsJsp(true);
                config.addDynamicServletRegistration(servletName, sconfig);
                sconfig.setServletWrapper(jspAwareCreateServletWrapper(sconfig, servletName));

                // Add any existing mappings, replacing any previous.
                for (String mapping : sconfig.getMappings()) {
                    try {
                        if (requestMapper.exists(mapping))
                            requestMapper.replaceMapping(mapping, sconfig.getServletWrapper());
                        else
                            requestMapper.addMapping(mapping, sconfig.getServletWrapper());
                    } catch (Exception exc) {
                        // ignore for now
                    }
                }
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "addJspFile() : ServletRegistraion = " + sconfig);
        }

        return sconfig;
    }

}
