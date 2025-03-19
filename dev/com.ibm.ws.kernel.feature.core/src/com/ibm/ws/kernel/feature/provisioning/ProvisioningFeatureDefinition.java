/*******************************************************************************
 * Copyright (c) 2013,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.provisioning;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.ibm.ws.kernel.feature.FeatureDefinition;

/**
 * Extension of a feature definition.
 *
 * A feature definition {@link FeatureDefinition} encodes minimal feature
 * information.
 *
 * This extension adds provisioning information.
 *
 * The split enables more lightweight processing of feature definitions
 * outside of provisioning operations.
 */
public interface ProvisioningFeatureDefinition extends FeatureDefinition {
    /**
     * Tell the IBM short name of this feature.
     *
     * Answer null if this feature does not have a short name.
     *
     * The IBM short name is not the same as the feature name.
     *
     * All public features and all versionless features have a short name.
     *
     * @return The IBM short name of this feature.
     */
    String getIbmShortName();

    int getIbmFeatureVersion();

    boolean isSupportedFeatureVersion();

    /**
     * Tell if this is an auto feature. That is, a feature which is
     * automatically provisioned when all required capabilities are
     * satisfied.
     *
     * @return True or false telling if this is an auto feature.
     */
    boolean isAutoFeature();

    /**
     * Tell if this is a singleton feature. That is, at most one
     * version of this feature may be provisioned.
     *
     * @return True or false telling if this is a singleton feature.
     */
    boolean isSingleton();

    /**
     * Answer the type of bundle repository of this feature.
     *
     * A null or empty value indicates that the feature is a core feature.
     *
     * The value "usr" indicates that the feature is a user extension feature.
     *
     * Other values indicate that the feature is in a product extension.
     *
     * See {@link com.ibm.ws.kernel.provisioning.ExtensionConstants#CORE_EXTENSION}
     * and {@link com.ibm.ws.kernel.provisioning.ExtensionConstants#USER_EXTENSION}.
     *
     * @return The type of the bundle repository of this feature.
     */
    String getBundleRepositoryType();

    /**
     * Answer the file which contains the definition of this feature.
     *
     * @return The file which contains the definition of this feature.
     */
    File getFeatureDefinitionFile();

    /**
     * Answer the checksum file of this feature.
     *
     * @return The checksum file of this feature.
     */
    File getFeatureChecksumFile();

    /**
     * Answer an attribute value from the manifest of this feature.
     *
     * @param name The name of the attribute value which is to be retrieved.
     *
     * @return The value of the named attribute.
     */
    String getHeader(String name);

    /**
     * Answer an attribute value from the manifest of this feature.
     *
     * @param name   The name of the attribute value which is to be retrieved.
     * @param locale The locale which is to be used to create the attribute
     *                   value from the raw manifest contents.
     * @return The value of the named attribute.
     */
    String getHeader(String name, Locale locale);

    Collection<HeaderElementDefinition> getHeaderElements(String name);

    Collection<String> getIcons();

    /**
     * Select feature resources of the constituents of this feature.
     *
     * @param type The type of constituents which are to be selected.
     *                 Null means select all constituent resources.
     *
     * @return The selected constituent resources.
     */
    Collection<FeatureResource> getConstituents(SubsystemContentType type);

    /**
     * Answer the locations of localization resources
     * of this feature. The locations are not guaranteed
     * to exist.
     *
     * @return The locations of localization resources of
     *         this feature.
     */
    Collection<File> getLocalizationFiles();

    /**
     * Tell if this feature is superseded.
     *
     * See {@link #getSupersededBy()}.
     *
     * @return True or false telling if this feature is superseded.
     */
    boolean isSuperseded();

    /**
     * Print the features which supersede this feature as a comma
     * delimited list enclosed in square braces.
     *
     * See {@link #isSuperseded()}.
     *
     * @return A printout of the features which supersede this feature.
     */
    String getSupersededBy();

    /**
     * Tell if the capability requirements of this feature are satisfied.
     *
     * @param supplyingDefs Features supplying capabilities.
     *
     * @return True or false telling if the capability requirements are
     *         satisfied by the supplying features.
     */
    boolean isCapabilitySatisfied(Collection<ProvisioningFeatureDefinition> supplyingDefs);

    /**
     * Answer the names of the platform (or platforms) of this feature.
     *
     * Platform names are stored as header attribute WLP-Platform.
     *
     * When more than one platform name is specified, each should be a
     * different version of the same base platform.
     *
     * Platform names are stored on versioned public features which have
     * associated versionless features, and are stored on compatibility
     * features.
     *
     * @return The platform names of this feature.
     */
    List<String> getPlatformNames();

    /**
     * Answer the first platform name of the feature. Answer null if the
     * feature has no platform names. (See {@see #getPlatformNames()}.)
     *
     * Versioned features which have associated versionless features
     * and compatibility features all provide platform names.
     *
     * @return The platform name of this versioned or compatibility feature.
     */
    String getPlatformName();

    /**
     * Tell if this is a versionless feature.
     *
     * Versionless features do not have a platform, but are associated
     * with versioned features which have platform values.
     *
     * Versionless features are public. The feature short name is the
     * base name of the associated versioned features.
     *
     * @return True or false telling if this is a versionless feature.
     */
    boolean isVersionless();

    /**
     * Tell if this is a platform convenience feature.
     *
     * Platform convenience features are used to provision most (but not
     * all) features which are associated with a platform / programming model.
     *
     * Convenience features are public and do not have a platform value.
     *
     * @return True or false telling if this is a platform convenience feature.
     */
    boolean isConvenience();

    /**
     * Tell if the feature is a platform compatibility feature.
     *
     * A compatibility feature sets a platform version, which is used
     * to select versions of the versionless features which are associated
     * with the platform version.
     *
     * Platform compatibility features are private and have a platform value.
     *
     * @return True or false telling if this is a platform compatibility
     *         feature.
     */
    boolean isCompatibility();

    /**
     * Answer the alternate names of this feature. Answer an empty collection
     * if there are none.
     *
     * @return The alternate names of this feature.
     */
    // List<String> getAltNames();
}
