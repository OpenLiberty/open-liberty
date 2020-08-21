/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.websphere.config.ConfigRetrieverException;
import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ConfigurationDictionary;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.config.xml.internal.EvaluationContext.NestedInfo;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.EntryAction;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;
import com.ibm.wsspi.kernel.service.utils.PathUtils;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

class ConfigEvaluator {

    private static final TraceComponent tc = Tr.register(ConfigEvaluator.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    private static final String ALL_PIDS = "*";

    private final ConfigRetriever configRetriever;
    private final MetaTypeRegistry metatypeRegistry;
    private final ServerXMLConfiguration serverXMLConfig;

    private final VariableEvaluator variableEvaluator;

    ConfigEvaluator(ConfigRetriever retriever, MetaTypeRegistry metatypeRegistry, ConfigVariableRegistry variableRegistry, ServerXMLConfiguration serverXmlConfig) {
        this.configRetriever = retriever;
        this.metatypeRegistry = metatypeRegistry;
        this.serverXMLConfig = serverXmlConfig;
        this.variableEvaluator = new VariableEvaluator(variableRegistry, this);
    }

    @Trivial
    private Object evaluateSimple(Object rawValue, EvaluationContext context, boolean ignoreWarnings) throws ConfigEvaluatorException {
        if (rawValue instanceof String) {
            return convertObjectToSingleValue(rawValue, null, context, -1, ignoreWarnings);
        } else if (rawValue instanceof List) {
            return convertListToStringArray((List<?>) rawValue, null, context, ignoreWarnings);
        } else {
            throw new IllegalStateException("Unsupported type: " + rawValue.getClass());
        }
    }

    EvaluationResult evaluate(ConfigElement config, RegistryEntry registryEntry) throws ConfigEvaluatorException {
        return evaluate(config, registryEntry, "", false);
    }

    EvaluationResult evaluate(ConfigElement config, RegistryEntry registryEntry, String flatPrefix, boolean ignoreWarnings) throws ConfigEvaluatorException {

        Map<String, ExtendedAttributeDefinition> attributeMap;
        List<AttributeDefinition> requiredAttributes;
        if (registryEntry != null) {
            if (registryEntry.getExtends() != null) {
                //extended type, get the hierarchy complete attributes
                //HOPEFULLLY TEMPORARY
                String pid = registryEntry.getPid();
                attributeMap = metatypeRegistry.getHierarchyCompleteAttributeMap(pid);
                requiredAttributes = metatypeRegistry.getRequiredAttributesForHierarchy(pid);
            } else {
                attributeMap = registryEntry.getAttributeMap();
                requiredAttributes = registryEntry.getObjectClassDefinition().getRequiredAttributes();
            }
        } else {
            attributeMap = null;
            requiredAttributes = Collections.emptyList();
        }
        return evaluate(config, registryEntry, attributeMap, requiredAttributes, flatPrefix, ignoreWarnings);
    }

    //this method is private to enforce the hierarchy processing on the required attributes
    //if it is made public then the hierarchy processing needs to be moved or done twice
    private EvaluationResult evaluate(ConfigElement config, RegistryEntry registryEntry, Map<String, ExtendedAttributeDefinition> attributeMap,
                                      List<AttributeDefinition> requiredAttributes, String flatPrefix, boolean ignoreWarnings) throws ConfigEvaluatorException {
        Dictionary<String, Object> map = new ConfigurationDictionary();
        EvaluationContext context = new EvaluationContext(registryEntry, variableEvaluator);
        context.setConfigElement(config);
        context.setProperties(map);

        // Warn if the ID attribute contains a variable
        if (registryEntry != null &&
            config.getId() != null &&
            XMLConfigConstants.VAR_PATTERN.matcher(config.getId()).matches() &&
            !ignoreWarnings) {
            String nodeName = getNodeNameForMessage(registryEntry, config);
            Tr.warning(tc, "variables.in.id.not.supported", nodeName, config.getId());
        }

        // process attributes based on metatype info
        if (attributeMap != null) {
            context.setAttributeDefinitionMap(attributeMap);
            for (Map.Entry<String, ExtendedAttributeDefinition> entry : attributeMap.entrySet()) {
                ExtendedAttributeDefinition attributeDef = entry.getValue();
                if (!attributeDef.isFinal()) {
                    String attributeName = entry.getKey();
                    if (XMLConfigConstants.CFG_PARENT_PID.equals(attributeName))
                        continue;
                    Object value = evaluateMetaTypeAttribute(attributeName, context, attributeDef, flatPrefix, ignoreWarnings);
                    if (value == null &&
                        required(attributeDef.getID(), requiredAttributes) &&
                        //                    requiredAttributes.contains(attributeDef.getDelegate()) &&
                        !context.hasUnresolvedAttribute(attributeDef) &&
                        !ignoreWarnings) {
                        String nodeName = getNodeNameForMessage(registryEntry, config);

                        if (config.getId() == null) {
                            Tr.error(tc, "error.missing.required.attribute.singleton", nodeName, attributeName);
                        } else {
                            Tr.error(tc, "error.missing.required.attribute", nodeName, attributeName, config.getId());
                        }
                        context.setValid(false);
                    }
                }
            }
            for (Map.Entry<String, ExtendedAttributeDefinition> entry : attributeMap.entrySet()) {
                Object rawValue;
                ExtendedAttributeDefinition attributeDef = entry.getValue();
                if (attributeDef.isFinal()) {
                    String attributeName = entry.getKey();
                    if (XMLConfigConstants.CFG_INSTANCE_ID.equals(attributeName)) {
                        rawValue = config.getAttribute(XMLConfigConstants.CFG_INSTANCE_ID);
                        if (!attributeDef.getDefaultValue()[0].equals(rawValue)) {
                            // User has overridden a ibm:final value in server.xml
                            if (rawValue != null) {
                                Tr.warning(tc, "warning.supplied.config.not.valid", attributeName, rawValue);
                                context.setValid(false);
                            }
                        }
                    }
                    if (XMLConfigConstants.CFG_PARENT_PID.equals(attributeName))
                        continue;
                    evaluateMetaTypeAttribute(attributeName, context, attributeDef, flatPrefix, ignoreWarnings);
                }
            }
        }

        // process any remaining attributes specified in config
        for (Map.Entry<String, Object> entry : config.getAttributes().entrySet()) {
            String attributeName = entry.getKey();
            if (attributeName.startsWith(XMLConfigConstants.CFG_CONFIG_PREFIX)
                || context.isProcessed(attributeName)) {
                continue;
            }
            if (flatPrefix != null
                && attributeName.equals(XMLConfigConstants.CFG_INSTANCE_ID)
                // This instanceof should always be true since this is a
                // flattened element.
                && config instanceof SimpleElement
                && !((SimpleElement) config).isUsingNonDefaultId()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "skipping default id for flattened element");
                }
                continue;
            }
            Object attributeValue = entry.getValue();
            evaluateSimpleAttribute(attributeName, attributeValue, context, flatPrefix, ignoreWarnings);

        }

        if (config.getId() != null) {
            map.put(XMLConfigConstants.CFG_CONFIG_INSTANCE_ID, config.getConfigID().toString());
        }

        map.put(XMLConfigConstants.CFG_CONFIG_INSTANCE_DISPLAY_ID, config.getDisplayId());

        if (config.getParent() != null && (flatPrefix == null || flatPrefix.equals(""))) {
            if (config.getParent().getId() == null) {
                map.put(XMLConfigConstants.CFG_PARENT_PID, config.getParent().getConfigID().toString());
            } else {
                String parentPid = lookupPid(config.getParent().getConfigID());
                if (parentPid != null)
                    map.put(XMLConfigConstants.CFG_PARENT_PID, parentPid);
            }
        }

        //  Evaluate all CopyOf Attributes
        context.evaluateCopiedAttributes();

