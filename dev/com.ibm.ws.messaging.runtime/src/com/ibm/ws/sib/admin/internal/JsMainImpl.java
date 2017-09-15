/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.messaging.mbean.MessagingEngineMBean;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.websphere.sib.exception.SINotSupportedException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.AliasDestination;
import com.ibm.ws.sib.admin.BaseDestination;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.JsBus;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.admin.JsMEConfig;
import com.ibm.ws.sib.admin.JsMain;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.JsProcessComponent;
import com.ibm.ws.sib.admin.SIBDestination;
import com.ibm.ws.sib.admin.SIBExceptionBusNotFound;
import com.ibm.ws.sib.admin.SIBLocalizationPoint;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * 
 * The main sib service class that is responsible for initailizing starting
 * stopping and destroying messaging runtime service
 */
public class JsMainImpl implements JsMain {

    private static final String CLASS_NAME = "com.ibm.ws.sib.admin.internal.JsMainImpl";

    private static final TraceComponent tc = SibTr.register(JsMainImpl.class,
                                                            JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);

    private static final TraceNLS nls = TraceNLS.getTraceNLS("com.ibm.ws.sib.admin.internal.CWSIDText");
    protected JsMEConfig meConfig = null;
    JsBusImpl bus = null;
    String defaultMEUUID = "DefaultMEUUID";
    ArrayList services;
    BundleContext bContext;
    ServiceRegistration<MessagingEngineMBean> mbeanServiceReg;

    private boolean _serverStarted = false;

    // Flag: Is the WAS server stopping?
    private boolean _serverStopping = false;

    // The messaging engine instances configured in the process
    protected Hashtable _messagingEngines = new Hashtable();

    class ComponentList {

        private final String _className;

        private final JsProcessComponent _componentRef;

        ComponentList(String className, JsProcessComponent c) {
            _className = className;
            _componentRef = c;
        }

        // Get the name of the class
        String getClassName() {
            return _className;
        }

        // Get a reference to the instantiated class
        JsProcessComponent getRef() {
            return _componentRef;
        }
    }

    // Object to represent a Messaging Engine process component
    protected class MessagingEngine {

        private JsMEConfig _meConfig = null;
        private JsMessagingEngine _me = null;

        MessagingEngine(JsMEConfig meConfig, JsMessagingEngine me) {
            _meConfig = meConfig;
            _me = me;
        }

        JsMEConfig getConfig() {
            return _meConfig;
        }

        void setConfig(JsMEConfig newConfig) {
            _meConfig = newConfig;
        }

        public JsMessagingEngine getRuntime() {
            return _me;
        }
    }

