/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.impl.manager;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.impl.inject.ComponentMethods;
import org.apache.felix.scr.impl.inject.LifecycleMethod;
import org.apache.felix.scr.impl.inject.MethodResult;
import org.apache.felix.scr.impl.inject.OpenStatus;
import org.apache.felix.scr.impl.inject.RefPair;
import org.apache.felix.scr.impl.inject.ReferenceMethod;
import org.apache.felix.scr.impl.logger.InternalLogger.Level;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.TargetedPID;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentInstance;
import org.osgi.util.promise.Deferred;


/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 */
public class SingleComponentManager<S> extends AbstractComponentManager<S> implements ServiceFactory<S>
{

    // keep the using bundles as reference "counters" for instance deactivation
    private final AtomicInteger m_useCount = new AtomicInteger( );

    // The context that will be passed to the implementationObject
    private volatile ComponentContextImpl<S> m_componentContext;

    // Merged properties from xml descriptor and all configurations
    private Map<String, Object> m_configurationProperties;

    // optional properties provided in the ComponentFactory.newInstance method
    private Map<String, Object> m_factoryProperties;

    // the component properties, also used as service properties
    private Map<String, Object> m_properties;

    // properties supplied ot ExtComponentContext.updateProperties
    // null if properties are not to be overwritten
    private Dictionary<String, Object> m_serviceProperties;

    /**
     * The constructor receives both the activator and the metadata
     * @param componentMethods
     */
    public SingleComponentManager( final ComponentContainer<S> container, final ComponentMethods<S> componentMethods )
    {
        this(container, componentMethods, false);
    }

    public SingleComponentManager( final ComponentContainer<S> container, final ComponentMethods<S> componentMethods,
            final boolean factoryInstance )
    {
        super( container, componentMethods, factoryInstance );
    }

    @Override
    void clear()
    {
        m_container.disposed( this );

        super.clear();
    }


    // 1. Load the component implementation class
    // 2. Create the component instance and component context
    // 3. Bind the target services
    // 4. Call the activate method, if present
    // if this method is overwritten, the deleteComponent method should
    // also be overwritten
    private boolean createComponent(ComponentContextImpl<S> componentContext)
    {
        if ( !isStateLocked() )
        {
            throw new IllegalStateException( "need write lock (createComponent)" );
        }
        if ( m_componentContext == null )
        {
            S tmpComponent = createImplementationObject( null, new SetImplementationObject<S>()
            {
                @Override
                public void presetComponentContext( ComponentContextImpl<S> componentContext )
                {
                    m_componentContext = componentContext;
                }


                @Override
                public void resetImplementationObject( S implementationObject )
                {
                    m_componentContext = null;
                }
            }, componentContext );

            // if something failed creating the component instance, return false
            if ( tmpComponent == null )
            {
                return false;
            }

            // otherwise set the context and component instance and return true
            getLogger().log(Level.DEBUG, "Set implementation object for component",
                null);

            //notify that component was successfully created so any optional circular dependencies can be retried
           try
           {
                m_container.getActivator().missingServicePresent( getServiceReference() );
           }
           catch ( final IllegalStateException ise )
           {
               return false;
           }
        }
        return true;
    }


    @Override
    protected void deleteComponent( final int reason )
    {
        if ( !isStateLocked() )
        {
            throw new IllegalStateException( "need write lock (deleteComponent)" );
        }
        if ( m_componentContext != null )
        {
            m_useCount.set( 0 );
            disposeImplementationObject( m_componentContext, reason );
            m_componentContext = null;
            getLogger().log(Level.DEBUG,
                "Unset and deconfigured implementation object for component in deleteComponent for reason {0}",
                null, REASONS[reason]);
            clearServiceProperties();
        }
    }

    void clearServiceProperties()
    {
        m_properties = null;
        m_serviceProperties = null;
    }


    public ComponentInstance<S> getComponentInstance()
    {
        return m_componentContext == null? null: m_componentContext.getComponentInstance();
    }


    //**********************************************************************************************************

