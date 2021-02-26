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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.ws.config.xml.internal.metatype.ExtendedObjectClassDefinition;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;

class ConfigValidator {

    private static final String LINE_SEPARATOR = ConfigUtil.getSystemProperty("line.separator");

    // The value to display for a secure/password attribute.
    // The value is selected to be exactly the same as used by
    // com.ibm.websphere.ras.ProtectedString.toString().
    private static final String SECURE_VALUE = "*****";

    private static final TraceComponent tc = Tr.register(ConfigValidator.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    private final MetaTypeRegistry metatypeRegistry;

    private ServerXMLConfiguration configuration;

    private final ConfigVariableRegistry variableRegistry;

    ConfigValidator(MetaTypeRegistry metatypeRegistry, ConfigVariableRegistry variableRegistry) {
        this.metatypeRegistry = metatypeRegistry;
        this.variableRegistry = variableRegistry;
    }

    public void setConfiguration(ServerXMLConfiguration configuration) {
        this.configuration = configuration;
    }

    public boolean validateSingleton(String pid, String alias) {
        String name = (alias == null) ? pid : alias;
        return validate(name, null, configuration.getConfiguration().getSingletonElements(pid, alias));
    }

    public boolean validateFactoryInstance(String pid, String alias, ConfigID id) {
        String name = (alias == null) ? pid : alias;
        return validate(name, id, configuration.getConfiguration().getFactoryElements(pid, alias, id.getId()));
    }

    public void validate(Set<RegistryEntry> entries) {
        // validate singletons
        for (RegistryEntry registry : entries) {
            if (registry.isSingleton()) {
                String alias;
                String name;

                alias = registry.getAlias() == null ? registry.getChildAlias() : registry.getAlias();
                name = (alias == null) ? registry.getPid() : alias;
                validate(registry, name, null, configuration.getConfiguration().getSingletonElements(registry.getPid(), alias));
            } else {
                String alias;
                String name;
                String defaultId;
                alias = registry.getAlias() == null ? registry.getChildAlias() : registry.getAlias();
                name = (alias == null) ? registry.getPid() : alias;
                defaultId = registry.getDefaultId();
                Map<ConfigID, List<SimpleElement>> instances = configuration.getConfiguration().getAllFactoryElements(registry.getPid(), alias, defaultId);
                for (Map.Entry<ConfigID, List<SimpleElement>> entry : instances.entrySet()) {
                    validate(registry, name, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Answer the definition of an attribute. Answer null if a definition
     * is not available.
     *
     * @param registryEntry The registry entry from which to retrieve the attribute definition.
     * @param attributeName The name of the attribute to locate.
     *
     * @return The definition of the attribute. Null if no definition is available for the attribute.
     */
    private ExtendedAttributeDefinition getAttributeDefinition(RegistryEntry registryEntry, String attributeName) {
        if (registryEntry == null) {
            return null;
        }

        ExtendedObjectClassDefinition definition = registryEntry.getObjectClassDefinition();
        if (definition == null) {
            return null;
        }

        Map<String, ExtendedAttributeDefinition> attributeMap = definition.getAttributeMap();
        ExtendedAttributeDefinition attributeDefinition = attributeMap.get(attributeName);
        return attributeDefinition;
    }

    /**
     * Test if an attribute is a secured / password type attribute.
     *
     * This is tested using the attribute type, as obtained from {@link ExtendedAttributeDefinition#getType()}.
     *
     * Attribute types {@link MetaTypeFactory#PASSWORD_TYPE} and {@link MetaTypeFactory#HASHED_PASSWORD_TYPE} are secured. All other attribute types are not secured.
     *
     * Answer false the attribute type cannot be obtained, either because the
     * registry entry was null, had no class definition, or had no definition
     * for the attribute.
     *
     * @param registryEntry The registry entry from which to obtain the attribute type.
     * @param attributeName The name of the attribute which is to be tested.
     *
     * @return True or false telling if the attribute is a secured/password type attribute.
     */
    private boolean isSecureAttribute(RegistryEntry registryEntry, String attributeName) {
        ExtendedAttributeDefinition attributeDefinition = getAttributeDefinition(registryEntry, attributeName);
        if (attributeDefinition == null) {
            return false; // No available definition; default to false.
        }

        // @formatter:off
        int attributeType = attributeDefinition.getType();
        return (attributeDefinition.isObscured()  || (attributeType == MetaTypeFactory.PASSWORD_TYPE) ||
                (attributeType == MetaTypeFactory.HASHED_PASSWORD_TYPE));
        // @formatter:on
    }

    private void logRegistryEntry(RegistryEntry registryEntry) {
        if (!tc.isDebugEnabled()) {
            return;
        }

        if (registryEntry == null) {
            Tr.debug(tc, "Registry Entry [ null ]");
            return;
        }

        Tr.debug(tc, "Registry Entry [ " + registryEntry.getPid() + " ]");
        Tr.debug(tc, "  Bundle [ " + registryEntry.getBundleId() + " : " + registryEntry.getBundleName() + " ]");
        Tr.debug(tc, "  Alias [ " + registryEntry.getAlias() + " ]");
        Tr.debug(tc, "  Child Alias [ " + registryEntry.getChildAlias() + " ]");
        Tr.debug(tc, "  Extends : [ " + registryEntry.getExtends() + " ]");

        ExtendedObjectClassDefinition definition = registryEntry.getObjectClassDefinition();
        Tr.debug(tc, "  Definition: [ " + definition + " ]");
        if (definition == null) {
            return;
        }

        Map<String, ExtendedAttributeDefinition> attributeMap = definition.getAttributeMap();
        for (Map.Entry<String, ExtendedAttributeDefinition> attributeEntry : attributeMap.entrySet()) {
            String attributeName = attributeEntry.getKey();
            ExtendedAttributeDefinition attributeDefinition = attributeEntry.getValue();

            Tr.debug(tc, "    [ " + attributeName + " ] [ " + attributeDefinition + " ]");

            if (attributeDefinition != null) {
                Tr.debug(tc, "      Type [ " + attributeDefinition.getType() + " ]");
                Tr.debug(tc, "      Card [ " + attributeDefinition.getCardinality() + " ]");
                Tr.debug(tc, "      Desc [ " + attributeDefinition.getDescription() + " ]");
            }
        }
    }

    /**
     * Valid configuration elements. If a validation problem is found, generate a validation
     * message and emit this as a trace warning. A validation message, if generated, will
     * usually be a multi-line message, with a half-dozen lines or more per problem which
     * is detected.
     *
     * @param pid      TBD
     * @param id       TBD
     * @param elements The configuration elements which are to be validated.
     *
     * @return True or false telling if the configuration elements are valid.
     */
    public boolean validate(String pid, ConfigID id, List<? extends ConfigElement> elements) {
        RegistryEntry registryEntry = metatypeRegistry.getRegistryEntryByPidOrAlias(pid);
        return validate(registryEntry, pid, id, elements);
    }

    /**
     * Valid configuration elements. If a validation problem is found, generate a validation
     * message and emit this as a trace warning. A validation message, if generated, will
     * usually be a multi-line message, with a half-dozen lines or more per problem which
     * is detected.
     *
     * @param registryEntry The registry entry associated with the PID of the configuration elements.
     * @param pid           TBD
     * @param id            TBD
     * @param elements      The configuration elements which are to be validated.
     *
     * @return True or false telling if the configuration elements are valid.
     */
    public boolean validate(RegistryEntry registryEntry, String pid, ConfigID id, List<? extends ConfigElement> elements) {
        Map<String, ConfigElementList> conflictedElementLists = generateConflictMap(registryEntry, elements);
        if (conflictedElementLists.isEmpty()) {
            return true;
        }

        logRegistryEntry(registryEntry);

        String validationMessage = generateCollisionMessage(pid, id, registryEntry, conflictedElementLists);

        Tr.audit(tc, "info.config.conflict", validationMessage);
        return false;
    }

    /**
     * Look for conflicts between single-valued attributes. No metatype registry
     * entry is available.
     *
     * @param elements The configuration elements to test.
     *
     * @return A table of conflicts between the configuration elements.
     */
    protected Map<String, ConfigElementList> generateConflictMap(List<? extends ConfigElement> list) {
        return generateConflictMap(null, list);
    }

    /**
     * Look for conflicts between single-valued attributes of a list of configuration elements.
     *
     * Match attributes by name.
     *
     * Conflicts are keyed by attribute name.
     *
     * @param registryEntry The registry entry for the configuration elements.
     * @param list          The configuration elements to test.
     *
     * @return A mapping of conflicts detected across the configuration elements.
     */
    protected Map<String, ConfigElementList> generateConflictMap(RegistryEntry registryEntry, List<? extends ConfigElement> list) {
        if (list.size() <= 1) {
            return Collections.emptyMap();
        }

        boolean foundConflict = false;

        Map<String, ConfigElementList> conflictMap = new HashMap<String, ConfigElementList>();
        for (ConfigElement element : list) {
            for (Map.Entry<String, Object> entry : element.getAttributes().entrySet()) {
                String attributeName = entry.getKey();
                Object attributeValue = entry.getValue();

                // consider single-values attributes only
                if (!(attributeValue instanceof String)) {
                    continue;
                }

                ConfigElementList configList = conflictMap.get(attributeName);
                if (configList == null) {
                    configList = new ConfigElementList(attributeName);
                    conflictMap.put(attributeName, configList);
                }

                if (configList.add(element)) { // Validation occurs within 'add'.
                    foundConflict = true;
                }
            }
        }

        if (!foundConflict) {
            return Collections.emptyMap();
        } else {
            return conflictMap;
        }
    }

    /**
     * Emit a message for all detected conflicting elements.
     *
     * Message lines are generated for each attribute which has a conflict, and
     * for each value of each conflicted attribute.
     *
     * The conflict map should never be empty.
     *
     * Do not emit values for protected (password flagged) attributes. See {@link #isSecureAttribute(RegistryEntry, String)}.
     *
     * @param pid                    TBD
     * @param id                     TBD
     * @param registryEntry          The registry entry of the conflicted elements.
     * @param conflictedElementLists All detected conflicted elements.
     */
    private String generateCollisionMessage(String pid, ConfigID id, RegistryEntry registryEntry,
                                            Map<String, ConfigElementList> conflictMap) {
        StringBuilder builder = new StringBuilder();

        if (id == null) {
            builder.append(Tr.formatMessage(tc, "config.validator.foundConflictSingleton", pid));
        } else {
            builder.append(Tr.formatMessage(tc, "config.validator.foundConflictInstance", pid, id.getId()));
        }
        builder.append(LINE_SEPARATOR);

        for (Map.Entry<String, ConfigElementList> entry : conflictMap.entrySet()) {
            String attributeName = entry.getKey();
            ConfigElementList configList = entry.getValue();

            boolean secureAttribute = isSecureAttribute(registryEntry, attributeName);

            if (configList.hasConflict()) {
                builder.append("  ");
                builder.append(Tr.formatMessage(tc, "config.validator.attributeConflict", attributeName));
                builder.append(LINE_SEPARATOR);

                for (ConfigElement element : configList) {
                    Object value = element.getAttribute(attributeName);
                    String docLocation = element.getDocumentLocation();
                    builder.append("    ");
                    if (secureAttribute) {
                        builder.append(Tr.formatMessage(tc, "config.validator.valueConflictSecure", docLocation));
                    } else if (value == null || value.equals("")) {
                        builder.append(Tr.formatMessage(tc, "config.validator.valueConflictNull", docLocation));
                    } else {
                        builder.append(Tr.formatMessage(tc, "config.validator.valueConflict", value, docLocation));
                    }
                    builder.append(LINE_SEPARATOR);
                }

                builder.append("  ");
                Object activeValue = configList.getActiveValue();
                if (secureAttribute) {
                    String activeLoc = configList.getActiveElement().getMergedLocation();
                    builder.append(Tr.formatMessage(tc, "config.validator.activeValueSecure", attributeName, activeLoc));
                } else if (activeValue == null || activeValue.equals("")) {
                    builder.append(Tr.formatMessage(tc, "config.validator.activeValueNull", attributeName));
                } else {
                    builder.append(Tr.formatMessage(tc, "config.validator.activeValue", attributeName, activeValue));
                }
                builder.append(LINE_SEPARATOR);
            }
        }

        return builder.toString();
    }

    protected class ConfigElementList extends ArrayList<ConfigElement> {

        private static final long serialVersionUID = -8472291303190806069L;

        private final String attribute;
        private boolean hasConflict;

        public ConfigElementList(String attribute) {
            this.attribute = attribute;
            this.hasConflict = false;
        }

        @Override
        public boolean add(ConfigElement element) {
            if (!hasConflict && !isEmpty()) {
                Object lastValue = getLastValue();
                Object currentValue = variableRegistry.resolveRawString((String) element.getAttribute(attribute));

                if (lastValue == null) {
                    hasConflict = (currentValue != null);
                } else {
                    hasConflict = !lastValue.equals(currentValue);
                }
            }

            super.add(element);

            // Note the change of meaning.  'add' now tells if the
            // newly added element conflicts with a prior element.
            // The defined meaning is whether the element was added.

            return hasConflict;
        }

        public boolean hasConflict() {
            return hasConflict;
        }

        public Object getLastValue() {
            ConfigElement lastConfigElement = get(size() - 1);
            return lastConfigElement.getAttribute(attribute);
        }

        protected ConfigElement getActiveElement() {
            ArrayList<ConfigElement> list = new ArrayList<ConfigElement>(this);
            ConfigElement merged = new SimpleElement(list.get(0));
            merged.merge(list);
            return merged;
        }

        /**
         * Answer the value of the target attribute which results
         * from the merging of the elements of this list.
         *
         * @return The merged attribute value for this element list.
         */
        public Object getActiveValue() {
            // Updated by defect 172453:
            //
            // Previously, the active attribute value was obtained by overriding
            // the initial configuration element with all of the following
            // configuration elements.
            //
            // The update changes that to merge all elements of the list onto a
            // copy of the first list element.
            //
            // The merge does a merge includes a step of merging the first element
            // onto itself.  Is that merge step necessary?
            //
            // Also, if the list has only one element, can this processing be replaced
            // entirely with a get of the first element of the list?  We do note that
            // 'getActiveValue' is only currently used to determine the effective value
            // in case of a merge conflict.  That is only possible if the list has more
            // than one element, meaning, the optimization is not necessary used under
            // current usage.  However, 'getActiveValue' is a public API, meaning,
            // other uses must be considered.
            //
            // Since the intent is to obtain a single merged attribute value, could
            // the merge processing be abbreviated to process just the single target
            // attribute?

            return getActiveElement().getAttribute(attribute);
        }
    }
}
