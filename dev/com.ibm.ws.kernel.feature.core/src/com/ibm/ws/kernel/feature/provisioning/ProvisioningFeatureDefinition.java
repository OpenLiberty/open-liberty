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
package com.ibm.ws.kernel.feature.provisioning;

import java.io.File;
import java.util.Collection;
import java.util.Locale;

import com.ibm.ws.kernel.feature.FeatureDefinition;

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
 * main-line path..).
 */
public interface ProvisioningFeatureDefinition extends FeatureDefinition {

    /**
     * @return The bundle repository this feature belongs to: null or empty for core,
     *         "usr" for the user extension, or a product extension
     * 
     * @see ExtensionConstants#CORE_EXTENSION
     * @see ExtensionConstants#USER_EXTENSION
     */
    String getBundleRepositoryType();

    /**
     * Get a collection of {@link FeatureResource} that represent the content of this feature as defined by the headers.
     * 
     * @param type the type of content to return, or null for all content.
     * @return
     */
    public Collection<FeatureResource> getConstituents(SubsystemContentType type);

    /**
     * Get the file representing this feature definition, used by minify to know which features to retain.
     * 
     * @return The file backing this feature definition
     */
    public File getFeatureDefinitionFile();

    /**
     * Get the file representing the checksum file for this feature.
     * 
     * @return The file of checksums.
     */
    public File getFeatureChecksumFile();

    /**
     * Get an abitrary header from the feature manifest.
     * 
     * @param string
     * @return
     */
    public String getHeader(String string);

    /**
     * @param string
     * @param locale
     * @return
     */
    public String getHeader(String string, Locale locale);

    /**
     * @param string
     * @return
     */
    public Collection<HeaderElementDefinition> getHeaderElements(String string);

    /**
     * Get the Short Name for this feature, as defined by its header.
     * 
     * @return
     */
    public String getIbmShortName();

    /**
     * Get a collection of File objects representing the locations that may be the NLS resources for this Feature.
     * The File objects returned may or may not exist.
     * 
     * @return
     */
    public Collection<File> getLocalizationFiles();

    /**
     * Get the list of comma separated superseded features. Each separated feature is in [].
     * 
     * @return
     */
    public String getSupersededBy();

    /**
     * @param featureDefinitionsToCheck
     * @return
     */
    boolean isCapabilitySatisfied(Collection<ProvisioningFeatureDefinition> featureDefinitionsToCheck);

    /**
     * Get whether the feature is superseded or not.
     * 
     * @return
     */
    public boolean isSuperseded();

    /**
     * @return true if this is a supported feature version, false otherwise
     */
    public boolean isSupportedFeatureVersion();

    /**
     * @return
     */
    boolean isAutoFeature();

    /**
     * @return the value of the IBM-Feature-Version header (0 if not present)
     */
    int getIbmFeatureVersion();

    /**
     * @return the files representing all icons available to the feature.
     */
    public Collection<String> getIcons();

    /**
     * @return true if the feature is a singleton, false otherwise
     */
    boolean isSingleton();

}