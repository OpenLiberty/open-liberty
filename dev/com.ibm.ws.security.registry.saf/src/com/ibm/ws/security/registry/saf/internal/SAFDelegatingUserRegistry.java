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
package com.ibm.ws.security.registry.saf.internal;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.bnd.metatype.annotation.Ext;
import com.ibm.ws.security.credentials.saf.SAFCredentialsService;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.CustomRegistryException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.NotImplementedException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * Wrapper class for loading SAF UserRegistry implementations.
 *
 * The wrapper loads either an "unauthorized" version of SAFRegistry or an
 * "authorized" version, depending on whether the server has access to
 * authorized native code. The unauthorized version uses LE services to
 * access the SAF registry. The authorized version uses authorized services
 * (e.g. initACEE) to access the SAF registry.
 */
//<OCD id="com.ibm.ws.security.registry.saf.config" name="%saf.config" description="%saf.config.desc" ibm:alias="safRegistry">
//<AD id="realm" name="%realm" description="%realm.desc" required="false" type="String" />
//<AD id="enableFailover" name="%enableFailover.name" description="%enableFailover.desc" required="false" type="Boolean" default="true"/>
//</OCD>
//
//<Designate factoryPid="com.ibm.ws.security.registry.saf.config">
//<Object ocdref="com.ibm.ws.security.registry.saf.config" />
//</Designate>

@ObjectClassDefinition(pid = "com.ibm.ws.security.registry.saf.config",
                       name = "%saf.config",
                       description = "%saf.config.desc",
                       localization = Ext.LOCALIZATION)
@Ext.Alias("safRegistry")
@Ext.ObjectClassClass(UserRegistry.class)
@Ext.RequireExplicitConfiguration
@interface SAFRegistryConfig {

    @AttributeDefinition(name = "%realm", description = "%realm.desc", required = false)
    String realm() default "";

    @AttributeDefinition(name = "%enableFailover.name", description = "%enableFailover.desc", required = false, defaultValue = "true")
    boolean enableFailover() default true;

    @AttributeDefinition(name = "%reportPasswordExpired.name", description = "%reportPasswordExpired.desc", required = false, defaultValue = "false")
    boolean reportPasswordExpired() default false;

    @AttributeDefinition(name = "%reportPasswordChangeDetails.name", description = "%reportPasswordChangeDetails.desc", required = false, defaultValue = "false")
    boolean reportPasswordChangeDetails() default false;

    @AttributeDefinition(name = "%reportUserRevoked.name", description = "%reportUserRevoked.desc", required = false, defaultValue = "false")
    boolean reportUserRevoked() default false;

    @AttributeDefinition(name = "%includeSafGroups.name", description = "%includeSafGroups.desc", required = false, defaultValue = "false")
    boolean includeSafGroups() default false;

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, defaultValue = "*")
    @Ext.ReferencePid("com.ibm.ws.security.credentials.saf")
    @Ext.Final
    String[] safCredentialService();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, defaultValue = "${count(safCredentialService)}")
    String safCredentialService_cardinality_minimum();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, defaultValue = "com.ibm.ws.security.registry.saf.config")
    String config_id();
}

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           configurationPid = "com.ibm.ws.security.registry.saf.config",
           immediate = true,
           property = { "service.vendor=IBM", "com.ibm.ws.security.registry.type=SAF" })
public class SAFDelegatingUserRegistry implements UserRegistry {

    /**
     * Reference to NativeMethodManager for loading JNI methods.
     */
    @Reference
    NativeMethodManager nativeMethodManager;

