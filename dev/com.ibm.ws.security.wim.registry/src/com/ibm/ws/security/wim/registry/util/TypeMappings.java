/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.registry.util;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.Service;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.wim.RealmConfig;
import com.ibm.ws.security.wim.registry.WIMUserRegistryDefines;

/**
 * Utility class for UserRegistry type to WIM property mappings.
 *
 */
public class TypeMappings implements WIMUserRegistryDefines {

    private static final TraceComponent tc = Tr.register(TypeMappings.class);

    /**
     * Mappings utility class.
     */
    private BridgeUtils mappingUtils = null;

    /**
     * Default constructor.
     */
    public TypeMappings(BridgeUtils mappingUtil) {
        this.mappingUtils = mappingUtil;
    }

    /**
     * Get the input unique user ID mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The input unique user ID property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getInputUniqueUserId(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getInputMapping(inputVirtualRealm, Service.CONFIG_DO_UNIQUE_USER_ID_MAPPING,
                                             UNIQUE_USER_ID_DEFAULT);
        return returnValue;
    }

    /**
     * Get the output unique user ID mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The output unique user ID property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getOutputUniqueUserId(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getOutputMapping(inputVirtualRealm, Service.CONFIG_DO_UNIQUE_USER_ID_MAPPING,
                                              UNIQUE_USER_ID_DEFAULT);
        return returnValue;
    }

    /**
     * Get the input user security name mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The input user security name property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getInputUserSecurityName(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getInputMapping(inputVirtualRealm, Service.CONFIG_DO_USER_SECURITY_NAME_MAPPING,
                                             INPUT_USER_SECURITY_NAME_DEFAULT);
        return returnValue;
    }

    /**
     * Get the output user security name mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The output user security name property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getOutputUserSecurityName(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getOutputMapping(inputVirtualRealm, Service.CONFIG_DO_USER_SECURITY_NAME_MAPPING,
                                              OUTPUT_USER_SECURITY_NAME_DEFAULT);
        return returnValue;
    }

    /**
     * Get the output user security name mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The output user security name property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getOutputUserPrincipal(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getOutputMapping(inputVirtualRealm, Service.CONFIG_DO_USER_SECURITY_NAME_MAPPING,
                                              USER_SECURITY_NAME_DEFAULT);
        return returnValue;
    }

    /**
     * Get the input user display name mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The input user display name property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getInputUserDisplayName(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getInputMapping(inputVirtualRealm, Service.CONFIG_DO_USER_DISPLAY_NAME_MAPPING,
                                             USER_DISPLAY_NAME_DEFAULT);
        return returnValue;
    }

    /**
     * Get the output user display name mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The output user display name property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getOutputUserDisplayName(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getOutputMapping(inputVirtualRealm, Service.CONFIG_DO_USER_DISPLAY_NAME_MAPPING,
                                              USER_DISPLAY_NAME_DEFAULT);
        return returnValue;
    }

    /**
     * Get the input unique group ID mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The input unique group ID property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getInputUniqueGroupId(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getInputMapping(inputVirtualRealm, Service.CONFIG_DO_UNIQUE_GROUP_ID_MAPPING,
                                             INPUT_UNIQUE_GROUP_ID_DEFAULT);
        return returnValue;
    }

    /**
     * Get the output unique group ID mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The output unique group ID property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getOutputUniqueGroupId(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getOutputMapping(inputVirtualRealm, Service.CONFIG_DO_UNIQUE_GROUP_ID_MAPPING,
                                              OUTPUT_UNIQUE_GROUP_ID_DEFAULT);
        return returnValue;
    }

    /**
     * Get the input group security name mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The input group security name property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getInputGroupSecurityName(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getInputMapping(inputVirtualRealm, Service.CONFIG_DO_GROUP_SECURITY_NAME_MAPPING,
                                             INPUT_GROUP_SECURITY_NAME_DEFAULT);
        return returnValue;
    }

    /**
     * Get the output group security name mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The output group security name property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getOutputGroupSecurityName(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getOutputMapping(inputVirtualRealm, Service.CONFIG_DO_GROUP_SECURITY_NAME_MAPPING,
                                              OUTPUT_GROUP_SECURITY_NAME_DEFAULT);
        return returnValue;
    }

    /**
     * Get the input group display name mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The input group display name property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getInputGroupDisplayName(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getInputMapping(inputVirtualRealm, Service.CONFIG_DO_GROUP_DISPLAY_NAME_MAPPING,
                                             GROUP_DISPLAY_NAME_DEFAULT);
        return returnValue;
    }

    /**
     * Get the output group display name mapping for the UserRegistry.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     *
     * @return The output group display name property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != ""
     * @post $return != ""
     * @post $return != null
     */
    public String getOutputGroupDisplayName(String inputVirtualRealm) {
        // initialize the return value
        String returnValue = getOutputMapping(inputVirtualRealm, Service.CONFIG_DO_GROUP_DISPLAY_NAME_MAPPING,
                                              GROUP_DISPLAY_NAME_DEFAULT);
        return returnValue;
    }

    /**
     * Get the WIM input property that maps to the UserRegistry input property.
     *
     * @param inputVirtualRealm Virtual realm to find the mappings.
     * @param inputProperty String representing the input UserRegistry property.
     * @return String representing the input WIM property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != empty
     * @pre inputProperty != null
     * @pre inputProperty != ""
     * @pre inputDefaultProperty != null
     * @pre inputDefaultProperty != ""
     * @post $return != ""
     * @post $return != null
     */
    @FFDCIgnore(Exception.class)
    private String getInputMapping(String inputVirtualRealm, String inputProperty, String inputDefaultProperty) {
        String methodName = "getInputMapping";
        // initialize the return value
        String returnValue = null;

        RealmConfig realmConfig = mappingUtils.getCoreConfiguration().getRealmConfig(inputVirtualRealm);
        if (realmConfig != null) {
            try {
                returnValue = realmConfig.getURMapInputPropertyInRealm(inputProperty);
                if ((returnValue == null) || (returnValue.equals(""))) {
                    returnValue = inputDefaultProperty;
                }
            } catch (Exception toCatch) {
                returnValue = inputDefaultProperty;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " " + toCatch.getMessage(), toCatch);
                }
            }
        } else {
            returnValue = inputDefaultProperty;
        }
        return returnValue;
    }

    /**
     * Get the WIM output property that maps to the UserRegistry output property.
     *
     * @param inputProperty String representing the output UserRegistry property.
     * @return String representing the output WIM property.
     *
     * @pre inputVirtualRealm != null
     * @pre inputVirtualRealm != empty
     * @pre inputProperty != null
     * @pre inputProperty != ""
     * @pre inputDefaultProperty != null
     * @pre inputDefaultProperty != ""
     * @post $return != ""
     * @post $return != null
     */
    @FFDCIgnore(Exception.class)
    private String getOutputMapping(String inputVirtualRealm, String inputProperty, String inputDefaultProperty) {
        String methodName = "getOutputMapping";
        // initialize the return value
        String returnValue = null;
        RealmConfig realmConfig = mappingUtils.getCoreConfiguration().getRealmConfig(inputVirtualRealm);
        if (realmConfig != null) {
            try {
                returnValue = realmConfig.getURMapOutputPropertyInRealm(inputProperty);
                if ((returnValue == null) || (returnValue.equals(""))) {
                    returnValue = inputDefaultProperty;
                }
            } catch (Exception toCatch) {
                returnValue = inputDefaultProperty;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " " + toCatch.getMessage(), toCatch);
                }
            }
        } else {
            returnValue = inputDefaultProperty;
        }
        return returnValue;
    }
}
