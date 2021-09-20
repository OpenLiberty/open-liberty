/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.registry;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.bnd.metatype.annotation.Ext;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.CustomRegistryException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.FederationRegistry;
import com.ibm.ws.security.registry.NotImplementedException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.wim.ConfigManager;
import com.ibm.ws.security.wim.VMMService;
import com.ibm.ws.security.wim.registry.util.BridgeUtils;
import com.ibm.ws.security.wim.registry.util.DisplayNameBridge;
import com.ibm.ws.security.wim.registry.util.LoginBridge;
import com.ibm.ws.security.wim.registry.util.MembershipBridge;
import com.ibm.ws.security.wim.registry.util.SearchBridge;
import com.ibm.ws.security.wim.registry.util.SecurityNameBridge;
import com.ibm.ws.security.wim.registry.util.UniqueIdBridge;
import com.ibm.ws.security.wim.registry.util.ValidBridge;
import com.ibm.wsspi.security.wim.exception.NoUserRepositoriesFoundException;

@ObjectClassDefinition(pid = "com.ibm.ws.security.wim.registry.WIMUserRegistry", name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, localization = Ext.LOCALIZATION)
@Ext.ObjectClassClass(FederationRegistry.class)
interface WIMUserRegistryConfig {
}

/*
 *
 * This component shares configuration with the ConfigManager, which is in another bundle.
 * I'd think the registry adapter should go in core and be one component.
 */
//TODO policy REQUIRE when we count this....
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM", "com.ibm.ws.security.registry.type=WIM" })
public class WIMUserRegistry implements FederationRegistry, UserRegistry {

    private static final TraceComponent tc = Tr.register(WIMUserRegistry.class);

    @Reference
    ConfigManager configManager;

    @Reference
    VMMService vmmService;

    /**
     * WIM Delimiter to seperate token
     */
    private final static String TOKEN_DELIMETER = "::";

    /**
     * Mapping utility class.
     */
    private BridgeUtils mappingUtils;

    /**
     * Bridge classes for the WIM APIs.
     */
    private LoginBridge loginBridge;

    private DisplayNameBridge displayBridge;

    private SecurityNameBridge securityBridge;

    private UniqueIdBridge uniqueBridge;

    private ValidBridge validBridge;

    private SearchBridge searchBridge;

    private MembershipBridge membershipBridge;

    private final Random failResponseRandom = new Random();