    // Constructor for liberty release
    public JsMainImpl() {

        String thisMethodName = "<init>";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, services);
        }

        constructorCode();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    // Constructor for liberty release
    public JsMainImpl(BundleContext bContext) {
        this();
        this.bContext = bContext;
        String thisMethodName = "<init>(BundleContext)";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, services);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    protected void constructorCode() {

        String thisMethodName = CLASS_NAME + ".constructorCode()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        com.ibm.ws.sib.admin.JsAdminService adminService = JsMainAdminComponentImpl.getJsAdminService();
        adminService.setAdminMain(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /**
     * Is this JVM running on the zOS platform?
     * 
     * @return boolean true if the platform is zOS, otherwise false
     */
    /*
     * public boolean isZOSPlatform() {
     * 
     * String thisMethodName = CLASS_NAME + ".isZOSPlatform()";
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.entry(tc, thisMethodName); }
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.exit(tc, thisMethodName, new Boolean(_platform_zOS)); }
     * 
     * return _platform_zOS; }
     */

    /**
     * Is this JVM running in a zOS Control Region Adjunct (CRA)?
     * 
     * @return boolean true if in the zOS CRA, otherwise false
     */
    /*
     * public boolean isZOSCRA() {
     * 
     * String thisMethodName = CLASS_NAME + ".isZOSCRA()";
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.entry(tc, thisMethodName); }
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.exit(tc, thisMethodName, new Boolean(_platform_zOS_CRA)); }
     * 
     * return _platform_zOS_CRA; }
     */

    /**
     * Is this JVM running in a zOS Servant Region (SR)?
     * 
     * @return boolean true if in the zOS SR, otherwise false
     * @throws Exception
     */
    /*
     * public boolean isZOSServant() {
     * 
     * String thisMethodName = CLASS_NAME + ".isZOSServant()";
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.entry(tc, thisMethodName); }
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.exit(tc, thisMethodName, new Boolean(_platform_zOS_servant)); }
     * 
     * return _platform_zOS_servant; }
     */

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.runtime.component.WsComponent#initialize(java.lang.Object)
     */
    @Override
    public void initialize(JsMEConfig config) throws Exception {

        String thisMethodName = CLASS_NAME + ".initialize(Object)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, "");
        }

        meConfig = config;
        createMessageEngine(meConfig);

        // Initialize the ME's we created
        Enumeration meEnum = _messagingEngines.elements();
        while (meEnum.hasMoreElements()) {
            Object o = meEnum.nextElement();
            Object c = ((MessagingEngine) o).getRuntime();
            if (c instanceof BaseMessagingEngineImpl) {
                try {
                    ((BaseMessagingEngineImpl) c).initialize(null);
                    setAttributes((BaseMessagingEngineImpl) c);
                } catch (Exception e) {
                    FFDCFilter.processException(e, thisMethodName,
                                                "1:656:1.108", this);
                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", e);
                    SibTr.error(tc, "ME_ERROR_REPORTED_SIAS0029",
                                ((BaseMessagingEngineImpl) c).getName());
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.runtime.component.Component#start()
     */
    @Override
    public void start() throws Exception {

        String thisMethodName = CLASS_NAME + ".start()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            // SibTr.entry(tc, thisMethodName);
        }

        Enumeration meEnum = _messagingEngines.elements();

        while (meEnum.hasMoreElements()) {
            Object o = meEnum.nextElement();
            Object c = ((MessagingEngine) o).getRuntime();
            try {

                ((BaseMessagingEngineImpl) c).startConditional();
                Dictionary<String, Object> properties = new Hashtable<String, Object>();
                properties.put("service.vendor", "IBM");
                properties.put("jmx.objectname", "WebSphere:feature=wasJmsServer,type=MessagingEngine,name=" + ((BaseMessagingEngineImpl) c)._name);
                mbeanServiceReg = (ServiceRegistration<MessagingEngineMBean>) this.bContext.registerService(MessagingEngineMBean.class.getName(), c, properties);

            } catch (Exception w) {
                // Not serious enough to warrant server stop.
                FFDCFilter.processException(w, thisMethodName, "1:725:1.108",
                                            this);
                SibTr.exception(tc, w);
                SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", w);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    public void serverStarted() {

        String thisMethodName = CLASS_NAME + ".serverStarted()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            // SibTr.entry(tc, thisMethodName);
        }

        _serverStarted = true;

        Enumeration meEnum = _messagingEngines.elements();

        // Call each ME on this server. Any exceptions are caught and
        // deliberately
        // not
        // thrown as the failure of one ME must not affect any others that might
        // exist.
        while (meEnum.hasMoreElements()) {
            Object o = meEnum.nextElement();
            Object c = ((MessagingEngine) o).getRuntime();
            if (c instanceof BaseMessagingEngineImpl) {
                try {
                    ((BaseMessagingEngineImpl) c).serverStarted();
                } catch (Exception e) {
                    FFDCFilter.processException(e, thisMethodName,
                                                "1:772:1.108", this);
                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", e);
                    SibTr.error(tc, "ME_ERROR_REPORTED_SIAS0029",
                                ((BaseMessagingEngineImpl) c).getName());
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    public void serverStopping() {

        String thisMethodName = CLASS_NAME + ".serverStopping()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            // SibTr.entry(tc, thisMethodName);
        }

        _serverStopping = true;
        _serverStarted = false;

        Enumeration meEnum = _messagingEngines.elements();

        // Call each ME on this server. Any exceptions are caught and
        // deliberately
        // not
        // thrown as the failure of one ME must not affect any others that might
        // exist.
        while (meEnum.hasMoreElements()) {
            Object o = meEnum.nextElement();
            Object c = ((MessagingEngine) o).getRuntime();
            if (c instanceof BaseMessagingEngineImpl) {
                try {
                    ((BaseMessagingEngineImpl) c).serverStopping();
                } catch (Exception e) {
                    FFDCFilter.processException(e, thisMethodName,
                                                "1:810:1.108", this);
                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", e);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.runtime.component.WsComponent#stop()
     */
    public void stop() {

        String thisMethodName = CLASS_NAME + ".stop()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            // SibTr.entry(tc, thisMethodName);
        }

        // Get the MEs on this server
        Enumeration meEnum = _messagingEngines.elements();

        // Stop each ME on this server. Any exceptions are caught and
        // deliberately
        // not
        // rethrown as errors in one ME must not affect any others that might
        // exist.
        while (meEnum.hasMoreElements()) {
            Object o = meEnum.nextElement();
            Object c = ((MessagingEngine) o).getRuntime();
            try {

                ((BaseMessagingEngineImpl) c)
                                .stopConditional(JsConstants.ME_STOP_IMMEDIATE);
            } catch (Exception e) {
                FFDCFilter.processException(e, thisMethodName, "1:854:1.108",
                                            this);
                SibTr.exception(tc, e);
                SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.runtime.component.WsComponent#destroy()
     */
    @Override
    public void destroy() throws Exception {

        String thisMethodName = CLASS_NAME + ".destroy()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, "");
        }

        // Destroy the ME's we created
        Enumeration meEnum = _messagingEngines.elements();

        // Destroy each ME on this server. Any exceptions are caught and
        // deliberately not
        // rethrown as errors in one ME must not affect any others that might
        // exist.
        while (meEnum.hasMoreElements()) {
            Object o = meEnum.nextElement();
            Object c = ((MessagingEngine) o).getRuntime();
            if (c instanceof BaseMessagingEngineImpl) {
                try {
                    ((BaseMessagingEngineImpl) c).destroy();
                    mbeanServiceReg.unregister();
                } catch (Exception e) {
                    FFDCFilter.processException(e, thisMethodName,
                                                "1:910:1.108", this);
                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", e);
                }
            }
        }
        _messagingEngines = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /**
     * Create a single Message Engine admin object using suppled config object.
     */
    private MessagingEngine createMessageEngine(JsMEConfig me) throws Exception {

        String thisMethodName = CLASS_NAME + ".createMessageEngine(JsMEConfig)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, "replace ME name here");
        }

        JsMessagingEngine engineImpl = null;

        bus = new JsBusImpl(me, this, (me.getSIBus().getName()));// getBusProxy(me);
        engineImpl = new JsMessagingEngineImpl(this, bus, me);

        MessagingEngine engine = new MessagingEngine(me, engineImpl);
        _messagingEngines.put(defaultMEUUID, engine);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, engine.toString());
        }

        return engine;
    }

    /**
     * Returns the runtime configuration of the bus to which the supplied
     * messaging engine belongs. If the bus runtime configuration does not yet
     * exist, it is created. In liberty this is default bus configuration
     * 
     * @param me
     * @return the runtime configuration of the bus to which the supplied
     *         messaging engine belongs.
     */
    private JsBusImpl getBusProxy(JsMEConfig me) {

        String thisMethodName = CLASS_NAME + ".getBusProxy(ConfigObject)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, "ME Name");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return this.bus;
    }

    /**
     * Returns the runtime configuration of the bus to which the named messaging
     * engine belongs. If the bus runtime configuration does not yet exist, it
     * is created.
     * 
     * @param name
     * @return the runtime configuration of the bus to which the named messaging
     *         engine belongs.
     */
    private JsBusImpl getBusProxy(String name) throws SIBExceptionBusNotFound {

        String thisMethodName = CLASS_NAME + ".getBusProxy(String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, name);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return this.bus;
    }

    /**
     * Returns the runtime configuration of the named bus. For liberty is always
     * default bus
     * 
     * @param busName
     * @return the runtime configuration of the named bus.
     */
    public JsBus getBus(String busName) throws SIBExceptionBusNotFound {

        String thisMethodName = CLASS_NAME + ".getBus(String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, busName);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return this.bus;
    }

    public JsBus getDefinedBus(final String busName)
                    throws SIBExceptionBusNotFound {
        String thisMethodName = "getDefinedBus";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, busName);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return this.bus;
    }

    /**
     * Return a reference to the instance of the named class.
     * 
     * @param className
     * @return JsProcessComponent
     */
    /*
     * public JsProcessComponent getProcessComponent(String className) {
     * 
     * String thisMethodName = CLASS_NAME + ".getProcessComponent(String)";
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.entry(tc, thisMethodName, className); }
     * 
     * Enumeration<ComponentList> vEnum = _processComponents.elements();
     * JsProcessComponent foundProcessComponent = null;
     * 
     * while (vEnum.hasMoreElements() && foundProcessComponent == null) {
     * 
     * ComponentList c = vEnum.nextElement();
     * 
     * if (c.getClassName().equals(className)) { foundProcessComponent =
     * c.getRef(); } }
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.exit(tc, thisMethodName); }
     * 
     * return foundProcessComponent; }
     */

    /**
     * Get an instance of a messaging engine
     * 
     * @param name
     * @return JsMessagingEngineImpl
     */
    public JsMessagingEngineImpl getMessagingEngine(String name) {

        String thisMethodName = CLASS_NAME + ".getMessagingEngine(String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, name);
        }

        Enumeration vEnum = _messagingEngines.elements();
        JsMessagingEngineImpl foundMessagingEngine = null;

        while (vEnum.hasMoreElements() && foundMessagingEngine == null) {

            //Liberty COMMs change
            //In Liberty only one Messaging Engine and connection fctory properties
            //cannot specify Target name.
            //Hence changing the logic to not to compare.

            Object o = vEnum.nextElement();
            Object c = ((MessagingEngine) o).getRuntime();

            foundMessagingEngine = (JsMessagingEngineImpl) c;

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return foundMessagingEngine;
    }

    /**
     * Get an instance of a messaging engine
     * 
     * @param busName
     * @param engine
     * @return JsMessagingEngine
     */
    public JsMessagingEngine getMessagingEngine(String busName, String engine) {

        String thisMethodName = CLASS_NAME
                                + ".getMessagingEngine(String, String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { busName, engine });
        }

        AuditManager auditManager = new AuditManager();
        auditManager.setJMSBusName(busName);
        auditManager.setJMSMessagingEngine(engine);

        Enumeration vEnum = _messagingEngines.elements();
        JsMessagingEngine foundMessagingEngine = null;

        while (vEnum.hasMoreElements() && foundMessagingEngine == null) {

            //Liberty COMMs change
            //In Liberty only one Messaging Engine and connection fctory properties
            //cannot specify Target name.
            //Hence changing the logic to not to compare.

            Object o = vEnum.nextElement();
            Object c = ((MessagingEngine) o).getRuntime();

            foundMessagingEngine = (JsMessagingEngine) c;
            break;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return foundMessagingEngine;
    }

    /**
     * Return a list of Messaging Engines
     * 
     * @return Enumeration
     */
    public Enumeration listMessagingEngines() {

        String thisMethodName = CLASS_NAME + ".listMessagingEngines()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        Vector v = new Vector();

        Enumeration vEnum = _messagingEngines.elements();
        while (vEnum.hasMoreElements()) {
            Object o = vEnum.nextElement();
            Object c = ((MessagingEngine) o).getRuntime();
            if (c instanceof BaseMessagingEngineImpl)
                v.addElement(c);
        }

        Enumeration elements = v.elements();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return elements;
    }

    /**
     * Return a list of Messaging Engines
     * 
     * @param busName
     * @return Enumeration
     */
    public Enumeration listMessagingEngines(String busName) {

        String thisMethodName = CLASS_NAME + ".listMessagingEngines(String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, busName);
        }

        Vector v = new Vector();

        Enumeration e = listMessagingEngines();
        while (e.hasMoreElements()) {
            Object c = e.nextElement();
            if (((BaseMessagingEngineImpl) c).getBusName().equals(busName))
                v.addElement(c);
        }

        Enumeration elements = v.elements();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return elements;
    }

    /**
     * Returns the set of messaging engines on the named bus.
     * 
     * @param busName
     * @return the set of messaging engines on the named bus.
     */
    public Set getMessagingEngineSet(String busName) {

        String thisMethodName = CLASS_NAME + ".getMessagingEngineSet(String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, busName);
        }
        Set retSet = new HashSet();
        if (meConfig != null) {
            String meName = meConfig.getMessagingEngine().getName();
            BaseMessagingEngineImpl engineImpl = getMessagingEngine(meName);
            retSet.add(engineImpl.getUuid().toString());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Integer i = new Integer(retSet.size());
            SibTr.exit(tc, thisMethodName, i.toString());
        }

        return retSet;
    }

    public String getLibertyMEUuid() {
        JsMessagingEngineImpl engineImpl = getMessagingEngine(meConfig.getMessagingEngine().getName());
        return engineImpl.getUuid();
    }

    /**
     * Return a readable string of messaging engines in the process
     * 
     * @return String[]
     */
    public String[] showMessagingEngines() {

        String thisMethodName = CLASS_NAME + ".showMessagingEngines()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        final String[] list = new String[_messagingEngines.size()];
        Enumeration e = listMessagingEngines();
        int i = 0;
        while (e.hasMoreElements()) {

            Object c = e.nextElement();
            list[i++] = ((BaseMessagingEngineImpl) c).getBusName() + ":"
                        + ((BaseMessagingEngineImpl) c).getName() + ":"
                        + ((BaseMessagingEngineImpl) c).getState();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return list;
    }

    /**
     * Start a messaging engine
     * 
     * @param busName
     * @param name
     */
    public void startMessagingEngine(String busName, String name)
                    throws Exception {

        String thisMethodName = CLASS_NAME
                                + ".startMessagingEngine(String, String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { busName, name });
        }

        BaseMessagingEngineImpl me = (BaseMessagingEngineImpl) getMessagingEngine(
                                                                                  busName, name);
        if (me != null) {
            me.startConditional();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Unable to locate engine <bus=" + busName
                                + " name=" + name + ">");
            throw new Exception("The messaging engine <bus=" + busName
                                + " name=" + name + "> does not exist");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /**
     * Stop a messaging engine
     * 
     * @param busName
     * @param name
     */
    public void stopMessagingEngine(String busName, String name)
                    throws Exception {

        String thisMethodName = CLASS_NAME
                                + ".stopMessagingEngine(String, String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { busName, name });
        }

        stopMessagingEngine(busName, name, JsConstants.ME_STOP_IMMEDIATE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /**
     * Stop a messaging engine
     * 
     * @param busName
     * @param name
     * @param mode
     */
    public void stopMessagingEngine(String busName, String name, String mode)
                    throws Exception {

        String thisMethodName = CLASS_NAME
                                + ".stopMessagingEngine(String, String, String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName,
                        new Object[] { busName, name, mode });
        }

        int iMode = Integer.parseInt(mode);
        stopMessagingEngine(busName, name, iMode);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /**
     * Stop a messaging engine
     * 
     * @param busName
     * @param name
     * @param mode
     */
    private void stopMessagingEngine(String busName, String name, int mode)
                    throws Exception {

        String thisMethodName = CLASS_NAME
                                + ".stopMessagingEngine(String, String, int)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { busName, name,
                                                          Integer.toString(mode) });
        }

        BaseMessagingEngineImpl me = (BaseMessagingEngineImpl) getMessagingEngine(
                                                                                  busName, name);
        if (me != null) {
            me.stopConditional(mode);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Unable to locate engine <bus=" + busName
                                + " name=" + name + ">");
            throw new Exception("The messaging engine <bus=" + busName
                                + " name=" + name + "> does not exist");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /**
     * @param c
     */
    protected void setAttributes(BaseMessagingEngineImpl c) {

        String thisMethodName = CLASS_NAME
                                + ".setAttributes(BaseMessagingEngineImpl)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { c.getBusName(),
                                                          c.getName() });
        }

        ((JsEngineComponent) c).setConfig(c);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /*
     * This method will return null in liberty
     */
    public JsProcessComponent getProcessComponent(String className) {

        String thisMethodName = CLASS_NAME + ".getProcessComponent(String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, className);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return null;
    }

    /**
     * Has the WAS server in which we are contained now started?
     * 
     * @return true if the server is sterted; else false.
     */
    public boolean isServerStarted() {

        String thisMethodName = CLASS_NAME + ".isServerStarted()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, new Boolean(_serverStarted));
        }

        return _serverStarted;
    }

    /**
     * Is the WAS server in which we are contained stopping?
     * 
     * @return true if the server is stopping; else false.
     */
    public boolean isServerStopping() {

        String thisMethodName = CLASS_NAME + ".isServerStopping()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, new Boolean(_serverStopping));
        }

        return _serverStopping;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMain#getSibServiceStatsGroup()
     * 
     * public StatsGroup getSibServiceStatsGroup() {
     * 
     * String thisMethodName = CLASS_NAME + ".getSibServiceStatsGroup()";
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.entry(tc, thisMethodName, this);
     * }
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.exit(tc, thisMethodName);
     * }
     * 
     * return _sibServiceStatsGroup;
     * }
     */

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMain#getSibEnginesStatsGroup()
     */
    // public StatsGroup getSibEnginesStatsGroup() {
    //
    // String thisMethodName = CLASS_NAME + ".getSibEnginesStatsGroup()";
    //
    // if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
    // SibTr.entry(tc, thisMethodName, this);
    // }
    //
    // if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
    // SibTr.exit(tc, thisMethodName);
    // }
    //
    // return _meStatsGroup;
    // }

    // 250606.3 recovery mode support
    public boolean isServerInRecoveryMode() {

        String thisMethodName = CLASS_NAME + ".isServerInRecoveryMode()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        boolean ret = false;// (_serverMode == Server.RECOVERY_MODE); TBD

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, new Boolean(ret));
        }

        return ret;
    }

    /*
     * This method will return null for liberty
     */
    public Object getService(Class c) {

        String thisMethodName = CLASS_NAME + ".getService(Class)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return null;
    }

    /**
     * Returns a list of configured buses in this cell. For liberty this method
     * will return the null value, because no directory structure is maintained
     * in liberty for a bus.
     * 
     * @return String[] list of buses in cell
     */
    public List<String> listDefinedBuses() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "listDefinedBuses", this);
        }

        List buses = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "listDefinedBuses", buses);
        }

        return buses;
    }

    /** {@inheritDoc} */
    @Override
    public void alterDestinationLocalization(BaseDestination config)
                    throws Exception {
        SibTr.entry(tc, "alterDestinationLocalization : ", config);
        String meName = meConfig.getMessagingEngine().getName();
        BaseMessagingEngineImpl engine = getMessagingEngine(meName);
        SIBLocalizationPoint lpConfig = new SIBLocalizationPointImpl();
        lpConfig.setIdentifier(config.getName() + "@" + meName);
        if (!config.isAlias()) {
            SIBDestination d = (SIBDestination) config;
            lpConfig.setHighMsgThreshold(d.getHighMessageThreshold());
            engine.alterLocalizationPoint(config, lpConfig);
        } else {
            AliasDestination aliasDest = (AliasDestination) config;
            engine.alterLocalizationPoint(aliasDest, lpConfig);
        }
        SibTr.exit(tc, "alterDestinationLocalization : ", config);

    }

    @Override
    public void createDestinationLocalization(BaseDestination config)
                    throws Exception {
        SibTr.entry(tc, "createDestinationLocalization", this);
        try {
            String meName = meConfig.getMessagingEngine().getName();
            JsDestinationCache dCache = bus.getDestinationCache();
            BaseDestinationDefinition dd = dCache.addNewDestinationToCache(config);

            if (!config.isAlias()) {
                BaseMessagingEngineImpl engine = getMessagingEngine(meName);
                SIBLocalizationPoint lpConfig = new SIBLocalizationPointImpl();
                lpConfig.setIdentifier(config.getName() + "@" + meName);
                SIBDestination d = (SIBDestination) config;
                lpConfig.setHighMsgThreshold(d.getHighMessageThreshold());
                engine.addLocalizationPoint(lpConfig, (DestinationDefinition) dd);
                ArrayList list = new ArrayList();
                list.add(dd);
                bus.getDestinationCache().populateUuidCache(list);
            }

        } catch (Exception e) {
            SibTr.exception(tc, e);
        }

        SibTr.exit(tc, "createDestinationLocalization for destination: ", config.getName());

    }

    @Override
    public void deleteDestinationLocalization(BaseDestination config)
                    throws Exception {
        SibTr.entry(tc, "deleteDestinationLocalization ", config.getName());
        try {
            JsMessagingEngineImpl me = getMessagingEngine(meConfig.getMessagingEngine().getName());
            me.deleteLocalizationPoint(bus, config);
            JsDestinationCache cache = bus.getDestinationCache();
            cache.deleteDestination(bus.getName(), config.getName());
        } catch (Exception e) {
            SibTr.exception(tc, e);
        }
        SibTr.exit(tc, "deleteDestinationLocalization ", config.getName());
    }

    @Override
    public void reloadEngine(long highMessageThreshold) throws Exception {
        throw new SINotSupportedException();

    }

}
