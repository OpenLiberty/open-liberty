/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.UnavailableException;
import javax.transaction.xa.XAResource;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;

import com.ibm.tx.jta.DestroyXAResourceException;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jca.cm.JcaServiceUtilities;
import com.ibm.ws.jca.internal.ActivationConfig;
import com.ibm.ws.jca.internal.BootstrapContextImpl;
import com.ibm.ws.jca.internal.ResourceAdapterMetaData;
import com.ibm.ws.jca.internal.Utils;
import com.ibm.ws.jca.metadata.ConnectorModuleMetaData;
import com.ibm.ws.jca.osgi.JCARuntimeVersion;
import com.ibm.ws.jca.osgi.JCARuntimeVersion16;
import com.ibm.ws.kernel.service.util.PrivHelper;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.tx.rrs.RRSXAResourceFactory;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 * This factory creates activationSpec instances.
 */
//as documentation only at this point:
//@Component(pid="com.ibm.ws.jca.activationSpec.supertype")
public class EndpointActivationService implements XAResourceFactory, ApplicationRecycleComponent {

    private static final TraceComponent tc = Tr.register(EndpointActivationService.class);

    /**
     * Prefix for flattened config properties.
     */
    private static final String CONFIG_PROPS_PREFIX = "properties.0.";

    /**
     * Length of prefix for flattened config properties.
     */
    private static final int CONFIG_PROPS_PREFIX_LENGTH = CONFIG_PROPS_PREFIX.length();

    /**
     * Name of the destination property for activation spec.
     */
    public static final String DESTINATION = "destination";

    /**
     * Name of the destination lookup property for activation spec.
     */
    public static final String DESTINATION_LOOKUP = "destinationLookup";

    /**
     * Name of the unique identifier property.
     */
    private static final String ID = "id";

    /**
     * Name of the password property for activation spec.
     */
    public static final String PASSWORD = "password";

    /**
     * Name of the userName property for activation spec.
     */
    private static final String USER_NAME = "userName";

    /**
     * Implementation class name for the activation spec.
     */
    private String activationSpecClassName;

    /**
     * Reference to the default authentication data.
     */
    private ServiceReference<?> authDataRef;

    /**
     * Component context.
     */
    private ComponentContext componentContext;

    /**
     * Config properties for the activation spec.
     */
    private final Map<String, Object> configProps = new HashMap<String, Object>();

    /**
     * Reference to the destination.
     */
    private final AtomicServiceReference<AdminObjectService> destinationRef = new AtomicServiceReference<AdminObjectService>(DESTINATION);

    /**
     * List of parameters used for each endpoint activation.
     * Parameters are removed upon endpoint deactivation.
     */
    final ConcurrentLinkedQueue<ActivationParams> endpointActivationParams = new ConcurrentLinkedQueue<ActivationParams>();

    /**
     * Unique identifier for this activation spec configuration.
     */
    private String id;

    /**
     * Set of names of applications that have activated this endpoint.
     * The set supports concurrent modifications.
     */
    protected final Set<String> appsToRecycle = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Liberty transaction manager
     */
    private EmbeddableWebSphereTransactionManager transactionManager;

    /**
     * Reference to the resource adapter bootstrap context.
     */
    private final AtomicServiceReference<BootstrapContextImpl> bootstrapContextRef = new AtomicServiceReference<BootstrapContextImpl>("bootstrapContext");

    /**
     * Names of configuration properties marked as required-config-property.
     */
    private String[] requiredConfigProps;

    private final AtomicServiceReference<RRSXAResourceFactory> rrsXAResFactorySvcRef = new AtomicServiceReference<RRSXAResourceFactory>("rRSXAResourceFactory");

    /**
     * JCA service utilities.
     */
    private JcaServiceUtilities jcasu;

    /**
     * Thread context classloader to apply when starting/stopping the resource adapter.
     */
    private ClassLoader raClassLoader;

    /**
     * This class contains parameters used for endpoint activation.
     */
    static class ActivationParams {
        final Object activationSpec;
        final WSMessageEndpointFactory messageEndpointFactory;

