/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.scr.ext.annotation.DSExt;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 * White-board model. The services are stored based on id, or in the
 * case a ServiceReference has no id, its service.id. The case where
 * an id is unavailable is when we are in a "config-less" mode for
 * that service.
 *
 * Note the removal of the service is always done on the service.id
 * and id as those values may have been set.
 */
@Component(configurationPid = "com.ibm.ws.security.service",
           property = "service.vendor=IBM",
           immediate = true)
@DSExt.ConfigurableServiceProperties
public class SecurityServiceImpl implements SecurityService {
    private static final TraceComponent tc = Tr.register(SecurityServiceImpl.class);

    static final String KEY_CONFIGURATION = "Configuration";
    static final String KEY_AUTHENTICATION = "Authentication";
    static final String KEY_AUTHORIZATION = "Authorization";
    public static final String KEY_USERREGISTRY = "UserRegistry";
    static final String KEY_ID = "id";
    static final String KEY_SERVICE_ID = "service.id";
    static final String KEY_CONFIG_SOURCE = "config.source";

    static final String CFG_KEY_SYSTEM_DOMAIN = "systemDomain";
    static final String CFG_KEY_DEFAULT_APP_DOMAIN = "defaultAppDomain";

    private ComponentContext cc;
    final ConcurrentServiceReferenceMap<String, SecurityConfiguration> configs = new ConcurrentServiceReferenceMap<String, SecurityConfiguration>(KEY_CONFIGURATION);
    final ConcurrentServiceReferenceMap<String, AuthenticationService> authentication = new ConcurrentServiceReferenceMap<String, AuthenticationService>(KEY_AUTHENTICATION);
    final ConcurrentServiceReferenceMap<String, AuthorizationService> authorization = new ConcurrentServiceReferenceMap<String, AuthorizationService>(KEY_AUTHORIZATION);
    final ConcurrentServiceReferenceMap<String, UserRegistryService> userRegistry = new ConcurrentServiceReferenceMap<String, UserRegistryService>(KEY_USERREGISTRY);

    // Keep track of the actual services to use
    final AtomicReference<AuthenticationService> authnService = new AtomicReference<AuthenticationService>();
    final AtomicReference<AuthorizationService> authzService = new AtomicReference<AuthorizationService>();
    final AtomicReference<UserRegistryService> userRegistryService = new AtomicReference<UserRegistryService>();

    private volatile String cfgSystemDomain = null;
    private volatile String cfgDefaultAppDomain = null;

    private Map<String, Object> props;

    /**
     * Method will be called for each SecurityConfiguration that is registered
     * in the OSGi service registry. We maintain an internal map of these for
     * easy access.
     *
     * If the ID is not defined for the configuration, it is incomplete and
     * can not be used.
     *
     * @param ref Reference to a registered SecurityConfiguration
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected Map<String, Object> setConfiguration(ServiceReference<SecurityConfiguration> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        if (id != null) {
            configs.putReference(id, ref);
        } else {
            Tr.error(tc, "SECURITY_SERVICE_REQUIRED_SERVICE_WITHOUT_ID", "securityConfiguration");
        }
        return getServiceProperties();
    }

    /**
     * Method will be called for each SecurityConfiguration that is unregistered
     * in the OSGi service registry. We must remove this instance from our
     * internal map.
     *
     * @param ref Reference to an unregistered SecurityConfiguration
     */
    protected Map<String, Object> unsetConfiguration(ServiceReference<SecurityConfiguration> ref) {
        configs.removeReference((String) ref.getProperty(KEY_ID), ref);
        return getServiceProperties();
    }

    /**
     * Determine if the properties for this ServiceReference came from
     * a file.
     *
     * @return true if the config.source is "file"
     */
    private boolean hasPropertiesFromFile(ServiceReference<?> ref) {
        return "file".equals(ref.getProperty(KEY_CONFIG_SOURCE));
    }

