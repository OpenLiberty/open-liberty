/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.ext.annotation.DSExt;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.bnd.metatype.annotation.Ext;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.registry.ExternalUserRegistryWrapper;
import com.ibm.ws.security.registry.FederationRegistry;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryChangeListener;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

//there are two schema validation problems with refId.
//The label "Reference to a UserRegistry configuration element" for attribute refId of pid com.ibm.ws.security.registry should be in sentence case.
//The label for element refId of type com.ibm.ws.security.registry contains the banned word element in the label "Reference to a UserRegistry configuration element".
@ObjectClassDefinition(factoryPid = "com.ibm.ws.security.registry", name = Ext.INTERNAL/* "%registry.config" */, description = "%registry.config.desc",
                       localization = Ext.LOCALIZATION)
@Ext.Alias("userRegistry")
interface UserRegistryConfig {

    @AttributeDefinition(name = "%refId", description = "%refId.desc", required = false)
    String[] refId();

    @AttributeDefinition(name = "%realm", description = "%realm.desc", required = false)
    String realm();
}

@ObjectClassDefinition(pid = "com.ibm.ws.security.registry.internal.UserRegistryRefConfig", name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, localization = Ext.LOCALIZATION)
interface UserRegistryRefConfig {

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, required = false)
    @Ext.ServiceClass(FederationRegistry.class)
    String federationRegistry();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, defaultValue = "${count(federationRegistry)}")
    String FederationRegistry_cardinality_minimum();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, required = false, defaultValue = "*")
    @Ext.ServiceClass(UserRegistry.class)
    @Ext.Final
    String[] UserRegistry();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, defaultValue = "${count(UserRegistry)}")
    String UserRegistry_cardinality_minimum();

}

/**
 * Implements the UserRegistryService to enable runtime discovery of configured
 * UserRegistry objects.
 */
@Component(immediate = true,
           //order means config.displayId comes from factory config
           configurationPid = { "com.ibm.ws.security.registry.internal.UserRegistryRefConfig", "com.ibm.ws.security.registry" },
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           property = "service.vendor=IBM")
@DSExt.ConfigurableServiceProperties
public class UserRegistryServiceImpl implements UserRegistryService {

