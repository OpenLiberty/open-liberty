/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.resolver.internal.namespace;

import java.util.EnumSet;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.subsystem.SubsystemConstants;

import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.resolver.internal.resource.FeatureResource;

/**
 * This class contains the names of the ad-hoc attributes used for installable entities' {@link IdentityNamespace} requirements and capabilities.
 */
public class InstallableEntityIdentityConstants {

    /** Attribute name for the capability attribute representing the short name of the installable entity */
    public static final String CAPABILITY_SHORT_NAME_ATTRIBUTE = "shortName";
    /** Attribute name for the capability attribute representing the lower case short name of the installable entity */
    public static final String CAPABILITY_LOWER_CASE_SHORT_NAME_ATTRIBUTE = "lowerCaseShortName";
    /** The attribute value when the type of the entity is an iFix */
    public static final String TYPE_IFIX = "ifix";
    /** The attribute value when the type of the entity is a feature */
    public static final String TYPE_FEATURE = SubsystemConstants.SUBSYSTEM_TYPE_FEATURE;
    /** The attribute value when the type of the entity is a sample */
    public static final String TYPE_SAMPLE = "sample";
    /**
     * The attribute value identifying a {@link FeatureResource}'s requirement as being to an auto-feature through the provision capability header.
     * 
     * @see IdentityNamespace#REQUIREMENT_CLASSIFIER_DIRECTIVE
     * */
    public static final String CLASSIFIER_AUTO = "auto";

    /**
     * This enum lists which name attributes can be set for an installable entity. Note that it is ordered in the order of preference for matching so {@link #SYMBOLIC_NAME} should
     * be attempted to matched first, then {@link #SHORT_NAME} and finally {@link #CASE_INSENSITIVE_SHORT_NAME}.
     */
    public static enum NameAttributes {
        SYMBOLIC_NAME(IdentityNamespace.IDENTITY_NAMESPACE, FilterableAttribute.SYMBOLIC_NAME),
        SHORT_NAME(InstallableEntityIdentityConstants.CAPABILITY_SHORT_NAME_ATTRIBUTE, FilterableAttribute.SHORT_NAME),
        CASE_INSENSITIVE_SHORT_NAME(InstallableEntityIdentityConstants.CAPABILITY_LOWER_CASE_SHORT_NAME_ATTRIBUTE, FilterableAttribute.LOWER_CASE_SHORT_NAME);

        final String filterAttributeName;
        final FilterableAttribute featureFilterAttribute;

        private NameAttributes(String filterAttributeName, FilterableAttribute featureFilterAttribute) {
            this.filterAttributeName = filterAttributeName;
            this.featureFilterAttribute = featureFilterAttribute;
        }

        /**
         * Returns the attribute name to use in a filter when matching against this type.
         * 
         * @return
         */
        public String getFilterAttributeName() {
            return this.filterAttributeName;
        }

        /**
         * Get the {@link FilterableAttribute} that can be used to construct a filter for a feature in Massive.
         * 
         * @return the featureFilterAttribute
         */
        public FilterableAttribute getFeatureFilterAttribute() {
            return featureFilterAttribute;
        }
    }

    /** The {@link NameAttributes} to use when matching a sample. */
    public static final EnumSet<NameAttributes> SAMPLES_NAME_ATTRIBUTES = EnumSet.of(NameAttributes.SHORT_NAME, NameAttributes.CASE_INSENSITIVE_SHORT_NAME);

}