        // Return the EvaluationResult
        return context.getEvaluationResult();
    }

    /**
     * Returns the node name to use in error/warning messages based on the metatype and config
     */
    private String getNodeNameForMessage(RegistryEntry registryEntry, ConfigElement config) {
        if (registryEntry == null)
            return config == null ? null : config.getNodeName();

        String nodeName = registryEntry.getAlias();
        if (nodeName == null) {
            nodeName = registryEntry.getChildAlias();
        }
        if (nodeName == null) {
            nodeName = config.getNodeName();
        }
        return nodeName;
    }

    /**
     * @param id
     * @param requiredAttributes
     * @return
     */
    private boolean required(String id, List<AttributeDefinition> requiredAttributes) {
        for (AttributeDefinition ad : requiredAttributes) {
            if (id.equals(ad.getID())) {
                return true;
            }
        }
        return false;
    }

    @Trivial
    protected Object evaluateSimpleAttribute(String attributeName, Object attributeValue, EvaluationContext context, String flatPrefix,
                                             boolean ignoreWarnings) throws ConfigEvaluatorException {
        context.setAttributeName(attributeName);
        context.addProcessed(attributeName);

        Object value = evaluateSimple(attributeValue, context, ignoreWarnings);
        if (value != null) {
            context.setProperty(flatPrefix + attributeName, value);
        }
        evaluateFinish(context);

        return value;
    }

    static class AttributeValueCopy {

        private final String copiedAttribute;
        private final String attributeName;

        /**
         * @param copyOf
         */
        public AttributeValueCopy(String attributeName, String copyOf) {
            this.attributeName = attributeName;
            this.copiedAttribute = copyOf;
        }

        /**
         * @return
         */
        public String getCopiedAttribute() {
            return this.copiedAttribute;
        }

        public String getAttributeName() {
            return this.attributeName;
        }

    }

    /**
     * Find everything in the context related to the AD and evaluate it.
     *
     * @param attributeName  name used in context for this AD: may differ from ad.getId() due to renames.(???)
     * @param context        evaluation context for ocd
     * @param attributeDef   AD
     * @param flatPrefix     prefix from nested flattening
     * @param ignoreWarnings
     * @return
     * @throws ConfigEvaluatorException
     */
    @FFDCIgnore({ ConfigEvaluatorException.class, ConfigRetrieverException.class })
    Object evaluateMetaTypeAttribute(final String attributeName, final EvaluationContext context,
                                     final ExtendedAttributeDefinition attributeDef, final String flatPrefix,
                                     final boolean ignoreWarnings) throws ConfigEvaluatorException {
        final ConfigElement config = context.getConfigElement();
        context.setAttributeName(attributeName);
        context.addProcessed(attributeName);

        final String prefixedAttributeName = flatPrefix.length() == 0 ? attributeName : flatPrefix + attributeName;

        Object rawValue = null;
        if (attributeDef.getCopyOf() != null) {
            AttributeValueCopy copy = new AttributeValueCopy(prefixedAttributeName, attributeDef.getCopyOf());
            context.addAttributeValueCopy(copy);
        }

        if (attributeDef.isFlat()) {

            RegistryEntry nestedRegistryEntry = getRegistryEntry(attributeDef.getReferencePid());
            if (nestedRegistryEntry == null) {
                return null;
            }
            //TODO rename + flat wont' work yet.
            final Set<String> processedNames = new HashSet<String>();
            final AtomicInteger i = new AtomicInteger();
            String[] referenceAttributes = getReferenceAttributes(attributeName);
            evaluateFlatReference(referenceAttributes, context, nestedRegistryEntry, flatPrefix, config, i, processedNames, ignoreWarnings, attributeDef);
            ConfigEvaluatorException e = nestedRegistryEntry.traverseHierarchy(new EntryAction<ConfigEvaluatorException>() {

                private ConfigEvaluatorException result;

                @Override
                public boolean entry(RegistryEntry registryEntry) {
                    String elementName = registryEntry.getEffectiveAD(attributeName);
                    if (elementName != null) {
                        context.addProcessed(elementName);
                        try {
                            evaluateFlatAttribute(attributeName, elementName, context, registryEntry, flatPrefix, config, i, processedNames, ignoreWarnings);
                        } catch (ConfigEvaluatorException e) {
                            result = e;
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public ConfigEvaluatorException getResult() {
                    return result;
                }
            });
            if (e != null) {
                throw e;
            }
            evaluateFlatAttribute(attributeName, attributeName, context, nestedRegistryEntry, flatPrefix, config, i, processedNames, ignoreWarnings);

            int cardinality = attributeDef.getCardinality();
            cardinality = cardinality == Integer.MIN_VALUE ? Integer.MAX_VALUE : cardinality < 0 ? -cardinality : cardinality == 0 ? 1 : cardinality;
            if (i.get() > cardinality) {

                throw new ConfigEvaluatorException("Attribute " + attributeDef.getID() + " exceeded maximum allowed size " + cardinality);
            }
            return i.get() == 0 ? null : i;
        }

        //Check the properties to pick up already evaluated attributes
        Object actualValue = context.getProperties().get(attributeDef.getID());
        if (actualValue != null) {
            return actualValue;
        }

        if (!attributeDef.isFinal()) {
            if (attributeDef.getType() == MetaTypeFactory.PID_TYPE) {
                if (attributeDef.getReferencePid() != null) {
                    RegistryEntry registryEntry = getRegistryEntry(attributeDef.getReferencePid());
                    if (registryEntry != null) {
                        rawValue = registryEntry.traverseHierarchy(new EntryAction<Object>() {

                            private Object rawValue;

                            @Override
                            public boolean entry(RegistryEntry registryEntry) {
                                if (registryEntry.getEffectiveAD(attributeName) != null) {
                                    rawValue = mergeReferenceAttributes(registryEntry.getEffectiveAD(attributeName), context, attributeDef, config, rawValue);
                                }
                                return true;
                            }

                            @Override
                            public Object getResult() {
                                return rawValue;
                            }
                        });
                    }
                }
                rawValue = mergeReferenceAttributes(attributeName, context, attributeDef, config, rawValue);
            } else {
                rawValue = config.getAttribute(attributeName);
            }
        } else if (isWildcardReference(attributeDef)) {
            String factoryPid = attributeDef.getReferencePid();
            WildcardReference ref = new WildcardReference(factoryPid, attributeDef, context.getConfigElement().getConfigID());
            context.addUnresolvedReference(ref);
            try {
                ExtendedConfiguration[] configs = configRetriever.findAllConfigurationsByPid(factoryPid);
                configs = configs == null ? new ExtendedConfiguration[0] : configs;
                ArrayList<String> pids = new ArrayList<String>(configs.length);
                for (ExtendedConfiguration extConfig : configs) {
                    pids.add(extConfig.getPid());
                }
                if (attributeDef.getCardinality() > 0) {
                    actualValue = pids.toArray(new String[0]);
                } else {
                    actualValue = pids;
                }
                if (actualValue != null) {
                    context.setProperty(prefixedAttributeName, actualValue);
                }
                evaluateFinish(context);
                return actualValue;
            } catch (ConfigRetrieverException e) {
                throw new ConfigEvaluatorException("problem looking up configurations with factoryPid " + factoryPid);
            }
        } else if (isWildcardService(attributeDef)) {
            String service = attributeDef.getService();
            WildcardService ref = new WildcardService(service, attributeDef, context.getConfigElement().getConfigID());
            context.addUnresolvedReference(ref);
            try {
                ArrayList<String> pids = new ArrayList<String>();
                List<RegistryEntry> entries = metatypeRegistry.getEntriesExposingService(service);
                if (entries != null) {
                    for (RegistryEntry entry : entries) {
                        ExtendedConfiguration[] configs = configRetriever.findAllConfigurationsByPid(entry.getPid());
                        configs = configs == null ? new ExtendedConfiguration[0] : configs;
                        for (ExtendedConfiguration extConfig : configs) {
                            pids.add(extConfig.getPid());
                        }
                    }
                }
                if (attributeDef.getCardinality() > 0) {
                    actualValue = pids.toArray(new String[0]);
                } else {
                    actualValue = pids;
                }
                if (actualValue != null) {
                    context.setProperty(prefixedAttributeName, actualValue);
                }
                evaluateFinish(context);
                return actualValue;
            } catch (ConfigRetrieverException e) {
                throw new ConfigEvaluatorException("problem looking up configurations with service " + service);
            }
        }

        if (rawValue == null) {
            rawValue = getUnconfiguredAttributeValue(context, attributeDef);
        }

        // Process any list variables of the form ${list(variableName)}. We do this here separately from the other variable expression
        // processing so that the values are available prior to cardinality checks.
        if (rawValue != null) {
            rawValue = variableEvaluator.processVariableLists(rawValue, attributeDef, context, ignoreWarnings);
        }
        if (rawValue != null) {
            try {
                actualValue = evaluateMetaType(rawValue, attributeDef, context, ignoreWarnings);
            } catch (ConfigEvaluatorException iae) {
                // try to fall-back to default value if validation of an option fails.
                Object badValue = rawValue;
                rawValue = getUnconfiguredAttributeValue(context, attributeDef);

                //ignoreWarnings indicates that we are trying to get the default or variable value here and do not
                //care about any validation failures.

                String validOptions[] = attributeDef.getOptionValues();
                if (validOptions == null) {
                    if (rawValue != null && !ignoreWarnings) {
                        Tr.warning(tc, "warn.config.validate.failed", iae.getMessage());
                        Tr.warning(tc, "warn.config.invalid.using.default.value", attributeDef.getID(), badValue, rawValue);
                        actualValue = evaluateMetaType(rawValue, attributeDef, context, ignoreWarnings);
                    } else {
                        // Either (1) There is no default so we have to throw an exception or (2) We are trying to get
                        // the default or variable value (ignoreWarnings == true) but it doesn't exist so we throw an
                        // exception.
                        throw iae;
                    }
                } else {
                    if (!ignoreWarnings) {
                        StringBuffer strBuffer = new StringBuffer();
                        // This formatter is consistent with the message in nlsprops
                        for (int i = 0; i < validOptions.length; i++) {
                            strBuffer.append("[");
                            strBuffer.append(validOptions[i]);
                            strBuffer.append("]");
                        }

                        String defaultString = "";
                        if (rawValue != null)
                            defaultString = Tr.formatMessage(tc, "default.value.in.use", rawValue);
                        Tr.warning(tc, "warn.config.invalid.value", attributeDef.getID(), badValue, strBuffer.toString(), defaultString);

                    }
                    if (rawValue != null) {
                        actualValue = evaluateMetaType(rawValue, attributeDef, context, ignoreWarnings);
                    }
                }
            }

            if (actualValue != null) {
                context.setProperty(prefixedAttributeName, actualValue);
            }
            evaluateFinish(context);
        }

        return actualValue;
    }

    boolean isWildcardReference(ExtendedAttributeDefinition attributeDef) {
        return attributeDef.getType() == MetaTypeFactory.PID_TYPE
               && attributeDef.getDefaultValue() != null
               && attributeDef.getDefaultValue().length > 0
               && ALL_PIDS.equals(attributeDef.getDefaultValue()[0])
               && attributeDef.getReferencePid() != null;
    }

    boolean isWildcardService(ExtendedAttributeDefinition attributeDef) {
        return attributeDef.getType() == MetaTypeFactory.PID_TYPE
               && attributeDef.getDefaultValue() != null
               && attributeDef.getDefaultValue().length > 0
               && ALL_PIDS.equals(attributeDef.getDefaultValue()[0])
               && attributeDef.getService() != null;
    }

    /**
     * Merge references and nested elements prior to evaluation.
     */
    private Object mergeReferenceAttributes(String attributeName, EvaluationContext context, ExtendedAttributeDefinition attributeDef, ConfigElement config, Object rawValues) {
        Object rawValue;
        String[] referenceAttributes = getReferenceAttributes(attributeName);
        context.addProcessed(referenceAttributes[0]);
        context.addProcessed(referenceAttributes[1]);
        // get value for the nested element
        rawValue = config.getAttribute(referenceAttributes[1]);
        if (rawValue == null) {
            // if no nested element, then lookup the attribute
            rawValue = config.getAttribute(referenceAttributes[0]);
        } else if (attributeDef.getCardinality() != 0) {
            // we have nested element and attribute allows multiple
            // we must merge if attribute is non null
            Object refRawValue = config.getAttribute(referenceAttributes[0]);
            if (refRawValue != null) {
                rawValue = mergeReferenceValues(attributeDef, refRawValue, rawValue);
            }
        } else {
            // nested value wins if cardinality == 0
        }
        if (rawValues == null) {
            return rawValue;
        }
        if (rawValue == null || attributeDef.getCardinality() == 0) {
            return rawValues;
        }
        return mergeReferenceValues(attributeDef, rawValues, rawValue);
    }

    private void evaluateFlatAttribute(String attributeName, String elementName, EvaluationContext context, RegistryEntry nestedRegistryEntry, String flatPrefix,
                                       ConfigElement config, AtomicInteger i, Set<String> processedNames, boolean ignoreWarnings) throws ConfigEvaluatorException {
        if (processedNames.contains(elementName)) {
            return;
        }
        processedNames.add(elementName);

        Object rawValue;
        rawValue = config.getAttribute(elementName);
        if (rawValue == null) {
            return;
        }
        if (!!!(rawValue instanceof List)) {
            //TODO error
            return;
        }
        @SuppressWarnings("unchecked")
        List<Object> rawList = (List<Object>) rawValue;
        int cardinality = context.getAttributeDefinition(attributeName).getCardinality();
        if ((rawList.size()) > 1 && (-1 <= cardinality) && (cardinality <= 1)) {
            SimpleElement element = mergeConfigElementValues(rawList, context);
            flattenConfigElement(element, flatPrefix, i, attributeName, elementName, context, nestedRegistryEntry, ignoreWarnings);
        } else {
            for (Object o : rawList) {
                if (!(o instanceof ConfigElement)) {
                    //TODO error
                    continue;
                }
                flattenConfigElement((ConfigElement) o, flatPrefix, i, attributeName, elementName, context, nestedRegistryEntry, ignoreWarnings);
            }
        }
        return;
    }

    private void flattenConfigElement(ConfigElement nestedElement, String flatPrefix, AtomicInteger i, String attributeName, String elementName, EvaluationContext context,
                                      RegistryEntry nestedRegistryEntry, boolean ignoreWarnings) throws ConfigEvaluatorException {
        String subPrefix = flatPrefix + attributeName + "." + i.get() + ".";
        context.addProcessed(elementName);
        EvaluationResult result = context.getEvaluationResult();
        EvaluationResult nestedResult = evaluate(nestedElement, nestedRegistryEntry, subPrefix, ignoreWarnings);
        if (nestedResult.isValid()) {
            i.incrementAndGet();
            Dictionary<String, Object> nestedProperties = nestedResult.getProperties();
            context.setProperty(subPrefix + "config.referenceType", nestedRegistryEntry.getPid());
            for (Enumeration<String> keys = nestedProperties.keys(); keys.hasMoreElements();) {
                String key = keys.nextElement();
                Object value = nestedProperties.get(key);
                context.setProperty(key, value);
            }
            for (Map.Entry<ConfigID, EvaluationResult> entry : nestedResult.getNested().entrySet()) {
                result.addNested(entry.getKey(), entry.getValue());
            }
        }
        for (UnresolvedPidType child : nestedResult.getUnresolvedReferences()) {
            if (child instanceof UnresolvedReference) {
                UnresolvedReference childRef = (UnresolvedReference) child;
                result.addUnresolvedReference(new UnresolvedReference(childRef.getPid(), childRef.getAttributeDefinition(), childRef.value, context.getConfigElement().getConfigID()));
            } else if (child instanceof UnresolvedService) {
                UnresolvedService filter = (UnresolvedService) child;
                result.addUnresolvedReference(new UnresolvedService(filter.getService(), filter.getAttributeDefinition(), filter.value, context.getConfigElement().getConfigID(), filter.getCount()));
            }

        }
        return;
    }

    private void evaluateFlatReference(final String[] attributeName,
                                       final EvaluationContext context,
                                       RegistryEntry nestedRegistryEntry,
                                       final String flatPrefix,
                                       ConfigElement config,
                                       final AtomicInteger i, final Set<String> processedNames,
                                       final boolean ignoreWarnings, ExtendedAttributeDefinition attributeDef) throws ConfigEvaluatorException {
        String refName = null;
        Object rawValue = null;
        for (String attName : attributeName) {
            rawValue = config.getAttribute(attName);
            if (rawValue instanceof String) {
                refName = attName;
                break;
            }
        }
        if (refName != null) {
            processedNames.add(attributeName[0]);
            processedNames.add(attributeName[1]);
            ServerConfiguration serverConfiguration = serverXMLConfig.getConfiguration();
            List<String> ids = getAsList((String) rawValue, attributeDef);
            for (String id : ids) {
                final ConfigElement refElement;
                try {
                    //TODO  refines this is wrong, will not pick up refining or refined stuff, need to traverse hierarchy?
                    refElement = serverConfiguration.getFactoryInstance(nestedRegistryEntry.getPid(), nestedRegistryEntry.getAlias(), id);
                } catch (ConfigMergeException e) {
                    throw new ConfigEvaluatorException(e);
                }
                if (refElement == null) {
                    //TODO use RegistryEntry.... make it available.
//                    new UnresolvedReference(context.getRegistryEntry(), attributeDef, null, config.getConfigID()).reportError();
                    new UnresolvedReference(null, attributeDef, null, config.getConfigID()).reportError();
                    continue;
                }
                final String elementName = refElement.getNodeName();
                ConfigEvaluatorException e = nestedRegistryEntry.traverseHierarchyWithRoot(new EntryAction<ConfigEvaluatorException>() {
                    private ConfigEvaluatorException result;

                    @Override
                    public boolean entry(RegistryEntry registryEntry) {
                        if (elementName.equals(registryEntry.getPid()) || elementName.equals(registryEntry.getAlias())) {
                            context.addProcessed(attributeName[0]);
                            try {
                                flattenConfigElement(refElement, flatPrefix, i, attributeName[1], elementName, context,
                                                     registryEntry,
                                                     ignoreWarnings);
                            } catch (ConfigEvaluatorException e) {
                                result = e;
                                return false;
                            }
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public ConfigEvaluatorException getResult() {
                        return result;
                    }
                });
                if (e != null) {
                    throw e;
                }
            }
        }
    }

    /**
     * Try to determine a value from variable or defaults for the AD.
     *
     * @param attributeDef
     * @param context
     * @return
     * @throws ConfigEvaluatorException
     */
    private Object getUnconfiguredAttributeValue(EvaluationContext context, ExtendedAttributeDefinition attributeDef) throws ConfigEvaluatorException {

        Object rawValue = null;

        // no value in config then try ibm:variable if set

        rawValue = variableEvaluator.lookupVariableExtension(context, attributeDef);

        // no value in config, no ibm:variable, try defaults
        if (rawValue == null) {
            String[] defaultValues = attributeDef.getDefaultValue();
            if (defaultValues != null) {
                rawValue = Arrays.asList(defaultValues);
            }
        }

        return rawValue;
    }

    private void evaluateFinish(EvaluationContext context) throws ConfigEvaluatorException {
        // Post Processing of Evaluation Context

        // Step 1: Evaluate nested elements

        for (NestedInfo nestedInfo : context.getNestedInfo()) {
            RegistryEntry registryEntry = nestedInfo.registryEntry;
            ConfigElement configElement = nestedInfo.configElement;

            EvaluationResult nestedResult = evaluate(configElement, registryEntry);
            nestedResult.setPid(nestedInfo.pid);

            if (nestedResult.isValid()) {
                EvaluationResult result = context.getEvaluationResult();
                result.addNested(configElement.getConfigID(), nestedResult);
            }
        }
        context.getNestedInfo().clear();

    }

    private List<?> mergeValues(List<?> values, ExtendedAttributeDefinition attrDef, EvaluationContext context) throws ConfigEvaluatorException {
        int size = values.size();
        if (size == 1) {
            return values;
        } else {
            // merge into a single ConfigElement if rawValues is a list of ConfigElements
            // otherwise, just evaluate the first value in the list
            SimpleElement configElement = mergeConfigElementValues(values, context);
            if (configElement == null) {
                // TODO: Warn? validateCardinality is currently a no-op for single-value
                return Collections.singletonList(values.get(0));
            } else {
                return Collections.singletonList(configElement);
            }
        }
    }

    private Object evaluateMetaType(Object value, ExtendedAttributeDefinition attrDef, EvaluationContext context, boolean ignoreWarnings) throws ConfigEvaluatorException {
        Object convertedValue = null;

        /**
         * x = Integer.MIN_VALUE no limit, but use Vector
         * x < 0 -x = max occurrences, store in Vector
         * x > 0 x = max occurrences, store in array []
         * x = Integer.MAX_VALUE no limit, but use array []
         * x = 0 1 occurrence required
         */
        int cardinality = attrDef.getCardinality();

        if (cardinality == 0) { // single value
            if (value instanceof String) {
                convertedValue = convertObjectToSingleValue(value, attrDef, context, -1, ignoreWarnings);
            } else if (value instanceof List) {
                List<?> values = (List<?>) value;
                validateCardinality(values, attrDef);
                convertedValue = convertListToSingleValue(values, attrDef, context, ignoreWarnings);
            } else {
                throw new IllegalStateException("Unsupported type: " + value.getClass());
            }
        } else if ((cardinality == -1) || (cardinality == 1)) {
            // We treat -1 and 1 as if they are 0 for the purposes of merging together elements. However, we have historically treated array/vector value
            // parsing of string values differently than single values. I'm maintaining that here, which means we call parseValue() to convert the string
            // first before converting to a single value.
            if (value instanceof String) {
                List<?> values = MetaTypeHelper.parseValue((String) value);
                values = mergeValues(values, attrDef, context);
                if (cardinality == -1) {
                    convertedValue = convertListToVector(values, attrDef, context, ignoreWarnings);
                } else {
                    convertedValue = convertListToArray(values, attrDef, context, ignoreWarnings);
                }
            } else if (value instanceof List) {
                List<?> values = (List<?>) value;
                validateCardinality(values, attrDef);
                values = mergeValues(values, attrDef, context);
                if (cardinality == -1) {
                    convertedValue = convertListToVector(values, attrDef, context, ignoreWarnings);
                } else {
                    convertedValue = convertListToArray(values, attrDef, context, ignoreWarnings);
                }
            } else {
                throw new IllegalStateException("Unsupported type: " + value.getClass());
            }

        } else {
            List<?> values;
            if (value instanceof String) {
                values = getAsList((String) value, attrDef);
            } else if (value instanceof List) {
                values = (List<?>) value;
            } else {
                throw new IllegalStateException("Unsupported type: " + value.getClass());
            }

            validateCardinality(values, attrDef);

            if (cardinality < 0) {
                convertedValue = convertListToVector(values, attrDef, context, ignoreWarnings);
            } else {
                convertedValue = convertListToArray(values, attrDef, context, ignoreWarnings);
            }
        }

        return convertedValue;
    }

    /**
     * @param values
     * @param attrDef
     * @throws ConfigEvaluatorException
     */
    private void validateCardinality(List<?> values, ExtendedAttributeDefinition attrDef) throws ConfigEvaluatorException {
        int maxSize = attrDef.getCardinality() < 1 ? (0 - attrDef.getCardinality()) : attrDef.getCardinality();
        // Can't directly get absolute value of Integer.MIN_VALUE
        maxSize = maxSize == Integer.MIN_VALUE ? Integer.MAX_VALUE : maxSize;

        // Cardinality 0 means 1 element. For some reason we have decided that this means we should merge multiple elements
        // with distinct IDs together rather than properly issuing a validation warning, so just return.
        if ((-1 <= maxSize) && (maxSize <= 1))
            return;

        // If the list size is maxSize or less, we're fine
        if (values.size() <= maxSize)
            return;

        // Non-PID types handled elsewhere
        if (attrDef.getType() == MetaTypeFactory.PID_TYPE) {

            // If this is a singleton, it will be merged
            RegistryEntry entry = metatypeRegistry.getRegistryEntry(attrDef.getReferencePid());
            if (entry != null && entry.isSingleton())
                return;

            // This is a factory. Count the number of distinct IDs. Count each null as distinct
            Set<String> ids = new HashSet<String>();
            int nullIds = 0;
            for (Object value : values) {
                if (value instanceof ConfigElement) {
                    ConfigElement element = (ConfigElement) value;
                    if (element.getId() == null)
                        nullIds++;
                    else
                        ids.add(element.getId());
                } else if (value instanceof String) {
                    ids.add(value.toString());
                }

            }

            // Total number of distinct children
            int size = ids.size() + nullIds;

            if (size > maxSize)
                throw new ConfigEvaluatorException("Attribute " + attrDef.getID() + " exceeded maximum allowed size " + maxSize);
        } else {
            // Simple Validation
            if (values.size() > maxSize)
                throw new ConfigEvaluatorException("Attribute " + attrDef.getID() + " exceeded maximum allowed size " + maxSize);
        }

    }

    private boolean isHiddenExtension(ExtendedAttributeDefinition attrDef, EvaluationContext context) {
        if (attrDef == null) {
            RegistryEntry registryEntry = context.getEvaluationResult().getRegistryEntry();
            return registryEntry != null && registryEntry.supportsHiddenExtensions();
        }
        return false;
    }

    /**
     * Process and evaluate a raw value.
     *
     * @return the converted value, or null if the value was unresolved
     */
    @Trivial
    private Object convertObjectToSingleValue(Object rawValue, ExtendedAttributeDefinition attrDef, EvaluationContext context, int index,
                                              boolean ignoreWarnings) throws ConfigEvaluatorException {
        if (rawValue instanceof String) {
            return convertStringToSingleValue((String) rawValue, attrDef, context, ignoreWarnings);
        } else if (rawValue instanceof ConfigElement.Reference) {
            ConfigElement.Reference reference = processReference((ConfigElement.Reference) rawValue, context, ignoreWarnings);
            if (attrDef != null && attrDef.getType() == MetaTypeFactory.PID_TYPE) {
                return evaluateReference(reference.getId(), attrDef, context);
            } else {
                return evaluateReference(reference, context);
            }
        } else if (rawValue instanceof ConfigElement) {
            return convertConfigElementToSingleValue((ConfigElement) rawValue, attrDef, context, index, ignoreWarnings);
        } else {
            throw new IllegalStateException("Unsupported type: " + rawValue.getClass());
        }
    }

    /**
     * Resolve a raw value to a String, and process it so that it can be evaluated.
     */
    private String resolveStringValue(Object rawValue, ExtendedAttributeDefinition attrDef, EvaluationContext context, boolean ignoreWarnings) throws ConfigEvaluatorException {
        if (rawValue instanceof String) {
            return processString((String) rawValue, attrDef, context, ignoreWarnings);
        } else if (rawValue instanceof ConfigElement) {
            // Like convertConfigElementToSingleValue, handle elements with
            // attributes: <top><bool attr="ignored">true></bool></top>
            return processString(((ConfigElement) rawValue).getElementValue(), attrDef, context, ignoreWarnings);
        } else {
            throw new IllegalStateException("Attribute type mismatch. Expected String type, got " + rawValue);
        }
    }

    /**
     * Process and evaluate a raw String value.
     */
    @Trivial
    private Object convertStringToSingleValue(String rawValue, ExtendedAttributeDefinition attrDef, EvaluationContext context,
                                              boolean ignoreWarnings) throws ConfigEvaluatorException {
        String value = processString(rawValue, attrDef, context, ignoreWarnings);
        return evaluateString(value, attrDef, context);
    }

    /**
     * Evaluate a string to a value of the target attribute type.
     *
     * @see #convertListToArray
     */
    @Trivial
    private Object evaluateString(String strVal, ExtendedAttributeDefinition attrDef, EvaluationContext context) throws ConfigEvaluatorException {
        if (attrDef == null) {
            return strVal;
        }

        int type = attrDef.getType();
        if (type == AttributeDefinition.BOOLEAN) {
            return Boolean.valueOf(strVal);
        } else if (type == AttributeDefinition.BYTE) {
            return Byte.valueOf(strVal);
        } else if (type == AttributeDefinition.CHARACTER) {
            return Character.valueOf(strVal.charAt(0));
        } else if (type == AttributeDefinition.DOUBLE) {
            return Double.valueOf(strVal);
        } else if (type == AttributeDefinition.FLOAT) {
            return Float.valueOf(strVal);
        } else if (type == AttributeDefinition.INTEGER) {
            return Integer.valueOf(strVal);
        } else if (type == AttributeDefinition.LONG) {
            return Long.valueOf(strVal);
        } else if (type == AttributeDefinition.SHORT) {
            return Short.valueOf(strVal);
        } else if (type == MetaTypeFactory.DURATION_TYPE) {
            return MetatypeUtils.evaluateDuration(strVal, TimeUnit.MILLISECONDS);
        } else if (type == MetaTypeFactory.DURATION_S_TYPE) {
            return MetatypeUtils.evaluateDuration(strVal, TimeUnit.SECONDS);
        } else if (type == MetaTypeFactory.DURATION_M_TYPE) {
            return MetatypeUtils.evaluateDuration(strVal, TimeUnit.MINUTES);
        } else if (type == MetaTypeFactory.DURATION_H_TYPE) {
            return MetatypeUtils.evaluateDuration(strVal, TimeUnit.HOURS);
        } else if (type == MetaTypeFactory.PASSWORD_TYPE || type == MetaTypeFactory.HASHED_PASSWORD_TYPE || attrDef.isObscured()) {
            return new SerializableProtectedString(strVal.toCharArray());
        } else if (type == MetaTypeFactory.ON_ERROR_TYPE) {
            return Enum.valueOf(OnError.class, strVal.trim().toUpperCase());
        } else if (type == MetaTypeFactory.TOKEN_TYPE) {
            return MetatypeUtils.evaluateToken(strVal);
        } else if (type == MetaTypeFactory.PID_TYPE) {
            return evaluateReference(strVal, attrDef, context);
        } else {
            // STRING and all other unknown/invalid types.
            return strVal;
        }
    }

    private Object convertListToSingleValue(List<?> rawValues, ExtendedAttributeDefinition attrDef, EvaluationContext context,
                                            boolean ignoreWarnings) throws ConfigEvaluatorException {
        int size = rawValues.size();
        if (size == 1) {
            return convertObjectToSingleValue(rawValues.get(0), attrDef, context, -1, ignoreWarnings);
        } else {
            // merge into a single ConfigElement if rawValues is a list of ConfigElements
            // otherwise, just evaluate the first value in the list
            SimpleElement configElement = mergeConfigElementValues(rawValues, context);
            if (configElement == null) {
                // TODO: Warn? validateCardinality is currently a no-op for single-value
                return convertObjectToSingleValue(rawValues.get(0), attrDef, context, -1, ignoreWarnings);
            } else {
                return convertConfigElementToSingleValue(configElement, attrDef, context, -1, ignoreWarnings);
            }
        }
    }

    /*
     * String[0] is always name + "Ref", and String[1] is always name.
     */
    private static String[] getReferenceAttributes(String name) {
        if (name.endsWith(XMLConfigConstants.CFG_REFERENCE_SUFFIX)) {
            return new String[] { name, name.substring(0, name.length() - XMLConfigConstants.CFG_REFERENCE_SUFFIX.length()) };
        } else {
            return new String[] { name + XMLConfigConstants.CFG_REFERENCE_SUFFIX, name };
        }
    }

    /*
     * Only called when ibm:type=pid and cardinality != 0
     */
    private static List<Object> mergeReferenceValues(AttributeDefinition attrDef, Object... values) {
        List<Object> merged = new ArrayList<Object>();
        for (Object value : values) {
            if (value instanceof String) {
                merged.addAll(getAsList((String) value, attrDef));
            } else if (value instanceof List<?>) {
                for (Object ce : (List<?>) value) {
                    if (ce instanceof ConfigElement) {
                        // Get the ID for this ConfigElement. If it's specified, remove the ID from the merged list.
                        // This gets rid of ref attributes that point to a nested element
                        String id = (String) ((ConfigElement) ce).getAttribute("id");
                        if (id != null) {
                            merged.remove(id);
                        }
                        merged.add(ce);
                    } else if (ce instanceof ConfigElement.Reference) {
                        merged.add(ce);
                    } else if (ce instanceof String) {
                        if (!merged.contains(ce))
                            merged.add(ce);
                    }
                }

            }
        }

        return merged;
    }

    private static List<String> getAsList(String rawValue, AttributeDefinition attrDef) {
        if (attrDef.getType() == MetaTypeFactory.PID_TYPE) {
            String[] pids = rawValue.split("\\s*,\\s*");
            return Arrays.asList(pids);
        } else {
            return MetaTypeHelper.parseValue(rawValue);
        }
    }

    /**
     * @return the vector of converted values, or null if all values were hidden
     */
    private Vector<Object> convertListToVector(List<?> rawValues, ExtendedAttributeDefinition attrDef, EvaluationContext context,
                                               boolean ignoreWarnings) throws ConfigEvaluatorException {
        int size = rawValues.size();
        boolean hiddenAttr = isHiddenExtension(attrDef, context);
        boolean hiddenValue = false;

        Vector<Object> vector = new Vector<Object>(size);
        for (int i = 0; i < size; i++) {
            Object rawValue = rawValues.get(i);
            Object value = convertObjectToSingleValue(rawValue, attrDef, context, i, ignoreWarnings);

            if (hiddenAttr && rawValue instanceof ConfigElement) {
                hiddenValue = true;
            } else if (value != null) {
                vector.add(value);
            }
        }

        if (vector.isEmpty() && hiddenValue) {
            return null;
        }
        return vector;
    }

    /**
     * Converts a list of raw configuration values to a string array.
     *
     * @param attrDef a string-based attribute definition, or null if this is a simple evaluation
     * @return the array of converted values, or null if all values are unresolved
     */
    private String[] convertListToStringArray(List<?> rawValues, ExtendedAttributeDefinition attrDef, EvaluationContext context,
                                              boolean ignoreWarnings) throws ConfigEvaluatorException {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Collection<String> collection = (Collection) convertListToVector(rawValues, attrDef, context, ignoreWarnings);
        return collection == null ? null : collection.toArray(new String[collection.size()]);
    }

    /**
     * Evaluate a list of values to an array of the target attribute type.
     *
     * @see #evaluateString
     */
    private Object convertListToArray(List<?> rawValues, ExtendedAttributeDefinition attrDef, EvaluationContext context, boolean ignoreWarnings) throws ConfigEvaluatorException {
        // The implementation of this method is similar to convertListToVector,
        // but this method needs to handle primitive array types.  This could
        // be implemented by calling convertListToVector first, but that would
        // require boxing/unboxing the primitive values.  To avoid that, the
        // logic from the convertListToVector and convertObjectToSingleValue
        // methods is inlined for the primitive array types.

        int size = rawValues.size();
        int type = attrDef.getType();
        int i = 0;
        if (type == AttributeDefinition.BOOLEAN) {
            boolean[] retVal = new boolean[size];
            for (Object rawValue : rawValues) {
                String value = resolveStringValue(rawValue, attrDef, context, ignoreWarnings);
                retVal[i++] = Boolean.parseBoolean(value);
            }
            return retVal;
        } else if (type == AttributeDefinition.BYTE) {
            byte[] retVal = new byte[size];
            for (Object rawValue : rawValues) {
                String value = resolveStringValue(rawValue, attrDef, context, ignoreWarnings);
                retVal[i++] = Byte.parseByte(value);
            }
            return retVal;
        } else if (type == AttributeDefinition.CHARACTER) {
            char[] retVal = new char[size];
            for (Object rawValue : rawValues) {
                String value = resolveStringValue(rawValue, attrDef, context, ignoreWarnings);
                retVal[i++] = value.charAt(0);
            }
            return retVal;
        } else if (type == AttributeDefinition.DOUBLE) {
            double[] retVal = new double[size];
            for (Object rawValue : rawValues) {
                String value = resolveStringValue(rawValue, attrDef, context, ignoreWarnings);
                retVal[i++] = Double.parseDouble(value);
            }
            return retVal;
        } else if (type == AttributeDefinition.FLOAT) {
            float[] retVal = new float[size];
            for (Object rawValue : rawValues) {
                String value = resolveStringValue(rawValue, attrDef, context, ignoreWarnings);
                retVal[i++] = Float.parseFloat(value);
            }
            return retVal;
        } else if (type == AttributeDefinition.INTEGER) {
            int[] retVal = new int[size];
            for (Object rawValue : rawValues) {
                String value = resolveStringValue(rawValue, attrDef, context, ignoreWarnings);
                retVal[i++] = Integer.parseInt(value);
            }
            return retVal;
        } else if (type == AttributeDefinition.LONG) {
            long[] retVal = new long[size];
            for (Object rawValue : rawValues) {
                String value = resolveStringValue(rawValue, attrDef, context, ignoreWarnings);
                retVal[i++] = Long.parseLong(value);
            }
            return retVal;
        } else if (type == AttributeDefinition.SHORT) {
            short[] retVal = new short[size];
            for (Object rawValue : rawValues) {
                String value = resolveStringValue(rawValue, attrDef, context, ignoreWarnings);
                retVal[i++] = Short.parseShort(value);
            }
            return retVal;
        } else if (type == MetaTypeFactory.DURATION_TYPE) {
            return convertListToDurationArray(rawValues, attrDef, context, TimeUnit.MILLISECONDS, ignoreWarnings);
        } else if (type == MetaTypeFactory.DURATION_S_TYPE) {
            return convertListToDurationArray(rawValues, attrDef, context, TimeUnit.SECONDS, ignoreWarnings);
        } else if (type == MetaTypeFactory.DURATION_M_TYPE) {
            return convertListToDurationArray(rawValues, attrDef, context, TimeUnit.MINUTES, ignoreWarnings);
        } else if (type == MetaTypeFactory.DURATION_H_TYPE) {
            return convertListToDurationArray(rawValues, attrDef, context, TimeUnit.HOURS, ignoreWarnings);
        } else {
            // STRING, PID_TYPE, TOKEN_TYPE, and all other unknown/invalid types.
            return convertListToStringArray(rawValues, attrDef, context, ignoreWarnings);
        }
    }

    private long[] convertListToDurationArray(List<?> rawValues, ExtendedAttributeDefinition attrDef, EvaluationContext context, TimeUnit timeUnit,
                                              boolean ignoreWarnings) throws ConfigEvaluatorException {
        long[] retVal = new long[rawValues.size()];
        int i = 0;
        for (Object rawValue : rawValues) {
            String value = resolveStringValue(rawValue, attrDef, context, ignoreWarnings);
            retVal[i++] = MetatypeUtils.evaluateDuration(value, timeUnit);
        }
        return retVal;
    }

    // this is out-of-line for unit tests to override
    protected String lookupPid(ConfigID referenceId) {
        return configRetriever.lookupPid(referenceId);
    }

    // this is out-of-line for unit tests to override
    protected String getPid(ConfigID configId) throws ConfigNotFoundException {
        return configRetriever.getPid(configId);
    }

    // This method evaluates references that come from reference attributes (eg, privateLibraryRef="myLib"), not nested
    // elements.
    private String evaluateReference(String id, ExtendedAttributeDefinition attrDef, EvaluationContext context) throws ConfigEvaluatorException {

        if (attrDef.getReferencePid() != null) {
            String referencePid = attrDef.getReferencePid();
            if (metatypeRegistry.getRegistryEntry(referencePid) == null) {
                //We don't know about this reference pid.  If it shows up later, we will reprocess this configuration
                return null;
            }
            ConfigID referenceId = new ConfigID(referencePid, id);
            String pid = lookupPid(referenceId);

            if (pid == null) {
                UnresolvedReference ref = new UnresolvedReference(referencePid, attrDef, id, context.getConfigElement().getConfigID());
                context.addUnresolvedReference(ref);
            }

            context.getEvaluationResult().addReference(referenceId);

            return pid;
        } else if (attrDef.getService() != null) {
            List<RegistryEntry> exposers = metatypeRegistry.getEntriesExposingService(attrDef.getService());
            List<String> matchedPids = new ArrayList<String>();
            if (exposers != null) {
                if (attrDef.getServiceFilter() == null) {
                    for (RegistryEntry entry : exposers) {
                        ConfigID referenceId = new ConfigID(entry.getPid(), id);
                        context.getEvaluationResult().addReference(referenceId);//what?
                        String pid = lookupPid(referenceId);
                        if (pid != null)
                            matchedPids.add(pid);
                    }
                } else {
                    String idClause = FilterUtils.createPropertyFilter("id", id);
                    for (RegistryEntry entry : exposers) {
                        String pidClause = FilterUtils.createPropertyFilter(ConfigurationAdmin.SERVICE_FACTORYPID, entry.getPid());
                        String filter = "(&" + pidClause + idClause + attrDef.getServiceFilter() + ")";
                        try {
                            Configuration[] configs = configRetriever.listConfigurations(filter);
                            if (configs != null)
                                for (Configuration config : configs) {
                                    matchedPids.add(config.getPid());
                                }
                        } catch (ConfigRetrieverException e) {
                            throw new ConfigEvaluatorException("Failed retrieving configuration for " + attrDef, e);
                        }
                    }
                }
            }
            if (matchedPids.size() == 1) {
                return matchedPids.get(0);
            } else {
                // If matchedPids > 1, there is more than one possible reference.
                // If matchedPids == 0, there are no resolved instances yet.
                // In either case, add it to the unresolved list to check later
                UnresolvedPidType ref = new UnresolvedService(attrDef.getService(), attrDef, id, context.getConfigElement().getConfigID(), matchedPids.size());
                context.addUnresolvedReference(ref);
                return null;
            }
        }

        throw new ConfigEvaluatorException("Reference pid is not defined for " + attrDef);
    }

    /**
     * @param attrDef the attribute definition, or null if this is a simple evaluation
     */
    @FFDCIgnore(URISyntaxException.class)
    @Trivial
    private String processString(String value, ExtendedAttributeDefinition attrDef, EvaluationContext context, boolean ignoreWarnings) throws ConfigEvaluatorException {

        if (attrDef == null) {
            return context.resolveString(value, ignoreWarnings);
        }

        String strValue = attrDef.resolveVariables() ? context.resolveString(value, ignoreWarnings) : value;

        // Normalize the value if this is a file or directory location type
        if (attrDef.getType() == MetaTypeFactory.LOCATION_DIR_TYPE ||
            attrDef.getType() == MetaTypeFactory.LOCATION_FILE_TYPE) {
            strValue = PathUtils.normalize(strValue);
        } else if (attrDef.getType() == MetaTypeFactory.LOCATION_TYPE) {
            // If it's the generic location type, it may be a URL or a file path.
            // If we can distinguish that this is a URL, don't do normalization.
            try {
                new URI(strValue);
            } catch (URISyntaxException e) {
                // assume this is a file path and normalize
                strValue = PathUtils.normalize(strValue);
            }
        }

        boolean optionFound = false;
        // Convert option value to correct case.
        String[] optionValues = attrDef.getOptionValues();
        if (optionValues != null) {
            for (int i = 0; i < optionValues.length; i++) {
                if (strValue.equalsIgnoreCase(optionValues[i])) {
                    strValue = optionValues[i];
                    optionFound = true;
                    break;
                }
            }
            // only get the labels if it isn't found in the values.  Usually it will be a value.
            if (!optionFound) {
                String[] optionLabels = attrDef.getOptionLabels();
                for (int i = 0; i < optionValues.length; i++) {
                    if (strValue.equalsIgnoreCase(optionLabels[i])) {
                        strValue = optionValues[i];
                        optionFound = true;
                        break;
                    }
                }
            }
        }

        // If it is an option and it was found then skip the validate since it isn't needed.
        if (!optionFound) {
            // Special case for Boolean, since we want to validate "true" or "false" and anything is fail to validate (rather than being treated as false)
            if (attrDef.getType() == AttributeDefinition.BOOLEAN) {
                if (!!!("true".equalsIgnoreCase(strValue))
                    && !!!("false".equalsIgnoreCase(strValue))) {
                    Object[] inserts = new Object[] { strValue, attrDef.getID(), "false" }; // "false" is the default default
                    if (attrDef.getDefaultValue() != null && attrDef.getDefaultValue().length > 0)
                        inserts[2] = attrDef.getDefaultValue()[0];
                    throw new AttributeValidationException(attrDef, strValue, Tr.formatMessage(tc, "error.invalid.boolean.attribute", inserts));
                }
            } else if (attrDef.getType() != MetaTypeFactory.PID_TYPE) {
                // The validate method treats whitespace, commas, and
                // backslashes specially, but we've already done all that
                // processing and just want to validate a single value, so
                // escape all the special characters.  (It might be less effort
                // to just reimplement the validate method entirely.)
                String validateResult = attrDef.validate(MetaTypeHelper.escapeValue(strValue));
                if (validateResult != null && validateResult.length() > 0) {
                    throw new AttributeValidationException(attrDef, strValue, validateResult);
                }
            }
        }

        return strValue;
    }

    /**
     * Note, the "reference" here is something of the form:
     * <parent>
     * <child ref="someOtherElement"/>
     * </parent>
     *
     * <otherElement id="someOtherElement"/>
     *
     * This is not called for ibm:type="pid" references.
     *
     * This method will resolve any variables in the ref attribute and return a new ConfigElement.Reference if anything changed.
     *
     * @param reference      The ConfigElement reference. Contains the element name and the ref attribute value
     * @param context
     * @param ignoreWarnings
     * @return
     * @throws ConfigEvaluatorException
     */
    private ConfigElement.Reference processReference(ConfigElement.Reference reference, EvaluationContext context, boolean ignoreWarnings) throws ConfigEvaluatorException {
        // Note, ref attributes don't correspond to an attribute definition, so we will always resolve variables (ie, there is no way to specify
        // substitution="deferred" for a ref attribute.
        String resolvedId = context.resolveString(reference.getId(), ignoreWarnings);
        if (reference.getId().equals(resolvedId)) {
            return reference;
        } else {
            return new ConfigElement.Reference(reference.getPid(), resolvedId);
        }
    }

    private String evaluateReference(ConfigElement.Reference reference, EvaluationContext context) {
        String factoryPid = resolvePid(reference.getPid());
        ConfigID referenceId = new ConfigID(factoryPid, reference.getId());
        String pid = lookupPid(referenceId);
        if (pid == null) {
            Tr.warning(tc, "warning.pid.not.found", context.getAttributeName(), reference.getId());
        }
        context.getEvaluationResult().addReference(referenceId);
        return pid;
    }

    private RegistryEntry getRegistryEntry(String pid) {
        return metatypeRegistry == null ? null : metatypeRegistry.getRegistryEntryByPidOrAlias(pid);
    }

    /**
     * Find the "super" PID from the <code>ibm:extends</code> attribute of the OCD for <code>pid</code>.
     *
     * @param pid must never be null
     * @return the super PID or <code>null</code> if no <code>ibm:extends</code> attribute could be found
     */
    private String getExtends(String pid) {
        RegistryEntry re = getRegistryEntry(pid);
        return re == null ? null : re.getExtends();
    }

    private String resolvePid(String pid) {
        RegistryEntry registryEntry = getRegistryEntry(pid);
        return (registryEntry == null) ? pid : registryEntry.getPid();
    }

    private SimpleElement mergeConfigElementValues(List<?> values, EvaluationContext context) throws ConfigEvaluatorException {
        SimpleElement merged = null;
        for (Object value : values) {
            if (value instanceof SimpleElement) {
                SimpleElement in = (SimpleElement) value;
                if (merged == null) {
                    merged = (SimpleElement) value;
                }

                merged.override(in);

            } else {
                return null;
            }
        }
        merged.setIdAttribute();
        return merged;
    }

    /**
     * Process a raw ConfigElement value and evaluate it.
     */
    private Object convertConfigElementToSingleValue(ConfigElement childElement, ExtendedAttributeDefinition attrDef, EvaluationContext context, int index,
                                                     boolean ignoreWarnings) throws ConfigEvaluatorException {
        // Handle elements with attributes for non-PID ADs.  For example,
        //     <top><string attr="ignored">value</string></top>
        // Note, if the <string> element had no attributes, the parser would
        // have added a String instead of a ConfigElement.
        if (attrDef != null && attrDef.getType() != MetaTypeFactory.PID_TYPE) {
            return convertStringToSingleValue(childElement.getElementValue(), attrDef, context, ignoreWarnings);
        }

        return evaluateConfigElement(childElement, attrDef, context, index);
    }

    /**
     * Creates a NestedInfo object containing the service pid, config element, and registry entry for a nested config
     * element. If this service pid has not been seen yet in this context, the pid value will be returned. Otherwise,
     * the method returns null.
     *
     * @param childElement    The nested element
     * @param parentAttribute The AttributeDefinition on the parent that points to the nested element. Null for child-first
     * @param context         The current context
     * @param index           the attribute index
     * @return the pid, or null if the element should be ignored
     * @throws ConfigEvaluatorException
     */
    private String evaluateConfigElement(ConfigElement childElement, final ExtendedAttributeDefinition parentAttribute, EvaluationContext context,
                                         int index) throws ConfigEvaluatorException {
        RegistryEntry childRegistryEntry = null;
        if (parentAttribute != null && parentAttribute.getType() == MetaTypeFactory.PID_TYPE) {
            final String elementName = childElement.getNodeName();
            RegistryEntry specifiedChildRegistryEntry = metatypeRegistry.getRegistryEntry(parentAttribute.getReferencePid());
            if (specifiedChildRegistryEntry == null) {
                //if the pid shows up an another update, we will reprocess it then
                return null;
            }
            childRegistryEntry = specifiedChildRegistryEntry.traverseHierarchy(new EntryAction<RegistryEntry>() {

                private RegistryEntry result;

                @Override
                public boolean entry(RegistryEntry registryEntry) {
                    //TODO pid? really?
                    //TODO the flat case does not assume that the attribute id is the actual prefix, due to possible renaming.
                    if (elementName.equals(registryEntry.getPid()) || elementName.equals(registryEntry.getEffectiveAD(parentAttribute.getID()))) {
                        result = registryEntry;
                        return false;
                    }
                    return true;
                }

                @Override
                public RegistryEntry getResult() {
                    return result;
                }
            });
            if (childRegistryEntry == null) {
                childRegistryEntry = specifiedChildRegistryEntry;
            }
        } else {
            childRegistryEntry = getRegistryEntryForChildFirstConfig(context.getEvaluationResult().getRegistryEntry(), childElement.getNodeName());
        }

        String factoryPid = childElement.getNodeName();
        if (childRegistryEntry != null) {
            factoryPid = childRegistryEntry.getPid();
        }

        ConfigElement nestedElement = childElement;
        if (childElement instanceof SimpleElement) {
            SimpleElement original = (SimpleElement) childElement;
            if (childRegistryEntry != null) {
                if (childRegistryEntry.isSingleton()) {
                    nestedElement = new SingletonElement(original, factoryPid);
                } else {
                    nestedElement = new FactoryElement(original, index, childRegistryEntry);
                }

                nestedElement.setIdAttribute();

            } else {
                original.setDefaultId(index);
                nestedElement = original;
            }

            nestedElement.setParent(context.getConfigElement());
        }

        ConfigID configId = nestedElement.getConfigID();

        String pid = null;
        try {
            pid = getPid(configId);
        } catch (ConfigNotFoundException ex) {
            throw new ConfigEvaluatorException("Could not obtain configuration for nested info", ex);
        }

        NestedInfo nestedInfo = new NestedInfo();
        nestedInfo.configElement = nestedElement;
        nestedInfo.pid = pid;
        nestedInfo.registryEntry = childRegistryEntry;

        if (!context.addNestedInfo(nestedInfo) || isHiddenExtension(parentAttribute, context)) {
            // context already knows about this element or current (parent) metatype excludes unknown elements
            return null;
        }

        context.getEvaluationResult().addReference(configId);
        return pid;
    }

    /**
     * This method can retrieve a registry entry for an element using an ibm:childAlias for a node name.
     *
     * @param parent
     * @param childNodeName This now seems to be the pid due to confusion about what "nodeName" might mean.
     * @return the registry entry for the child, or <code>null</code> if no match was found
     */
    private RegistryEntry getRegistryEntryForChildFirstConfig(final RegistryEntry parentEntry, String childNodeName) {
        if (parentEntry == null)
            return null;
        //This will work in the unlikely event that childNodeName is actually the xml element name
        RegistryEntry pe = parentEntry;
        while (pe != null) {
            // The parent entry must have ibm:supportsExtensions defined for ibm:childAlias to be valid here
            if (pe.getObjectClassDefinition().supportsExtensions()) {
                RegistryEntry childAliasEntry = metatypeRegistry.getRegistryEntry(pe.getPid(), childNodeName);
                if (childAliasEntry != null)
                    return childAliasEntry;
            }
            pe = pe.getExtendedRegistryEntry();
        }

        //This will work in the more likely scenario that the node name has been replaced by the pid.
        // We didn't find an entry for the child alias, try using the PID/Alias
        RegistryEntry childEntry = getRegistryEntry(childNodeName);
        if (childEntry == null)
            return null;

        // check whether the parent element matches the child's ibm:parentPid
        // or the parent ibm:extends an OCD matching the child's ibm:parentPid
        final String ibmParentPid = childEntry.getObjectClassDefinition().getParentPID();
        for (String s = parentEntry.getPid(); s != null; s = getExtends(s))
            if (s.equals(ibmParentPid))
                return childEntry;

        return null;
    }

    public static class EvaluationResult {

        private ConfigElement configElement;
        private Dictionary<String, Object> properties;
        private Set<ConfigID> references;
        private Map<ConfigID, EvaluationResult> nestedResults;
        private Map<String, Object> variables;
        private String pid;
        private boolean valid = true;
        private final Set<UnresolvedPidType> unresolvedReferences = new HashSet<UnresolvedPidType>();
        private final RegistryEntry registryEntry;

        public EvaluationResult(RegistryEntry registryEntry) {
            this.registryEntry = registryEntry;
        }

        public RegistryEntry getRegistryEntry() {
            return this.registryEntry;
        }

        /**
         * @param attributeDef
         * @return
         */
        public boolean hasUnresolvedReference(AttributeDefinition attributeDef) {
            if (attributeDef.getType() != MetaTypeFactory.PID_TYPE)
                return false;

            for (UnresolvedPidType ref : unresolvedReferences) {
                if (attributeDef.equals(ref.getAttributeDefinition()))
                    return true;
            }
            return false;
        }

        /**
         * @param b
         */
        public void setValid(boolean b) {
            this.valid = b;

        }

        /**
         * @return
         */
        public boolean isValid() {
            return this.valid;
        }

        /**
         * @param referencePid
         * @param value
         * @param attrID
         */
        protected void addUnresolvedReference(UnresolvedPidType ref) {
            this.unresolvedReferences.add(ref);
        }

        protected void addReference(ConfigID referenceId) {
            if (references == null) {
                references = new HashSet<ConfigID>();
            }
            references.add(referenceId);
        }

        public Set<UnresolvedPidType> getUnresolvedReferences() {
            return this.unresolvedReferences;
        }

        public Set<ConfigID> getReferences() {
            return (references == null) ? Collections.<ConfigID> emptySet() : references;
        }

        protected void addNested(ConfigID configId, EvaluationResult result) {
            if (nestedResults == null) {
                nestedResults = new HashMap<ConfigID, EvaluationResult>();
            }
            nestedResults.put(configId, result);
        }

        public Map<ConfigID, EvaluationResult> getNested() {
            return (nestedResults == null) ? Collections.<ConfigID, EvaluationResult> emptyMap() : nestedResults;
        }

        protected void setConfigElement(ConfigElement configElement) {
            this.configElement = configElement;
        }

        public ConfigElement getConfigElement() {
            return configElement;
        }

        protected void setProperties(Dictionary<String, Object> properties) {
            this.properties = properties;
        }

        public Dictionary<String, Object> getProperties() {
            return properties;
        }

        protected void setVariables(Map<String, Object> variables) {
            this.variables = variables;
        }

        public Map<String, Object> getVariables() {
            return (variables == null) ? Collections.<String, Object> emptyMap() : variables;
        }

        protected void setPid(String pid) {
            this.pid = pid;
        }

        public String getPid() {
            return pid;
        }

        /**
         * @return
         */
        public Set<UnresolvedPidType> getAllUnresolvedReferences() {
            Set<UnresolvedPidType> allUnresolved = new HashSet<UnresolvedPidType>();
            allUnresolved.addAll(unresolvedReferences);
            if (nestedResults != null) {
                for (Map.Entry<ConfigID, EvaluationResult> entry : nestedResults.entrySet()) {
                    allUnresolved.addAll(entry.getValue().getAllUnresolvedReferences());
                }
            }
            return allUnresolved;
        }

    }

    protected abstract class UnresolvedPidType {
        protected final String value;
        protected final AttributeDefinition attribute;
        protected final ConfigID elementId;

        public UnresolvedPidType(AttributeDefinition attr, String id, ConfigID referringID) {
            this.attribute = attr;
            this.value = id;
            this.elementId = referringID;
        }

        public AttributeDefinition getAttributeDefinition() {
            return this.attribute;
        }

        public abstract void reportError();

        /**
         * @return
         */
        private ExtendedConfiguration getReferringConfiguration(ConfigElement element) {
            if (element == null)
                return null;
            RegistryEntry entry = metatypeRegistry.getRegistryEntry(element.getConfigID().getPid());

            // The metatype will not exist if we removed the metatype after adding an unresolved reference. This
            // happens when you remove a feature and removing something it has a metatype reference to at the same
            // time. Just return null here -- we will ignore the reference upstream.
            if (entry == null)
                return null;

            if (entry.isFactory()) {
                return configRetriever.lookupConfiguration(element.getConfigID());
            } else {
                try {
                    return configRetriever.getConfiguration(element.getConfigID());
                } catch (ConfigNotFoundException e) {
                    e.getStackTrace();
                    return null;
                }
            }
        }

        public ConfigurationInfo getReferringConfigurationInfo() throws ConfigMergeException {

            RegistryEntry entry = metatypeRegistry.getRegistryEntry(elementId.getPid());
            if (entry == null) {
                // The metatype has gone away, so we don't have a reference any more. Return null
                // so that we can ignore this upstream.
                return null;
            }

            // Get the current referring element from the configuration. We can't cache this because it
            // can change if new default instances get added by a late arriving bundle.
            ConfigElement referringElement = getReferringElement(entry, elementId);

            ExtendedConfiguration referringConfiguration = getReferringConfiguration(referringElement);
            if (referringConfiguration == null)
                return null;

            // We may have gone up the hierarchy, so get the current registry entry
            entry = metatypeRegistry.getRegistryEntry(referringElement);

            return new ConfigurationInfo(referringElement, referringConfiguration, entry, false);
        }

        private ConfigElement getReferringElement(RegistryEntry entry, ConfigID id) throws ConfigMergeException {
            ServerConfiguration configuration = serverXMLConfig.getConfiguration();

            ConfigElement referringElement = null;

            // If this is nested, get the ConfigElement from the parent
            if (id.getParent() != null) {
                ConfigID parentID = id.getParent();
                RegistryEntry parentEntry = metatypeRegistry.getRegistryEntry(parentID.getPid());
                if (parentEntry == null)
                    return null;
                referringElement = getReferringElement(parentEntry, parentID);

            } else if (entry.isFactory()) {
                // If this is a top level factory element, retrieve it.
                referringElement = configuration.getFactoryInstance(entry.getPid(),
                                                                    entry.getAlias(),
                                                                    id.getId());

            } else {
                // This is a singleton, retrieve it.
                referringElement = configuration.getSingleton(entry.getPid(), entry.getAlias());
            }
            return referringElement;

        }

        /**
         * @return
         */
        public boolean permanent() {
            return false;
        }

    }

    protected class UnresolvedReference extends UnresolvedPidType {

        private final String pid;

        public UnresolvedReference(String pid, AttributeDefinition attr, String id, ConfigID referringID) {
            super(attr, id, referringID);
            this.pid = pid;
        }

        public String getPid() {
            return this.pid;
        }

        @Override
        public void reportError() {
            // Only report an error when the metatype for the target PID has been loaded
            if (pidExistsInRegistry())
                Tr.warning(tc, "warning.pid.not.found", getAttributeDefinition().getID(), value);
        }

        /**
         * @return
         */
        private boolean pidExistsInRegistry() {
            return metatypeRegistry.getRegistryEntry(pid) != null;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            result = prime * result + ((pid == null) ? 0 : pid.hashCode());
            result = prime * result + ((elementId == null) ? 0 : elementId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof UnresolvedReference)) {
                return false;
            }
            UnresolvedReference other = (UnresolvedReference) obj;
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            if (pid == null) {
                if (other.pid != null) {
                    return false;
                }
            } else if (!pid.equals(other.pid)) {
                return false;
            }
            if (elementId == null) {
                if (other.elementId != null) {
                    return false;
                }
            } else if (!elementId.equals(other.elementId)) {
                return false;
            }
            return true;
        }

    }

    protected class UnresolvedService extends UnresolvedPidType {

        private final String service;
        private final int count;

        public UnresolvedService(String service, AttributeDefinition attr, String id, ConfigID referringID, int i) {
            super(attr, id, referringID);
            this.service = service;
            count = i;
        }

        public String getService() {
            return this.service;
        }

        public int getCount() {
            return count;
        }

        @Override
        public void reportError() {
            // Only report an error when the metatype for the target PID has been loaded
            if (count == 0 && serviceExistsInRegistry())
                Tr.warning(tc, "warning.pid.not.found", getAttributeDefinition().getID(), value);
            else if (count > 1)
                Tr.warning(tc, "warning.multiple.matches", getAttributeDefinition().getID(), value);
        }

        /**
         * @return
         */
        private boolean serviceExistsInRegistry() {
            return metatypeRegistry.getEntriesExposingService(service) != null;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            result = prime * result + ((service == null) ? 0 : service.hashCode());
            result = prime * result + ((elementId == null) ? 0 : elementId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof UnresolvedService)) {
                return false;
            }
            UnresolvedService other = (UnresolvedService) obj;
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            if (service == null) {
                if (other.service != null) {
                    return false;
                }
            } else if (!service.equals(other.service)) {
                return false;
            }
            if (elementId == null) {
                if (other.elementId != null) {
                    return false;
                }
            } else if (!elementId.equals(other.elementId)) {
                return false;
            }
            return true;
        }

    }

    protected class WildcardReference extends UnresolvedPidType {

        private final String pid;

        public WildcardReference(String pid, AttributeDefinition attr, ConfigID referringID) {
            super(attr, ALL_PIDS, referringID);
            this.pid = pid;
        }

        public String getPid() {
            return this.pid;
        }

        @Override
        public void reportError() {
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.xml.internal.ConfigEvaluator.UnresolvedPidType#permanent()
         */
        @Override
        public boolean permanent() {
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((pid == null) ? 0 : pid.hashCode());
            result = prime * result + ((elementId == null) ? 0 : elementId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof WildcardReference)) {
                return false;
            }
            WildcardReference other = (WildcardReference) obj;
            if (pid == null) {
                if (other.pid != null) {
                    return false;
                }
            } else if (!pid.equals(other.pid)) {
                return false;
            }
            if (elementId == null) {
                if (other.elementId != null) {
                    return false;
                }
            } else if (!elementId.equals(other.elementId)) {
                return false;
            }
            return true;
        }

    }

    protected class WildcardService extends UnresolvedPidType {

        private final String service;

        public WildcardService(String service, AttributeDefinition attr, ConfigID referringID) {
            super(attr, ALL_PIDS, referringID);
            this.service = service;
        }

        public String getService() {
            return this.service;
        }

        @Override
        public void reportError() {
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.xml.internal.ConfigEvaluator.UnresolvedPidType#permanent()
         */
        @Override
        public boolean permanent() {
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((service == null) ? 0 : service.hashCode());
            result = prime * result + ((elementId == null) ? 0 : elementId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof WildcardService)) {
                return false;
            }
            WildcardService other = (WildcardService) obj;
            if (service == null) {
                if (other.service != null) {
                    return false;
                }
            } else if (!service.equals(other.service)) {
                return false;
            }
            if (elementId == null) {
                if (other.elementId != null) {
                    return false;
                }
            } else if (!elementId.equals(other.elementId)) {
                return false;
            }
            return true;
        }

    }

}