        private ActivationParams(Object activationSpec, WSMessageEndpointFactory messageEndpointFactory) {
            this.activationSpec = activationSpec;
            this.messageEndpointFactory = messageEndpointFactory;
        }

        /**
         * Compare fields based on reference equality so that even if a resource adapter implements
         * .equals in such a way that two instances match, we still consider them separate endpoint activations.
         */
        @Override
        public boolean equals(Object o) {
            ActivationParams a;
            return o instanceof ActivationParams
                   && (a = ((ActivationParams) o)).activationSpec == activationSpec
                   && a.messageEndpointFactory == messageEndpointFactory;
        }
    }

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context DeclarativeService defined/populated component context
     * @throws Exception
     */
    @Trivial
    protected void activate(ComponentContext context) throws Exception {
        Dictionary<String, ?> props = context.getProperties();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activate", props);

        destinationRef.activate(context);
        bootstrapContextRef.activate(context);
        rrsXAResFactorySvcRef.activate(context);

        componentContext = context;
        activationSpecClassName = (String) props.get(CONFIG_PROPS_PREFIX + "activationspec-class");
        id = (String) context.getProperties().get(ID);

        // filter out actual config properties for the activation specification
        for (Enumeration<String> keys = props.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            if (key.length() > CONFIG_PROPS_PREFIX_LENGTH && key.charAt(CONFIG_PROPS_PREFIX_LENGTH - 1) == '.' && key.startsWith(CONFIG_PROPS_PREFIX)) {
                String propName = key.substring(CONFIG_PROPS_PREFIX_LENGTH);
                if (propName.indexOf('.') < 0 && propName.indexOf('-') < 0 && !"destinationRef".equals(propName))
                    configProps.put(propName, props.get(key));
            }
        }
        requiredConfigProps = (String[]) props.get(CONFIG_PROPS_PREFIX + "required-config-property");
        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /**
     * This method is called to create an ActivationSpecInstance and populate it with the final list of properties
     * prior to passing it to the resource adapter.
     *
     * @param activationProperties The properties passed in from the container
     * @param authAlias The authentication alias passed in from the container.
     * @param adminObjSvc AdminObjectService determined from settings on the MDB, or null. If set, destinationID will be ignored.
     * @param destinationID The id of a destination. If determined by MDB will be null, otherwise must be set.
     * @param destinationIDOrJNDIName The id of the destination or the JNDI name. EJB container might supply either. This is error prone, but we have already shipped this behavior.
     * @param appName The name of the application associated with the messages endpoint.
     *
     * @return the fully configured activationSpec instance
     * @throws ResourceException
     */
    private Object createActivationSpec(@Sensitive Properties activationProperties,
                                        String authAlias,
                                        AdminObjectService adminObjSvc,
                                        String destinationID, // this is for the XAResource scenario and when MDB can not find the admin object
                                        String destinationIDOrJNDIName,
                                        String appName) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "createActivationSpec",
                     activationProperties, authAlias, adminObjSvc, destinationID, appName);

        BootstrapContextImpl bootstrapContext = bootstrapContextRef.getServiceWithException();
        Class<?> activationSpecClass = bootstrapContext.loadClass(activationSpecClassName);
        String adapterName = bootstrapContext.getResourceAdapterName();
        String embeddedApp = null;
        ResourceAdapterMetaData metadata = bootstrapContext.getResourceAdapterMetaData();
        if (metadata != null && metadata.isEmbedded()) { // metadata is null for SIB/MQ
            embeddedApp = metadata.getJ2EEName().getApplication();
            Utils.checkAccessibility(id, adapterName, embeddedApp, appName, true);
        }
        Object activationSpec = activationSpecClass.getConstructor().newInstance();