    /**
     * Method will be called for each AuthenticationService that is registered
     * in the OSGi service registry. We maintain an internal map of these for
     * easy access.
     *
     * @param ref Reference to a registered AuthenticationService
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected Map<String, Object> setAuthentication(ServiceReference<AuthenticationService> ref) {
        if (hasPropertiesFromFile(ref)) {
            String id = (String) ref.getProperty(KEY_ID);
            if (id != null) {
                authentication.putReference(id, ref);
            } else {
                Tr.error(tc, "SECURITY_SERVICE_REQUIRED_SERVICE_WITHOUT_ID", KEY_AUTHENTICATION);
            }
        } else {
            authentication.putReference(String.valueOf(ref.getProperty(KEY_SERVICE_ID)), ref);
        }

        // determine a new authentication service
        authnService.set(null);
        return getServiceProperties();
    }

    /**
     * Method will be called for each AuthenticationService that is unregistered
     * in the OSGi service registry. We must remove this instance from our
     * internal map.
     *
     * @param ref Reference to an unregistered AuthenticationService
     */
    protected Map<String, Object> unsetAuthentication(ServiceReference<AuthenticationService> ref) {
        authentication.removeReference((String) ref.getProperty(KEY_ID), ref);
        authentication.removeReference(String.valueOf(ref.getProperty(KEY_SERVICE_ID)), ref);

        // determine a new authentication service
        authnService.set(null);
        return getServiceProperties();
    }

    /**
     * Method will be called for each AuthorizationService that is registered
     * in the OSGi service registry. We maintain an internal map of these for
     * easy access.
     *
     * @param ref Reference to a registered AuthorizationService
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected Map<String, Object> setAuthorization(ServiceReference<AuthorizationService> ref) {
        if (hasPropertiesFromFile(ref)) {
            String id = (String) ref.getProperty(KEY_ID);
            if (id != null) {
                authorization.putReference(id, ref);
            } else {
                Tr.error(tc, "SECURITY_SERVICE_REQUIRED_SERVICE_WITHOUT_ID", KEY_AUTHORIZATION);
            }
        } else {
            authorization.putReference(String.valueOf(ref.getProperty(KEY_SERVICE_ID)), ref);
        }

        // determine a new authorization service
        authzService.set(null);
        return getServiceProperties();
    }

    /**
     * Method will be called for each AuthorizationService that is unregistered
     * in the OSGi service registry. We must remove this instance from our
     * internal map.
     *
     * @param ref Reference to an unregistered AuthorizationService
     */
    protected Map<String, Object> unsetAuthorization(ServiceReference<AuthorizationService> ref) {
        authorization.removeReference((String) ref.getProperty(KEY_ID), ref);
        authorization.removeReference(String.valueOf(ref.getProperty(KEY_SERVICE_ID)), ref);

        // determine a new authorization service
        authzService.set(null);
        return getServiceProperties();
    }

    /**
     * Method will be called for each UserRegistryService that is registered
     * in the OSGi service registry. We maintain an internal map of these for
     * easy access.
     *
     * @param ref Reference to a registered UserRegistryService
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, target = "(config.displayId=*)")
    protected Map<String, Object> setUserRegistry(ServiceReference<UserRegistryService> ref) {
        adjustUserRegistryServiceRef(ref);

        // determine a new user registry service
        userRegistryService.set(null);
        return getServiceProperties();
    }

    private void adjustUserRegistryServiceRef(ServiceReference<UserRegistryService> ref) {
        if (!"com.ibm.ws.security.registry.internal.UserRegistryRefConfig".equals(ref.getProperty("config.displayId"))) {
            String id = (String) ref.getProperty(KEY_ID);
            if (id != null) {
                userRegistry.putReference(id, ref);
                userRegistry.removeReference(String.valueOf(ref.getProperty(KEY_SERVICE_ID)), ref);
            } else {
                Tr.error(tc, "SECURITY_SERVICE_REQUIRED_SERVICE_WITHOUT_ID", KEY_USERREGISTRY);
            }
        } else {
            userRegistry.putReference(String.valueOf(ref.getProperty(KEY_SERVICE_ID)), ref);
        }
    }

    protected Map<String, Object> updatedUserRegistry(ServiceReference<UserRegistryService> ref) {
        // determine a new user registry service
        adjustUserRegistryServiceRef(ref);
        userRegistryService.set(null);
        return getServiceProperties();
    }

    /**
     * Method will be called for each UserRegistryService that is unregistered
     * in the OSGi service registry. We must remove this instance from our
     * internal map.
     *
     * @param ref Reference to an unregistered UserRegistryService
     */
    protected Map<String, Object> unsetUserRegistry(ServiceReference<UserRegistryService> ref) {
        userRegistry.removeReference((String) ref.getProperty(KEY_ID), ref);
        userRegistry.removeReference(String.valueOf(ref.getProperty(KEY_SERVICE_ID)), ref);

        // determine a new user registry service
        userRegistryService.set(null);
        return getServiceProperties();
    }