    private static final TraceComponent tc = Tr.register(UserRegistryServiceImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    final ConcurrentServiceReferenceMap<String, UserRegistry> userRegistries = new ConcurrentServiceReferenceMap<String, UserRegistry>(KEY_USER_REGISTRY);
    private final ConcurrentServiceReferenceSet<UserRegistryChangeListener> listeners = new ConcurrentServiceReferenceSet<UserRegistryChangeListener>(KEY_LISTENER);

    private volatile FederationRegistry federationRegistry;

    // Keep track of the actual user registry to use
    private final AtomicReference<UserRegistry> userRegistry = new AtomicReference<UserRegistry>();
    private final Object userRegistrySync = new Object() {};
    private final List<String> registryTypes = new ArrayList<String>();

    static final String KEY_USER_REGISTRY = "UserRegistry";
    static final String KEY_LISTENER = "UserRegistryChangeListener";
    static final String KEY_CONFIG_ID = "config.id";
    static final String KEY_TYPE = UserRegistryService.REGISTRY_TYPE;
    static final String KEY_COMPONENT_NAME = "component.name";

    static final String UNKNOWN_TYPE = "UNKNOWN";

    static final String CFG_KEY_REFID = "refId";
    static final String CFG_KEY_REALM = "realm";
    private static final String CFG_KEY_UR_COUNT = "UserRegistry.cardinality.minimum";

    private static final String FEDERATION_REGISTRY_TYPE = "WIM";

    private String[] refId;
    private String realm;
    private int urCount;

    static final String SERVICE_PROPERTY_USER_REGISTRY_CONFIGURED = "userRegistryConfigured";
    private static final String SERVICE_PROPERTY_REALM = "realm";

    private volatile Map<String, Object> props;

    private boolean isFederationActive = false;

    /**
     * Method will be called for each UserRegistryConfiguration that is
     * registered in the OSGi service registry. We maintain an internal
     * map of these for easy access.
     * <p>
     * Since id values are NOT guaranteed to be unique across types,
     * it is possible to have a config like this:
     * <p>
     * 
     * <pre> {@code
     * <basicRegistry id="basic1" realm="SampleRealm">
     * <user name="admin" password="adminpwd" />
     * </basicRegistry>
     * 
     * <ldapRegistry id="basic1" /> <-- DUPLICATE ID
     * <ldapRegistry id="ldap1" /> * } </pre>
     * 
     * <p>
     * In this case, there are two basic1 IDs. If this situatoin occurs,
     * the first instance of the ID is used, and all subsequent instances
     * are ignored. The decision of which one to ignore was arbitrary and
     * due to the nature of Declarative Services, non-deterministic as the
     * order of which the services get set is not guaranteed.
     * 
     * @param ref Reference to a registered UserRegistryConfiguration
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, target = "(!(objectClass=com.ibm.ws.security.registry.FederationRegistry))")
    protected Map<String, Object> setUserRegistry(ServiceReference<UserRegistry> ref) {
        String configId = (String) ref.getProperty(KEY_CONFIG_ID);
        String type = (String) ref.getProperty(KEY_TYPE);

        if (configId != null && type != null) {
            configId = parseIdFromConfigID(configId);
            userRegistries.putReference(configId, ref);
        } else {
            if (type == null) {
                Tr.error(tc, "USER_REGISTRY_SERVICE_WITHOUT_TYPE", ref.getProperty(KEY_COMPONENT_NAME));
            }
            if (configId == null) {
                if (type != null) {
                    Tr.error(tc, "USER_REGISTRY_SERVICE_CONFIGURATION_WITHOUT_ID", type);
                } else {
                    Tr.error(tc, "USER_REGISTRY_SERVICE_CONFIGURATION_WITHOUT_ID", UNKNOWN_TYPE);
                }
            }
        }

        if (type != null) {
            registryTypes.add(type);
        } else {
            registryTypes.add(UNKNOWN_TYPE);
        }

        notifyListeners();

        return refreshUserRegistryCache();
    }

    /**
     * @param configId
     * @return
     */
    private String parseIdFromConfigID(String configId) {
        // NOTE: This is designed to extract the ID from the config.id string. In 8.5.5,
        // the config.id for a top level element was equivalent to the ID attribute. Now,
        // it will always contain either the nodeName or the pid. For example:
        // com.ibm.ws.security.registry.basic[basic1]
        //
        // This method will give the security code its old behavior, but there are better
        // ways to do this.
        Matcher idMatcher = Pattern.compile("\\Q[\\E(.+?)\\Q]\\E").matcher(configId);

        if (idMatcher.find())
            return idMatcher.group(1);
        else
            return configId;
    }

    protected Map<String, Object> updatedUserRegistry(ServiceReference<UserRegistry> ref) {
        notifyListeners();

        return refreshUserRegistryCache();
    }

    /**
     * Method will be called for each UserRegistryConfiguration that is
     * unregistered in the OSGi service registry. We must remove this
     * instance from our internal map.
     * <p>
     * The ConcurrentServiceReferenceMap automatically guards against removing
     * a reference of the wrong type, so this method does not need special
     * logic to protect against the case where multiple configurations define
     * the same ID but are of different types.
     * 
     * @param ref Reference to an unregistered UserRegistryConfiguration
     */
    protected Map<String, Object> unsetUserRegistry(ServiceReference<UserRegistry> ref) {
        String id = parseIdFromConfigID((String) ref.getProperty(KEY_CONFIG_ID));
        userRegistries.removeReference(id, ref);
        notifyListeners();

        String type = (String) ref.getProperty(KEY_TYPE);
        if (type != null) {
            registryTypes.remove(type);
        } else {
            registryTypes.remove(UNKNOWN_TYPE);
        }

        /*
         * Don't refresh the user registry cache if we are shutting down.
         */
        if (FrameworkState.isStopping()) {
            return null;
        } else {
            return refreshUserRegistryCache();
        }
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected Map<String, Object> setFederationRegistry(FederationRegistry federationRegistry) {
        this.federationRegistry = federationRegistry;
        return refreshUserRegistryCache();
    }

    protected Map<String, Object> unsetFederationRegistry(FederationRegistry federationRegistry) {
        if (this.federationRegistry == federationRegistry) {
            this.federationRegistry = null;
        }

        /*
         * Don't refresh the user registry cache if we are shutting down.
         */
        if (FrameworkState.isStopping()) {
            return null;
        } else {
            return refreshUserRegistryCache();
        }
    }

    /**
     * @param ref Reference to a registered UserRegistryChangeListener
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setUserRegistryChangeListener(ServiceReference<UserRegistryChangeListener> ref) {
        listeners.addReference(ref);
    }

    /**
     * @param ref Reference to an unregistered UserRegistryChangeListener
     */
    protected void unsetUserRegistryChangeListener(ServiceReference<UserRegistryChangeListener> ref) {
        listeners.removeReference(ref);
    }

    @Activate
    protected Map<String, Object> activate(ComponentContext cc, Map<String, Object> props) {
        this.props = props;
        refId = (String[]) props.get(CFG_KEY_REFID);
        realm = (String) props.get(CFG_KEY_REALM);
        urCount = props.get(CFG_KEY_UR_COUNT) == null ? -1 : Integer.valueOf((String) props.get(CFG_KEY_UR_COUNT));

        userRegistries.activate(cc);
        listeners.activate(cc);
        return getServiceProperties();
    }

    @Modified
    protected Map<String, Object> modify(Map<String, Object> props) {
        refId = (String[]) props.get(CFG_KEY_REFID);
        realm = (String) props.get(CFG_KEY_REALM);
        urCount = props.get(CFG_KEY_UR_COUNT) == null ? -1 : Integer.valueOf((String) props.get(CFG_KEY_UR_COUNT));
        this.props = props;
        notifyListeners();

        return refreshUserRegistryCache();
    }

    @Deactivate
    protected Map<String, Object> deactivate(ComponentContext cc) {
        refId = null;
        realm = null;
        userRegistries.deactivate(cc);
        listeners.deactivate(cc);
        return getServiceProperties();
    }

    /**
     * Notify the registered listeners of the change to the UserRegistry
     * configuration.
     */
    private void notifyListeners() {
        for (UserRegistryChangeListener listener : listeners.services()) {
            listener.notifyOfUserRegistryChange();
        }
    }

    /**
     * Determine if configuration data is defined in the server.xml
     * 
     * @return true if the config.source is non-null
     */
    private boolean isConfigured() {
        return refId != null;
    }

    /**
     * Determines which UserRegistryFactory (possibly multiple)
     * to load based on the configured refId(s). If the refId is undefined,
     * a RegistryException is thrown. If there are multiple refIds specified,
     * then they will be delegated to via the UserRegistryProxy. Otherwise,
     * the single, requested UserRegistryFactory is found and its associated
     * UserRegistry instance is returned.
     * 
     * @return UserRegistry instance. {@code null} is not returned.
     * @throws RegistryException if there is a problem obtaining the UserRegistry instance.
     */
    private UserRegistry determineActiveUserRegistry(boolean exceptionOnError) throws RegistryException {
        synchronized (userRegistrySync) {
            UserRegistry ur = userRegistry.get();
            if (ur == null) {
                if (isConfigured()) {
                    ur = getUserRegistryFromConfiguration();
                } else {
                    ur = autoDetectUserRegistry(exceptionOnError);
                }

                // remember the user registry to use
                userRegistry.set(ur);
            }
            return ur;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUserRegistryConfigured() throws RegistryException {
        return null != determineActiveUserRegistry(false);
    }

    /** {@inheritDoc} */
    @Override
    public UserRegistry getUserRegistry() throws RegistryException {
        return determineActiveUserRegistry(true);
    }

    /**
     * When a configuration element is not defined, use some "auto-detect"
     * logic to try and return the single UserRegistry. If there is no
     * service, or multiple services, that is considered an error case which
     * "auto-detect" can not resolve.
     * 
     * @return
     * @throws RegistryException
     */
    private UserRegistry autoDetectUserRegistry(boolean exceptionOnError) throws RegistryException {
        // Determine if there is a federation registry configured.
        UserRegistry ur = getFederationRegistry(exceptionOnError);
        synchronized (userRegistrySync) {
            if (ur != null) {
                setRegistriesToBeFederated((FederationRegistry) ur, exceptionOnError);
                isFederationActive = true;
                return ur;
            } else
                isFederationActive = false;
        }

        if (userRegistries.isEmpty()) {
            if (exceptionOnError) {
                Tr.error(tc, "USER_REGISTRY_SERVICE_NO_USER_REGISTRY_AVAILABLE");
                throw new RegistryException(TraceNLS.getFormattedMessage(this.getClass(), TraceConstants.MESSAGE_BUNDLE,
                                                                         "USER_REGISTRY_SERVICE_NO_USER_REGISTRY_AVAILABLE",
                                                                         new Object[] {},
                                                                         "CWWKS3005E: A configuration exception has occurred. No UserRegistry implementation service is available.  Ensure that you have a user registry configured."));
            } else {
                return null;
            }
        } else if (urCount > 1) {
            if (exceptionOnError) {
                Tr.error(tc, "USER_REGISTRY_SERVICE_MULTIPLE_USER_REGISTRY_AVAILABLE");
                throw new RegistryException(TraceNLS.getFormattedMessage(
                                                                         this.getClass(),
                                                                         TraceConstants.MESSAGE_BUNDLE,
                                                                         "USER_REGISTRY_SERVICE_MULTIPLE_USER_REGISTRY_AVAILABLE",
                                                                         new Object[] {},
                                                                         "CWWKS3006E: A configuration error has occurred. Multiple available UserRegistry implementation services, unable to determine which to use."));
            } else {
                return null;
            }
        } else {
            String id = userRegistries.keySet().iterator().next();
            return getUserRegistry(id, exceptionOnError);
        }
    }

    /**
     * @return
     * @throws RegistryException
     * 
     */
    //TODO lazy??
    private UserRegistry getFederationRegistry(boolean exceptionOnError) throws RegistryException {
        return federationRegistry;
    }

    /**
     * When a configuration element is defined, use it to resolve the effective
     * UserRegistry configuration.
     * 
     * @return
     * @throws RegistryException
     */
    private UserRegistry getUserRegistryFromConfiguration() throws RegistryException {
        String[] refIds = this.refId;
        if (refIds == null || refIds.length == 0) {
            // Can look for config.source = file
            // If thats set, and we're missing this, we can error.
            // If its not set, we don't have configuration from the
            // file and we should try to resolve if we have one instance defined?
            Tr.error(tc, "USER_REGISTRY_SERVICE_CONFIG_ERROR_NO_REFID");
            throw new RegistryException(TraceNLS.getFormattedMessage(
                                                                     this.getClass(),
                                                                     TraceConstants.MESSAGE_BUNDLE,
                                                                     "USER_REGISTRY_SERVICE_CONFIG_ERROR_NO_REFID",
                                                                     null,
                                                                     "CWWKS3000E: A configuration error has occurred. There is no configured refId parameter for the userRegistry configuration."));
        } else if (refIds.length == 1) {
            return getUserRegistry(refIds[0]);
        } else {
            // Multiple refIds, we'll use the UserRegistryProxy.
            List<UserRegistry> delegates = new ArrayList<UserRegistry>();
            for (String refId : refIds) {
                delegates.add(getUserRegistry(refId));
            }
            return new UserRegistryProxy(realm, delegates);
        }
    }

    /**
     * {@inheritDoc} This lookup is done for all registered UserRegistryFactory
     * instances. Please note that this method does not use the configuration
     * data for the UserRegistryService for this lookup.
     */
    @Override
    public UserRegistry getUserRegistry(String id) throws RegistryException {
        return getUserRegistry(id, true);
    }

    private UserRegistry getUserRegistry(String id, boolean exceptionOnError) throws RegistryException {
        if (id == null) {
            throw new IllegalArgumentException("getUserRegistry(String) does not support null id");
        }

        UserRegistry userRegistry = userRegistries.getService(id);
        if (userRegistry != null) {
            return userRegistry;
        } else {
            Tr.error(tc, "USER_REGISTRY_SERVICE_CONFIG_ERROR_NO_SUCH_ID", id);
            if (exceptionOnError) {
                throw new RegistryException(TraceNLS.getFormattedMessage(
                                                                         this.getClass(),
                                                                         TraceConstants.MESSAGE_BUNDLE,
                                                                         "USER_REGISTRY_SERVICE_CONFIG_ERROR_NO_SUCH_ID",
                                                                         new Object[] { id },
                                                                         "CWWKS3001E: A configuration error has occurred. The requested UserRegistry instance with id {0} could not be found."));
            }
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getUserRegistryType() {
        synchronized (userRegistrySync) {
            if (isFederationActive && userRegistry != null) {
                return FEDERATION_REGISTRY_TYPE;
            }
        }
        if (registryTypes.isEmpty() || registryTypes.size() > 1) {
            return UNKNOWN_TYPE;
        } else {
            return registryTypes.get(0);
        }
    }

    /**
     * Determine a new user registry to use by resetting the cache and calling getServiceProperties
     */
    private Map<String, Object> refreshUserRegistryCache() {
        synchronized (userRegistrySync) {
            userRegistry.set(null);
            return getServiceProperties();
        }
    }

    @FFDCIgnore(RegistryException.class)
    private Map<String, Object> getServiceProperties() {
        Map<String, Object> props = this.props;
        if (props == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<String, Object>(props);
        try {
            UserRegistry userRegistry = determineActiveUserRegistry(false);
            if (userRegistry == null) {
                result.put(SERVICE_PROPERTY_USER_REGISTRY_CONFIGURED, false);
            } else {
                result.put(SERVICE_PROPERTY_USER_REGISTRY_CONFIGURED, true);
                result.put(SERVICE_PROPERTY_REALM, userRegistry.getRealm());

            }
        } catch (RegistryException e) {
            //apparently it wasn't configured very successfully
        }
        return result;
    }

    private void setRegistriesToBeFederated(FederationRegistry federationRegistry, boolean exceptionOnError) throws RegistryException {
        federationRegistry.removeAllFederatedRegistries();

        try {
            List<UserRegistry> urs = new ArrayList<UserRegistry>();
            for (String id : userRegistries.keySet()) {
                urs.add(userRegistries.getServiceWithException(id));
            }
            federationRegistry.addFederationRegistries(urs);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to federate user registries.");
            }

            Tr.error(tc, "USER_REGISTRY_SERVICE_FEDERATION_FAILED", e);
            throw new RegistryException(TraceNLS.getFormattedMessage(
                                                                     this.getClass(),
                                                                     TraceConstants.MESSAGE_BUNDLE,
                                                                     "USER_REGISTRY_SERVICE_FEDERATION_FAILED",
                                                                     new Object[] { e },
                                                                     "CWWKS3010E: An unexpected exception occurred federating user registries: " + e), e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistryService#getExternalUserRegistry(com.ibm.ws.security.registry.UserRegistry)
     */
    @Override
    public com.ibm.websphere.security.UserRegistry getExternalUserRegistry(UserRegistry userRegistry) {
        if (userRegistry instanceof ExternalUserRegistryWrapper) {
            return ((ExternalUserRegistryWrapper) userRegistry).getExternalUserRegistry();
        }
        return new UserRegistryWrapper(userRegistry);
    }
}