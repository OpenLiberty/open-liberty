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

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * An instance of ExtendedObjectClassDefinition will wrap a metatype ObjectClassDefinition
 * and provide extensions for retrieving IBM extensions.
 */
public interface ExtendedObjectClassDefinition extends ObjectClassDefinition {

    String PARENT_PID_ATTRIBUTE = "parentPid";

    String SUPPORTS_EXTENSIONS_ATTRIBUTE = "supportExtensions";

    String SUPPORTS_HIDDEN_EXTENSIONS_ATTRIBUTE = "supportHiddenExtensions";

    String ALIAS_ATTRIBUTE = "alias";

    String CHILD_ALIAS_ATTRIBUTE = "childAlias";

    String EXTENDS_ALIAS_ATTRIBUTE = "extendsAlias";

    String METATYPE_EXTRA_PROPERTIES = "extraProperties";

    String METATYPE_ACTION_ATTRIBUTE = "action";

    String LOCALIZATION_ATTRIBUTE = "localization";

    String EXTENDS_ATTRIBUTE = "extends";

    String XSD_ANY_ATTRIBUTE = "any";
    String EXCLUDED_CHILDREN_ATTRIBUTE = "excludeChildren";

    String REQUIRE_EXPLICIT_CONFIGURATION = "requireExplicitConfiguration";

    String OBJECT_CLASS = "objectClass";
    String BETA_ATTRIBUTE = "beta";

    /**
     * Return the alias value that should be used in the configuration to refer to this object
     * class definition. The alias value must be unique across all alias values defined in
     * metatype.
     * 
     * @return a String containing the alias value or null if not used
     */
    public String getAlias();

    /**
     * Used ONLY for child-first nested elements.
     * Return the alias value that should be used if this object class definition represents an element
     * that can only exist when nested under another element. This is only valid when there is a value
     * returned from getParentPid. The alias value does not have to be unique.
     * 
     * @return a String containing the alias value or null if not used.
     */
    public String getChildAlias();

    /**
     * Return the suffix for constructing nested element names of the form adId.suffix for OCDs that extend reference pids in a parent-first nesting.
     * Used ONLY for extending (ibm:extends) elements with an ancestor pid used as a reference pid in nested xml.
     * 
     * @return a String containing the suffix for constructing element names or null if not used.
     */
    String getExtendsAlias();

    /**
     * Returns the name of the pid this object is extending.
     * 
     * @return the ibm:extends String or null if the value does not exist
     */
    public String getExtends();

    /**
     * Used ONLY for child-first nested elements.
     * Returns the pid or factory pid of the object class definition that this definition should
     * be nested under.
     * 
     * @return a String containing the parent pid value or null if not used
     */
    public String getParentPID();

    /**
     * Used ONLY to support child-first nested elements under this element.
     * Return true if this object class definition supports nested elements not specified in metatype
     * 
     * @return true if nested elements are supported, false otherwise
     */
    public boolean supportsExtensions();

    /**
     * Return true if this object class definition supports nested elements not specified in metatype
     * and these nested elements are child first and should not appear in the parent at all.
     * 
     * @return true if nested elements are supported, false otherwise
     */
    public boolean supportsHiddenExtensions();

    /**
     * Return the underlying ObjectClassDefinition
     * 
     * @return
     */
    public ObjectClassDefinition getDelegate();

    /**
     * Computes a map keyed with attribute ID with values wrapped AttributeDefinitions.
     * USED ONLY BY SCHEMA BUILDER
     * 
     * @return a map of attribute ID to wrapped attribute definition.
     */
    public Map<String, ExtendedAttributeDefinition> getAttributeMap();

    public boolean hasAllRequiredDefaults();

    public List<AttributeDefinition> getRequiredAttributes();

    /**
     * return the ibmui:extraProperties value
     * 
     * @return
     */
    public boolean hasExtraProperties();

    /**
     * return the ibmui:action value
     * 
     * @return
     */
    public String getAction();

    /**
     * Returns the resource bundle name.
     * 
     * @return the ibm:localization String or null if the value does not exist
     */
    public String getLocalization();

    /**
     * @return the number of xsd any elements allowed as a child of this OCD
     */
    public int getXsdAny();

    /**
     * @return
     */
    public String getExcludedChildren();

    /**
     * return the objectclasses for the OCD or null if not specified.
     * 
     * @return
     */
    public List<String> getObjectClass();

    /**
     * @return
     */
    public boolean isBeta();
}