    @Activate
    protected Map<String, Object> activate(ComponentContext cc, Map<String, Object> props) {
        this.cc = cc;
        this.props = props;
        configs.activate(cc);
        authentication.activate(cc);
        authorization.activate(cc);
        userRegistry.activate(cc);

        setAndValidateProperties((String) props.get(CFG_KEY_SYSTEM_DOMAIN),
                                 (String) props.get(CFG_KEY_DEFAULT_APP_DOMAIN));
        return getServiceProperties();
    }

    @Modified
    protected Map<String, Object> modify(Map<String, Object> newProperties) {
        this.props = newProperties;
        // determine a new set of services
        authnService.set(null);
        authzService.set(null);
        userRegistryService.set(null);
        setAndValidateProperties((String) newProperties.get(CFG_KEY_SYSTEM_DOMAIN),
                                 (String) newProperties.get(CFG_KEY_DEFAULT_APP_DOMAIN));
        return getServiceProperties();
    }

    @Deactivate
    protected Map<String, Object> deactivate(ComponentContext cc, Map<String, Object> props) {
        this.cc = null;
        this.props = props;
        configs.deactivate(cc);
        authentication.deactivate(cc);
        authorization.deactivate(cc);
        userRegistry.deactivate(cc);
        cfgSystemDomain = null;
        cfgDefaultAppDomain = null;
        return getServiceProperties();
    }

    /**
     * @return
     */
    private Map<String, Object> getServiceProperties() {
        if (props == null)
            return null;
        Map<String, Object> result = new HashMap<String, Object>(props);
        if (!authentication.isEmpty()) {
            result.put(KEY_AUTHENTICATION, authentication.keySet().toArray(new String[authentication.size()]));
        }
        if (!authorization.isEmpty()) {
            result.put(KEY_AUTHORIZATION, authorization.keySet().toArray(new String[authorization.size()]));
        }
        if (!configs.isEmpty()) {
            result.put(KEY_CONFIGURATION, configs.keySet().toArray(new String[configs.size()]));
        }
        if (!userRegistry.isEmpty()) {
            List<String> userRegistryRealms = new ArrayList<String>();
            for (ServiceReference<UserRegistryService> ref : userRegistry.references()) {
                if (ref.getProperty("realm") != null) {
                    userRegistryRealms.add((String) ref.getProperty("realm"));
                }

            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setting " + KEY_USERREGISTRY + " to " + userRegistryRealms);
            }
            result.put(KEY_USERREGISTRY, userRegistryRealms.toArray(new String[userRegistryRealms.size()]));
        }
        return result;
    }

    /**
     * Determine if configuration data is defined in the server.xml
     *
     * @return true if the config.source is "file"
     */
    private boolean isConfigurationDefinedInFile() {
        return "file".equals(cc.getProperties().get(KEY_CONFIG_SOURCE));
    }

    /**
     * Sets and validates the configuration properties.
     * If the {@link #CFG_KEY_DEFAULT_APP_DOMAIN} property is not set, then
     * it will use the value of {@link #CFG_KEY_SYSTEM_DOMAIN}.
     *
     * Note this method will be a no-op if there is no configuration data
     * from the file.
     *
     * @param systemDomain
     * @param defaultAppDomain
     * @throws IllegalArgumentException if {link #CFG_KEY_SYSTEM_DOMAIN} is not set.
     */
    private void setAndValidateProperties(String systemDomain, String defaultAppDomain) {
        if (isConfigurationDefinedInFile()) {
            if ((systemDomain == null) || systemDomain.isEmpty()) {
                Tr.error(tc, "SECURITY_SERVICE_ERROR_MISSING_ATTRIBUTE", CFG_KEY_SYSTEM_DOMAIN);
                throw new IllegalArgumentException(Tr.formatMessage(tc, "SECURITY_SERVICE_ERROR_MISSING_ATTRIBUTE", CFG_KEY_SYSTEM_DOMAIN));
            }
            this.cfgSystemDomain = systemDomain;
            if ((defaultAppDomain == null) || defaultAppDomain.isEmpty()) {
                this.cfgDefaultAppDomain = systemDomain;
            } else {
                this.cfgDefaultAppDomain = defaultAppDomain;
            }
        }
    }

