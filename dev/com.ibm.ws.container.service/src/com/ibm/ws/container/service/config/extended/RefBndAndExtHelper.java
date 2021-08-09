/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.container.service.config.extended;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.dd.commonbnd.AuthenticationAlias;
import com.ibm.ws.javaee.dd.commonbnd.CustomLoginConfiguration;
import com.ibm.ws.javaee.dd.commonbnd.DataSource;
import com.ibm.ws.javaee.dd.commonbnd.EJBRef;
import com.ibm.ws.javaee.dd.commonbnd.EnvEntry;
import com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef;
import com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.ws.resource.ResourceRefInfo;

/**
 * EJB bindings configuration helper code that is common for
 * web apps and EJB modules.
 * 
 */
public class RefBndAndExtHelper
{
    public static void configureEJBRefBindings(com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup refBindingsGroup,
                                               Map<String, String> ejbRefBndMap) {
        if (refBindingsGroup != null) {
            List<EJBRef> ejbEjbRefList = refBindingsGroup.getEJBRefs();
            if (!ejbEjbRefList.isEmpty()) {
                for (com.ibm.ws.javaee.dd.commonbnd.EJBRef ejbRefBnd : ejbEjbRefList) {
                    String name = ejbRefBnd.getName();
                    String bindingName = ejbRefBnd.getBindingName();
                    if (name != null && bindingName != null) {
                        ejbRefBndMap.put(name, bindingName);
                    }
                }
            }
        }
    }

    public static void configureResourceRefBindings(com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup refBindingsGroup,
                                                    Map<String, String> resourceRefBindings,
                                                    ResourceRefConfigList resRefConfigList) {

        if (refBindingsGroup != null) {
            List<com.ibm.ws.javaee.dd.commonbnd.ResourceRef> resRefBnds = refBindingsGroup.getResourceRefs();
            if (!resRefBnds.isEmpty()) {
                for (com.ibm.ws.javaee.dd.commonbnd.ResourceRef resRefBnd : resRefBnds) {
                    String name = resRefBnd.getName();
                    if (name != null) {
                        String bindingName = resRefBnd.getBindingName();
                        if (bindingName != null) {
                            resourceRefBindings.put(name, bindingName);
                        }

                        CustomLoginConfiguration clc = resRefBnd.getCustomLoginConfiguration();
                        if (clc != null) {
                            String clcName = clc.getName();
                            if (clcName != null) {
                                ResourceRefConfig resRefConfig = getResourceRefConfig(resRefConfigList, name);
                                resRefConfig.setLoginConfigurationName(clcName);
                                for (com.ibm.ws.javaee.dd.commonbnd.Property property : clc.getProperties()) {
                                    String propertyName = property.getName();
                                    String propertyValue = property.getValue();
                                    if (propertyName != null && propertyValue != null) {
                                        resRefConfig.addLoginProperty(propertyName, propertyValue);
                                    }
                                }
                            }
                        } else {
                            AuthenticationAlias authAlias = resRefBnd.getAuthenticationAlias();
                            if (authAlias != null) {
                                String authAliasName = authAlias.getName();
                                if (authAliasName != null) {
                                    ResourceRefConfig resRefConfig = getResourceRefConfig(resRefConfigList, name);
                                    resRefConfig.addLoginProperty("DefaultPrincipalMapping", authAliasName);
                                }
                            }
                        }

                        // No one is sure what these do.
                        //String defaultAuthUserid = resRefBnd.getDefaultAuthUserid();
                        //String defaultAuthPassword = resRefBnd.getDefaultAuthPassword()
                    }
                }
            }
        }
    }