    /**
     * Get the object that is implementing this descriptor
     *
     * @return the object that implements the services
     */
    private S getInstance()
    {
        return m_componentContext == null? null: m_componentContext.getImplementationObject( true );
    }

    /**
     * The <code>SetImplementationObject</code> interface provides an
     * API for component managers to setup the implementation object and
     * potentially other parts as part of the {@link #createImplementationObject} method
     * processing.
     */
    protected interface SetImplementationObject<S>
    {

        /**
         * Presets the implementation object. This method is called before
         * the component's activator method is called and is intended to
         * temporarily set the implementation object during the activator
         * call.
         */
        void presetComponentContext( ComponentContextImpl<S> componentContext );


        /**
         * Resets the implementation object. This method is called after
         * the activator method terminates with an error and is intended to
         * revert any temporary settings done in the {@link #presetComponentContext(ComponentContextImpl)}
         * method.
         */
        void resetImplementationObject( S implementationObject );

    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected S createImplementationObject( Bundle usingBundle, SetImplementationObject<S> setter, ComponentContextImpl<S> componentContext )
    {
        S implementationObject = null;

        // 1. Load the component implementation class
        // 2. Create the component instance and component context
        // If the component is not immediate, this is not done at this moment
        Bundle bundle = getBundle();
        if (bundle == null)
        {
            getLogger().log(Level.WARN,
                "Bundle shut down during instantiation of the implementation object",
                null);
            return null;
        }

        // bind target services
        final List<OpenStatus<S, ?>> openStatusList = new ArrayList<>();

        final Map<ReferenceMetadata, OpenStatus<S, ?>> paramMap = (getComponentMetadata()
                .getNumberOfConstructorParameters() > 0 ? new HashMap<ReferenceMetadata, OpenStatus<S, ?>>() : null);
        boolean failed = false;
        for ( DependencyManager<S, ?> dm : getDependencyManagers())
        {
            // if a dependency turned unresolved since the validation check,
            // creating the instance fails here, so we deactivate and return
            // null.
            OpenStatus<S, ?> open = dm.open(componentContext, componentContext.getEdgeInfo(dm));
            if ( open == null )
            {
                getLogger().log(Level.DEBUG,
                    "Cannot create component instance due to failure to bind reference {0}",
                        null, dm.getName()  );

                failed = true;
                break;
            }
            openStatusList.add(open);
            if ( dm.getReferenceMetadata().getParameterIndex() != null)
            {
                if ( !dm.bindDependency(componentContext, ReferenceMethod.NOPReferenceMethod, (OpenStatus) open) )
                {
                    getLogger().log(Level.DEBUG,
                        "Cannot create component instance due to failure to bind reference {0}",
                            null, dm.getName()  );
                    failed = true;
                    break;
                }

                paramMap.put(dm.getReferenceMetadata(), open);
            }
        }

        if ( !failed )
        {
            try
            {
                implementationObject = getComponentMethods().getConstructor().newInstance(
                        componentContext,
                        paramMap);

            }
            catch ( final InstantiationException ie)
            {
                // we don't need to log the stack trace
                getLogger().log(Level.ERROR,
                    "Error during instantiation of the implementation object: {0}",
                    null, ie.getMessage());
                this.setFailureReason(ie);
                return null;
            }
            catch ( final Throwable t )
            {
                // failed to instantiate, return null
                getLogger().log(Level.ERROR,
                    "Error during instantiation of the implementation object", t);
                this.setFailureReason(t);
                return null;
            }

            componentContext.setImplementationObject(implementationObject);

            // 3. set the implementation object prematurely
            setter.presetComponentContext( componentContext );

            // 4. Bind the target services
            final Iterator<OpenStatus<S, ?>> iter = openStatusList.iterator();
            for ( DependencyManager<S, ?> dm: getDependencyManagers())
            {
                final OpenStatus<S, ?> open = iter.next();
                if ( !dm.bind(componentContext, (OpenStatus) open) )
                {
                    getLogger().log(Level.DEBUG,
                        "Cannot create component instance due to failure to bind reference {0}",
                            null, dm.getName()  );
                    failed = true;
                    break;
                }
            }
        }
        if (failed)
        {
            // make sure, we keep no bindings. Only close the dm's we opened.
            int skipCount = getReversedDependencyManagers().size() - openStatusList.size();
            for ( DependencyManager<S, ?> md: getReversedDependencyManagers() )
            {
                if ( skipCount > 0 )
                {
                    skipCount--;
                }
                else
                {
                    md.close( componentContext, componentContext.getEdgeInfo( md ) );
                }
                md.deactivate();
            }

            setter.resetImplementationObject( implementationObject );
            return null;

        }

        // 5. Call the activate method, if present
        final MethodResult failedResult = new MethodResult(true, new HashMap<String, Object>());
        final MethodResult result = getComponentMethods().getActivateMethod().invoke( implementationObject,
                componentContext, 1, failedResult );
        if ( result == failedResult )
        {
            this.setFailureReason((Throwable)failedResult.getResult().get("exception"));
            // 112.5.8 If the activate method throws an exception, SCR must log an error message
            // containing the exception with the Log Service and activation fails
            for ( DependencyManager<S, ?> md: getReversedDependencyManagers() )
            {
                md.close( componentContext, componentContext.getEdgeInfo( md ) );
            }

            if ( implementationObject != null )
            {
                // make sure the implementation object is not available
                setter.resetImplementationObject( implementationObject );
            }

            return null;
        }
        else
        {
            componentContext.setImplementationAccessible( true );
            //call to leaveCreate must be done here since the change in service properties may cause a getService,
            //so the threadLocal must be cleared first.
            try
            {
                m_container.getActivator().leaveCreate(getServiceReference());
            }
            catch ( final IllegalStateException ise)
            {
                // already unregistered again
                this.setFailureReason(ise);

                for ( DependencyManager<S, ?> md: getReversedDependencyManagers() )
                {
                    md.close( componentContext, componentContext.getEdgeInfo( md ) );
                }

                if ( implementationObject != null )
                {
                    // make sure the implementation object is not available
                    setter.resetImplementationObject( implementationObject );
                }
                return null;
            }

            //this may cause a getService as properties now match a filter.
            setServiceProperties( result, null );
        }

        return implementationObject;
    }


