/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webserver.plugin.runtime.listeners;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.servlet.SessionCookieConfig;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.ModuleStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.ws.webcontainer.httpsession.SessionManager;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webserver.plugin.runtime.interfaces.PluginUtilityConfigGenerator;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;
import com.ibm.wsspi.webcontainer.osgi.mbeans.GeneratePluginConfig;

/**
 *
 */
@Component(service = { ApplicationStateListener.class,
                       RuntimeUpdateListener.class,
                       ModuleStateListener.class,
                       ServerQuiesceListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class GeneratePluginConfigListener implements RuntimeUpdateListener, ApplicationStateListener, ModuleStateListener, ServerQuiesceListener {

    private static final TraceComponent tc = Tr.register(GeneratePluginConfigListener.class);

    private int updatesInProgress = 0, appsInService = 0;
    private volatile ExecutorService executorSrvc;

    private GeneratePluginConfig gpc;
    private ConcurrentHashMap<String, String> cookieNames = null;

    private SessionManager smgr;
    private WsLocationAdmin locationService;

    private static GeneratePluginConfigListener theListener = null;

    // keep track of the generatePluginConfig() task so that we can cancel it in the event of a server shutdown
    private volatile Future<?> generatePluginXmlFuture = null;

    public static GeneratePluginConfigListener getGeneratePluginConfigListener() {
        return theListener;
    }

    public GeneratePluginConfigListener() {
        cookieNames = new ConcurrentHashMap<String, String>();
    }

    @Activate
    protected void activate(BundleContext bc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "GPCL: activate called.");
        theListener = this;
    }

    @Deactivate
    protected void deactivate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "GPCL: deactivate called.");
    }

    @Reference(service = com.ibm.wsspi.webcontainer.osgi.mbeans.GeneratePluginConfig.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setGeneratePluginConfig(GeneratePluginConfig mb) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "GPCL: GPC set");
        gpc = mb;
    }

    protected void unsetGeneratePluginConfig(GeneratePluginConfig mb) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "GPCL: GPC unset");
        gpc = null;
    }

    @Reference(service = java.util.concurrent.ExecutorService.class, cardinality = ReferenceCardinality.MANDATORY)
    /** Required static reference: called before activate */
    protected void setExecutor(ExecutorService executorSrvc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "GPCL: executorService set");
        this.executorSrvc = executorSrvc;
    }

    protected void unsetExecutor(ExecutorService executorSrvc) {
        this.executorSrvc = null;
    }

    @Reference(service = SessionManager.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setSessionManager(SessionManager ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Session Manager set");
        this.smgr = ref;
    }

    /** Required static reference: will be called after deactivate. Avoid NPE */
    protected void unsetSessionManager(SessionManager ref) {
    }

    @Reference(service = WsLocationAdmin.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setLocationService(WsLocationAdmin ref) {
        locationService = ref;
    }

    protected void unsetLocationService(WsLocationAdmin ref) {
    }

    /*
     * (non-Javadoc
     *
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStarting(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        setFutureGeneratePluginTask();
        this.appsInService++;
        if (smgr != null)
            this.cookieNames.put(appInfo.getName(), smgr.getDefaultAffinityCookie());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "application starting, add app to cookie map. app name : " + appInfo.getName() + ", cookie name : " + smgr.getDefaultAffinityCookie());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStarted(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "application started : " + appInfo.getName());

        runFutureGeneratePluginTask();

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStopping(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "application stopping, remove stored cookie name : " + appInfo.getName() + ", cookie name : " + this.cookieNames.get(appInfo.getName()));

        setFutureGeneratePluginTask();
        this.cookieNames.remove(appInfo.getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStopped(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "application stopped : " + appInfo.getName());

        runFutureGeneratePluginTask();
        this.appsInService--;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.runtime.update.RuntimeUpdateListener#notificationCreated(com.ibm.ws.runtime.update.RuntimeUpdateManager, com.ibm.ws.runtime.update.RuntimeUpdateNotification)
     */
    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "GPCL: RuntimeUpdate notification " + notification.getName() + ", apps in service = " + appsInService);

        if (this.appsInService > 0 && notification.getName().equals(RuntimeUpdateNotification.CONFIG_UPDATES_DELIVERED)) {
            setFutureGeneratePluginTask();
            notification.onCompletion(new ConfigUpdateListener());
        }
    }

    private class ConfigUpdateListener implements CompletionListener {

        GeneratePluginConfigListener gpcl;

        public ConfigUpdateListener() {
            gpcl = GeneratePluginConfigListener.this;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.threading.listeners.CompletionListener#successfulCompletion(java.util.concurrent.Future, java.lang.Object)
         */
        @Override
        public void successfulCompletion(Future future, Object result) {
            gpcl.runFutureGeneratePluginTask();
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.threading.listeners.CompletionListener#failedCompletion(java.util.concurrent.Future, java.lang.Throwable)
         */
        @Override
        public void failedCompletion(Future future, Throwable t) {
            gpcl.unsetFutureGeneratePluginTask();
        }

    }

    private synchronized void setFutureGeneratePluginTask() {
        this.updatesInProgress++;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setFutureGeneratePluginTask : future updates now = " + updatesInProgress);
    }

    private synchronized boolean unsetFutureGeneratePluginTask() {
        this.updatesInProgress--;

        // correct a bad value
        if (this.updatesInProgress < 0)
            this.updatesInProgress = 0;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "unsetFutureGeneratePluginTask : future updates now = " + updatesInProgress);
        return updatesInProgress == 0;
    }

    private synchronized boolean isFuturePluginTask() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "isFuturePluginTask : updates in progress = " + updatesInProgress);
        return this.updatesInProgress == 0;
    }

    private synchronized void runFutureGeneratePluginTask() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "runFutureGeneratePluginTask : future updates = " + updatesInProgress);
        if (unsetFutureGeneratePluginTask()) {
            submitGeneratePluginTask();
        }
    }

    private void submitGeneratePluginTask() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "submitGeneratePluginTask : FrameworkState.isStopping() = " + FrameworkState.isStopping());

        if (!FrameworkState.isStopping() && gpc != null && executorSrvc != null) {
            generatePluginXmlFuture = executorSrvc.submit(new Runnable() {
                @Override
                public void run() {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "generating webserver plugin");

                    // Pass the server.output.dir/logs/state/ directory to the generatePluginConfig method to ensure that
                    // the plugin-cfg.xml is written to the logs/state directory.
                    WsResource writeDirectory = locationService.getServerOutputResource("logs" + File.separatorChar + "state" + File.separatorChar);
                    ((PluginUtilityConfigGenerator) gpc).generatePluginConfig(null, writeDirectory.asFile());
                }
            });
        }
    }

    public void applicationInitialized(WebApp webApp, SessionCookieConfig sccfg) {
        String cookieName = this.cookieNames.get(webApp.getApplicationName());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "application initialized, app : " + webApp.getApplicationName() + ", old cookie name : " + this.cookieNames.get(webApp.getApplicationName())
                               + ", new cookie name : " + sccfg == null ? null : sccfg.getName());

        if (cookieName != null && sccfg != null && !cookieName.equals(sccfg.getName())) {
            synchronized (this) {
                if (isFuturePluginTask()) {
                    submitGeneratePluginTask();
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void moduleStarting(ModuleInfo moduleInfo) throws StateChangeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "module starting: " + moduleInfo.getName());
            Tr.debug(this, tc, "module starting application name: " + moduleInfo.getApplicationInfo().getName());
        }

        setFutureGeneratePluginTask();

    }

    /** {@inheritDoc} */
    @Override
    public void moduleStarted(ModuleInfo moduleInfo) throws StateChangeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "module started: " + moduleInfo.getName());
            Tr.debug(this, tc, "module started application name: " + moduleInfo.getApplicationInfo().getName());
        }

        runFutureGeneratePluginTask();
    }

    /** {@inheritDoc} */
    @Override
    public void moduleStopping(ModuleInfo moduleInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "module stopping: " + moduleInfo.getName());
            Tr.debug(this, tc, "module stopping application name: " + moduleInfo.getApplicationInfo().getName());
        }

        setFutureGeneratePluginTask();

    }

    /** {@inheritDoc} */
    @Override
    public void moduleStopped(ModuleInfo moduleInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "module stopped: " + moduleInfo.getName());
            Tr.debug(this, tc, "module stopped application name: " + moduleInfo.getApplicationInfo().getName());
        }

        runFutureGeneratePluginTask();

    }

    /** {@inheritDoc} */
    @Override
    public void serverStopping() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "serverStopping");
        }

        // cancel the current future associated with this listener
        if (generatePluginXmlFuture != null && !generatePluginXmlFuture.isDone()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "cancel outstanding future: " + generatePluginXmlFuture);
            }
            generatePluginXmlFuture.cancel(true);
        }
    }

}
