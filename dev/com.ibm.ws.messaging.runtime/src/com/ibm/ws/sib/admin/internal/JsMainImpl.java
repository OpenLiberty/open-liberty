/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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
package com.ibm.ws.sib.admin.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.messaging.mbean.MessagingEngineMBean;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.websphere.sib.exception.SINotSupportedException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.messaging.lifecycle.Singleton;
import com.ibm.ws.messaging.security.RuntimeSecurityService;
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
@Component (
		service= {JsMainImpl.class, JsMain.class, Singleton.class},
		configurationPolicy=IGNORE,
        property={"type=com.ibm.ws.sib.admin.internal.JsMain", "service.vendor=IBM"})
public final class JsMainImpl implements JsMain, Singleton {

    private static final String CLASS_NAME = "com.ibm.ws.sib.admin.internal.JsMainImpl";

    private static final TraceComponent tc = SibTr.register(JsMainImpl.class,
                                                            JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);

    private static final TraceNLS nls = TraceNLS.getTraceNLS("com.ibm.ws.sib.admin.internal.CWSIDText");
    private JsMEConfig meConfig = null;
    JsBusImpl bus = null;
    ArrayList services;
    BundleContext bContext;
    ServiceRegistration<?> mbeanServiceReg;
    private final RuntimeSecurityService runtimeSecurityService;

    /**
     * @return the runtimeSecurityService
     */
    public final RuntimeSecurityService getRuntimeSecurityService() {
        return runtimeSecurityService;
    }

    private boolean _serverStarted = false;

    // Flag: Is the WAS server stopping?
    private boolean _serverStopping = false;

    // The messaging engine instance configured in this process
    private final AtomicReference<JsMessagingEngineImpl> messagingEngineRef = new AtomicReference<>();

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

