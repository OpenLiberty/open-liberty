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
import com.ibm.ws.kernel.provisioning.ExtensionConstants;

/**
 * Provisioning operations incur overhead, and must be invoked within the
 * confines of a provisioning operation (for the server), or from a client
 * utility.
 * <p>
 * Methods in this interface (not in the parent) may throw Exceptions if
 * called outside of a provisioning operation.
 * <p>
 * PROVISIONING OPERATIONS SHOULD BE SINGLE THREADED.
 * <p>
 * The FeatureManager already ensured only one thread was performing
 * provisioning operations at a time. Any information exposed by this
 * interface, in particular, is vulnerable to thread safety due to the
 * extra storage backing the operations being subject to cleanup when
 * the provisioning operation completes. (The exact meaning of this
 * will be different for client utilities, which are usually a single
 * main-line path.).
 */
public interface ProvisioningFeatureDefinition extends FeatureDefinition {
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
     * @see ExtensionConstants#CORE_EXTENSION
     * @see ExtensionConstants#USER_EXTENSION
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
     * Answer the platform of this feature.
     *
     * The feature platform is stored as a header attribute.
     *
     * @return The platform of this feature.
     */
    List<String> getPlatforms();

    /**
     * Tell if this is a versionless feature.
     *
     * @return
     */
    boolean isVersionless();

    /**
     * Tell if this is a convenience feature.
     *
     * @return
     */
    boolean isConvenience();

    /**
     * Tell if the feature is a compatibility feature.
     *
     * @return
     */
    boolean isCompatibility();

    /**
     * Answer the value of the platform when the feature is a compatibility feature.
     *
     * Always returns the first platform the WLP_Platform: list. See { {@see #getPlatforms()}
     *
     * @return The platform value of this compatibility feature
     */
    String getPlatformValue();
}