    /**
     * Reference to SAFCredentialsService. The SAFCredentialsService is loaded
     * only if the server has access to authorized SAF services (i.e. the Angel
     * is up and the server is authorized to use the authorized routines).
     * Thus, if this reference is set, then the factory will load the authorized
     * version of SAFRegistry.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.STATIC,
               policyOption = ReferencePolicyOption.GREEDY)
    SAFCredentialsService safCredentialsService;

    private static final TraceComponent tc = Tr.register(SAFRegistry.class);

    private boolean validURConfig;
    UserRegistry delegate;

    @Activate
    protected void activate(SAFRegistryConfig config) {
        validURConfig = true;
        if (safCredentialsService != null) {
            delegate = new SAFAuthorizedRegistry(config, nativeMethodManager, safCredentialsService);
            Tr.info(tc, "SAF_AUTH_SERVICES_ENABLED");
        } else {
            delegate = new SAFRegistry(config, nativeMethodManager);
            if (delegate instanceof SAFRegistry && !((SAFRegistry) delegate).isFailoverEnabled()) {
                validURConfig = false;
                Tr.info(tc, "SAF_AUTH_SERVICES_DISABLED_NO_FAILOVER");
            } else {
                Tr.info(tc, "SAF_AUTH_SERVICES_DISABLED_AND_FAILOVER");
            }

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getType()
     */
    @Override
    public String getType() {
        return delegate.getType();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getRealm()
     */
    @Override
    public String getRealm() {
        return delegate.getRealm();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#checkPassword(java.lang.String, java.lang.String)
     */
    @Override
    public String checkPassword(String userSecurityName, @Sensitive String password) throws RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.checkPassword(userSecurityName, password);
    }

    @Override
    public String mapCertificate(X509Certificate[] certs) throws CertificateMapNotSupportedException, CertificateMapFailedException, RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.mapCertificate(certs);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#isValidUser(java.lang.String)
     */
    @Override
    public boolean isValidUser(String userSecurityName) throws RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.isValidUser(userSecurityName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUsers(java.lang.String, int)
     */
    @Override
    public SearchResult getUsers(String pattern, int limit) throws RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.getUsers(pattern, limit);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUserDisplayName(java.lang.String)
     */
    @Override
    public String getUserDisplayName(String userSecurityName) throws EntryNotFoundException, RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.getUserDisplayName(userSecurityName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUniqueUserId(java.lang.String)
     */
    @Override
    public String getUniqueUserId(String userSecurityName) throws EntryNotFoundException, RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.getUniqueUserId(userSecurityName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUserSecurityName(java.lang.String)
     */
    @Override
    public String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.getUserSecurityName(uniqueUserId);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUsersForGroup(java.lang.String, int)
     */
    @Override
    public SearchResult getUsersForGroup(String groupSecurityName,
                                         int limit) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException, RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.getUsersForGroup(groupSecurityName, limit);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#isValidGroup(java.lang.String)
     */
    @Override
    public boolean isValidGroup(String groupSecurityName) throws RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.isValidGroup(groupSecurityName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getGroups(java.lang.String, int)
     */
    @Override
    public SearchResult getGroups(String pattern, int limit) throws RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.getGroups(pattern, limit);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getGroupDisplayName(java.lang.String)
     */
    @Override
    public String getGroupDisplayName(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.getGroupDisplayName(groupSecurityName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUniqueGroupId(java.lang.String)
     */
    @Override
    public String getUniqueGroupId(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.getUniqueGroupId(groupSecurityName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getGroupSecurityName(java.lang.String)
     */
    @Override
    public String getGroupSecurityName(String uniqueGroupId) throws EntryNotFoundException, RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.getGroupSecurityName(uniqueGroupId);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUniqueGroupIdsForUser(java.lang.String)
     */
    @Override
    public List<String> getUniqueGroupIdsForUser(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.getUniqueGroupIdsForUser(uniqueUserId);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getGroupsForUser(java.lang.String)
     */
    @Override
    public List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException, RegistryException {
        if (!validURConfig) {
            throw new RegistryException("The server is not permitted to use the authorized SAF registry and is configured to not allow the use unauthorized SAF registry services through the enableFailover attribute under the safRegistry configuration element. This operation is not permitted.");
        }
        return delegate.getGroupsForUser(userSecurityName);
    }

    public boolean getReportPasswordChangeDetailsConfig() {
        return ((SAFRegistry) delegate).isReportPasswordChangeDetailsEnabled();
    }

    public String getProfilePrefix() {
        String prefix = null;
        if (safCredentialsService != null)
            prefix = safCredentialsService.getProfilePrefix();
        return prefix;
    }

}