    @Activate
    public JsMainImpl(BundleContext bContext, @Reference RuntimeSecurityService runtimeSecurityService) throws IllegalStateException {
        String methodName = "<init>";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            SibTr.entry(tc, methodName, new Object[] {this, bContext, runtimeSecurityService, services});
        
        this.bContext = bContext;
        this.runtimeSecurityService = runtimeSecurityService; 
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, methodName);
        }
    }

    @Override
    public void initialize(JsMEConfig config) throws Exception {

        String methodName = "initialize";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, methodName, "");
        }

        meConfig = config;
		
		bus = new JsBusImpl(meConfig, this, (meConfig.getSIBus().getName()));
		JsMessagingEngineImpl engineImpl = new JsMessagingEngineImpl(this, bus, meConfig);		
		messagingEngineRef.set(engineImpl);
        
		try {
			engineImpl.initialize(null);
			engineImpl.setConfig(engineImpl);
		} catch (Exception e) {
			FFDCFilter.processException(e, methodName, "1:656:1.108", this);
			SibTr.exception(tc, e);
			SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", e);
			SibTr.error(tc, "ME_ERROR_REPORTED_SIAS0029", engineImpl.getName());
		}
   
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, methodName, engineImpl);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.runtime.component.Component#start()
     */
    @Override
    public void start() throws Exception {

        String methodName = "start";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, methodName);
        }

		Optional.ofNullable(messagingEngineRef.get()).ifPresent(me -> {
			try {

				me.startConditional();
				Dictionary<String, Object> properties = new Hashtable<>();
				properties.put("service.vendor", "IBM");
				properties.put("jmx.objectname", "WebSphere:feature=wasJmsServer,type=MessagingEngine,name="+me._name);
				mbeanServiceReg = this.bContext.registerService(MessagingEngineMBean.class.getName(), me, properties);

			} catch (Exception w) {
				// Not serious enough to warrant server stop.
				FFDCFilter.processException(w, methodName, "1:725:1.108", this);
				SibTr.exception(this, tc, w);
				SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", w);
			}
		});

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, methodName);
        }
    }

    public void serverStarted() {

        String methodName = "serverStarted";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, methodName);
        }

		_serverStarted = true;

		Optional.ofNullable(messagingEngineRef.get()).ifPresent(me -> {
			try {
				me.serverStarted();
			} catch (Exception e) {
				FFDCFilter.processException(e, methodName, "1:772:1.108", this);
				SibTr.exception(this, tc, e);
				SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", e);
				SibTr.error(tc, "ME_ERROR_REPORTED_SIAS0029", me.getName());
			}
		});


        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, methodName);
        }
    }

    public void serverStopping() {

        String methodName = "serverStopping";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, methodName);
        }

        _serverStopping = true;
        _serverStarted = false;

		Optional.ofNullable(messagingEngineRef.get()).ifPresent(me -> {
			try {
				me.serverStopping();
			} catch (Exception e) {
				FFDCFilter.processException(e, methodName, "1:810:1.108", this);
				SibTr.exception(this, tc, e);
				SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", e);
			}
		});

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, methodName);
        }
    }

   public void stop() {

        String methodName = "stop";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
             SibTr.entry(this, tc, methodName);
        }

		Optional.ofNullable(messagingEngineRef.get()).ifPresent(me -> {
			try {
				me.stopConditional(JsConstants.ME_STOP_IMMEDIATE);
			} catch (Exception e) {
				FFDCFilter.processException(e, methodName, "1:854:1.108", this);
				SibTr.exception(this, tc, e);
				SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", e);
			}
		});

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, methodName);
        }
    }

    @Override
    public void destroy() throws Exception {

        String methodName = "destroy";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, methodName);
        }
        
		Optional.ofNullable(messagingEngineRef.getAndSet(null)).ifPresent(me -> {
			try {
				me.destroy();
				mbeanServiceReg.unregister();
			} catch (Exception e) {
				FFDCFilter.processException(e, methodName, "1:910:1.108", new Object[] {this,me});
				SibTr.exception(this, tc, e);
				SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", e);
			}
		});

        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, methodName);
        }
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

        String methodName = "getBusProxy(ConfigObject)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, methodName, "ME Name");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, methodName);
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

        String methodName = "getBusProxy(String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, methodName, name);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, methodName);
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

        String methodName = "getBus(String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, methodName, busName);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, methodName);
        }

        return this.bus;
    }

    public JsBus getDefinedBus(final String busName)
                    throws SIBExceptionBusNotFound {
        String methodName = "getDefinedBus";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, methodName, busName);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, methodName);
        }

        return this.bus;
    }

    public JsMessagingEngine getMessagingEngine(String busName, String engine) {

        String methodName = "getMessagingEngine";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, methodName, new Object[] { busName, engine });
        }

        AuditManager auditManager = new AuditManager();
        auditManager.setJMSBusName(busName);
        auditManager.setJMSMessagingEngine(engine);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, methodName);
        }

        return messagingEngineRef.get();
    }

    public Enumeration listMessagingEngines() {

        String methodName = "listMessagingEngines";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, methodName, this);
        }

        Vector<JsMessagingEngine> v = new Vector();
        Optional.ofNullable(messagingEngineRef.get()).ifPresent(v::add);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, methodName);
        }

        return v.elements();
    }

    public Enumeration listMessagingEngines(String busName) {

        String methodName = "listMessagingEngines";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, methodName, busName);
        }

        Vector<JsMessagingEngine> v = new Vector();
        Optional.ofNullable(messagingEngineRef.get())
                .filter(me -> Objects.equals(me.getBusName(), busName))
        		.ifPresent(v::add);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, methodName);
        }

        return v.elements();
    }

    public Set getMessagingEngineSet(String busName) {

        String methodName = "getMessagingEngineSet";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, methodName, busName);
        }
        Set retSet = new HashSet();
        if (meConfig != null) {
            String meName = meConfig.getMessagingEngine().getName();
            BaseMessagingEngineImpl engineImpl = messagingEngineRef.get();
            retSet.add(engineImpl.getUuid().toString());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Integer i = new Integer(retSet.size());
            SibTr.exit(tc, methodName, i.toString());
        }

        return retSet;
    }

    public String getLibertyMEUuid() {
        JsMessagingEngineImpl engineImpl = messagingEngineRef.get();
        return engineImpl.getUuid();
    }

   /**
     * Start a messaging engine
     * 
     * @param busName
     * @param name
     */
    public void startMessagingEngine(String busName, String name)
                    throws Exception {

        String methodName = CLASS_NAME
                                + ".startMessagingEngine(String, String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, methodName, new Object[] { busName, name });
        }

        BaseMessagingEngineImpl me = (BaseMessagingEngineImpl) getMessagingEngine(busName, name);
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
            SibTr.exit(tc, methodName);
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

        String methodName = CLASS_NAME
                                + ".stopMessagingEngine(String, String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, methodName, new Object[] { busName, name });
        }

        stopMessagingEngine(busName, name, JsConstants.ME_STOP_IMMEDIATE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, methodName);
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

        String methodName = CLASS_NAME
                                + ".stopMessagingEngine(String, String, String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, methodName,
                        new Object[] { busName, name, mode });
        }

        int iMode = Integer.parseInt(mode);
        stopMessagingEngine(busName, name, iMode);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, methodName);
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

        String methodName = CLASS_NAME
                                + ".stopMessagingEngine(String, String, int)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, methodName, new Object[] { busName, name,
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
            SibTr.exit(tc, methodName);
        }
    }

    /*
     * This method will return null in liberty
     */
    public JsProcessComponent getProcessComponent(String className) {

        String methodName = "getProcessComponent(String)";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, methodName, className);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, methodName);
        }

        return null;
    }

    /**
     * Has the WAS server in which we are contained now started?
     * 
     * @return true if the server is sterted; else false.
     */
    public boolean isServerStarted() {

        String methodName = "isServerStarted()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, methodName, this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, methodName, new Boolean(_serverStarted));
        }

        return _serverStarted;
    }

    /**
     * Is the WAS server in which we are contained stopping?
     * 
     * @return true if the server is stopping; else false.
     */
    public boolean isServerStopping() {

        String methodName = "isServerStopping()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, methodName, this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, methodName, new Boolean(_serverStopping));
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
     * String methodName = "getSibServiceStatsGroup()";
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.entry(tc, methodName, this);
     * }
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.exit(tc, methodName);
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
    // String methodName = "getSibEnginesStatsGroup()";
    //
    // if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
    // SibTr.entry(tc, methodName, this);
    // }
    //
    // if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
    // SibTr.exit(tc, methodName);
    // }
    //
    // return _meStatsGroup;
    // }

    // 250606.3 recovery mode support
    public boolean isServerInRecoveryMode() {

        String methodName = "isServerInRecoveryMode()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, methodName, this);
        }

        boolean ret = false;// (_serverMode == Server.RECOVERY_MODE); TBD

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, methodName, new Boolean(ret));
        }

        return ret;
    }

    public Object getService(Class c) {

        String methodName = "getService";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, methodName);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, methodName);
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
        BaseMessagingEngineImpl engine = messagingEngineRef.get();
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
                BaseMessagingEngineImpl engine = messagingEngineRef.get();
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
            JsMessagingEngineImpl me = messagingEngineRef.get();
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
