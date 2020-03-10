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
package com.ibm.ws.config.xml.internal.metatype;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.metatype.AttributeDefinition;

/**
 *
 */
public interface ExtendedAttributeDefinition extends AttributeDefinition {

    static final String ATTRIBUTE_TYPE_NAME = "type";
    static final String ATTRIBUTE_REFERENCE_NAME = "reference";

    static final String FINAL_ATTR_NAME = "final";
    static final String VARIABLE_ATTR_NAME = "variable";
    static final String UNIQUE_ATTR_NAME = "unique";
    static final String REQUIRES_TRUE_ATTR_NAME = "requiresTrue";
    static final String REQUIRES_FALSE_ATTR_NAME = "requiresFalse";
    static final String GROUP_ATTR_NAME = "group";
    static final String RENAME_ATTR_NAME = "rename";
    static final String FLAT_ATTR_NAME = "flat";
    static final String COPY_OF_ATTR_NAME = "copyOf";
    static final String BETA_NAME = "beta";
    static final String OBSCURE_NAME = "obscure";

    String SERVICE = "service";
    String SERVICE_FILTER = "serviceFilter";
    String UI_REFERENCE = "uiReference";

    static final String VARIABLE_SUBSTITUTION_NAME = "variableSubstitution";
    static final String FALSE = "false";

    /**
     * Returns the pid or factory pid of the ObjectClassDefinition that is referenced by this attribute.
     * This is only valid when the type is PID_TYPE
     *
     * @return the pid or factoryPid
     */
    String getReferencePid();

    /**
     * Returns true if the value of this attribute can not be specified in the configuration.
     *
     * @return true if the value can not be overridden, false otherwise
     */
    public boolean isFinal();

    /**
     * Gets the value of a system variable that should be used prior to any default values.
     *
     * @return the variable name or null if not specified
     */
    public String getVariable();

    /**
     * Returns true if values for this AttributeDefinition should be unique. If the attribute definition
     * is marked unique, no other elements in the configuration may use the same value.
     *
     * @return true if the value of the AttributeDefinition should be unique
     */
    boolean isUnique();

    /**
     * Returns the category to be used for unique value checking. For example, if the unique
     * category is jndiName, the value of this attribute must be unique across all attributes
     * that have a unique category value of jndiName.
     *
     * @return the category String or null if the value is not specified
     */
    public String getUniqueCategory();

    /**
     * Returns true if the nested config element should be flattened
     *
     * @return true if the nested config element should be flattened
     */
    boolean isFlat();

    /**
     * Returns the name of the attribute that should be used to determine the value of this
     * attribute.
     *
     * @return The name of the attribute that gives this attribute its value
     */
    public String getCopyOf();

    /**
     * @return
     */
    AttributeDefinition getDelegate();

    /**
     * Get a Set of all the ExtensionUris associated with the Attribute.
     *
     * @return A Set of Strings containing the extension Uris.
     */
    public Set<String> getExtensionUris();

    /**
     * Returns a Map of the extensions for the current extension URI
     *
     * @param extensionUri - The Uri that you want the extensions for.
     * @return - A Map of the extensions for the selected extension Uri.
     */
    public Map<String, String> getExtensions(String extensionUri);

    /**
     * Returns the attribute name that must be true for this attribute to be enabled.
     *
     * @return the ibm:requiresTrue String or null if the value does not exist
     */
    public String getRequiresTrue();

    /**
     * Returns the attribute name that must be false for this attribute to be enabled.
     *
     * @return the ibm:requiresFalse String or null if the value does not exist
     */
    public String getRequiresFalse();

    /**
     * Returns the group name of the this attribute.
     *
     * @return the ibm:group String or null if the value does not exist
     */
    public String getGroup();

    /**
     * Returns the id of the attribute on the supertype that should be renamed.
     * The id on this attribute definition will be used instead.
     *
     * @return the ibm:rename String or null if the value does not exist
     */
    public String getRename();

    /**
     * Returns a name to be used if the AD is an attribute rather than an element.
     *
     * @return
     */
    public String getAttributeName();

    /**
     * Returns true if variable resolution should be performed by the configuration runtime. The default is true.
     */
    public boolean resolveVariables();

    /**
     * returns the service if specified.
     *
     * @return
     */
    String getService();

    /**
     * returns the additional filter for the service if specified.
     *
     * @return
     */
    String getServiceFilter();

    /**
     * Returns the pids that the schema should list as choices for this type.
     *
     */
    List<String> getUIReference();

    /**
     * @return
     */
    boolean isBeta();

    /**
     * @return
     */
    boolean isObscured();
}