        // Activation config properties might be specified in the form "propName" or "PropName"
        Map<String, Object> activationProps = new HashMap<String, Object>();
        for (Map.Entry<?, ?> prop : activationProperties.entrySet()) {
            String key = (String) prop.getKey();
            char firstChar = key.charAt(0);
            if (Character.isUpperCase(firstChar))
                key = new StringBuilder(key.length()).append(Character.toLowerCase(firstChar)).append(key.substring(1)).toString();
            activationProps.put(key, prop.getValue());
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "activation config: " + key + '=' + (key.toLowerCase().contains(PASSWORD) ? "***" : prop.getValue()));
        }

        boolean atLeastJCA17 = atLeastJCAVersion(JCARuntimeVersion.VERSION_1_7);
        if (!(activationProps.containsKey(DESTINATION) || atLeastJCA17 && activationProps.containsKey(DESTINATION_LOOKUP))) {
            boolean added = false;
            if (atLeastJCA17 && hasDestinationLookupProp(activationSpec)) {
                String jndiName = destinationIDOrJNDIName == null ? destinationRef.getServiceWithException().getJndiName() : destinationIDOrJNDIName;
                if (jndiName != null) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "add destinationLookup " + jndiName + " to activation props");
                    activationProps.put(DESTINATION_LOOKUP, jndiName);
                    added = true;
                }
            }

            if (!added && hasDestinationProp(activationSpec)) {
                if (destinationID == null)
                    destinationID = destinationRef.getServiceWithException().getId();
                if (destinationID == null)
                    destinationID = destinationRef.getServiceWithException().getJndiName();
                if (destinationID != null) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "add destination " + destinationID + " to activation props");
                    activationProps.put(DESTINATION, destinationID);
                }
            }
        }

        if (authAlias != null)
            processAuthData(authAlias, activationProps);
        else if (authDataRef != null) {
            activationProps.put(USER_NAME, authDataRef.getProperty("user"));
            activationProps.put(PASSWORD, authDataRef.getProperty("password"));
        }

        if (requiredConfigProps != null)
            for (String name : requiredConfigProps)
                if (!configProps.containsKey(name)
                    && !activationProps.containsKey(name)
                    && bootstrapContextRef.getReference().getProperty(name) == null
                    && (!DESTINATION.equals(name) || destinationRef.getService() == null))
                    throw new ResourceException(Tr.formatMessage(tc, "J2CA8813.required.activation.prop.not.set", name, id, bootstrapContext.getResourceAdapterName()));

        ComponentMetaDataAccessorImpl cmda = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        try {
            if (metadata != null) // push ra metadata
                cmda.beginContext(metadata);
            bootstrapContext.configure(activationSpec, id, configProps, activationProps, adminObjSvc, destinationRef);
        } finally {
            if (metadata != null) // pop ra metadata
                cmda.endContext();
        }
        if (activationSpec instanceof ActivationSpec) {
            try {
                ((ActivationSpec) activationSpec).validate();
            } catch (java.lang.UnsupportedOperationException uoe) {
                if (trace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The ActivationSpec type " + activationSpec.getClass().getName() + " does not support the validate() method.", uoe);
                }
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "createActivationSpec", destinationRef);
        return activationSpec;
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context DeclarativeService defined/populated component context
     */
    protected void deactivate(ComponentContext context) {
        destinationRef.deactivate(context);
        bootstrapContextRef.deactivate(context);
        rrsXAResFactorySvcRef.deactivate(context);
    }

    @Override
    public ApplicationRecycleContext getContext() {
        ApplicationRecycleContext context = bootstrapContextRef.getService();
        if (context != null) {
            return context;
        }
        return null;
    }

    @Override
    public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>(appsToRecycle);
        appsToRecycle.removeAll(members);
        return members;
    }

    /**
     * Determines whether or not an activation spec is RRS transactional.
     *
     * @param activationSpec activation spec
     * @return true if the activation spec is RRS transactional. False if not.
     */
    @FFDCIgnore(NoSuchMethodException.class)
    private static boolean isRRSTransactional(Object activationSpec) {
        try {
            return (Boolean) activationSpec.getClass().getMethod("getRRSTransactional").invoke(activationSpec);
        } catch (NoSuchMethodException x) {
            return false;
        } catch (Exception x) {
            return false;
        }
    }

    @FFDCIgnore(NoSuchMethodException.class)
    private static boolean hasDestinationProp(Object activationSpec) {
        try {
            activationSpec.getClass().getMethod("getDestination");
        } catch (NoSuchMethodException x) {
            return false;
        } catch (Exception x) {
            return false;
        }
        return true;
    }

    @FFDCIgnore(NoSuchMethodException.class)
    private static boolean hasDestinationLookupProp(Object activationSpec) {
        try {
            activationSpec.getClass().getMethod("getDestinationLookup");
        } catch (NoSuchMethodException x) {
            return false;
        } catch (Exception x) {
            return false;
        }
        return true;
    }

    /**
     * Update the activation config properties with the userName/password of the authAlias.
     *
     * @param id unique identifier for an authData config element.
     * @param activationProps activation config properties.
     * @throws UnavailableException if the authData element is not available.
     */
    private void processAuthData(String id, Map<String, Object> activationProps) throws UnavailableException {
        final String filter = FilterUtils.createPropertyFilter(ID, id);
        ServiceReference<?> authDataRef;
        try {
            ServiceReference<?>[] authDataRefs = PrivHelper.getServiceReferences(componentContext, "com.ibm.websphere.security.auth.data.AuthData", filter);
            if (authDataRefs == null || authDataRefs.length != 1)
                throw new UnavailableException("authData: " + id);
            authDataRef = authDataRefs[0];
        } catch (Exception x) {
            throw new UnavailableException("authData: " + id, x);
        }
        activationProps.put(USER_NAME, authDataRef.getProperty("user"));
        activationProps.put(PASSWORD, authDataRef.getProperty("password"));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.jta.XAResourceFactory#destroyXAResource(javax.transaction.xa.XAResource)
     */
    @Override
    public void destroyXAResource(XAResource xa) throws DestroyXAResourceException {
        //TODO custom method to destroy an XAResource
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.jta.XAResourceFactory#getXAResource(java.io.Serializable)
     */
    @Override
    public XAResource getXAResource(Serializable xaresinfo) throws XAResourceNotAvailableException {
        XAResource xa = null;
        if (xaresinfo != null) {
            @SuppressWarnings("unchecked")
            ArrayList<Byte> byteList = (ArrayList<Byte>) xaresinfo;
            byte[] bytes = new byte[byteList.size()];
            int i = 0;
            for (Byte b : byteList)
                bytes[i++] = b;
            try {
                ActivationConfig config = (ActivationConfig) Utils.deserialize(bytes);
                Object activationSpec = createActivationSpec(config.getActivationConfigProps(),
                                                             config.getAuthenticationAlias(),
                                                             null,
                                                             config.getDestinationRef(),
                                                             null,
                                                             config.getApplicationName());
                BootstrapContextImpl bootstrapContext = bootstrapContextRef.getServiceWithException();
                ActivationSpec[] actspecs = new ActivationSpec[] { (ActivationSpec) activationSpec };
                XAResource[] resources = bootstrapContext.resourceAdapter.getXAResources(actspecs);
                if (resources != null && resources.length == 1) {
                    xa = resources[0];
                } else if (resources != null && resources.length > 1) {
                    throw new IllegalStateException(Utils.getMessage("J2CA8800.multiple.xa.resources", bootstrapContext.getResourceAdapterName(), id));
                }
            } catch (Exception e) {
                throw new XAResourceNotAvailableException(e);
            }
        }
        return xa;
    }

    /**
     * This method will identify the BootstrapContext corresponding to the activationSpec and invoke
     * activateEndpoint on the resource adapter, returning a deactivationKey which can be used for deactivating the endpoint.
     *
     * @param mef MessageEndpointFactory that is passed from the container.
     * @param activationProperties The activation properties that are passed from the container
     * @param authenticationAlias The authentication alias of the server
     * @param destinationIDOrJNDIName The id of the destination or the JNDI name. EJB container might supply either. This is error prone, but we have already shipped this behavior.
     * @param adminObjSvc admin object service that the mdb runtime found
     * @param adminObjSvcRefId id of the admin object service that the mdb runtime found
     * @return activation spec instance.
     * @throws ResourceException
     */
    public Object activateEndpoint(WSMessageEndpointFactory mef,
                                   @Sensitive Properties activationProperties,
                                   String authenticationAlias,
                                   String destinationIDOrJNDIName,
                                   AdminObjectService adminObjSvc,
                                   String adminObjSvcRefId) throws ResourceException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activateEndpoint", mef, activationProperties, authenticationAlias, destinationIDOrJNDIName, adminObjSvc, adminObjSvcRefId);

        // Identify the RA Service to call endpointActivation on
        Object activationSpec;
        BootstrapContextImpl bootstrapContext = null;
        try {
            bootstrapContext = bootstrapContextRef.getServiceWithException();
            String adapterPid = (String) bootstrapContextRef.getReference().getProperty(Constants.SERVICE_PID);

            // If the mdb runtime found the admin object service, then get the id from the service,
            // otherwise pass in the destinationID
            if (adminObjSvcRefId == null)
                adminObjSvcRefId = destinationIDOrJNDIName;
            activationSpec = createActivationSpec(activationProperties,
                                                  authenticationAlias,
                                                  adminObjSvc,
                                                  adminObjSvcRefId,
                                                  destinationIDOrJNDIName,
                                                  mef.getJ2EEName().getApplication());
            int[] fullJCAVersionArray = getFullJCAVersion(bootstrapContext);
            mef.setJCAVersion(fullJCAVersionArray[0], // Major Version
                              fullJCAVersionArray[1]); // Minor Version
            mef.setRAKey(adapterPid);

            ActivationConfig config = new ActivationConfig(activationProperties, adminObjSvcRefId, authenticationAlias, mef.getJ2EEName().getApplication());
            // register with the TM
            int recoveryId = isRRSTransactional(activationSpec) ? registerRRSXAResourceInfo(id) : registerXAResourceInfo(config);
            mef.setRecoveryID(recoveryId);
            //TODO Add support for deferred Endpoint Activation.
            if (activationSpec instanceof ActivationSpec) {
                appsToRecycle.add(mef.getJ2EEName().getApplication());

                jcasu = new JcaServiceUtilities();
                raClassLoader = bootstrapContext.getRaClassLoader();

                ClassLoader previousClassLoader = jcasu.beginContextClassLoader(raClassLoader);
                try {
                    bootstrapContext.resourceAdapter.endpointActivation(mef, (ActivationSpec) activationSpec);
                } finally {
                    jcasu.endContextClassLoader(raClassLoader, previousClassLoader);
                }
                endpointActivationParams.add(new ActivationParams(activationSpec, mef));
            } else {
                //TODO We need to handle the case when @Activation is used.
                throw new UnsupportedOperationException();
            }
            Tr.info(tc, "J2CA8801.act.spec.active", id, mef.getJ2EEName());
        } catch (Exception ex) {
            Tr.error(tc, "J2CA8802.activation.failed", bootstrapContext == null ? null : bootstrapContext.getResourceAdapterName(), ex);
            throw ex instanceof ResourceException ? (ResourceException) ex : new ResourceException(ex);
        }
        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activateEndpoint", activationSpec);
        return activationSpec;
    }

    /**
     * Deactivate the endpoint corresponding to the specified activation spec instance.
     *
     * @param activationSpec the activation spec instance.
     * @param messageEndpointFactory the message endpoint factory.
     * @throws ResourceException
     */
    public void deactivateEndpoint(Object activationSpec, WSMessageEndpointFactory messageEndpointFactory) throws ResourceException {
        try {
            if (activationSpec instanceof ActivationSpec) {
                if (endpointActivationParams.remove(new ActivationParams(activationSpec, messageEndpointFactory)))
                    endpointDeactivation((ActivationSpec) activationSpec, messageEndpointFactory);
                else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "already deactivated");
            } else {
                //TODO We need to handle the case when @Activation is used.
                throw new UnsupportedOperationException();
            }
        } catch (Exception ex) {
            Tr.error(tc, "J2CA8803.deactivation.failed", bootstrapContextRef.getReference().getProperty(Constants.SERVICE_PID), ex);
            throw new ResourceException(ex);
        }
    }

    /**
     * Utility method to perform endpoint deactivation.
     *
     * @param activationSpec activation specification
     * @param messageEndpointFactory message endpoint factory
     */
    void endpointDeactivation(ActivationSpec activationSpec, WSMessageEndpointFactory messageEndpointFactory) {
        ClassLoader previousClassLoader = jcasu.beginContextClassLoader(raClassLoader);
        try {
            BootstrapContextImpl bootstrapContext = bootstrapContextRef.getServiceWithException();
            bootstrapContext.resourceAdapter.endpointDeactivation(messageEndpointFactory, activationSpec);
        } finally {
            jcasu.endContextClassLoader(raClassLoader, previousClassLoader);
        }
        Tr.info(tc, "J2CA8804.act.spec.inactive", id, messageEndpointFactory.getJ2EEName());
    }

    /**
     * Returns the JCA specification major and minor version with which the resource adapter declares support.
     *
     * @param bootstrapContext bootstrap context supplied to the resource adapter.
     * @return the JCA specification version with which the resource adapter declares support.
     */
    private static int[] getFullJCAVersion(BootstrapContextImpl bootstrapContext) {
        int[] fullVersionIntArray = { 1, 5 }; // SIB and WMQ resource adapter "bundles" are compliant with JCA spec version 1.5
        // TODO Set the correct JCA Version on the MessageEndpointFactory. Currently hardcoding to 1.5
        ResourceAdapterMetaData raMetadata = bootstrapContext.getResourceAdapterMetaData();
        if (raMetadata != null) {
            ConnectorModuleMetaData connectorMetadata = (ConnectorModuleMetaData) raMetadata.getModuleMetaData();
            String fullVersionString = connectorMetadata.getSpecVersion();
            String[] fullVersionStrArray = fullVersionString.split("\\.");
            fullVersionIntArray[0] = Integer.valueOf(fullVersionStrArray[0]);
            fullVersionIntArray[1] = Integer.valueOf(fullVersionStrArray[1]);
        }
        return fullVersionIntArray;
    }

    /**
     * Register XA resource information with the transaction manager.
     *
     * @param config Object containing information necessary for producing an XAResource object using the XAResourceFactory.
     * @return the recovery ID (or -1 if an error occurs)
     * @throws UnavailableException if the transaction manager is not available.
     */
    private final int registerXAResourceInfo(ActivationConfig config) throws UnavailableException {
        if (transactionManager == null)
            throw new UnavailableException(EmbeddableWebSphereTransactionManager.class.getName());

        // Transaction service will use the filter we provide to query the service registry for an XAResourceFactory.
        String filter = FilterUtils.createPropertyFilter(ID, id);
        // Pre-serialize resource info so that the transactions bundle can deserialize
        // back to the pre-serialized form without without having access to classes in our bundle.
        // Need to use List<Byte> instead of byte[] because XARecoveryWrapper does a shallow compare.
        ArrayList<Byte> resInfo;
        try {
            byte[] bytes = Utils.serObjByte(config);
            resInfo = new ArrayList<Byte>(bytes.length);
            for (byte b : bytes)
                resInfo.add(b);
        } catch (IOException x) {
            throw new IllegalArgumentException(x);
        }
        int recoveryToken = transactionManager.registerResourceInfo(filter, resInfo);
        return recoveryToken;
    }

    /**
     * Registers RRS XA resource information with the transaction manager. This is used for inbound.
     *
     * @param actSpecId The id of the activation spec
     * @return the recovery ID (or -1 if an error occurs)
     */
    public final int registerRRSXAResourceInfo(String actSpecId) {
        RRSXAResourceFactory xaFactory = rrsXAResFactorySvcRef.getService();
        // Make sure that the bundle is active.
        if (xaFactory == null) {
            String formattedMessage = Utils.getMessage("J2CA8807.native.rrs.not.available", new Object[0]);
            throw new IllegalStateException(formattedMessage);
        }
        // Create a filter for the transaction manager to be able to find the native
        // transaction factory in the service registry during recovery.
        String filter = FilterUtils.createPropertyFilter("native.xa.factory", (xaFactory.getClass().getCanonicalName()));
        // NOTE: At this point in time, the transaction manager does not support logging
        // XAResourceInfo type objects; However, they do allow generic serializable objects such as a String
        // to be logged and retrieved during recovery. So, a String is what is currently passed as resource info to
        // the registerResourceInfo call.
        Serializable xaResInfo = xaFactory.getXAResourceInfo(null);
        int recoveryToken = transactionManager.registerResourceInfo(filter, xaResInfo);
        return recoveryToken;
    }

    /**
     * Declarative Services method for setting the service reference for the default auth data
     *
     * @param ref reference to the service
     */
    protected void setAuthData(ServiceReference<?> ref) { // com.ibm.websphere.security.auth.data.AuthData
        authDataRef = ref;
    }

    /**
     * Declarative Services method for setting the BootstrapContext reference
     *
     * @param ref reference to the service
     */
    protected void setBootstrapContext(ServiceReference<BootstrapContextImpl> ref) {
        bootstrapContextRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the destination reference
     *
     * @param ref reference to the service
     */
    protected void setDestination(ServiceReference<AdminObjectService> ref) {
        destinationRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the transaction manager.
     *
     * @param tm the transaction manager.
     */
    protected void setEmbeddableWebSphereTransactionManager(EmbeddableWebSphereTransactionManager tm) {
        transactionManager = tm;
    }

    /**
     * Declarative Services method for setting the RRS XA resource factory service implementation reference.
     *
     * @param ref reference to the service
     */
    protected void setRRSXAResourceFactory(ServiceReference<RRSXAResourceFactory> ref) {
        rrsXAResFactorySvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the service reference for default auth data
     *
     * @param ref reference to the service
     */
    protected void unsetAuthData(ServiceReference<?> ref) { // com.ibm.websphere.security.auth.data.AuthData
        authDataRef = null;
    }

    /**
     * Declarative Services method for unsetting the BootstrapContext reference
     *
     * @param ref reference to the service
     */
    protected void unsetBootstrapContext(ServiceReference<BootstrapContextImpl> ref) {
        bootstrapContextRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the destination reference
     *
     * @param ref reference to the service
     */
    protected void unsetDestination(ServiceReference<AdminObjectService> ref) {
        destinationRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the transaction manager.
     *
     * @param tm the transaction manager.
     */
    protected void unsetEmbeddableWebSphereTransactionManager(EmbeddableWebSphereTransactionManager tm) {
        transactionManager = null;
    }

    /**
     * Declarative Services method for unsetting the RRS XA resource factory service implementation reference.
     *
     * @param ref reference to the service
     */
    protected void unsetRRSXAResourceFactory(ServiceReference<RRSXAResourceFactory> ref) {
        rrsXAResFactorySvcRef.unsetReference(ref);
    }

    private static final JCARuntimeVersion DEFAULT_VERSION = new JCARuntimeVersion16();
    private static JCARuntimeVersion runtimeVersion = DEFAULT_VERSION;

    public static JCARuntimeVersion getJCARuntimeVersion() {
        return runtimeVersion;
    }

    /**
     * Declarative Services method for setting the JCA feature version
     *
     * @param ref the version
     */
    protected void setJcaRuntimeVersion(JCARuntimeVersion ref) {
        runtimeVersion = ref;
    }

    /**
     * Declarative Services method for unsetting the JCA feature version
     *
     * @param ref the version
     */
    protected void unsetJcaRuntimeVersion(JCARuntimeVersion ref) {
        Version toUnset = ref.getVersion();

        // If we are removing the jca feature completely, unset jca version to DEFAULT_VERSION
        if (runtimeVersion.getVersion().compareTo(toUnset) == 0)
            runtimeVersion = DEFAULT_VERSION;
    }

    public static boolean atLeastJCAVersion(Version ver) {
        return (runtimeVersion.getVersion().compareTo(ver) >= 0);
    }

    public static boolean beforeJCAVersion(Version ver) {
        return !atLeastJCAVersion(ver);
    }

}