    @Activate
    protected void activate() {

        Map<String, Object> props = configManager.getConfigurationProperties();
        initializeUtils(props);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getType()
     */
    @Override
    public String getType() {
        return "WIM";
    }

    private void initializeUtils(Map<String, Object> configProps) {
        mappingUtils = new BridgeUtils(vmmService, configManager);
        loginBridge = new LoginBridge(mappingUtils);
        displayBridge = new DisplayNameBridge(mappingUtils);
        securityBridge = new SecurityNameBridge(mappingUtils);
        uniqueBridge = new UniqueIdBridge(mappingUtils);
        validBridge = new ValidBridge(mappingUtils);
        searchBridge = new SearchBridge(mappingUtils);
        membershipBridge = new MembershipBridge(mappingUtils);

        mappingUtils.initialize(configProps);
    }

    @Deactivate
    protected void deinitializeUtils() {
        mappingUtils = null;
        loginBridge = null;
        displayBridge = null;
        securityBridge = null;
        uniqueBridge = null;
        validBridge = null;
        searchBridge = null;
        membershipBridge = null;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public String checkPassword(final String inputUser, @Sensitive final String inputPassword) throws RegistryException {
        if (loginBridge == null) {
            return null;
        }

        String methodName = "checkPassword";
        String returnValue = null;
        try {
            returnValue = loginBridge.checkPassword(inputUser, inputPassword);
            return returnValue;
        } catch (Exception excp) {
            if (excp instanceof RegistryException) {
                Throwable t = excp.getCause();
                if (t != null && t instanceof NoUserRepositoriesFoundException) {
                    /**
                     * Throw the original exception back to the user. Otherwise, they receive a misleading
                     * message that their user was not found instead of the root of the problem (no registries
                     * to search).
                     */
                    throw excp;
                }
                // New:: Change in Input/Output mapping
                // throw (RegistryException) excp;
                return null;
            } else
                throw new RegistryException(excp.getMessage(), excp);
        } finally {
            if (returnValue == null) {
                try {
                    /*
                     * Pad return time on a failed user, if enabled.
                     */
                    int failResponseDelayMax = this.mappingUtils.getCoreConfiguration().getFailResponseDelayMax();
                    int failResponseDelayMin = this.mappingUtils.getCoreConfiguration().getFailResponseDelayMin();

                    if (failResponseDelayMax > 0) {
                        int random = failResponseRandom.nextInt(failResponseDelayMax + 1 - failResponseDelayMin) + failResponseDelayMin;
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc,
                                     methodName + " " + "failed response login delay is " + random + " ms. The minimum and maximum delay for failed logons are "
                                         + failResponseDelayMin
                                         + " ms and " + failResponseDelayMax + " ms.");
                        }
                        Thread.sleep(random);
                    }
                } catch (InterruptedException ie) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc,
                                 methodName + " " + "failed response login delay sleep was interrupted.");
                    }
                } catch (Exception e) {
                    if (tc.isEventEnabled()) {
                        Tr.event(tc,
                                 methodName + " " + "failed response login delay processing hit an exception. Ignore so we return the failed login.", e);
                    }
                }
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public String mapCertificate(X509Certificate[] chain) throws CertificateMapNotSupportedException, CertificateMapFailedException, RegistryException {
        try {
            String returnValue = loginBridge.mapCertificate(chain);
            return returnValue;
        } catch (Exception excp) {
            if (excp instanceof CertificateMapFailedException) {
                throw (CertificateMapFailedException) excp;
            } else if (excp instanceof CertificateMapNotSupportedException) {
                throw (CertificateMapNotSupportedException) excp;
            } else if (excp instanceof RegistryException) {
                throw (RegistryException) excp;
            } else {
                throw new RegistryException(excp.getMessage(), excp);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getRealm() {
        return vmmService.getRealmName();
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public SearchResult getUsers(final String inputPattern, final int inputLimit) throws RegistryException {
        try {

            SearchResult returnValue = searchBridge.getUsers(inputPattern, inputLimit);
            return returnValue;
        } catch (Exception excp) {
            if (excp instanceof RegistryException)
                throw (RegistryException) excp;
            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public String getUserDisplayName(final String inputUserSecurityName) throws EntryNotFoundException, RegistryException {
        try {
            // bridge the APIs
            String returnValue = displayBridge.getUserDisplayName(inputUserSecurityName);
            return returnValue;
        } catch (Exception excp) {
            if (excp instanceof EntryNotFoundException)
                throw (EntryNotFoundException) excp;
            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    /**
     * In case of SAF registry securityName returned will be of format <userId>::<token>.
     *
     * @return the userId
     */
    protected String parseUserId(String securityName) {
        int idx = securityName.indexOf(TOKEN_DELIMETER); // Don't use String.split() - way too expensive.
        if (idx > 0) {
            return securityName.substring(0, idx);
        } else {
            return securityName;
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public String getUniqueUserId(final String inputUserSecurityName) throws EntryNotFoundException, RegistryException {
        HashMap<String, String> result = null;

        try {

            result = uniqueBridge.getUniqueUserId(parseUserId(inputUserSecurityName));

            return result.get("RESULT");

        } catch (Exception excp) {
            if (excp instanceof EntryNotFoundException)
                throw (EntryNotFoundException) excp;
            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public String getUserSecurityName(String inputUniqueUserId) throws EntryNotFoundException, RegistryException {
        HashMap<String, String> result = null;

        // New:: Change to Input/Output property
        try {
            result = uniqueBridge.getUniqueUserId(parseUserId(inputUniqueUserId));

            String returnValue = result.get("RESULT");
            if (!inputUniqueUserId.equalsIgnoreCase(returnValue)) {
                inputUniqueUserId = returnValue;
            }

        } catch (Exception excp) {
            /* Ignore and assume that inputUniqueUserId really is the uniqueUserId. */
        }

        try {
            // bridge the APIs
            String id = inputUniqueUserId;
            if (id.startsWith("user:") || id.startsWith("group:")) {
                // New method added as an alternative of getUserFromUniqueId method of WSSecurityPropagationHelper
                id = getUserFromUniqueID(id);
            }
            return securityBridge.getUserSecurityName(id);
        } catch (Exception excp) {
            if (excp instanceof EntryNotFoundException)
                throw (EntryNotFoundException) excp;
            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public boolean isValidUser(final String inputUserSecurityName) throws RegistryException {
        try {
            boolean returnValue = validBridge.isValidUser(inputUserSecurityName);
            return Boolean.valueOf(returnValue);
        } catch (Exception excp) {
            if (excp instanceof RegistryException)
                throw (RegistryException) excp;

            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public SearchResult getGroups(final String inputPattern, final int inputLimit) throws RegistryException {
        try {
            SearchResult returnValue = searchBridge.getGroups(inputPattern, inputLimit);
            return returnValue;
        } catch (Exception excp) {
            if (excp instanceof RegistryException)
                throw (RegistryException) excp;

            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public String getGroupDisplayName(final String inputGroupSecurityName) throws EntryNotFoundException, RegistryException {
        try {
            String returnValue = displayBridge.getGroupDisplayName(inputGroupSecurityName);
            return returnValue;
        } catch (Exception excp) {
            if (excp instanceof EntryNotFoundException)
                throw (EntryNotFoundException) excp;

            else if (excp instanceof RegistryException)
                throw (RegistryException) excp;

            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public String getUniqueGroupId(final String inputGroupSecurityName) throws EntryNotFoundException, RegistryException {

        try {
            String returnValue = uniqueBridge.getUniqueGroupId(inputGroupSecurityName);
            return returnValue;
        } catch (Exception excp) {
            if (excp instanceof EntryNotFoundException)
                throw (EntryNotFoundException) excp;

            else if (excp instanceof RegistryException)
                throw (RegistryException) excp;
            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public List<String> getUniqueGroupIdsForUser(final String inputUniqueUserId) throws EntryNotFoundException, RegistryException {

        try {
            // bridge the APIs
            String id = inputUniqueUserId;
            if (id.startsWith("user:") || id.startsWith("group:"))
                //New method added as an alternative of getUserFromUniqueId method of WSSecurityPropagationHelper
                id = getUserFromUniqueID(id);

            List<String> returnValue = membershipBridge.getUniqueGroupIds(id);
            return returnValue;
        } catch (Exception excp) {
            if (excp instanceof EntryNotFoundException)
                throw (EntryNotFoundException) excp;

            else if (excp instanceof RegistryException)
                throw (RegistryException) excp;
            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public String getGroupSecurityName(final String inputUniqueGroupId) throws EntryNotFoundException, RegistryException {
        try {
            // bridge the APIs
            String returnValue = securityBridge.getGroupSecurityName(inputUniqueGroupId);
            return returnValue;
        } catch (Exception excp) {
            if (excp instanceof EntryNotFoundException)
                throw (EntryNotFoundException) excp;

            else if (excp instanceof RegistryException)
                throw (RegistryException) excp;
            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public boolean isValidGroup(final String inputGroupSecurityName) throws RegistryException {
        try {
            // bridge the APIs
            boolean returnValue = validBridge.isValidGroup(inputGroupSecurityName);
            return Boolean.valueOf(returnValue);
        } catch (Exception excp) {
            if (excp instanceof RegistryException)
                throw (RegistryException) excp;

            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public List<String> getGroupsForUser(final String inputUserSecurityName) throws EntryNotFoundException, RegistryException {
        try {
            // bridge the APIs
            List<String> returnValue = membershipBridge.getGroupsForUser(inputUserSecurityName);
            return returnValue;
        } catch (Exception excp) {
            if (excp instanceof EntryNotFoundException)
                throw (EntryNotFoundException) excp;

            else if (excp instanceof RegistryException)
                throw (RegistryException) excp;

            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUsersForGroup(java.lang.String, int)
     */
    @Override
    @FFDCIgnore(Exception.class)
    public SearchResult getUsersForGroup(String groupSecurityName,
                                         int limit) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException, RegistryException {
        try {
            // bridge the APIs
            SearchResult returnValue = membershipBridge.getUsersForGroup(groupSecurityName, limit);
            return returnValue;
        } catch (Exception excp) {
            if (excp instanceof EntryNotFoundException)
                throw (EntryNotFoundException) excp;

            else if (excp instanceof RegistryException)
                throw (RegistryException) excp;

            else
                throw new RegistryException(excp.getMessage(), excp);
        }
    }

    //New method added as an alternative of getUserFromUniqueId method of WSSecurityPropagationHelper
    private String getUserFromUniqueID(String id) {
        if (id == null) {
            return "";
        }
        id = id.trim();
        int realmDelimiterIndex = id.indexOf("/");
        if (realmDelimiterIndex < 0) {
            return "";
        } else {
            return id.substring(realmDelimiterIndex + 1);
        }
    }

    @Override
    public void addFederationRegistries(List<UserRegistry> registries) {
        mappingUtils.addFederationRegistries(registries);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.FederationRegistry#removeAllFederatedRegistries()
     */
    @Override
    public void removeAllFederatedRegistries() {
        mappingUtils.removeAllFederatedRegistries();
    }
}