    protected void disposeImplementationObject( ComponentContextImpl<S> componentContext,
            int reason )
    {
        componentContext.setImplementationAccessible( false );
        S implementationObject = componentContext.getImplementationObject( false );

        if ( implementationObject != null )
        {
            // 1. Call the deactivate method, if present
            // don't care for the result, the error (acccording to 112.5.12 If the deactivate
            // method throws an exception, SCR must log an error message containing the
            // exception with the Log Service and continue) has already been logged
            final MethodResult result = getComponentMethods().getDeactivateMethod().invoke( implementationObject,
                    componentContext, reason, null );
            if ( result != null )
            {
                setServiceProperties( result, null );
            }
            // 2. Unbind any bound services
            for ( DependencyManager<S, ?> md: getReversedDependencyManagers() )
            {
                md.close( componentContext, componentContext.getEdgeInfo( md ) );
            }
        }
        componentContext.cleanup();
    }

    @Override
    <T> void invokeBindMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> refPair, int trackingCount )
    {
        ComponentContextImpl<S> componentContext = m_componentContext;
        if ( componentContext != null )
        {
            EdgeInfo info = componentContext.getEdgeInfo( dependencyManager );
            dependencyManager.invokeBindMethod( componentContext, refPair, trackingCount, info );
        }
    }