    public static void configureResourceRefExtensions(List<com.ibm.ws.javaee.dd.commonext.ResourceRef> resRefExts,
                                                      ResourceRefConfigList resRefConfigList) {
        if (!resRefExts.isEmpty()) {
            for (com.ibm.ws.javaee.dd.commonext.ResourceRef resRefExt : resRefExts) {
                String name = resRefExt.getName();
                if (name != null) {
                    ResourceRefConfig resRefConfig = getResourceRefConfig(resRefConfigList, name);

                    if (resRefExt.isSetIsolationLevel()) {
                        switch (resRefExt.getIsolationLevel()) {
                            case TRANSACTION_NONE:
                                resRefConfig.setIsolationLevel(Connection.TRANSACTION_NONE);
                                break;
                            case TRANSACTION_READ_UNCOMMITTED:
                                resRefConfig.setIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED);
                                break;
                            case TRANSACTION_READ_COMMITTED:
                                resRefConfig.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
                                break;
                            case TRANSACTION_REPEATABLE_READ:
                                resRefConfig.setIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ);
                                break;
                            case TRANSACTION_SERIALIZABLE:
                                resRefConfig.setIsolationLevel(Connection.TRANSACTION_SERIALIZABLE);
                                break;
                        }
                    }

                    // No one is sure what this does.
                    //if (resRefExt.isSetConnectionManagementPolicy())

                    if (resRefExt.isSetCommitPriority()) {
                        resRefConfig.setCommitPriority(resRefExt.getCommitPriority());
                    }

                    if (resRefExt.isSetBranchCoupling()) {
                        switch (resRefExt.getBranchCoupling()) {
                            case LOOSE:
                                resRefConfig.setBranchCoupling(ResourceRefInfo.BRANCH_COUPLING_LOOSE);
                                break;
                            case TIGHT:
                                resRefConfig.setBranchCoupling(ResourceRefInfo.BRANCH_COUPLING_TIGHT);
                                break;
                        }
                    }
                }
            }
        }
    }

    public static ResourceRefConfig getResourceRefConfig(ResourceRefConfigList resRefConfigList, String name) {
        if (resRefConfigList != null) {
            return resRefConfigList.findOrAddByName(name);
        }
        return null;
    }

    public static void configureResourceEnvRefBindings(com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup refBindingsGroup,
                                                       Map<String, String> resourceEnvRefBindings) {
        if (refBindingsGroup != null) {
            List<ResourceEnvRef> resEnvRefBnds = refBindingsGroup.getResourceEnvRefs();
            if (!resEnvRefBnds.isEmpty()) {
                for (com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef resEnvRefBnd : resEnvRefBnds) {
                    String name = resEnvRefBnd.getName();
                    String bindingName = resEnvRefBnd.getBindingName();
                    if (name != null && bindingName != null) {
                        resourceEnvRefBindings.put(name, bindingName);
                    }
                }
            }
        }
    }

    public static void configureMessageDestinationRefBindings(com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup refBindingsGroup,
                                                              Map<String, String> msgDestValues) {
        if (refBindingsGroup != null) {
            List<MessageDestinationRef> msgDestRefBnds = refBindingsGroup.getMessageDestinationRefs();
            if (!msgDestRefBnds.isEmpty()) {
                for (com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef msgDestBnd : msgDestRefBnds) {
                    String name = msgDestBnd.getName();
                    String bindingName = msgDestBnd.getBindingName();
                    if (name != null && bindingName != null) {
                        msgDestValues.put(name, bindingName);
                    }
                }
            }
        }
    }

    public static void configureEnvEntryBindings(com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup refBindingsGroup,
                                                 Map<String, String> envEntryValues,
                                                 Map<String, String> envEntryBindings) {
        if (refBindingsGroup != null) {
            List<EnvEntry> envEntryBnds = refBindingsGroup.getEnvEntries();
            if (!envEntryBnds.isEmpty()) {
                for (com.ibm.ws.javaee.dd.commonbnd.EnvEntry envEntryBnd : envEntryBnds) {
                    String name = envEntryBnd.getName();
                    if (name != null) {
                        String value = envEntryBnd.getValue();
                        if (value != null) {
                            envEntryValues.put(name, value);
                        }

                        String bindingName = envEntryBnd.getBindingName();
                        if (bindingName != null) {
                            envEntryBindings.put(name, bindingName);
                        }
                    }
                }
            }
        }
    }

    public static void configureDataSourceBindings(com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup refBindingsGroup,
                                                   Map<String, String> dataSourceBindings) {
        if (refBindingsGroup != null) {
            List<DataSource> dataSourceBnds = refBindingsGroup.getDataSources();
            if (!dataSourceBnds.isEmpty()) {
                for (com.ibm.ws.javaee.dd.commonbnd.DataSource dataSourceBnd : dataSourceBnds) {
                    String name = dataSourceBnd.getName();
                    String bindingName = dataSourceBnd.getBindingName();
                    if (name != null && bindingName != null) {
                        dataSourceBindings.put(name, bindingName);
                    }
                }
            }
        }
    }
}
