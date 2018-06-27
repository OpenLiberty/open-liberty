/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resources.writeable;

import java.util.Collection;
import java.util.Map;

import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.resources.EsaResource;

/**
 * Represents a Feature Resource which may be uploaded to a repository.
 *
 * @see RepositoryResourceWritable
 */
public interface EsaResourceWritable extends WebDisplayable, ApplicableToProductWritable, EsaResource, RepositoryResourceWritable {

    /**
     * Set the provide feature field for this feature
     *
     * @param feature The name this feature should use.
     */
    public void setProvideFeature(String feature);

    /**
     * Add the supplied feature to the list of required features
     *
     * @param requiredFeatureSymbolicName The symbolic name of the feature to add
     * @deprecated See {@link #addRequireFeatureWithTolerates(String, Collection)}.
     */
    @Deprecated
    public void addRequireFeature(String requiredFeatureSymbolicName);

    /**
     * Sets the list of required features to the supplied list of features
     *
     * @param feats The list of symbolic names of features
     * @deprecated See {@link #setRequireFeatureWithTolerates(Map)}
     */
    @Deprecated
    public void setRequireFeature(Collection<String> feats);

    /**
     * Sets the list of required features along with the tolerates information
     * for those features.
     *
     * @param features
     */
    public void setRequireFeatureWithTolerates(Map<String, Collection<String>> features);

    /**
     * Add the supplied feature and tolerates info to the list of required features
     *
     * @param feature
     * @param tolerates
     */
    public void addRequireFeatureWithTolerates(String feature, Collection<String> tolerates);

    /**
     * Add the supplied fix to the list of required fixes
     *
     * @param fix The ID of the fix to add
     */
    public void addRequireFix(String fix);

    /**
     * Adds the supplied feature to the list of features which supersede this feature
     *
     * @param feature the symbolic name of the feature to add
     */
    public void addSupersededBy(String feature);

    /**
     * Gets the collection of features which supersede this feature
     *
     * @return a collection of symbolic names of features which supersede this feature
     */
    public Collection<String> getSupersededBy();

    /**
     * Adds the supplied feature to the list of features which may also be required by an application if this feature is replaced with the features which supersede it.
     * <p>
     * E.g. applicationSecurity-1.0 depends on servlet-3.0 but applicationSecurity-2.0 does not.
     * <p>
     * If a server config is changed to use applicationSecurity-2.0 instead of applicationSecurity-1.0, the server admin may also need to add servlet-3.0 if it was not explicitly
     * required before and applications
     * depend on it.
     *
     * @param feature the symbolic name of the feature to add
     */
    public void addSupersededByOptional(String feature);

    /**
     * Gets the list of features which may also be required by an application if this feature is replaced by the features which supersede it.
     *
     * @return collection of symbolic names of features which may be required if this feature is replaced by the features which supersede it.
     */
    public Collection<String> getSupersededByOptional();

    /**
     * Set the Visibility to the supplied {@link Visibility}
     *
     * @param vis The {@link Visibility} to use for this feature
     */
    public void setVisibility(Visibility vis);

    /**
     * Sets the ibm short name to use for this feature
     *
     * @param shortName The ibm short name to use
     */
    public void setShortName(String shortName);

    /**
     * Sets the ibmProvisionCapability field.
     *
     * @param ibmProvisionCapability
     *            The new ibmProvisionCapability to be used
     */
    public void setProvisionCapability(String provisionCapability);

    /**
     * Sets the install policy for the feature
     *
     * An install policy of {@link InstallPolicy#WHEN_SATISFIED} should only be used if {@link #getIbmProvisionCapability()} returns a non-<code>null</code> value. This indicates
     * that the feature should be automatically installed if all of its provision capability requirements are met.
     *
     * @param policy the new value for installPolicy
     */
    public void setInstallPolicy(InstallPolicy policy);

    /**
     * Specify the minimum/maximum Java version needed by this ESA, and the Require-Capability headers from each contained bundle which have led to the requirement. All fields are
     * allowed to be null.
     *
     * @param minimum an OSGI version string representing the minimum Java version required.
     * @param maximum an OSGI version string representing the minimum Java version required.
     * @param displayMinimum An alternative representation of the minimum version for display purposes
     * @param displayMaximum An alternative representation of the maximum version for display purposes
     * @param rawBundleRequirements The Require-Capability headers from all the bundles contained in this ESA
     */
    public void setJavaSEVersionRequirements(String minimum, String maximum, Collection<String> rawBundleRequirements);

    /**
     * Sete whether the feature is a singleton or not
     *
     * @param singleton
     */
    public void setSingleton(String singleton);

    /**
     * Sets the IBM-InstallTo field
     *
     * @param ibmInstallTo The value of the IBM-InstallTo field to use
     */
    public void setIBMInstallTo(String ibmInstallTo);
}