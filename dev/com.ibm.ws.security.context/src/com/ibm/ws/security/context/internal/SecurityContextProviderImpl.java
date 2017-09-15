/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.context.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;

/**
 * This class provides instances of SecurityContextImpl objects, which
 * can be used to establish security context for asynchronous work.
 */

@Component(service = ThreadContextProvider.class,
           name = "com.ibm.ws.security.context.provider",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class SecurityContextProviderImpl implements ThreadContextProvider {

    static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";
    public static final String KEY_SECURITY_SERVICE = "securityService";
    static final String KEY_UNAUTH_SERVICE = "unauthenticatedSubjectService";
    static final String KEY_NAME = "name";

    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>(KEY_CONFIGURATION_ADMIN);
    protected final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    private final AtomicServiceReference<UnauthenticatedSubjectService> unauthenticatedSubjectServiceRef = new AtomicServiceReference<UnauthenticatedSubjectService>(KEY_UNAUTH_SERVICE);

    /**
     * The key for the jaasLoginContextEntry ref used for deserialization
     */
    final static String JAAS_LOGINCONTEXTENTRY_REF = "deserializeLoginContextRef";

    @Activate
    protected void activate(ComponentContext cc) {
        configAdminRef.activate(cc);
        securityServiceRef.activate(cc);
        unauthenticatedSubjectServiceRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        configAdminRef.deactivate(cc);
        securityServiceRef.deactivate(cc);
        unauthenticatedSubjectServiceRef.deactivate(cc);
    }

    @Reference(service = ConfigurationAdmin.class,
               name = KEY_CONFIGURATION_ADMIN,
               policy = ReferencePolicy.DYNAMIC)
    protected void setConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.setReference(ref);
    }

    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.unsetReference(ref);
    }

    @Reference(service = SecurityService.class, name = KEY_SECURITY_SERVICE)
    protected void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    @Reference(service = UnauthenticatedSubjectService.class, name = KEY_UNAUTH_SERVICE)
    protected void setUnauthenticatedSubjectService(ServiceReference<UnauthenticatedSubjectService> ref) {
        unauthenticatedSubjectServiceRef.setReference(ref);
    }

    protected void unsetUnauthenticatedSubjectService(ServiceReference<UnauthenticatedSubjectService> ref) {
        unauthenticatedSubjectServiceRef.unsetReference(ref);
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        String jaasLoginContextEntry = getConfigNameForRef((String) threadContextConfig.get(JAAS_LOGINCONTEXTENTRY_REF));
        return new SecurityContextImpl(true, jaasLoginContextEntry);
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        return new SecurityContextImpl(false, null);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, @Sensitive byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        SecurityContextImpl context = null;
        try {
            context = (SecurityContextImpl) in.readObject();
        } finally {
            in.close();
        }

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            final SecurityContextImpl finalContext = context;
            AccessController.doPrivileged(new PrivilegedAction() {

                @Override
                public Object run() {
                    finalContext.recreateFullSubjects(securityServiceRef.getService(), unauthenticatedSubjectServiceRef);
                    return null;
                }

            });
        } else {
            context.recreateFullSubjects(securityServiceRef.getService(), unauthenticatedSubjectServiceRef);
        }

        return context;
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#getPrerequisites()
     */
    @Override
    public List<ThreadContextProvider> getPrerequisites() {
        return null;
    }

    /**
     * Use the config admin service to get the name for a given service reference
     * 
     * @param ref the service reference string
     * @return the name or null if there were problems
     */
    @FFDCIgnore(PrivilegedActionException.class)
    private String getConfigNameForRef(final String ref) {
        String name = null;
        if (ref != null) {
            final ConfigurationAdmin configAdmin = configAdminRef.getService();
            Configuration config;
            try {
                config = AccessController.doPrivileged(new PrivilegedExceptionAction<Configuration>() {
                    @Override
                    public Configuration run() throws IOException {
                        return configAdmin.getConfiguration(ref);
                    }
                });
            } catch (PrivilegedActionException paex) {
                return null;
            }
            Dictionary<String, Object> props = config.getProperties();
            name = (String) props.get(KEY_NAME);
        }
        return name;
    }
}
