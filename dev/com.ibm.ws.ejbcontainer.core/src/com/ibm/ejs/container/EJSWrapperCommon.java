/*******************************************************************************
 * Copyright (c) 2001, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapper.LOCAL_BEAN_WRAPPER_FIELD;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.ejb.EJBException;
import javax.ejb.EJBLocalObject;
import javax.rmi.PortableRemoteObject;

import com.ibm.ejs.container.util.DeploymentUtil;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.ejs.container.util.NameUtil;
import com.ibm.ejs.util.Util;
import com.ibm.ejs.util.cache.Cache;
import com.ibm.ejs.util.cache.Element;
import com.ibm.ejs.util.cache.WrapperBucket;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ejbcontainer.failover.SfFailoverClient;
import com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapper;
import com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperProxy;
import com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType;
import com.ibm.ws.ejbcontainer.jitdeploy.JITDeploy;
import com.ibm.ws.ejbcontainer.util.FieldClassValue;
import com.ibm.ws.ejbcontainer.util.FieldClassValueFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * This is a wrapper of the EJSWrapper and EJSLocalWrapper objects for the
 * Local and Remote Interface implementations. This replaces the
 * syntactic functions of the old EJSWrapper object in the Container to
 * avoid duplicate maintenance in supporting the local and remote
 * interface simultaneously.
 **/
public final class EJSWrapperCommon extends Element // d195605
{
    // This class uses the following terminology:
    //
    // - proxy - the proxy object returned to an application as the client view
    // - proxy state - if local wrapper proxies are enabled, then the proxy is a
    //   WrapperProxy, which contains a WrapperProxyState
    // - wrapper - the proxy object that performs pre-/post-invoke processing
    // - wrapper base - the EJSWrapperBase state required to perform
    //   pre-/post-invoke processing.
    //
    // For a normal local component or business view, the proxy, wrapper, and
    // wrapper base are all the same instance of a generated class.  The local
    // component view class extends EJSLocalWrapper, and the local business view
    // class extends BusinessLocalWrapper.
    //
    // For a proxied local component or business view, the proxy is a generated
    // class that locates a normal local view.  The proxied local component view
    // class extends EJSLocalWrapperProxy or EJSLocalHomeWrapperProxy, and the
    // proxied local business view class extends BusinessLocalWrapperProxy.
    //
    // For a normal local no-interface view, the proxy and wrapper are the same
    // instance of a generated class that extends the enterprise bean class and
    // implements LocalBeanWrapper.  The wrapper base is stored in an
    // EJSWrapperBase in the generated class.
    //
    // For a proxied local no-interface view, the proxy is a generated class
    // that locates a normal local view.  The proxied class extends the
    // enterprise bean class and implements LocalBeanWrapperProxy.  The proxy
    // state is stored in a BusinessLocalWrapperProxy in the generated class.
    //
    // For a remote view, the proxy is a generated class that extends Stub.  The
    // wrapper and wrapper base are the same instance of a generated class.  The
    // remote component view class extends EJSWrapper, and the remote business
    // view extends BusinessRemoteWrapper.  The remote wrapper is set as the
    // target of the generated servant/tie class.
    //
    // The naming convention for methods in this class:
    //
    //   get*Object
    //     - Returns a proxy / client view to be returned to an application
    //
    //   get*Wrapper
    //     - Returns a subclass of EJSWrapperBase, or for no-interface view
    //       a class that implements LocalBeanWrapper.  For remote, this will be
    //       the servant.  For local, this will differ from the client view if
    //       local wrapper proxies are enabled.
    //
    //   get*WrapperBase
    //     - Returns an EJSWrapperBase.  This is typically the same as
    //       get*Wrapper, except if that is a no-interface view, and then the
    //       underlying wrapper base will be returned.
    //
    //   get*WrapperProxyState
    //     - If local wrapper proxies are enabled, returns the state that
    //       underlies a local wrapper proxy.

    //d121558
    private static final TraceComponent tc =
                    Tr.register(EJSWrapperCommon.class, "EJBContainer",
                                "com.ibm.ejs.container.container");

    private static final String CLASS_NAME = "com.ibm.ejs.container.EJSWrapperCommon";

    private static final boolean isZOS = EJSPlatformHelper.isZOS(); // LIDB2775-23.9

    /**
     * Reference to the remote home or remote component interface wrapper.
     **/
    protected EJSWrapper remoteWrapper;

    /**
     * Reference to the remote home, remote component interface or
     * remote EJBFactory wrapper.
     **/
    private EJSRemoteWrapper ivRemoteObjectWrapper;

    /**
     * Reference to the local EJSLocalWrapper object.
     */
    protected EJSLocalWrapper localWrapper;

    /**
     * Reference to either the wrapper or the wrapper proxy for the local
     * component views.
     * 
     * <p>Note: EJBLocalHome wrappers have traditionally also implemented
     * EJBLocalObject (by extending EJSLocalWrapper). We do the same for
     * EJBLocalHome wrapper proxies, so the type of this field can be
     * EJBLocalObject rather than requiring callers to cast.
     */
    private EJBLocalObject localObject; // F58064

    /**
     * The wrapper proxy state for the local component views.
     */
    private WrapperProxyState localWrapperProxyState; // F58064

    /**
     * Reference to the local business implementation (wrapper) objects.
     * May include No-Interface view wrapper as first entry. // F743-1756
     **/
    // d366807.1
    private Object ivBusinessLocal[];

    /**
     * Reference to either the wrapper or the wrapper proxy for each business
     * interface. May include the No-Interface view as the first entry. This
     * array will be the same as ivBusinessLocal if wrapper proxies are not in
     * use.
     */
    private Object[] ivBusinessLocalProxies; // F58064

    /**
     * The wrapper proxy states for each business interface. May include the
     * No-Interface view as the first entry. This array will be null if
     * wrapper proxies are not in use.
     */
    private WrapperProxyState[] ivBusinessLocalWrapperProxyStates; // F58064

    /**
     * Reference to the remote business implementation (wrapper) objects.
     **/
    // d366807.1
    private BusinessRemoteWrapper ivBusinessRemote[];

    /**
     * Indication of whether remote business wrapper is already registered or not.
     */
    private boolean ivBusinessRemoteRegistered[]; // d416391

    /**
     * Servant registration deferral flag.
     */
    private boolean isRemoteRegistered; // d173022.1

    /**
     * Cached access to the bean.
     */
    // F61004.6
    StatefulBeanO ivCachedBeanO;

