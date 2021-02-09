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
package com.ibm.ws.config.xml.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.websphere.config.ConfigUpdateException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ConfigurationDictionary;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.config.admin.SystemConfigSupport;
import com.ibm.ws.config.xml.internal.ConfigEvaluator.EvaluationResult;
import com.ibm.ws.config.xml.internal.ConfigEvaluator.UnresolvedPidType;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 *
 */
class ConfigUpdater {

    private static final TraceComponent tc = Tr.register(ConfigUpdater.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    private final ConfigEvaluator configEvaluator;
    private final ConfigVariableRegistry variableRegistry;
    private final SystemConfigSupport caSupport;
    private final ExtendedMetatypeManager extendedMetatypeManager;

    private final Set<UnresolvedPidType> unresolvedReferences = new HashSet<UnresolvedPidType>();

    private final MetaTypeRegistry metatypeRegistry;

    private final Map<Object, ConfigurationInfo> failedUpdateInfo = new HashMap<Object, ConfigurationInfo>();
    private final Map<Object, ExtendedAttributeDefinition> failedUpdateAttrDef = new HashMap<Object, ExtendedAttributeDefinition>();
    private final List<ConfigurationInfo> retryUpdateInfoList = new ArrayList<ConfigurationInfo>();
    private boolean ranUpdateRetry = false;

    /**
     * @param configEvaluator
     * @param configAdminImpl
     * @param variableRegistry
     * @param caFactory
     * @param onError
     */
    public ConfigUpdater(ConfigEvaluator ce,
                         SystemConfigSupport caSupport,
                         ConfigVariableRegistry variableRegistry,
                         MetaTypeRegistry metatypeRegistry,
                         ExtendedMetatypeManager emm) {
        this.configEvaluator = ce;
        this.caSupport = caSupport;
        this.variableRegistry = variableRegistry;
        this.metatypeRegistry = metatypeRegistry;
        this.extendedMetatypeManager = emm;

    }

    @Trivial
    public Collection<ConfigurationInfo> update(boolean encourageUpdates, Collection<ConfigurationInfo> updatedConfigurations) throws ConfigUpdateException {
        // Clear out unresolved references before updating. This desperately needs a redesign.
        Iterator<UnresolvedPidType> iter = unresolvedReferences.iterator();
        while (iter.hasNext()) {
            UnresolvedPidType ref = iter.next();
            if (!ref.permanent()) {
                iter.remove();
            }
        }

        return update(updatedConfigurations, false, encourageUpdates);
    }

    /**
     * @param encourageUpdates TODO
     * @return
     * @throws ConfigUpdateException
     *
     */
    @FFDCIgnore({ AttributeValidationException.class, ConfigUpdateException.class, ConfigEvaluatorException.class })
    private Collection<ConfigurationInfo> update(Collection<ConfigurationInfo> updatedConfigurations, boolean failOnError, boolean encourageUpdates) throws ConfigUpdateException {
        Collection<ConfigurationInfo> retVal = new ArrayList<ConfigurationInfo>();
        for (ConfigurationInfo info : updatedConfigurations) {
            try {
                updateConfiguration(info, retVal, encourageUpdates);
            } catch (ConfigUpdateException e) {
                String nodeName = getNodeNameForExceptions(info.configElement);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "update(). Exception while trying to update " + nodeName + " configuration on disk. Exception message = " + e.getMessage());
                }

                if (failOnError) {
                    throw e;
                } else {
                    Tr.error(tc, "error.config.update.exception", new Object[] { nodeName, e.getMessage(), info.configElement.getId() });
                    warnIfOldConfigExists(info, nodeName);
                }
            } catch (AttributeValidationException e) {
                String nodeName = getNodeNameForExceptions(info.configElement);
                if (failOnError) {
                    throw new ConfigUpdateException(e);
                } else {
                    Tr.error(tc, "error.attribute.validation.exception",
                             new Object[] { nodeName, e.getAttributeDefintion().getID(), e.getValue(), e.getMessage() });
                    warnIfOldConfigExists(info, nodeName);
                }
            } catch (ConfigEvaluatorException e) {
                String nodeName = getNodeNameForExceptions(info.configElement);
                if (failOnError) {
                    throw new ConfigUpdateException(e);
                } else {
                    Tr.error(tc, "error.config.update.exception", new Object[] { nodeName, e.getMessage(), info.configElement.getId() });
                    warnIfOldConfigExists(info, nodeName);
                }
            }
        }
        if (!ranUpdateRetry) {
            retryFailedUpdate(failOnError);
        }
        ranUpdateRetry = false;
        return retVal;

    }

    /**
     * @param info
     * @param nodeName
     */
    private void warnIfOldConfigExists(ConfigurationInfo info, String nodeName) {
        ExtendedConfiguration config = null;
        if (info.configElement.getId() == null) {
            // singleton
            config = caSupport.findConfiguration(info.configElement.getNodeName());
        } else {
            config = caSupport.lookupConfiguration(info.configElement.getConfigID());
        }

        if (config != null && config.getProperties() != null) {
            Tr.warning(tc, "warning.old.config.still.in.use", new Object[] { nodeName, info.configElement.getId() });
        }
    }

    /**
     * @param configElement
     * @return
     */
    private String getNodeNameForExceptions(ConfigElement configElement) {

        RegistryEntry entry = metatypeRegistry.getRegistryEntry(configElement.getNodeName());
        if (entry != null) {
            if (entry.getAlias() != null)
                return entry.getAlias();
            else if (entry.getChildAlias() != null)
                return entry.getChildAlias();
            else
                return entry.getPid();
        }

        return configElement.getNodeName();
    }

    private void updateConfiguration(ConfigurationInfo info, Collection<ConfigurationInfo> infos, boolean encourageUpdates) throws ConfigEvaluatorException, ConfigUpdateException {

        // evaluate the configuration and use MetaType to convert values into right types
        EvaluationResult result = configEvaluator.evaluate(info.configElement, info.registryEntry);
        unresolvedReferences.addAll(result.getAllUnresolvedReferences());

        if (result.isValid()) {
            updateConfiguration(result, info, infos, encourageUpdates);
        }

    }

    /**
     * Recursively updates the configuration in config admin
     *
     * @param encourageUpdates whether to update the configuration if not discouraged in metatype even with no nested or property updates.
     *
     * @return true if there were any updates to this element or any nested, unhidden elements or updates encouraged and not hidden.
     */
    private boolean updateConfiguration(EvaluationResult result, ConfigurationInfo info, Collection<ConfigurationInfo> infos,
                                        boolean encourageUpdates) throws ConfigUpdateException {

        //encourageUpdates is overruled by metatype declining nested notifications.
        encourageUpdates &= !!!(result.getRegistryEntry() != null && result.getRegistryEntry().supportsHiddenExtensions());

        // nestedUpdates is true if there are any updates to child elements that are not hidden
        boolean nestedUpdates = false;

        // propertiesUpdated is true if the properties of this configuration are updated
        boolean propertiesUpdated = false;

        // process nested configuration first
        Map<ConfigID, EvaluationResult> nested = result.getNested();

        for (Map.Entry<ConfigID, EvaluationResult> entry : nested.entrySet()) {
            EvaluationResult nestedResult = entry.getValue();
            ExtendedConfiguration nestedConfig = caSupport.findConfiguration(nestedResult.getPid());
            if (nestedConfig == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Configuration not found: " + nestedResult.getPid());
                }
            } else {
                ConfigurationInfo nestedInfo = new ConfigurationInfo(nestedResult.getConfigElement(), nestedConfig, nestedResult.getRegistryEntry(), false);
                // updated will be true if there was a nested properties update
                boolean updated = updateConfiguration(nestedResult, nestedInfo, infos, encourageUpdates);

                // If the nested update should be hidden, set updated to false so that it won't appear as a child properties update
                if (updated && (result.getRegistryEntry() != null && result.getRegistryEntry().supportsHiddenExtensions())) {
                    updated = false;
                }

                // If updated is true, set nestedUpdates to true to signify that there are unhidden nested updates
                nestedUpdates |= updated;
            }
        }

        ExtendedConfiguration config = info.config;

        // update configuration on disk
        config.setInOverridesFile(true);

        Set<ConfigID> oldReferences = config.getReferences();
        Set<ConfigID> newReferences = result.getReferences();

        Dictionary<String, Object> oldProperties = config.getReadOnlyProperties();
        Dictionary<String, Object> newProperties = result.getProperties();

        // Update the cache if either the properties have changed or if there was a nested, non-hidden update.
        // We force the update when there are nested updates because DS will not process an event unless the
        // change count is updated. This keeps DS behavior in sync with MS/MSF behavior with nested updates.
        if (encourageUpdates || nestedUpdates || !equalConfigProperties(oldProperties, newProperties, encourageUpdates) ||
            !equalConfigReferences(oldReferences, newReferences)) {
            propertiesUpdated = true;
            crossReference(oldProperties, newProperties);
            variableRegistry.updateVariableCache(result.getVariables());
            Dictionary<String, Object> newProp = massageNewConfig(oldProperties, newProperties);
            Set<String> newUniques = updateUniqueVariables(info, oldProperties, newProp);
            try {
                config.updateCache(newProp, newReferences, newUniques);
            } catch (IOException ex) {
                throw new ConfigUpdateException(ex);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Updated configuration: " + toTraceString(config, result.getConfigElement()));
            }
            // (1) If there are unhidden nested updates of any kind, add the config info
            // (2) If this element has its properties updated, add the config info
            //

            if (infos != null) {
                infos.add(info);
            }
        }

        // Create supertype config if necessary
        extendedMetatypeManager.createSuperTypes(info);

        // Return a boolean to indicate whether we updated anything here
        return (propertiesUpdated || nestedUpdates);

    }

    /**
     * Return a new config dictionary with initial "hidden" keys and values from
     * o,
     * then add all keys and values from n to the new config dictionary and
     * return.
     * If returning dictionary is empty, return null instead.
     *
     * @param o
     * @param n
     * @return Dictionary
     */
    private static Dictionary<String, Object> massageNewConfig(Dictionary<String, Object> oldProps, Dictionary<String, Object> newProps) {
        ConfigurationDictionary ret = new ConfigurationDictionary();

        if (oldProps != null) {
            for (Enumeration<String> keyItr = oldProps.keys(); keyItr.hasMoreElements();) {
                String key = keyItr.nextElement();
                if (((key.startsWith(XMLConfigConstants.CFG_CONFIG_PREFIX) && !(key.equals(XMLConfigConstants.CFG_PARENT_PID)))
                     || key.startsWith(XMLConfigConstants.CFG_SERVICE_PREFIX))) {
                    ret.put(key, oldProps.get(key));
                }

            }
        }

        if (newProps != null) {
            for (Enumeration<String> keyItr = newProps.keys(); keyItr.hasMoreElements();) {
                String key = keyItr.nextElement();
                ret.put(key, newProps.get(key));
            }
        }

        if (ret.isEmpty()) {
            ret = null;
        } else {
            // must add config.source, else things may not process properly
            ret.put(XMLConfigConstants.CFG_CONFIG_SOURCE, XMLConfigConstants.CFG_CONFIG_SOURCE_FILE);
        }
        return ret;
    }

    @Trivial
    private static Map<String, Object> toMap(Dictionary<String, Object> d) {
        if (d == null) {
            return Collections.emptyMap();
        }
        HashMap<String, Object> ret = new HashMap<String, Object>(d.size());
        for (Enumeration<String> keyIter = d.keys(); keyIter.hasMoreElements();) {
            String key = keyIter.nextElement();
            ret.put(key, d.get(key));
        }
        return ret;
    }

    /**
     * Compare two Maps and check for their content equality.
     * Keys starting with <code>config.</code> will be ignored (except for config.parentPID.)
     * Keys starting with <code>service.</code> will be ignored.
     *
     * @param oldProperties
     * @param newProperties
     * @param encourageUpdates TODO
     * @return boolean
     */
    private static boolean equalConfigProperties(Dictionary<String, Object> oldProperties, Dictionary<String, Object> newProperties, boolean encourageUpdates) {
        if ((oldProperties == null || oldProperties.isEmpty()) && (newProperties == null || newProperties.isEmpty())) {
            return true;
        }

        Map<String, Object> oriMapC = toMap(oldProperties);
        Map<String, Object> newMapC = toMap(newProperties);

        //if the old map has never had properties set and we have properties, update it, even if all the keys are ignored.
        if (oriMapC.isEmpty() && !newMapC.isEmpty()) {
            return false;
        }

        // Go through keys in oriMapC and compare with newMapC.
        // If same, remove key from newMapC (skip those starting with config.* other than config.parentPID and
        // service.*)
        // If there are any different ones, return false.
        // Then go through trimmed newMapC keys (skip those starting with config.* other than config.parentPID
        // and service.*)
        // and check for equality in value. If != found, return false.
        List<String> removeKeyList = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : oriMapC.entrySet()) {
            String keyObj = entry.getKey();
            if (!!!ignoreKey(keyObj, encourageUpdates)) {
                if (newMapC.containsKey(keyObj)) {
                    if (equalConfigValues(entry.getValue(), newMapC.get(keyObj))) {
                        removeKeyList.add(keyObj);
                    } else
                        return false;
                } else
                    return false;
            }
        }
        for (Object keyObj : removeKeyList) {
            if (keyObj != null)
                newMapC.remove(keyObj);
        }
        for (Map.Entry<String, ?> entry : newMapC.entrySet()) {
            String keyObj = entry.getKey();
            if ((keyObj != null) &&
                !!!ignoreKey(keyObj, encourageUpdates)) {
                if (oriMapC.containsKey(keyObj)) {
                    if (!equalConfigValues(entry.getValue(), oriMapC.get(keyObj)))
                        return false;
                } else
                    return false;
            }
        }
        return true;
    }

    private static boolean ignoreKey(String keyObj, boolean encourageUpdates) {
        return (keyObj.startsWith(XMLConfigConstants.CFG_CONFIG_PREFIX) && !!!keyObj.equals(XMLConfigConstants.CFG_PARENT_PID)) ||
               keyObj.startsWith(XMLConfigConstants.CFG_SERVICE_PREFIX) ||
               (!!!encourageUpdates && keyObj.equals(XMLConfigConstants.CFG_INSTANCE_ID));
    }

    private static boolean equalConfigReferences(Set<ConfigID> oldReferences, Set<ConfigID> newReferences) {
        if (oldReferences == null || oldReferences.isEmpty()) {
            return (newReferences == null || newReferences.isEmpty());
        } else {
            return oldReferences.equals(newReferences);
        }
    }

    /**
     * Compare two configuration values and determine if they are equal or not.
     * Acceptable value types are String, String array, or Map of Strings (the
     * liberty supported config value types).
     * For String array value comparison, all values and must be equal at same
     * index.
     * For Map of String value comparison, all keys and values must be equal.
     *
     * @param c1
     *            config value
     * @param c2
     *            config value
     * @return boolean
     */
    private static boolean equalConfigValues(Object c1, Object c2) {
        if (c1 instanceof String && c2 instanceof String)
            return c1.equals(c2);
        if (c1 instanceof String[] && c2 instanceof String[])
            return Arrays.equals((String[]) c1, (String[]) c2);
        if (c1 instanceof Map && c2 instanceof Map)
            return c1.equals(c2);
        if (c1 != null)
            return c1.equals(c2);
        return (c2 == null);
    }

    /**
     *
     * @param oldProperties
     * @param newProperties
     * @throws ConfigUpdateException
     */
    private Set<String> updateUniqueVariables(ConfigurationInfo info, Dictionary<String, Object> oldProperties,
                                              Dictionary<String, Object> newProperties) throws ConfigUpdateException {
        Map<String, ExtendedAttributeDefinition> metaTypeAttributes = info.metaTypeAttributes;
        Set<String> uniques = new HashSet<String>();
        if (metaTypeAttributes != null) {
            for (Map.Entry<String, ExtendedAttributeDefinition> entry : metaTypeAttributes.entrySet()) {
                ExtendedAttributeDefinition attrDef = entry.getValue();
                if (attrDef.isUnique()) {

                    Object oldValue = oldProperties == null ? null : oldProperties.get(attrDef.getID());
                    Object newValue = newProperties == null ? null : newProperties.get(attrDef.getID());

                    if (oldValue != null)
                        variableRegistry.removeUniqueVariable(attrDef, oldValue.toString());

                    if (newValue != null) {

                        String variable = variableRegistry.getUniqueVarString(attrDef, newValue.toString());
                        String varSymbol = "${" + variable + "}";
                        String existing = variableRegistry.resolveRawString(varSymbol);

                        if (existing == null || existing.equals(varSymbol)) {
                            // We have yet to see this attribute value, so put the value in the registry and return successfully
                            uniques.add(variable);
                            variableRegistry.addVariableInUse(variable);
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "updateUniqueVariables(). The value " + newValue + " for attribute " + attrDef.getID() +
                                             " is not unique. The update will be retried if possible.");
                            }
                            // There is another entry with this attribute value. It could be that another change later will resolve
                            // this problem so don't throw an error, just note down the value and info for later.
                            failedUpdateInfo.put(newValue, info);
                            failedUpdateAttrDef.put(newValue, attrDef);
                        }
                    }

                }

            }
        }
        return uniques;
    }

    private static String toTraceString(ExtendedConfiguration config, ConfigElement configElement) {
        if (config.getFactoryPid() == null) {
            return config.getPid();
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(config.getFactoryPid());

            if (configElement != null) {
                builder.append("-");
                builder.append(configElement.getId());
            }

            builder.append(" (");
            builder.append(config.getPid());
            builder.append(")");

            return builder.toString();
        }
    }

    /**
     * @param allInfos
     */
    public void fireConfigurationEvents(Collection<ConfigurationInfo> allInfos) {
        for (ConfigurationInfo info : allInfos) {
            info.fireEvents(null);
        }
    }

    /**
     * @throws ConfigMergeException
     *
     */
    public void updateSystemVariables(ServerXMLConfiguration serverXMLConfig) throws ConfigMergeException {
        variableRegistry.updateSystemVariables(serverXMLConfig.getVariables());
    }

    synchronized void processUnresolvedReferences(OnError onError) throws ConfigUpdateException {
        List<ConfigurationInfo> infos = new ArrayList<ConfigurationInfo>();

        for (UnresolvedPidType ref : unresolvedReferences) {
            // Create a configurationInfo object for the "From" side of the relationship so that
            // we can reevaluate it

            ConfigurationInfo referringConfigInfo = ref.getReferringConfigurationInfo();
            if (referringConfigInfo != null) {
                // Null referring configuration info indicates that the metatype was removed since we added
                // the unresolved reference to the list. It's no longer a reference, so don't worry about it.
                infos.add(referringConfigInfo);
            }

        }
        unresolvedReferences.clear();

        updateAndFireEvents(infos, onError);

        for (Iterator<UnresolvedPidType> iter = unresolvedReferences.iterator(); iter.hasNext();) {
            UnresolvedPidType ref = iter.next();
            ref.reportError();
            if (!ref.permanent()) {
                iter.remove();
            }
        }
    }

    void crossReference(Dictionary<String, Object> oldProperties, Dictionary<String, Object> newProperties) {
        // Check to see if any of the values in oldProperties match the keys in failedUpdateInfo, indicating that
        // we are making a change that could resolve an earlier conflict.
        if (oldProperties != null && !failedUpdateInfo.isEmpty()) {
            Enumeration<String> keys = oldProperties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String oldValue = oldProperties.get(key).toString();
                if (failedUpdateInfo.containsKey(oldValue)) {
                    String retryValue = oldValue;
                    // We have found an old property that matches one we were trying to update to earlier.
                    // Now check that this property is indeed being changed.
                    String newValue = newProperties.get(key).toString();
                    if (!oldValue.equals(newValue)) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "crossReference(). Found a value change match for the value " + retryValue +
                                         ". The update for attribute " + failedUpdateAttrDef.get(retryValue).getID() +
                                         " will be retried.");
                        }
                        // Move the previously failed update to a list of updates to be retried later
                        retryUpdateInfoList.add(failedUpdateInfo.get(retryValue));
                        failedUpdateInfo.remove(retryValue);
                    }
                }
            }
        }
    }

    void retryFailedUpdate(boolean failOnError) throws ConfigUpdateException {
        ranUpdateRetry = true;
        // If failedUpdateInfo is empty there were no conflicts so no need to retry anything
        if (!failedUpdateInfo.isEmpty()) {
            Set<Object> failedUpdates = failedUpdateInfo.keySet();
            for (Object value : failedUpdates) {
                ConfigurationInfo info = failedUpdateInfo.get(value);
                // If the info is non-null then we didn't find an update that would fix this non-unique attribute so
                // throw an error
                if (info != null) {
                    String id = failedUpdateAttrDef.get(value).getID();
                    Tr.error(tc, "error.unique.value.conflict", id, value);
                    String nodeName = getNodeNameForExceptions(info.configElement);
                    String exceptionMessage = "The value " + value + " for attribute " + id + " is not unique";

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "update(). Exception while trying to update " + nodeName + " configuration on disk. Exception message = " + exceptionMessage);
                    }
                    if (failOnError) {
                        throw new ConfigUpdateException(exceptionMessage);
                    } else {
                        Tr.error(tc, "error.config.update.exception", new Object[] { nodeName, exceptionMessage, info.configElement.getId() });
                        warnIfOldConfigExists(info, nodeName);
                    }
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "retryFailedUpdate(). Attempting to retry the following updates: " + retryUpdateInfoList);
            }
            updateAndFireEvents(retryUpdateInfoList, failOnError ? OnError.FAIL : OnError.WARN);
            failedUpdateInfo.clear();
        }
        retryUpdateInfoList.clear();
    }

    void updateAndFireEvents(List<ConfigurationInfo> infos, OnError onError) throws ConfigUpdateException {

        // Step 1. Save configuration info to disk and collect ConfigurationImpl objects
        Collection<ConfigurationInfo> allInfos = update(infos, onError == OnError.FAIL, false);

        // Step 2. Fire configuration change events
        fireConfigurationEvents(allInfos);
    }

    /**
     * Fire metatype added events for the specified PIDs. Events are sent to the config
     * update queue so that they are (mostly) dispatched in temporal order with the
     * config updates they cause.
     *
     * @param updatedPids
     */
    public void fireMetatypeAddedEvents(Set<String> updatedPids) {

        for (String pid : updatedPids) {
            caSupport.fireMetatypeAddedEvent(pid);
        }

    }

    /**
     * Fire metatype removed events for the specified PIDs. Events are sent to the config
     * update queue so that they are (mostly) dispatched in temporal order with the config
     * updates they cause.
     *
     * @param updatedPids
     */
    public void fireMetatypeDeletedEvents(Set<String> updatedPids) {
        for (String pid : updatedPids) {
            caSupport.fireMetatypeRemovedEvent(pid);
        }
    }
}
