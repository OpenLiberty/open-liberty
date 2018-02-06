/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.ws.jca.cm.ConnectorService;
import com.ibm.ws.security.jca.AuthDataService;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 * Provides access to services needed by various parts of J2C/RRA code.
 *
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 *
 * OSGi methods (activate/deactivate) should be protected.
 */
public class ConnectorServiceImpl extends ConnectorService {
    private static final TraceComponent tc = Tr.register(ConnectorServiceImpl.class, J2CConstants.traceSpec, J2CConstants.NLS_FILE);

    /**
     * Auth data service.
     */
    public final AtomicServiceReference<AuthDataService> authDataServiceRef = new AtomicServiceReference<AuthDataService>("authDataService");

    /**
     * Class loader identifier service.
     */
    private ClassLoaderIdentifierService classLoaderIdentifierService;

    /**
     * Scheduled executor service for deferrable alarms.
     */
    final AtomicServiceReference<ScheduledExecutorService> deferrableSchedXSvcRef = new AtomicServiceReference<ScheduledExecutorService>("deferrableScheduledExecutor");

    /**
     * Executor service for PoolManager.
     */
    final AtomicServiceReference<ExecutorService> execSvcRef = new AtomicServiceReference<ExecutorService>("executor");

    /**
     * Scheduled executor service for non-deferrable alarms.
     */
    final AtomicServiceReference<ScheduledExecutorService> nonDeferrableSchedXSvcRef = new AtomicServiceReference<ScheduledExecutorService>("nonDeferrableScheduledExecutor");

    /**
     * RRS XA resource factory service reference.
     */
    final AtomicServiceReference<Object> rrsXAResFactorySvcRef = new AtomicServiceReference<Object>("rRSXAResourceFactory");

    /**
     * Transaction manager service reference.
     */
    EmbeddableWebSphereTransactionManager transactionManager;