    /**
     * BeanId for the set of wrappers. Added for performance.
     **/
    // d366807.5
    private final BeanId ivBeanId;

    /**
     * Bean Meta Data associated with the contained wrappers.
     **/
    // d366807.1
    private BeanMetaData ivBMD;

    private final static ThreadContextAccessor svThreadContextAccessor =
                    AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    /**
     * EJSWrapperCommmon Constructor.
     */
    public EJSWrapperCommon(Class<?> remoteClass,
                            Class<?> localClass,
                            BeanId id,
                            BeanMetaData bmd,
                            EJBPMICollaborator pmiBean,
                            EJSContainer container,
                            WrapperManager wrapperManager,
                            boolean isHome)
        throws RemoteException
    {
        // The key to an EJSWrapperCommon element in the Wrapper Cache will
        // always be the byte array of the beanId.                       d195605
        super(id.getByteArray());

        // Previously, the BeanId was only stored in the wrappers themselves,
        // but there are now so many wrapper types, and most won't all exist,
        // so it is much quicker to find the BeanId if it is now also
        // stored on the EJSWrapperCommon as well.                     d366807.5
        ivBeanId = id;

        String className = null;
        try {
            this.ivBMD = bmd; // d366807.1
            if (remoteClass != null) {
                className = remoteClass.getName();
                remoteWrapper = (EJSWrapper) remoteClass.newInstance();
                ivRemoteObjectWrapper = remoteWrapper; // d440604
                remoteWrapper.beanId = id;
                remoteWrapper.bmd = bmd;
                remoteWrapper.methodInfos = isHome ? bmd.homeMethodInfos
                                : bmd.methodInfos;
                remoteWrapper.isolationAttrs = isHome ? bmd.homeIsolationAttrs
                                : bmd.isolationAttrs;
                remoteWrapper.methodNames = isHome ? bmd.homeMethodNames
                                : bmd.methodNames;
                remoteWrapper.container = container;
                remoteWrapper.wrapperManager = wrapperManager;
                remoteWrapper.ivPmiBean = pmiBean; // d174057.2
                remoteWrapper.ivCommon = this; // d140003.9
                remoteWrapper.isManagedWrapper = true; // d174057.2
                remoteWrapper.ivInterface = isHome ? WrapperInterface.HOME // d366807
                : WrapperInterface.REMOTE; // d366807

                // LIDB2018 start
                // For Stateful Session bean instances, with remote interfaces,
                // the Cluster Identity must be for a Cluster of one process, since
                // Stateful Session beans may not exist on multiple servers.   LI2401-11
                if (isHome || (bmd.isStatefulSessionBean() == false))
                {
                    // Bean is either a home bean or something other than SFSB.
                    // In that case, the bean is WLMable so use the
                    // cluster Identity in the BeanMetaData.
                    remoteWrapper.ivCluster = bmd.ivCluster; // LI2401-11
                }
                else
                {
                    // For Stateful Session bean instances, with remote interfaces,
                    // the Cluster Identity must be for a Cluster of one process,
                    // since Stateful Session beans can not exist on multiple
                    // servers.  So assume not WLMable and check whether
                    // SFSB failover is enabled.  No WLM cluster unless
                    // failover is enabled.
                    remoteWrapper.ivCluster = null; // LI2401-11
                    SfFailoverClient failover = bmd.ivSfFailoverClient;
                    if (failover != null)
                    {
                        // When SFSB failover is enabled, get the cluster Identity
                        // from the failover cache entry for this bean if one exists.
                        // Note, null is returned if failover entry does not exist.
                        remoteWrapper.ivCluster = failover.getWLMIdentity(id);
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) // d204278
                    {
                        Tr.debug(tc, "SFSB WLM Cluster Identity from failover: " +
                                     remoteWrapper.ivCluster);
                    }
                }
                // LIDB2018 end
            }
            if (localClass != null) {
                className = localClass.getName();
                localWrapper = (EJSLocalWrapper) localClass.newInstance();
                localWrapper.beanId = id;
                localWrapper.bmd = bmd;
                localWrapper.methodInfos = isHome ? bmd.localHomeMethodInfos
                                : bmd.localMethodInfos;
                localWrapper.methodNames = isHome ? bmd.localHomeMethodNames
                                : bmd.localMethodNames;
                localWrapper.container = container;
                localWrapper.wrapperManager = wrapperManager;
                localWrapper.ivPmiBean = pmiBean; // d174057.2
                localWrapper.ivCommon = this; // d140003.9
                localWrapper.isManagedWrapper = false; // d174057.2
                localWrapper.ivInterface = isHome ? WrapperInterface.LOCAL_HOME // d366807
                : WrapperInterface.LOCAL; // d366807

                localObject = localWrapper;

                if (bmd.ivIndirectLocalProxies) // F58064
                {
                    localWrapperProxyState = isHome ? new WrapperProxyState.LocalHome(bmd.getHome(), localWrapper) :
                                    new WrapperProxyState.LocalObject(bmd.getHome(), localWrapper);

                    Constructor<?> localProxyCtor = isHome ? bmd.homeLocalImplProxyConstructor :
                                    bmd.localImplProxyConstructor;
                    localObject = (EJBLocalObject) localProxyCtor.newInstance(localWrapperProxyState);
                }
            }

            // Create the Local Business Wrappers, if they exist.      d366807.1
            if (!isHome && bmd.ivBusinessLocalImplClasses != null)
            {
                Object wrapper = null;
                BusinessLocalWrapper bLocal = null;

                int numLocalWrappers = bmd.ivBusinessLocalImplClasses.length;
                ivBusinessLocal = new Object[numLocalWrappers];
                ivBusinessLocalProxies = ivBusinessLocal;

                if (bmd.ivIndirectLocalProxies) // F58064
                {
                    ivBusinessLocalProxies = new Object[numLocalWrappers];
                    ivBusinessLocalWrapperProxyStates = new WrapperProxyState[numLocalWrappers];
                }

                for (int i = 0; i < numLocalWrappers; ++i)
                {
                    className = bmd.ivBusinessLocalImplClasses[i].getName();
                    wrapper = bmd.ivBusinessLocalImplClasses[i].newInstance();

                    // For No-Interface view (LocalBean), the 'wrapper' will not
                    // subclass EJSWrapperBase, but the EJB itself, so a separate
                    // EJSWrapperBase must be created to hold the metadata and
                    // associated with the real wrapper.                 F743-1756
                    if (i == 0 && bmd.ivLocalBean)
                    {
                        bLocal = new BusinessLocalWrapper();
                        bmd.ivLocalBeanWrapperField.set(wrapper, bLocal);
                    }
                    // Otherwise, normal wrapper - subclasses BusinessLocalWrapper
                    else
                    {
                        bLocal = (BusinessLocalWrapper) wrapper;
                    }

                    bLocal.beanId = id;
                    bLocal.bmd = bmd;
                    bLocal.methodInfos = bmd.localMethodInfos;
                    bLocal.methodNames = bmd.localMethodNames;
                    bLocal.container = container;
                    bLocal.wrapperManager = wrapperManager;
                    bLocal.ivPmiBean = pmiBean;
                    bLocal.ivCommon = this;
                    bLocal.isManagedWrapper = false;
                    bLocal.ivInterface = WrapperInterface.BUSINESS_LOCAL;
                    bLocal.ivBusinessInterfaceIndex = i; // d452386

                    ivBusinessLocal[i] = wrapper;

                    if (bmd.ivIndirectLocalProxies) // F58064
                    {
                        WrapperProxyState state = new WrapperProxyState.BusinessLocal
                                        (bmd.getHome(), id, ivBusinessLocal[i], bmd.ivBusinessLocalInterfaceClasses[i], i);
                        ivBusinessLocalWrapperProxyStates[i] = state;

                        ivBusinessLocalProxies[i] = bmd.ivBusinessLocalImplProxyConstructors[i].newInstance(state);
                    }
                }
            }

            // Create the Remote Business Wrappers, if they exist.     d366807.1
            if (!isHome && bmd.ivBusinessRemoteImplClasses != null)
            {
                BusinessRemoteWrapper bRemote = null;

                int numRemoteWrappers = bmd.ivBusinessRemoteImplClasses.length;
                ivBusinessRemote = new BusinessRemoteWrapper[numRemoteWrappers];
                ivBusinessRemoteRegistered = new boolean[numRemoteWrappers]; // d416391

                for (int i = 0; i < numRemoteWrappers; ++i)
                {
                    className = bmd.ivBusinessRemoteImplClasses[i].getName();
                    bRemote = (BusinessRemoteWrapper) bmd.ivBusinessRemoteImplClasses[i].newInstance();
                    bRemote.beanId = id;
                    bRemote.bmd = bmd;
                    bRemote.methodInfos = bmd.methodInfos;
                    bRemote.isolationAttrs = bmd.isolationAttrs;
                    bRemote.methodNames = bmd.methodNames;
                    bRemote.container = container;
                    bRemote.wrapperManager = wrapperManager;
                    bRemote.ivPmiBean = pmiBean;
                    bRemote.ivCommon = this;
                    bRemote.isManagedWrapper = true;
                    if ((Remote.class).isAssignableFrom(bmd.ivBusinessRemoteInterfaceClasses[i]))
                        bRemote.ivInterface = WrapperInterface.BUSINESS_RMI_REMOTE;
                    else
                        bRemote.ivInterface = WrapperInterface.BUSINESS_REMOTE;

                    bRemote.ivBusinessInterfaceIndex = i; // d452386

                    // For Stateful Session bean instances, with remote interfaces,
                    // the Cluster Identity must be for a Cluster of one process, since
                    // Stateful Session beans may not exist on multiple servers.
                    if (bmd.isStatefulSessionBean() == false)
                    {
                        // Bean is something other than SFSB.
                        // In that case, the bean is WLMable so use the
                        // cluster Identity in the BeanMetaData.
                        bRemote.ivCluster = bmd.ivCluster;
                    }
                    else
                    {
                        // For Stateful Session bean instances, with remote interfaces,
                        // the Cluster Identity must be for a Cluster of one process,
                        // since Stateful Session beans can not exist on multiple
                        // servers.  So assume not WLMable and check whether
                        // SFSB failover is enabled.  No WLM cluster unless
                        // failover is enabled.
                        bRemote.ivCluster = null;
                        SfFailoverClient failover = bmd.ivSfFailoverClient;
                        if (failover != null)
                        {
                            // When SFSB failover is enabled, get the cluster Identity
                            // from the failover cache entry for this bean if one
                            // exists. Note, null is returned if failover cache entry
                            // does not exist.
                            bRemote.ivCluster = failover.getWLMIdentity(id);
                        }
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "SFSB WLM Cluster Identity from failover: " +
                                         bRemote.ivCluster);
                    }

                    ivBusinessRemote[i] = bRemote;
                }
            }
        } catch (Exception ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".EJSWrapperCommon", "123", this);
            throw new ContainerException(className, ex);
        }
    }

    /**
     * Get the value of a field from an object.
     * 
     * @param fieldClassValue the Field class value
     * @param obj the object
     * @return the field value
     */
    private static Object getFieldValue(FieldClassValue fieldClassValue, Object obj) {
        try {
            return fieldClassValue.get(obj.getClass()).get(obj);
        } catch (IllegalAccessException e) {
            // FieldClassValueFactory returns ClassValue that make the Field
            // accessible, so this should not happen.
            throw new IllegalStateException(e);
        }
    }

    private static FieldClassValue svLocalBeanWrapperBaseFieldClassValue = FieldClassValueFactory.create(EJBWrapper.LOCAL_BEAN_WRAPPER_FIELD);

    /**
     * Returns the EJSWrapperBase object for an object that implements
     * LocalBeanWrapper. This operation should never fail, but if it does,
     * exceptions will be thrown as EJBException.
     * 
     * @param obj the LocalBeanWrapper
     * @return the EJSWrapperBase for the local bean wrapper
     */
    public static EJSWrapperBase getLocalBeanWrapperBase(LocalBeanWrapper obj) {
        return (EJSWrapperBase) getFieldValue(svLocalBeanWrapperBaseFieldClassValue, obj);
    }

    private static FieldClassValue svLocalBeanWrapperProxyStateFieldClassValue = FieldClassValueFactory.create(EJBWrapperProxy.LOCAL_BEAN_PROXY_FIELD);

    /**
     * Returns the wrapper proxy object for an object that implements
     * LocalBeanWrapperProxy. This operation should never fail, but if it
     * does, exceptions will be thrown as EJBException.
     * 
     * @param obj the LocalBeanWrapperProxy
     * @return the wrapper proxy for the local bean wrapper proxy
     */
    public static WrapperProxyState getLocalBeanWrapperProxyState(LocalBeanWrapperProxy obj) { // F58064
        BusinessLocalWrapperProxy proxyStateHolder = (BusinessLocalWrapperProxy) getFieldValue(svLocalBeanWrapperProxyStateFieldClassValue, obj);
        return proxyStateHolder.ivState;
    }

    /**
     * EJSWrapperCommmon constructor for EJBFactory wrapper.
     * 
     * Initializes EJBFactory wrapper with minimal data. The EJBFactory
     * Wrapper is NOT intended to go through EjbPreInvoke. If EjbPreInvoke
     * processing is required, it will be invoked on the wrapper of the
     * home the factory is accessing.
     **/
    // d440604
    public EJSWrapperCommon(EJSRemoteWrapper ejbFactory,
                            BeanId beanId,
                            Object cluster,
                            EJSContainer container)
    {
        // The key to an EJSWrapperCommon element in the Wrapper Cache will
        // always be the byte array of the beanId.                       d195605
        super(beanId.getByteArray());

        // Previously, the BeanId was only stored in the wrappers themselves,
        // but there are now so many wrapper types, and most won't all exist,
        // so it is much quicker to find the BeanId if it is now also
        // stored on the EJSWrapperCommon as well.                     d366807.5
        ivBeanId = beanId;

        ivBMD = null; // EJBFactories have no BMD

        ivRemoteObjectWrapper = ejbFactory;

        ivRemoteObjectWrapper.beanId = beanId;
        ivRemoteObjectWrapper.bmd = null; // no metadata for HomeOfHomes
        ivRemoteObjectWrapper.methodInfos = null; // no methods go through preInvoke
        ivRemoteObjectWrapper.methodNames = null; // no methods go through preInvoke
        ivRemoteObjectWrapper.isolationAttrs = null; // not used after EJB 1.x
        ivRemoteObjectWrapper.container = container;
        ivRemoteObjectWrapper.wrapperManager = container.wrapperManager;
        ivRemoteObjectWrapper.ivPmiBean = null; // no PMI metrics for this factory
        ivRemoteObjectWrapper.ivCommon = this;
        ivRemoteObjectWrapper.isManagedWrapper = true;
        ivRemoteObjectWrapper.ivInterface = WrapperInterface.HOME; // Like a Home
        ivRemoteObjectWrapper.ivCluster = cluster;
    }

    /**
     * Return instance of remote EJSWrapper. <p>
     * 
     * This will return either the remote home or remote component interface.
     * The remote home wrapper extends EJSWrapper, which means it (somewhat
     * unexpectedly) implements EJBObject in addition to EJBHome. <p>
     * 
     * @throws IllegalStateException if the EJB does not have the interface
     */
    public EJSWrapper getRemoteWrapper()
    {
        // d116480 Begin
        if (remoteWrapper == null) {
            throw new IllegalStateException("Remote interface not defined");
        }
        // d116480 End
        registerServant(); // d173022.1
        return remoteWrapper;
    }

    /**
     * Return instance of remote object (wrapper). <p>
     * 
     * This will return either the remote home, remote component interface,
     * or remote EJBFactory wrapper. Will NOT return a remote business
     * interface wrapper. <p>
     * 
     * @throws IllegalStateException if the EJB does not have the interface
     **/
    public EJSRemoteWrapper getRemoteObjectWrapper()
    {
        // d739542 Begin
        if (ivRemoteObjectWrapper == null) {
            throw new IllegalStateException("Remote interface not defined");
        }
        // d739542 End
        registerServant();
        return ivRemoteObjectWrapper;
    }

    // d173022.1 Begins
    /**
     * Register the remote wrapper servant to ORB.
     */
    protected void registerServant()
    {
        if (ivRemoteObjectWrapper != null)
        {
            // synchronized on the remote Wrapper to set the isRemoteRegistered flag
            synchronized (ivRemoteObjectWrapper)
            {
                if (!isRemoteRegistered)
                {

                    try
                    {
                        if (!isZOS) // LIDB2775-23.9
                        {

                            // Servant registration must be performed with the application classloader
                            // in force.  This is especially needed for JITDeploy since
                            // the JIT pluggin will be on the application classloader.
                            // Without the JIT pluggin the classloader will not trigger
                            // JITDeploy to create the stub.  //d521388
                            Object originalLoader = ThreadContextAccessor.UNCHANGED;
                            try
                            {
                                if (ivBMD != null) // EJBFactories have no BMD d529446
                                {
                                    originalLoader = EJBThreadData.svThreadContextAccessor.pushContextClassLoaderForUnprivileged(ivBMD.classLoader); //PK83186

                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    {
                                        if (originalLoader != ThreadContextAccessor.UNCHANGED)
                                        {
                                            Tr.debug(tc, "registerServant : old ClassLoader = " + originalLoader);
                                            Tr.debug(tc, "registerServant : new ClassLoader = " + ivBMD.classLoader);
                                        }
                                        else
                                        {
                                            Tr.debug(tc, "registerServant : current ClassLoader = " + ivBMD.classLoader);
                                        }
                                    }
                                }
                                ivRemoteObjectWrapper.container.getEJBRuntime().registerServant(ivRemoteObjectWrapper.beanId.getByteArray(), ivRemoteObjectWrapper);
                            } finally
                            {
                                EJBThreadData.svThreadContextAccessor.popContextClassLoaderForUnprivileged(originalLoader);
                            } // finally

                        } // if( !isZOS )

                        isRemoteRegistered = true;
                    } catch (Exception ex)
                    {
                        FFDCFilter.processException(ex, CLASS_NAME +
                                                        ".registerServant",
                                                    "184", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        {
                            Tr.event(tc, "Failed to register wrapper instance",
                                     new Object[] { ivRemoteObjectWrapper, ex });
                        }
                    }

                } // if ( !isRemoteRegistered )

            } // synchronized( ivRemoteObject )

        } // if ( ivRemoteObject != null )

    }

    /**
     * Register the BusinessRemoteWrapper servant to ORB.
     */
    // d416391 added entire method.
    private void registerServant(BusinessRemoteWrapper wrapper, int i)
    {
        // synchronized on the remote Wrapper to set the isRemoteRegistered flag
        synchronized (wrapper)
        {
            if (!ivBusinessRemoteRegistered[i])
            {
                try
                {
                    if (!isZOS)
                    {
                        // Since there may be multiple remote interfaces per bean
                        // type, a unique WrapperId must be formed by combining the
                        // BeanId with specific remote interface information. d419704
                        WrapperId wrapperId = new WrapperId
                                        (wrapper.beanId.getByteArrayBytes(),
                                                        wrapper.bmd.ivBusinessRemoteInterfaceClasses[i].getName(),
                                                        i);

                        // Servant registration must be performed with the application classloader
                        // in force.  This is especially needed for JITDeploy since
                        // the JIT pluggin will be on the application classloader.
                        // Without the JIT pluggin the classloader will not trigger
                        // JITDeploy to create the stub.  //d521388
                        Object originalLoader = ThreadContextAccessor.UNCHANGED;
                        try
                        {
                            if (ivBMD != null) // EJBFactories have no BMD.   d529446
                            {
                                originalLoader = EJBThreadData.svThreadContextAccessor.pushContextClassLoaderForUnprivileged(ivBMD.classLoader); //PK83186

                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                {
                                    if (originalLoader != ThreadContextAccessor.UNCHANGED)
                                    {
                                        Tr.debug(tc, "registerServant : old ClassLoader = " + originalLoader);
                                        Tr.debug(tc, "registerServant : new ClassLoader = " + ivBMD.classLoader);
                                    }
                                    else
                                    {
                                        Tr.debug(tc, "registerServant : current ClassLoader = " + ivBMD.classLoader);
                                    }
                                }
                            }
                            wrapper.container.getEJBRuntime().registerServant(wrapperId, wrapper);
                        } finally
                        {
                            EJBThreadData.svThreadContextAccessor.popContextClassLoaderForUnprivileged(originalLoader);
                        } // finally

                    } // if( !isZOS )

                    ivBusinessRemoteRegistered[i] = true;
                } catch (Throwable ex)
                {
                    FFDCFilter.processException(ex, CLASS_NAME + ".registerServant", "439", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    {
                        Tr.event(tc, "Failed to register wrapper instance", new Object[] { wrapper, ex });
                    }
                }

            } // if ( ! ivBusinessRemoteRegistered[i] )

        } //synchronized( wrapper )

    }

    /**
     * Disconnect all wrappers. Unregisters the remote wrapper servant from
     * ORB, and disconnects local wrapper proxy states.
     */
    protected void disconnect()
    {
        if (localWrapperProxyState != null) // F58064
        {
            localWrapperProxyState.disconnect();
        }

        if (ivBusinessLocalWrapperProxyStates != null) // F58064
        {
            for (WrapperProxyState state : ivBusinessLocalWrapperProxyStates)
            {
                state.disconnect();
            }
        }

        if (ivRemoteObjectWrapper != null)
        { // synchronized on the remote Wrapper to set the isRemoteRegistered flag
            synchronized (ivRemoteObjectWrapper)
            {
                if (isRemoteRegistered)
                {
                    try
                    {
                        // @PQ98903A
                        // On zOS even though we don't call the object
                        // adapter to register the servant, we need
                        // to call it to unregister it if the wrapper
                        // is connected to the ORB (i.e. it has a Tie).
                        if ((!isZOS) || (ivRemoteObjectWrapper.intie != null)) // @PQ98903C
                        {
                            ivRemoteObjectWrapper.container.getEJBRuntime().unregisterServant(ivRemoteObjectWrapper);
                        }
                        isRemoteRegistered = false;
                    } catch (Exception ex)
                    {
                        FFDCFilter.processException(ex, CLASS_NAME +
                                                        ".unregisterServant",
                                                    "207", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        {
                            Tr.event(tc, "Failed to unregister wrapper instance",
                                     new Object[] { ivRemoteObjectWrapper, ex });
                        }
                    }
                }
            }
        }

        // d416391 start
        // Unregister any BusinessRemoteWrapper objects that are registered with ORB.
        if (ivBusinessRemote != null) // d416391 d424839
        {
            int numWrappers = ivBusinessRemote.length;
            for (int i = 0; i < numWrappers; ++i)
            {
                BusinessRemoteWrapper wrapper = ivBusinessRemote[i];
                synchronized (wrapper)
                {
                    if (ivBusinessRemoteRegistered[i])
                    {
                        try
                        {
                            // On zOS even though we don't call the object adapter to register
                            // the servant, we need to call it to unregister it if the wrapper
                            // is connected to the ORB (i.e. it has a Tie).
                            if ((!isZOS) || (wrapper.intie != null))
                            {
                                wrapper.container.getEJBRuntime().unregisterServant(wrapper);
                            }
                            ivBusinessRemoteRegistered[i] = false;
                        } catch (Throwable ex)
                        {
                            FFDCFilter.processException(ex, CLASS_NAME + ".unregisterServant",
                                                        "516", this);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            {
                                Tr.event(tc, "Failed to unregister wrapper instance",
                                         new Object[] { wrapper, ex });
                            }
                        }
                    }
                } // end synchronized
            } // end for
        } // d416391 end
    }

    // d173022.1 Ends

    /**
     * Returns a client proxy object for the local component view.
     */
    public EJBLocalObject getLocalObject()
    {
        if (localObject != null)
        {
            if (localWrapperProxyState != null &&
                localWrapperProxyState.ivWrapper == null)
            {
                localWrapperProxyState.connect(ivBeanId, localWrapper);
            }
            return localObject;
        }

        throw new IllegalStateException("Local interface not defined");
    }

    /**
     * Return instance of local EJSLocalWrapper. This method should only be
     * used in the rare cases when an actual EJSLocalWrapper object is needed
     * rather than a client proxy.
     * 
     * @see #getLocalObject
     */
    public EJSLocalWrapper getLocalWrapper()
    {
        // d116480 Begin
        if (localWrapper == null) {
            throw new IllegalStateException("Local interface not defined");
        }
        // d116480 End
        return localWrapper;
    }

    public WrapperProxyState getLocalWrapperProxyState() // F58064
    {
        return localWrapperProxyState;
    }

    /**
     * Method to get a business object given the name of the interface.
     * 
     * @param interfaceName the interface name
     * @return the business object
     * @throws IllegalStateException if the interface name is not valid
     * @throws RemoteException if an error occurs obtaining a remote object
     */
    public Object getBusinessObject(String interfaceName) throws RemoteException {
        int interfaceIndex = ivBMD.getLocalBusinessInterfaceIndex(interfaceName);
        if (interfaceIndex != -1) {
            return getLocalBusinessObject(interfaceIndex);
        }

        interfaceIndex = ivBMD.getRemoteBusinessInterfaceIndex(interfaceName);
        if (interfaceIndex != -1) {
            return getRemoteBusinessObject(interfaceIndex);
        }

        throw new IllegalStateException("Requested business interface not found : " + interfaceName);
    }

    /**
     * Method to get the local business object, given the index of the
     * interface. The returned object will be a wrapper, a no-interface
     * wrapper, or a local wrapper proxy.
     * 
     * @param interfaceIndex index of the interface
     * @return the object at the passed in interface
     */
    public Object getLocalBusinessObject(int interfaceIndex)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getLocalBusinessObject : " +
                         ivBusinessLocalProxies[interfaceIndex].getClass().getName());

        if (ivBusinessLocalWrapperProxyStates != null &&
            ivBusinessLocalWrapperProxyStates[interfaceIndex].ivWrapper == null)
        {
            ivBusinessLocalWrapperProxyStates[interfaceIndex].connect(ivBeanId,
                                                                      ivBusinessLocal[interfaceIndex]);
        }

        return ivBusinessLocalProxies[interfaceIndex];
    }

    /**
     * Method to get the local business wrapper base.
     */
    public EJSWrapperBase getLocalBusinessWrapperBase(int interfaceIndex) {
        if (interfaceIndex == 0 && ivBMD.ivLocalBean) {
            return getLocalBeanWrapperBase((LocalBeanWrapper) ivBusinessLocal[0]);
        }
        return (EJSWrapperBase) ivBusinessLocal[0];
    }
    
    public Object getRemoteHomeObject(Object stub, Class<?> homeInterfaceClass) throws RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getRemoteHomeObject : " + homeInterfaceClass);
        
        
        ClassLoader moduleContextClassLoader = ivBMD.ivContextClassLoader;
        ClassLoader contextClassLoader = svThreadContextAccessor.getContextClassLoaderForUnprivileged(Thread.currentThread());

        try {
            Class<?> contextHomeInterfaceClass;
            if (contextClassLoader == moduleContextClassLoader) {
                contextHomeInterfaceClass = homeInterfaceClass;
            } else {
                contextHomeInterfaceClass = contextClassLoader.loadClass(homeInterfaceClass.getName());
            }
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getRemoteHomeObject : Performing narrow with contextClassLoader and interface " + contextHomeInterfaceClass);
            return PortableRemoteObject.narrow(stub, contextHomeInterfaceClass);
        } catch (Exception ex) {
            // If the context class loader cannot be used to narrow the stub and
            // is not the module classloader, then this is probably a pure remote
            // client lookup.  In that case, the context class loader is the WAS
            // server class loader, which will not have application stubs.
            // Attempt the narrow again using the module ClassLoader; if that
            // still fails then return the servant stub directly and hope the ORB
            // does the right thing on the client.
            if (contextClassLoader != moduleContextClassLoader) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "ignoring loadClass/narrow exception, attempting again with bean ClassLoader", ex);

                Object origCL = svThreadContextAccessor.pushContextClassLoaderForUnprivileged(moduleContextClassLoader);
                try {
                    return PortableRemoteObject.narrow(stub, homeInterfaceClass);
                } catch (Exception ex2) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "ignoring loadClass/narrow exception", ex2);
                } finally {
                    svThreadContextAccessor.popContextClassLoaderForUnprivileged(origCL);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "ignoring loadClass/narrow exception", ex);
            }
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getRemoteHomeObject : Could not narrow, returning stub");
        return stub;
        
    }

    /**
     * Method to get the remote business object, given the index of the
     * interface.
     * 
     * @param interfaceIndex index of the interface
     * @return the object at the passed in interface
     * @throws RemoteException
     */
    public Object getRemoteBusinessObject(int interfaceIndex)
                    throws RemoteException
    {
        Class<?>[] bInterfaceClasses = ivBMD.ivBusinessRemoteInterfaceClasses;
        BusinessRemoteWrapper wrapper = ivBusinessRemote[interfaceIndex];

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getRemoteBusinessObject : " +
                         wrapper.getClass().getName());

        registerServant(wrapper, interfaceIndex);

        ClassLoader moduleContextClassLoader = ivBMD.ivContextClassLoader;
        Object result = getRemoteBusinessReference(wrapper, moduleContextClassLoader, bInterfaceClasses[interfaceIndex]);

        return result;
    }

    /**
     * Convert a remote business interface wrapper to a stub. The EJB 3
     * specification does not require clients to narrow the result of looking up
     * a remote business interface. There are two scenarios of interest:
     * <ul>
     * <li>When a colocated naming lookup occurs, the method creates a stub
     * that is properly narrowed for the caller (context) class loader.
     * <li>When a remote naming lookup occurs, the context class loader is a
     * server class loader, which is unlikely to have a proper stub. Instead,
     * the EJB class loader is returned, and we rely on the ORB returning a
     * properly narrowed stub when it is deserialized by the client. The ORB
     * is not required to return a properly narrowed stub, but we don't have any
     * better options.
     * </ul>
     * 
     * @param wrapper the remote business interface wrapper
     * @param moduleContextClassLoader the EJB module context class loader
     * @param businessClass the EJB business interface class
     * @return the stub
     */
    private static Object getRemoteBusinessReference(final EJSRemoteWrapper remote,
                                                     final ClassLoader moduleContextClassLoader,
                                                     final Class<?> businessClass) throws NoSuchObjectException {
        Object stub = remote.container.getEJBRuntime().getRemoteReference(remote);
        ClassLoader contextClassLoader = svThreadContextAccessor.getContextClassLoaderForUnprivileged(Thread.currentThread());

        try {
            Class<?> contextBusinessClass;
            if (contextClassLoader == moduleContextClassLoader) {
                contextBusinessClass = businessClass;
            } else {
                contextBusinessClass = contextClassLoader.loadClass(businessClass.getName());
            }

            return PortableRemoteObject.narrow(stub, contextBusinessClass);
        } catch (Exception ex) {
            // If the context class loader cannot be used to narrow the stub and
            // is not the module classloader, then this is probably a pure remote
            // client lookup.  In that case, the context class loader is the WAS
            // server class loader, which will not have application stubs.
            // Attempt the narrow again using the module ClassLoader; if that
            // still fails then return the servant stub directly and hope the ORB
            // does the right thing on the client.
            if (contextClassLoader != moduleContextClassLoader) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "ignoring loadClass/narrow exception, attempting again with bean ClassLoader", ex);

                Object origCL = svThreadContextAccessor.pushContextClassLoaderForUnprivileged(moduleContextClassLoader);
                try {
                    return PortableRemoteObject.narrow(stub, businessClass);
                } catch (Exception ex2) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "ignoring loadClass/narrow exception", ex2);
                } finally {
                    svThreadContextAccessor.popContextClassLoaderForUnprivileged(origCL);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "ignoring loadClass/narrow exception", ex);
            }
        }

        return stub;
    }

    public WrapperProxyState getLocalBusinessWrapperProxyState(Class<?> businessInterface) // F58064
    {
        if (ivBusinessLocalWrapperProxyStates == null)
        {
            return null;
        }

        Class<?>[] bInterfaces = ivBMD.ivBusinessLocalInterfaceClasses;
        if (bInterfaces != null) // d416391
        {
            int numInterfaces = bInterfaces.length;
            for (int i = 0; i < numInterfaces; i++)
            {
                if (bInterfaces[i] == businessInterface)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "getBusinessObjectWrapperProxyState : " +
                                     ivBusinessLocal[i].getClass().getName());
                    return ivBusinessLocalWrapperProxyStates[i];
                }
            }
        }

        throw new IllegalStateException("Requested local business interface not found : " +
                                        businessInterface.getName());
    }

    /**
     * Method to get the remote business wrapper.
     */
    public BusinessRemoteWrapper getRemoteBusinessWrapper(int interfaceIndex) {
        return ivBusinessRemote[interfaceIndex];
    }

    /**
     * Returns the specific Remote Business interface wrapper for the specified
     * WrapperId. <p>
     * 
     * The returned wrapper will be registered with the ORB/OA at this time, if
     * it has not already been registered. <p>
     * 
     * This method is intended for use during processing of a 'keyToObject'
     * request from the ORB. It knows how to extract the remote business
     * interface specific information from the WrapperId and use it to
     * determine the corresponding wrapper. <p>
     * 
     * This method will continue to work even when an application has been
     * changed by adding or removing remote business interfaces, as long
     * as the interface requested is not removed. <p>
     * 
     * @param wrapperId EJB Wrapper Identifier.
     * @throws IllegalStateException if the EJB does not have the interface
     **/
    // d419704 - rewrote for new WrapperId fields and remove the todo.
    public BusinessRemoteWrapper getRemoteBusinessWrapper(WrapperId wrapperId)
    {
        int remoteIndex = wrapperId.ivInterfaceIndex;
        BusinessRemoteWrapper wrapper = null;
        String wrapperInterfaceName = "";
        if (remoteIndex < ivBusinessRemote.length)
        {
            wrapper = ivBusinessRemote[remoteIndex];
            wrapperInterfaceName = ivBMD.ivBusinessRemoteInterfaceClasses[remoteIndex].getName();
        }

        // Is the BusinessRemoteWrapper for the correct interface name?
        String interfaceName = wrapperId.ivInterfaceClassName;
        if ((wrapper == null) || (!wrapperInterfaceName.equals(interfaceName)))
        {
            // Nope, index must be invalid, so we need to search the entire
            // array to find the one that matches the desired interface name.
            // Fix up the WrapperId with index that matches this server.
            wrapper = null;
            for (int i = 0; i < ivBusinessRemote.length; ++i)
            {
                wrapperInterfaceName = ivBMD.ivBusinessRemoteInterfaceClasses[i].getName();
                if (wrapperInterfaceName.equals(interfaceName))
                {
                    // This is the correct wrapper.
                    remoteIndex = i;
                    wrapper = ivBusinessRemote[remoteIndex];
                    wrapperId.ivInterfaceIndex = remoteIndex;
                    break;
                }
            }
            // d739542 Begin
            if (wrapper == null) {
                throw new IllegalStateException("Remote " + interfaceName + " interface not defined");
            }
            // d739542 End
        }

        registerServant(wrapper, remoteIndex);

        return wrapper;
    }

    /**
     * Returns an aggregate wrapper of all local business interfaces, including
     * the no-interface view. <p>
     * 
     * The aggregate local wrapper class will be generated with JITDeploy the
     * first time this method is called for a bean. <p>
     * 
     * @return the aggregate local wrapper instance.
     * @throws EJBException if a failure occurs attempting to generate the
     *             aggregate wrapper class or create an instance of it.
     */
    // F743-34304
    public Object getAggregateLocalWrapper()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (ivBusinessLocal == null) {
            J2EEName j2eeName = ivBeanId.getJ2EEName();
            throw new EJBException("The " + j2eeName.getComponent() + " bean in the " +
                                   j2eeName.getModule() + " module of the " + j2eeName.getApplication() +
                                   " application has no business local interfaces.");
        }

        if (ivBusinessLocal.length == 1)
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "getAggregateLocalWrapper: single interface: " +
                             Util.identity(ivBusinessLocal[0]));
            return ivBusinessLocal[0];
        }

        EJSHome home = ivBMD.getHome();
        String wrapperClassName = null;

        try
        {
            // Synchronize on the BMD (where the class to be generated is held) to
            // insure multiple threads do not attempt to generate it concurrently.
            synchronized (ivBMD)
            {
                if (ivBMD.ivAggregateLocalImplClass == null)
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "getAggregateLocalWrapper: generating wrapper class for " +
                                     ivBMD.j2eeName);

                    // Note, both component and business interfaces are required on
                    // the call to DeploymentUtil, as the returned list of methods
                    // is used to determine the method ids.                   d677413
                    Method[] localBeanMethods = DeploymentUtil.getMethods(ivBMD.localInterfaceClass,
                                                                          ivBMD.ivBusinessLocalInterfaceClasses);
                    wrapperClassName = NameUtil.getAggregateLocalImplClassName(ivBMD.ivBusinessLocalImplClasses[0].getName());

                    ivBMD.ivAggregateLocalImplClass = JITDeploy.generateEJBWrapper
                                    (ivBMD.classLoader,
                                     wrapperClassName,
                                     ivBMD.ivBusinessLocalInterfaceClasses,
                                     (ivBMD.ivLocalBean ? EJBWrapperType.LOCAL_BEAN : EJBWrapperType.BUSINESS_LOCAL),
                                     localBeanMethods,
                                     ivBMD.localMethodInfos,
                                     ivBMD.enterpriseBeanClassName,
                                     ivBMD.j2eeName.toString(),
                                     ivBMD.container.getEJBRuntime().getClassDefiner()); // F70650
                    wrapperClassName = null;
                }
            }

            BusinessLocalWrapper bLocal = null;
            Object wrapper = ivBMD.ivAggregateLocalImplClass.newInstance();

            // For No-Interface view (LocalBean), the 'wrapper' will not
            // subclass EJSWrapperBase, but the EJB itself, so a separate
            // EJSWrapperBase must be created to hold the metadata and
            // associated with the real wrapper.
            if (ivBMD.ivLocalBean)
            {
                try {
                    // Obtain the reflection Fields from the generated wrapper   d679175
                    Field wrapperField = AccessController.doPrivileged(new PrivilegedExceptionAction<Field>() {
                        @Override
                        public Field run() throws NoSuchFieldException {
                            return ivBMD.ivAggregateLocalImplClass.getDeclaredField(LOCAL_BEAN_WRAPPER_FIELD);
                        }
                    });

                    wrapperField.setAccessible(true);

                    bLocal = new BusinessLocalWrapper();
                    wrapperField.set(wrapper, bLocal);

                } catch (PrivilegedActionException paex) {
                    Throwable cause = paex.getCause();
                    if (cause instanceof NoSuchFieldException) {
                        throw (NoSuchFieldException) cause;
                    }
                    throw new Error(cause);
                }

            }
            // Otherwise, normal wrapper - subclasses BusinessLocalWrapper
            else
            {
                bLocal = (BusinessLocalWrapper) wrapper;
            }

            bLocal.beanId = ivBeanId;
            bLocal.bmd = ivBMD;
            bLocal.methodInfos = ivBMD.localMethodInfos;
            bLocal.methodNames = ivBMD.localMethodNames;
            bLocal.container = home.container;
            bLocal.wrapperManager = home.wrapperManager;
            bLocal.ivPmiBean = home.pmiBean;
            bLocal.ivCommon = this;
            bLocal.isManagedWrapper = false;
            bLocal.ivInterface = WrapperInterface.BUSINESS_LOCAL;
            bLocal.ivBusinessInterfaceIndex = EJSWrapperBase.AGGREGATE_LOCAL_INDEX;

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "getAggregateLocalWrapper: " + Util.identity(wrapper));

            return wrapper;
        } catch (Exception ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".getAggregateLocalWrapper",
                                        "970", this);
            if (wrapperClassName != null)
            {
                throw ExceptionUtil.EJBException
                                ("Failed to generate aggregate local reference class " +
                                 wrapperClassName + " for bean " + ivBMD.j2eeName + " : ", ex);
            }

            throw ExceptionUtil.EJBException
                            ("Failed to create aggregate local reference for bean " +
                             ivBMD.j2eeName + " : ", ex);
        }
    }

    /**
     * Returns the BeanId for this set of wrappers.
     */
    public BeanId getBeanId()
    {
        // Previously, the BeanId was just extracted from one of the wrappers,
        // but now there are so many wrapper types, which don't usually all
        // exist, that performance for finding the BeanId is much better if
        // it is stored locally.                                         d366807.1
        return ivBeanId;
    }

    /**
     * Pin this object in the EJS Wrapper Cache if it is not already
     * pinned; pinning an object in the cache ensures that the object will not
     * be cast-out during any cache trimming operations. Objects may be pinned
     * more than once, but pinOnce() will not obtain an additional pin if the
     * object is already pinned, so a call to <code>unpin()</code> is only
     * required if true is returned. <p>
     * 
     * Note that false is returned if the object does not exist in the cache. <p>
     * 
     * @return true if a pin is obtained, false if the element
     *         does not exist, or is already pinned.
     **/
    // d195605
    public boolean pinOnce()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "pinOnce: " + ivBeanId);

        boolean pinnedOnce = false;

        synchronized (ivBucket)
        {
            if (pinned == 0)
            {
                pinned++;
                pinnedOnce = true;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "pinOnce : " + pinned);

        return pinnedOnce;
    }

    /**
     * Unpin this object in the EJS Wrapper Cache. Objects which are
     * not pinned may be cast-out of the cache during trimming operations.
     * Calls to <code>unpin()</code> must be symmetric with calls to
     * <code>pinOnce()</code>. <p>
     * 
     * No action is taken if the object is either not pinned or not currently
     * in the EJS Wrapper Cache. <p>
     * 
     * @see Cache#pin
     **/
    // d195605
    public void unpin()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "unpin: " + ivBeanId);

        synchronized (ivBucket)
        {
            if (pinned > 0)
            {
                pinned--;

                // Touch the LRU flag; since an object cannot be evicted when
                // pinned, this is the only time when we bother to set the
                // flag
                ((WrapperBucket) ivBucket).ivWrapperCache.touch(this);
            }
            else
            {
                // If the application has been stopped/uninstalled, the wrapper
                // will have been forcibly evicted from the cache, so just
                // trace, but ignore this condition.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "unpin: Not pinned : " + pinned);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "unpin");
    }

    /**
     * Returns true if this object is in the EJS Wrapper Cache, and updates
     * the LRU data to defer eviction. <p>
     **/
    // d195605
    public boolean inCache()
    {
        boolean inCache = false;

        synchronized (ivBucket)
        {
            // Non-negative pin value indicates the object is in the cache.
            if (pinned >= 0)
            {
                ((WrapperBucket) ivBucket).ivWrapperCache.touch(this);
                inCache = true;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "touch: inCache = " + inCache + ", " + ivBeanId);

        return inCache;
    }

    /**
     * Overridden for trace.
     **/
    @Override
    public String toString()
    {
        // Note : cannot call toString on wrapper, as the customer may
        //        have overridden that method....

        // Added new Business Remote/Local interfaces to trace.          d366807.5
        String businessRemote = "NoBRemote, ";
        if (ivBusinessRemote != null)
        {
            businessRemote = "BR: ";
            for (BusinessRemoteWrapper wrapper : ivBusinessRemote)
            {
                businessRemote += wrapper.getClass().getName();
                businessRemote += ", ";
            }
        }

        String businessLocal = "NoBLocal, ";
        if (ivBusinessLocal != null)
        {
            businessLocal = ivBusinessLocalWrapperProxyStates == null ? "BL: " : "BLP: ";
            for (Object wrapper : ivBusinessLocal)
            {
                businessLocal += wrapper.getClass().getName();
                businessLocal += ", ";
            }
        }

        return ("EJSWrapperCommon(" + ivBeanId + ", " +
                ((ivRemoteObjectWrapper != null) ? ivRemoteObjectWrapper.getClass().getName()
                                : "NoRemote") + ", " +
                ((localWrapper != null) ? localWrapper.getClass().getName()
                                : "NoLocal") + ", " +
                businessRemote + businessLocal +
                "pinned = " + pinned + ", accessed = " + accessedSweep + ")");
    }
} // EJSWrapperCommon