    @Override
    <T> boolean invokeUpdatedMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> refPair, int trackingCount )
    {
        final ComponentContextImpl<S> componentContext = m_componentContext;
        if ( componentContext != null )
        {
            final EdgeInfo info = componentContext.getEdgeInfo( dependencyManager );
            return dependencyManager.invokeUpdatedMethod( componentContext, refPair, trackingCount, info );
        }
        return false;
    }

    @Override
    <T> void invokeUnbindMethod( DependencyManager<S, T> dependencyManager, RefPair<S, T> oldRefPair, int trackingCount )
    {
        ComponentContextImpl<S> componentContext = m_componentContext;
        if ( componentContext != null )
        {
            EdgeInfo info = componentContext.getEdgeInfo( dependencyManager );
            dependencyManager.invokeUnbindMethod( componentContext, oldRefPair, trackingCount, info );
        }
    }

    protected void setFactoryProperties( Dictionary<String, ?> dictionary )
    {
        m_factoryProperties = copyToMap( dictionary, true );
    }


    @Override
    void registerComponentId()
    {
        super.registerComponentId();
        this.m_properties = null;
    }


    @Override
    void unregisterComponentId()
    {
        super.unregisterComponentId();
        this.m_properties = null;
    }


    /**
     * Returns the (private copy) of the Component properties to be used
     * for the ComponentContext as well as eventual service registration.
     * <p/>
     * Method implements the Component Properties provisioning as described
     * in 112.6, Component Properties.
     *
     * @return a private map of component properties
     */
    @Override
    public Map<String, Object> getProperties()
    {
        if ( m_properties == null )
        {
            // 1. Merge all the config properties
            Map<String, Object> props = new HashMap<>();
            if ( m_configurationProperties != null )
            {
                props.putAll(m_configurationProperties);
            }
            if ( m_factoryProperties != null)
            {
                props.putAll(m_factoryProperties);
                if (getComponentMetadata().getDSVersion().isDS13() && m_factoryProperties.containsKey(Constants.SERVICE_PID))
                {
                    final List<String> servicePids = new ArrayList<>();
                    final Object configPropServicePids = m_configurationProperties.get(Constants.SERVICE_PID);
                    if ( configPropServicePids instanceof List )
                    {
                        @SuppressWarnings("unchecked")
                        List<String> l = (List<String>) configPropServicePids;
                        servicePids.addAll(l);
                    }
                    else
                    {
                        servicePids.add(configPropServicePids.toString());
                    }
                    if (m_factoryProperties.get(Constants.SERVICE_PID) instanceof String)
                    {
                        servicePids.add((String)m_factoryProperties.get(Constants.SERVICE_PID));
                    }

                    if ( servicePids.size() == 1 )
                    {
                        props.put(Constants.SERVICE_PID, servicePids.get(0));
                    }
                    else
                    {
                        props.put(Constants.SERVICE_PID, servicePids);
                    }
                }
            }

            // 2. set component.name and component.id
            props.put( ComponentConstants.COMPONENT_NAME, getComponentMetadata().getName() );
            props.put( ComponentConstants.COMPONENT_ID, getId() );

            m_properties = props;
        }

        return m_properties;
    }

    @Override
    public void setServiceProperties( Dictionary<String, ?> serviceProperties )
    {
        if ( serviceProperties == null || serviceProperties.isEmpty() )
        {
            m_serviceProperties = null;
        }
        else
        {
            m_serviceProperties = copyToDictionary( serviceProperties, false );
            // set component.name and component.id
            m_serviceProperties.put( ComponentConstants.COMPONENT_NAME, getComponentMetadata().getName() );
            m_serviceProperties.put( ComponentConstants.COMPONENT_ID, getId() );
        }

        updateServiceRegistration();
    }

    @Override
    void postRegister()
    {
        if (m_serviceProperties != null)
        {
            updateServiceRegistration();
        }
    }

    @Override
    void preDeregister()
    {
        if (m_componentContext != null)
        {
            m_componentContext.unsetServiceRegistration();
        }
    }

    @Override
    public Dictionary<String, Object> getServiceProperties()
    {
        if ( m_serviceProperties != null )
        {
            return m_serviceProperties;
        }
        return super.getServiceProperties();
    }

    final ServiceReference<S> getServiceReference()
    {
        ServiceRegistration<S> reg = getServiceRegistration();
        if (reg != null)
        {
            return reg.getReference();
        }
        return null;
    }

    @Override
    protected ServiceRegistration<S> getServiceRegistration()
    {
        if ( getComponentMetadata().getDSVersion() == DSVersion.DS12Felix )
        {
            return m_componentContext != null ? m_componentContext.getServiceRegistration() : null;
        }
        return super.getServiceRegistration();
    }

    private void updateServiceRegistration()
    {
        ServiceRegistration<S> sr = getServiceRegistration();
        if ( sr != null )
        {
            try
            {
                // Don't propagate if service properties did not change.
                final Dictionary<String, Object> regProps = getServiceProperties();
                if ( !servicePropertiesMatches( sr, regProps ) )
                {
                    sr.setProperties( regProps );
                }
                else
                {
                    getLogger().log(Level.DEBUG,
                        "Not updating service registration, no change in properties",
                        null);
                }
            }
            catch ( final IllegalStateException ise )
            {
                // service has been unregistered asynchronously, ignore
            }
            catch ( final IllegalArgumentException iae )
            {
                getLogger().log(Level.ERROR,
                        "Unexpected configuration property problem when updating service registration", iae );
            }
            catch ( final Throwable t )
            {
                getLogger().log(Level.ERROR,
                    "Unexpected problem when updating service registration", t);
            }
        }
        else
        {
            getLogger().log(Level.DEBUG, "No service registration to update", null);
        }
    }

    /**
     * Called by the Configuration Admin Service to update the component with
     * Configuration properties.
     * <p/>
     * This causes the component to be reactivated with the new configuration
     * unless no configuration has ever been set on this component and the
     * <code>configuration</code> parameter is <code>null</code>. In this case
     * nothing is to be done. If a configuration has previously been set and
     * now the configuration is deleted, the <code>configuration</code>
     * parameter is <code>null</code> and the component has to be reactivated
     * with the default configuration.
     *
     * @param configuration The configuration properties for the component from
     *                      the Configuration Admin Service or <code>null</code> if there is
     *                      no configuration or if the configuration has just been deleted.
     * @param configurationDeleted TODO
     * @param factoryPid TODO
     */
    @Override
    public void reconfigure( Map<String, Object> configuration, boolean configurationDeleted, TargetedPID factoryPid)
    {
        // store the properties
        m_configurationProperties = configuration;

        reconfigure(configurationDeleted);
    }

    void reconfigure(boolean configurationDeleted)
    {
        Deferred<Void> enableLatch = enableLatchWait();
        try
        {
            // clear the current properties to force using the configuration data
            m_properties = null;


            // reactivate the component to ensure it is provided with the
            // configuration data
            if ( !getState().isEnabled() )
            {
                // nothing to do for inactive components, leave this method
                getLogger().log(Level.DEBUG,
                    "Component can not be activated since it is in state {0}", null,
                    getState());
                //enabling the component will set the target properties, do nothing now.
                return;
            }

            // unsatisfied component and non-ignored configuration may change targets
            // to satisfy references
            obtainActivationWriteLock( );
            try
            {
                if ( !getState().isSatisfied() && !getComponentMetadata().isConfigurationIgnored() )
                {
                    getLogger().log(Level.DEBUG,
                        "Attempting to activate unsatisfied component", null);
                    updateTargets( getProperties() );
                    releaseActivationWriteeLock(  );
                    activateInternal( );
                    return;
                }

                if ( !modify(configurationDeleted) )
                {
                    // SCR 112.7.1 - deactivate if configuration is deleted or no modified method declared
                    getLogger().log(Level.DEBUG,
                        "Deactivating and Activating to reconfigure from configuration",
                        null);
                    int reason = configurationDeleted ? ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED
                            : ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_MODIFIED;

                    // FELIX-2368: cycle component immediately, reconfigure() is
                    //     called through ConfigurationListener API which itself is
                    //     called asynchronously by the Configuration Admin Service
                    releaseActivationWriteeLock(  );
                    //we have already determined that modify cannot be called. Therefore factory instances must be disposed.
                    boolean dispose = m_factoryInstance;
                    deactivateInternal( reason, dispose, dispose );
                    if ( !dispose )
                    {
                        obtainActivationWriteLock();
                        try
                        {
                            updateTargets(getProperties());
                        }
                        finally
                        {
                            releaseActivationWriteeLock();
                        }
                        activateInternal();
                    }
                }
            }
            finally
            {
                //used if modify succeeds or if there's an exception.
                releaseActivationWriteeLock(  );
            }
        }
        finally
        {
            enableLatch.resolve(null);
        }
    }

    private boolean modify(boolean configurationDeleted)
    {
        //0 SCR 112.7.1 If configuration is deleted, and version is < 1.3 and no flag set, then deactivate unconditionally.
        // For version 1.3 and later, or with a flag, more sensible behavior is allowed.
        if ( configurationDeleted && !getComponentMetadata().isDeleteCallsModify()){
            return false;
        }

        // 1. no live update if there is no declared method
        if ( getComponentMetadata().getModified() == null )
        {
            getLogger().log(Level.DEBUG,
                "No modified method, cannot update dynamically", null);
            return false;
        }
        // invariant: we have a modified method name

        // 2. get and check configured method
        // invariant: modify method is configured and found

        // 3. check whether we can dynamically apply the configuration if
        // any target filters influence the bound services
        final Map<String, Object> props = getProperties();
        for ( DependencyManager<S, ?> dm: getDependencyManagers() )
        {
            if ( !dm.canUpdateDynamically( props ) )
            {
                getLogger().log(Level.DEBUG,
                        "Cannot dynamically update the configuration due to dependency changes induced on dependency {0}",
                        null, dm.getName() );
                return false;
            }
        }
        // invariant: modify method existing and no static bound service changes

        // 4. call method (nothing to do when failed, since it has already been logged)
        //   (call with non-null default result to continue even if the
        //    modify method call failed)
        obtainStateLock(  );
        try
        {
            //cf 112.5.12 where invoking modified method before updating target services is specified.
            final MethodResult result = invokeModifiedMethod();
            updateTargets( props );
            if ( result == null )
            {
                // log an error if the declared method cannot be found
                getLogger().log(Level.ERROR,
                    "Declared modify method ''{0}'' cannot be found, configuring by reactivation",
                        null, getComponentMetadata().getModified() );
                return false;
            }

            // 5. update the target filter on the services now, this may still
            // result in unsatisfied dependencies, in which case we abort
            // this dynamic update and have the component be deactivated
            if ( !verifyDependencyManagers() )
            {
                getLogger().log(Level.DEBUG,
                        "Updating the service references caused at least one reference to become unsatisfied, deactivating component",
                        null );
                return false;
            }

            // 6. update service registration properties if we didn't just do it
            if ( result.hasResult() )
            {
                setServiceProperties( result, null );
            }
            else
            {
                updateServiceRegistration();
            }

            // 7. everything set and done, the component has been updated
            return true;
        }
        finally
        {
            releaseStateLock(  );
        }
    }

    protected MethodResult invokeModifiedMethod()
    {
        LifecycleMethod modifiedMethod = getComponentMethods().getModifiedMethod();
        if ( getInstance() != null )
        {
            return modifiedMethod.invoke( getInstance(), m_componentContext, -1,
                    MethodResult.VOID );
        }
        return MethodResult.VOID;
    }

    /**
     * Checks if the given service registration properties matches another set
     * of properties.
     *
     * @param reg   the service registration whose service properties will be
     *              compared to the props parameter
     * @param props the properties to be compared with the registration
     *              service properties.
     * @return <code>true</code> if the registration service properties equals
     *         the prop properties, false if not.
     */
    private boolean servicePropertiesMatches( ServiceRegistration<S> reg, Dictionary<String, Object> props )
    {
        Dictionary<String, Object> regProps = new Hashtable<>();
        String[] keys = reg.getReference().getPropertyKeys();
        for ( int i = 0; keys != null && i < keys.length; i++ )
        {
            if ( !keys[i].equals( org.osgi.framework.Constants.OBJECTCLASS )
                    && !keys[i].equals( org.osgi.framework.Constants.SERVICE_ID ) )
            {
                regProps.put( keys[i], reg.getReference().getProperty( keys[i] ) );
            }
        }
        return regProps.equals( props );
    }

    @Override
    public S getService( final Bundle bundle, final ServiceRegistration<S> serviceRegistration )
    {
        ServiceReference<S> ref = null;
        try
        {
            ref = serviceRegistration.getReference();
        }
        catch ( final IllegalStateException ise)
        {
            // the service already unregistered
            return null;
        }
        if ( m_container.getActivator().enterCreate( ref ) )
        {
            // circular dependency
            return null;
        }
        try
        {
            obtainStateLock();
            try
            {
                m_useCount.incrementAndGet();
            }
            finally
            {
                releaseStateLock( );
            }
            boolean decrement = true;
            try
            {
                boolean success = getServiceInternal(serviceRegistration);
                ComponentContextImpl<S> componentContext = m_componentContext;
                if ( success && componentContext != null)
                {
                    decrement = false;
                    return componentContext.getImplementationObject( true );
                }
                else
                {
                    return null;
                }

            }
            finally
            {
                if ( decrement )
                {
                    ungetService( bundle, serviceRegistration, null );
                }
            }
        }
        finally
        {
            m_container.getActivator().leaveCreate( ref );
        }
    }


    @Override
    boolean getServiceInternal(ServiceRegistration<S> serviceRegistration)
    {
        boolean success = true;
        if ( m_componentContext == null )
        {
            ComponentContextImpl<S> componentContext = new ComponentContextImpl<>(this, this.getBundle(), serviceRegistration);
            if ( collectDependencies(componentContext))
            {
                getLogger().log(Level.DEBUG,
                        "getService (single component manager) dependencies collected.",
                        null );
            }
            else
            {
                getLogger().log(Level.INFO,
                        "Could not obtain all required dependencies, getService returning null",
                        null );
                return false;
            }
            obtainStateLock(  );
            try
            {
                if ( m_componentContext == null )
                {
                    State previousState = getState();
                    //state should be "Registered"
                    S result = getService(componentContext );
                    if ( result == null )
                    {
                        success = false;
                    }
                    else
                    {
                        setState(previousState, State.active);
                    }
                }
            }
            finally
            {
                releaseStateLock(  );
            }
        }
        return success;
    }

    private S getService(ComponentContextImpl<S> componentContext)
    {
        //should be write locked
        if (!getState().isEnabled())
        {
            return null;
        }

        if ( createComponent(componentContext) )
        {
            return getInstance();
        }

        // log that the delayed component cannot be created (we don't
        // know why at this moment; this should already have been logged)
        getLogger().log(Level.DEBUG,
            "Failed creating the component instance; see log for reason", null);

        // component could not really be created. This may be temporary
        // so we stay in the registered state but ensure the component
        // instance is deleted
        try
        {
            deleteComponent( ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
        }
        catch ( final Throwable t )
        {
            getLogger().log(Level.DEBUG,
                "Cannot delete incomplete component instance. Ignoring.", t);
        }

        // no service can be returned (be prepared for more logging !!)
        return null;

    }

    @Override
    public void ungetService( final Bundle bundle, final ServiceRegistration<S> serviceRegistration, final S o )
    {
        obtainStateLock( );
        try
        {
            // unget the service instance if no bundle is using it
            // any longer unless delayed component instances have to
            // be kept (FELIX-3039)
            if ( m_useCount.decrementAndGet() == 0 && !isImmediate()
                    && !getComponentMetadata().isFactory() && !keepInstances() )
            {
                final State previousState = getState();
                deleteComponent( ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED );
                setState(previousState, State.satisfied);
            }
        }
        finally
        {
            releaseStateLock(  );
        }
    }

    private boolean keepInstances()
    {
        return getComponentMetadata().isDelayedKeepInstances();
    }

    @Override
    public void getComponentManagers(List<AbstractComponentManager<S>> cms)
    {
        cms.add(this);
    }
}
