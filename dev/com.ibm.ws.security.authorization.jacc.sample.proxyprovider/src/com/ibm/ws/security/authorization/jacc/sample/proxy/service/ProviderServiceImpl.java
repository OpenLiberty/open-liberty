/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.authorization.jacc.sample.proxy.service;

import java.security.AccessController;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.security.jacc.PolicyConfigurationFactory;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.security.authorization.jacc.ProviderService;

/**
 * The main purpose of this sample is to demonstrate an implementation of the {@link ProviderService} available in Liberty.
 * As such simplicity and not the performance was a major factor. This sample should be used only to get familiarized with this feature. An
 * actual implementation of a realistic provider {@link ProviderService} should consider various factors like performance, scalability,
 * thread safety, and so on.
 *
 * Note: The properties are not dynamically configurable. This is because {@link PolicyConfigurationFactory} caches away the class once it is
 * loaded and changing the properties at runtime will no longer have an effect. The server will need to be restarted.
 *
 * <p/>
 * This implementation uses Liberty's global shared library to load the JACC provider classes specified by so ensure that the classes specified
 * in the feature's configuration are located in one of the following locations:
 *
 * <ul>
 * <li>${shared.config.dir}/lib/global/</li>
 * <li>${server.config.dir}/lib/global/</li>
 * </ul>
 *
 * <p/>
 * Note: This sample is written to support both Java EE 8- and Jakarta EE 9+. The Liberty build will transform the class file to create a new
 * class file that supports Jakarta EE 9+ namespaces (jakarta.*). In your own implementation, you may need to only support one or the other.
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           name = "com.ibm.ws.security.authorization.jacc.sample.proxyprovider",
           service = ProviderService.class,
           immediate = true)
public class ProviderServiceImpl implements ProviderService {
    private static final TraceComponent tc = Tr.register(ProviderServiceImpl.class);

    // Java / Jakarta EE 8- properties
    private static final String JACC_FACTORY = "javax.security.jacc.PolicyConfigurationFactory.provider";
    private static final String JACC_POLICY_PROVIDER = "javax.security.jacc.policy.provider";

    // Jakarta EE 9+ properties
    private static final String JACC_FACTORY_JAKARTA = "jakarta.security.jacc.PolicyConfigurationFactory.provider";
    private static final String JACC_POLICY_PROVIDER_JAKARTA = "jakarta.security.jacc.policy.provider";

    /** Configuration key to retrieve the policyProviderClass value from the server.xml configuration. */
    private static final String POLICY_PROVIDER = "policyProviderClass";

    /** Configuration key to retrieve the policyConfigurationFactoryClass value from the server.xml configuration. */
    private static final String POLICY_CONFIGURATION_FACTORY = "policyConfigurationFactoryClass";

    private String policyProviderClass = null;
    private String policyConfigurationFactoryClass = null;

    private volatile Library sharedLibrary = null;

    /**
     * We depend on the shared library.
     *
     * @param svc The shared library references.
     */
    @Reference(service = Library.class,
               name = "sharedLibrary",
               target = "(id=global)",
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setSharedLibrary(Library svc) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "setSharedLibrary", svc);
        }

        sharedLibrary = svc;

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "setSharedLibrary");
        }
    }

    /**
     * Unset the shared library reference.
     *
     * @param ref The library reference that is being unset.
     */
    protected void unsetSharedLibrary(Library ref) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "unsetSharedLibrary", ref);
        }

        sharedLibrary = null;

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "unsetSharedLibrary");
        }
    }

    /**
     * Activate this component service.
     *
     * @param props The configuration for this component service.
     */
    @Activate
    protected synchronized void activate(Map<String, Object> props) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "activate", props);
        }

        initialize(props);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "activate");
        }
    }

    /**
     * Modify this component service.
     *
     * @param props The configuration for this component service.
     */
    @Modified
    protected synchronized void modify(Map<String, Object> props) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "modify", props);
        }

        initialize(props);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "modify");
        }
    }

    /**
     * Deactivate this component service.
     *
     * @param cc The component context.
     */
    @Deactivate
    protected void deactivate(ComponentContext cc) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "deactivate", cc);
        }

        // Nothing for us to do.

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "deactivate");
        }
    }

    /**
     * Initialize the JACC policy provider and policy configuration factory classes from the passed in configuration.
     *
     * @param props The configuration passed in from declarative services. This originates from the server.xml.
     */
    private void initialize(Map<String, Object> props) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "initialize", props);
        }

        /*
         * Retrieve the policy provider and policy configuration factory classes from the passed
         * in configuration from the server.xml
         */
        if (props != null) {
            policyProviderClass = (String) props.get(POLICY_PROVIDER);
            policyConfigurationFactoryClass = (String) props.get(POLICY_CONFIGURATION_FACTORY);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "config properties : policyName : " + policyProviderClass + " factoryName : " + policyConfigurationFactoryClass);
            }
        }

        /*
         * Set the JACC provider properties. This will only allow setting of each property one time.
         * This is because PolicyConfigurationFactory.getPolicyConfigurationFactory() looks at the property
         * one time and caches the class away.
         */
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public java.lang.Object run() {
                /*
                 * Get the JACC policy provider class.
                 */
                String systemPolicyProviderClass = System.getProperty(JACC_POLICY_PROVIDER) == null ? System.getProperty(JACC_POLICY_PROVIDER_JAKARTA) : System
                                .getProperty(JACC_POLICY_PROVIDER);
                if (policyProviderClass == null) {
                    policyProviderClass = systemPolicyProviderClass;
                } else if (systemPolicyProviderClass == null) {
                    /*
                     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                     * NOTE: This sample implementation supports both EE8 (javax.* namespace) and EE9 (jakarta.* namespace), but
                     * in a real-world application, it is only necessary to support the version(s) the implementation
                     * will run in.
                     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                     */
                    System.setProperty(JACC_POLICY_PROVIDER, policyProviderClass); // EE8- compatibility.
                    System.setProperty(JACC_POLICY_PROVIDER_JAKARTA, policyProviderClass); // EE9+ compatibility.
                }

                /*
                 * Get the JACC policy configuration factory class.
                 */
                String systemPolicyConfigurationFactoryClass = System.getProperty(JACC_FACTORY) == null ? System.getProperty(JACC_FACTORY_JAKARTA) : System
                                .getProperty(JACC_FACTORY);
                if (policyConfigurationFactoryClass == null) {
                    policyConfigurationFactoryClass = systemPolicyConfigurationFactoryClass;
                } else if (systemPolicyConfigurationFactoryClass == null) {
                    /*
                     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                     * NOTE: This sample implementation supports both EE8 (javax.* namespace) and EE9 (jakarta.* namespace), but
                     * in a real-world application, it is only necessary to support the version(s) the implementation
                     * will run in.
                     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                     */
                    System.setProperty(JACC_FACTORY, policyConfigurationFactoryClass); // EE8- compatibility.
                    System.setProperty(JACC_FACTORY_JAKARTA, policyConfigurationFactoryClass); // EE9+ compatibility.
                }

                /*
                 * Trace out the currently set system properties.
                 */
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc,
                             "JACC system properties are now: " + JACC_POLICY_PROVIDER + "=" + System.getProperty(JACC_POLICY_PROVIDER) + ", " + JACC_POLICY_PROVIDER_JAKARTA + "="
                                 + System.getProperty(JACC_POLICY_PROVIDER_JAKARTA) + ", " + JACC_FACTORY + "=" + System.getProperty(JACC_FACTORY) + ", " + JACC_FACTORY_JAKARTA
                                 + "=" + System.getProperty(JACC_FACTORY_JAKARTA));
                }
                return null;
            }
        });

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "initialize");
        }
    }

    @Override
    public Policy getPolicy() {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getPolicy");
        }

        // load the code via the global shared library.
        ClassLoader sharedLibClassLoader = sharedLibrary.getClassLoader();
        Policy policy = null;
        try {
            Class<?> myClass = sharedLibClassLoader.loadClass(policyProviderClass);
            policy = (Policy) myClass.newInstance();
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failuire to load Policy Object : " + e);
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getPolicy", policy);
        }
        return policy;
    }

    @Override
    public PolicyConfigurationFactory getPolicyConfigFactory() {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getPolicyConfigFactory");
        }

        PolicyConfigurationFactory output = AccessController.doPrivileged(new PrivilegedAction<PolicyConfigurationFactory>() {
            @Override
            public PolicyConfigurationFactory run() {
                ClassLoader cl = null;
                PolicyConfigurationFactory pcf = null;
                try {
                    cl = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(sharedLibrary.getClassLoader());
                    pcf = PolicyConfigurationFactory.getPolicyConfigurationFactory();
                } catch (Exception e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Failed to instantiate PolicyConfigurationFactory class");
                    }
                    return null;
                } finally {
                    Thread.currentThread().setContextClassLoader(cl);
                }
                return pcf;
            }
        });

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getPolicyConfigFactory", output);
        }
        return output;
    }
}