    /**
     * Variable registry service reference.
     */
    private final AtomicServiceReference<VariableRegistry> variableRegistrySvcRef = new AtomicServiceReference<VariableRegistry>("variableRegistry");

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context context for this component
     */
    protected void activate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "activate", context);
        authDataServiceRef.activate(context);
        deferrableSchedXSvcRef.activate(context);
        execSvcRef.activate(context);
        nonDeferrableSchedXSvcRef.activate(context);
        rrsXAResFactorySvcRef.activate(context);
        variableRegistrySvcRef.activate(context);
    }

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context context for this component
     */
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "deactivate", context);
        authDataServiceRef.deactivate(context);
        deferrableSchedXSvcRef.deactivate(context);
        execSvcRef.deactivate(context);
        nonDeferrableSchedXSvcRef.deactivate(context);
        rrsXAResFactorySvcRef.deactivate(context);
        variableRegistrySvcRef.deactivate(context);
    }

    @Override
    public final ClassLoaderIdentifierService getClassLoaderIdentifierService() {
        return classLoaderIdentifierService;
    }

    @Override
    public final ExecutorService getLibertyThreadPool() {
        return execSvcRef.getServiceWithException();
    }

    @Override
    public final EmbeddableWebSphereTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public final VariableRegistry getVariableRegistry() {
        return variableRegistrySvcRef.getServiceWithException();
    }

    /**
     * Utility method that gets the onError setting.
     * This method should be invoked every time it is needed in order to allow for
     * changes to the onError setting.
     *
     * @return the onError setting if configured. Otherwise the default value.
     */
    private final OnError ignoreWarnOrFail(TraceComponent tc) {
        String value = null;
        try {
            VariableRegistry variableRegistry = variableRegistrySvcRef.getService();
            String key = "${" + OnErrorUtil.CFG_KEY_ON_ERROR + "}";
            value = variableRegistry.resolveString(key);
            if (!key.equals(value))
                return OnError.valueOf(value.trim().toUpperCase());
        } catch (Exception x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "onError: " + value, x);
        }
        return OnErrorUtil.getDefaultOnError();
    }

    /**
     * Ignore, warn, or fail when a configuration error occurs.
     * This is copied from Tim's code in tWAS and updated slightly to
     * override with the Liberty ignore/warn/fail setting.
     *
     * @param tc the TraceComponent from where the message originates
     * @param throwable an already created Throwable object, which can be used if the desired action is fail.
     * @param exceptionClassToRaise the class of the Throwable object to return
     * @param msgKey the NLS message key
     * @param objs list of objects to substitute in the NLS message
     * @return either null or the Throwable object
     */
    @Override
    public <T extends Throwable> T ignoreWarnOrFail(TraceComponent tc, Throwable throwable, Class<T> exceptionClassToRaise, String msgKey, Object... objs) {
        tc = tc == null ? ConnectorServiceImpl.tc : tc;

        switch (ignoreWarnOrFail(tc)) {
            case IGNORE:
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "ignoring error: " + msgKey, objs);
                return null;
            case WARN:
                Tr.warning(tc, msgKey, objs);
                return null;
            case FAIL:
                try {
                    if (throwable != null && exceptionClassToRaise.isInstance(throwable))
                        return exceptionClassToRaise.cast(throwable);

                    String message;
                    if (msgKey == null)
                        message = throwable.getMessage();
                    else
                        message = Tr.formatMessage(tc, msgKey, objs);

                    Constructor<T> con = exceptionClassToRaise.getConstructor(String.class);
                    return exceptionClassToRaise.cast(con.newInstance(message).initCause(throwable));
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
        }

        return null;
    }

    /**
     * Declarative Services method for setting the AuthDataService reference.
     *
     * @param ref reference to the service
     */
    protected void setAuthDataService(ServiceReference<AuthDataService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setAuthDataService", ref);
        authDataServiceRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the class loader identifier service
     *
     * @param svc the service
     */
    protected void setClassLoaderIdentifierService(ClassLoaderIdentifierService svc) {
        classLoaderIdentifierService = svc;
    }

    /**
     * Declarative Services method for setting the deferrable scheduled executor service reference.
     *
     * @param ref reference to the service
     */
    protected void setDeferrableScheduledExecutor(ServiceReference<ScheduledExecutorService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setDeferrableScheduledExecutor", ref);
        deferrableSchedXSvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the executor service reference.
     *
     * @param ref reference to the service
     */
    protected void setExecutor(ServiceReference<ExecutorService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setExecutor", ref);
        execSvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the non-deferrable scheduled executor service reference.
     *
     * @param ref reference to the service
     */
    protected void setNonDeferrableScheduledExecutor(ServiceReference<ScheduledExecutorService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setNonDeferrableScheduledExecutor", ref);
        nonDeferrableSchedXSvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the RRS XA resource factory service implementation reference.
     *
     * @param ref reference to the service
     */
    protected void setRRSXAResourceFactory(ServiceReference<Object> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setRRSXAResourceFactory", ref);
        rrsXAResFactorySvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the variable registry service implementation reference.
     *
     * @param ref reference to the service
     */
    protected void setVariableRegistry(ServiceReference<VariableRegistry> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setVariableRegistry", ref);
        variableRegistrySvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the AuthDataService reference.
     *
     * @param ref reference to the service
     */
    protected void unsetAuthDataService(ServiceReference<AuthDataService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unsetAuthDataService", ref);
        authDataServiceRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the class loader identifier service
     *
     * @param svc the service
     */
    protected void unsetClassLoaderIdentifierService(ClassLoaderIdentifierService svc) {
        classLoaderIdentifierService = null;
    }

    /**
     * Declarative Services method for unsetting the deferrable scheduled executor service reference.
     *
     * @param ref reference to the service
     */
    protected void unsetDeferrableScheduledExecutor(ServiceReference<ScheduledExecutorService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unsetDeferrableScheduledExecutor", ref);
        deferrableSchedXSvcRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the executor service reference.
     *
     * @param ref reference to the service
     */
    protected void unsetExecutor(ServiceReference<ExecutorService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unsetExecutor", ref);
        execSvcRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the non-deferrable scheduled executor service reference.
     *
     * @param ref reference to the service
     */
    protected void unsetNonDeferrableScheduledExecutor(ServiceReference<ScheduledExecutorService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unsetNonDeferrableScheduledExecutor", ref);
        nonDeferrableSchedXSvcRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the RRS XA resource factory service implementation reference.
     *
     * @param ref reference to the service
     */
    protected void unsetRRSXAResourceFactory(ServiceReference<Object> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unsetRRSXAResourceFactory", ref);
        rrsXAResFactorySvcRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the variable registry service implementation reference.
     *
     * @param ref reference to the service
     */
    protected void unsetVariableRegistry(ServiceReference<VariableRegistry> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unsetVariableRegistry", ref);
        variableRegistrySvcRef.unsetReference(ref);
    }

    /**
     * Declarative Services method to set the transaction manager.
     */
    protected void setEmbeddableWebSphereTransactionManager(EmbeddableWebSphereTransactionManager tm) {
        transactionManager = tm;
    }

    /**
     * Declarative Services method to unset the transaction manager.
     */
    protected void unsetEmbeddableWebSphereTransactionManager(EmbeddableWebSphereTransactionManager tm) {
        transactionManager = null;
    }
}