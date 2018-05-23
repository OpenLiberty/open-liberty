/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wlp.mavenFeatures.model;

import java.util.Collection;
import java.util.Map;

import com.ibm.ws.wlp.mavenFeatures.utils.Constants;

public class LibertyFeature {

	private final String symbolicName;
	private final String shortName;
	private final String name;
	private final String description;
	private final Map<String, Collection<String>> requiredFeaturesWithTolerates;
	private final String productVersion;
	private final MavenCoordinates mavenCoordinates;
	private final boolean isWebsphereLiberty;
	private final boolean restrictedLicense;

	/**
	 * Construct LibertyFeature
	 * @param symbolicName symbolic name
	 * @param shortName short name (if this is a public feature or a bundle)
	 * @param name readable name
	 * @param description readable description
	 * @param requiredFeaturesWithTolerates map of each required feature+version to tolerated versions
	 * @param productVersion Liberty version
	 * @param mavenCoordinates Maven coordinates
	 * @param isWebsphereLiberty If true then it is WebSphere Liberty, else Open Liberty
	 */
	public LibertyFeature(String symbolicName, String shortName, String name, String description, Map<String, Collection<String>> requiredFeaturesWithTolerates, String productVersion, String mavenCoordinates, boolean isWebsphereLiberty, boolean restrictedLicense) throws MavenRepoGeneratorException {
		super();
		this.symbolicName = symbolicName;
		this.shortName = shortName;
		this.name = name;
		this.description = description;
		this.requiredFeaturesWithTolerates = requiredFeaturesWithTolerates;
		this.productVersion = productVersion;
		if (mavenCoordinates != null) {
		    try {
		        this.mavenCoordinates = new MavenCoordinates(mavenCoordinates);
		    } catch (IllegalArgumentException e) {
		        throw new MavenRepoGeneratorException("Invalid Maven coordinates defined for feature " + symbolicName, e);
		    }
		} else {
			String artifactId = shortName != null ? shortName : symbolicName;
			this.mavenCoordinates = new MavenCoordinates(
					isWebsphereLiberty ? Constants.WEBSPHERE_LIBERTY_FEATURES_GROUP_ID : Constants.OPEN_LIBERTY_FEATURES_GROUP_ID,
					artifactId, productVersion);
		}
		this.isWebsphereLiberty = isWebsphereLiberty;
		this.restrictedLicense = restrictedLicense;
	}

	public String getSymbolicName() {
		return symbolicName;
	}
	public String getShortName() {
		return shortName;
	}
	public String getName() {
		return name;
	}
	public String getDescription() {
		return description;
	}
	public String getProductVersion() {
		return productVersion;
	}
	public MavenCoordinates getMavenCoordinates() {
		return mavenCoordinates;
	}
	public boolean isWebsphereLiberty() {
		return isWebsphereLiberty;
	}
	public boolean isRestrictedLicense() {
		return restrictedLicense;
	}
	public Map<String, Collection<String>> getRequiredFeaturesWithTolerates() {
		return requiredFeaturesWithTolerates;
	}
	
}