    /**
     * Eventually this will be execution context aware and pick the right domain.
     * Till then, we're only accessing the system domain configuration.
     *
     * @return SecurityConfiguration representing the "effective" configuration
     *         for the execution context.
     */
    private SecurityConfiguration getEffectiveSecurityConfiguration() {
        SecurityConfiguration effectiveConfig = configs.getService(cfgSystemDomain);
        if (effectiveConfig == null) {
            Tr.error(tc, "SECURITY_SERVICE_ERROR_BAD_DOMAIN", cfgSystemDomain, CFG_KEY_SYSTEM_DOMAIN);
            throw new IllegalArgumentException(Tr.formatMessage(tc, "SECURITY_SERVICE_ERROR_BAD_DOMAIN", cfgSystemDomain, CFG_KEY_SYSTEM_DOMAIN));
        }
        return effectiveConfig;
    }

    /**
     * @throw Translated IllegalArgumentException representing an invalid attribute value error
     */
    private void throwIllegalArgumentExceptionInvalidAttributeValue(String attribute, String value) {
        Tr.error(tc, "SECURITY_SERVICE_ERROR_BAD_REFERENCE", value, attribute);
        throw new IllegalArgumentException(Tr.formatMessage(tc, "SECURITY_SERVICE_ERROR_BAD_REFERENCE", value, attribute));
    }

    /**
     * When the configuration element is not defined, use some "auto-detect"
     * logic to try and return the single Service of a specified field. If
     * there is no service, or multiple services, that is considered an error
     * case which "auto-detect" can not resolve.
     *
     * @param serviceName name of the service
     * @param map ConcurrentServiceReferenceMap of registered services
     * @return id of the single service registered in map. Will not return null.
     */
    private <V> V autoDetectService(String serviceName, ConcurrentServiceReferenceMap<String, V> map) {
        Iterator<V> services = map.getServices();
        if (services.hasNext() == false) {
            Tr.error(tc, "SECURITY_SERVICE_NO_SERVICE_AVAILABLE", serviceName);
            throw new IllegalStateException(Tr.formatMessage(tc, "SECURITY_SERVICE_NO_SERVICE_AVAILABLE", serviceName));
        }
        V service = services.next();
        if (services.hasNext()) {
            Tr.error(tc, "SECURITY_SERVICE_MULTIPLE_SERVICE_AVAILABLE", serviceName);
            throw new IllegalStateException(Tr.formatMessage(tc, "SECURITY_SERVICE_MULTIPLE_SERVICE_AVAILABLE", serviceName));
        }

        return service;
    }

    /** {@inheritDoc} */
    @Override
    public AuthenticationService getAuthenticationService() {
        AuthenticationService service = authnService.get();
        if (service == null) {
            if (isConfigurationDefinedInFile()) {
                String id = getEffectiveSecurityConfiguration().getAuthenticationServiceId();
                service = getAuthenticationService(id);
            } else {
                service = autoDetectService(KEY_AUTHENTICATION, authentication);
            }
            // remember the authentication service
            authnService.set(service);
        }
        // since the saf delegation code might be invoked from the authentication service,
        // activate the authentication service if it is available.
        if (!authorization.isEmpty()) {
            getAuthorizationService();
        }
        return service;
    }

    /**
     * Retrieve the AuthenticationService for the specified id.
     *
     * @param id AuthenticationService id to retrieve
     * @return A non-null AuthenticationService instance.
     */
    private AuthenticationService getAuthenticationService(String id) {
        AuthenticationService service = authentication.getService(id);
        if (service == null) {
            throwIllegalArgumentExceptionInvalidAttributeValue(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, id);
        }
        return service;
    }

    /** {@inheritDoc} */
    @Override
    public AuthorizationService getAuthorizationService() {
        AuthorizationService service = authzService.get();
        if (service == null) {
            if (isConfigurationDefinedInFile()) {
                String id = getEffectiveSecurityConfiguration().getAuthorizationServiceId();
                service = getAuthorizationService(id);
            } else {
                service = autoDetectAuthorizationService();
            }
            // remember the authorization service
            authzService.set(service);
        }
        return service;
    }

    /**
     * The addition of this logic is undesirable, however it is necessary to
     * keep the configuration "simple", and not have the authorization services
     * know about each other. Because the SAF and built-in authorization
     * can exist in the runtime together, we need to handle this case. The SAF
     * authorization service will not start unless configured, however, the
     * builtin authorization service will always start. Therefore, error cases:
     * 1. If there are no authorization services at all
     * 2. If there are more than two authorization services
     * 3. If there are two authorization services but the builtin isn't one
     *
     * @return ID of the active authorization service
     */
    private AuthorizationService autoDetectAuthorizationService() {
        Iterator<AuthorizationService> services = authorization.getServices();
        if (services.hasNext() == false) {
            Tr.error(tc, "SECURITY_SERVICE_NO_SERVICE_AVAILABLE", KEY_AUTHORIZATION);
            throw new IllegalStateException(Tr.formatMessage(tc, "SECURITY_SERVICE_NO_SERVICE_AVAILABLE", KEY_AUTHORIZATION));
        }

        AuthorizationService authzService = services.next();
        if (services.hasNext()) {
            authzService = null;
            services.next();

            if (services.hasNext()) {
                // more than 2 services
                Tr.error(tc, "SECURITY_SERVICE_MULTIPLE_SERVICE_AVAILABLE", KEY_AUTHORIZATION);
                throw new IllegalStateException(Tr.formatMessage(tc, "SECURITY_SERVICE_MULTIPLE_SERVICE_AVAILABLE", KEY_AUTHORIZATION));

            }

            boolean builtinAuthzFound = false;

            for (String key : authorization.keySet()) {
                String type = (String) authorization.getReference(key).getProperty(AuthorizationService.AUTHORIZATION_TYPE);
                if ("Builtin".equals(type)) {
                    builtinAuthzFound = true;
                } else {
                    authzService = authorization.getService(key);
                }
            }

            if (!builtinAuthzFound) {
                Tr.error(tc, "SECURITY_SERVICE_MULTIPLE_SERVICE_AVAILABLE", KEY_AUTHORIZATION);
                throw new IllegalStateException(Tr.formatMessage(tc, "SECURITY_SERVICE_MULTIPLE_SERVICE_AVAILABLE", KEY_AUTHORIZATION));
            }
        }

        return authzService;
    }

    /**
     * Retrieve the AuthorizationService for the specified id.
     *
     * @param id AuthorizationService id to retrieve
     * @return A non-null AuthorizationService instance.
     */
    private AuthorizationService getAuthorizationService(String id) {
        AuthorizationService service = authorization.getService(id);
        if (service == null) {
            throwIllegalArgumentExceptionInvalidAttributeValue(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, id);
        }
        return service;
    }

    /** {@inheritDoc} */
    @Override
    public UserRegistryService getUserRegistryService() {
        UserRegistryService service = userRegistryService.get();
        if (service == null) {
            if (isConfigurationDefinedInFile()) {
                String id = getEffectiveSecurityConfiguration().getUserRegistryServiceId();
                service = getUserRegistryService(id);
            } else {
                service = autoDetectService(KEY_USERREGISTRY, userRegistry);
            }
            // remember the user registry
            userRegistryService.set(service);
        }
        return service;
    }

    /**
     * Retrieve the UserRegistryService for the specified id.
     *
     * @param id UserRegistryService id to retrieve
     * @return A non-null UserRegistryService instance.
     */
    private UserRegistryService getUserRegistryService(String id) {
        UserRegistryService service = userRegistry.getService(id);
        if (service == null) {
            throwIllegalArgumentExceptionInvalidAttributeValue(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, id);
        }
        return service;
    }

}
