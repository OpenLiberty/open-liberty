package com.ibm.ws.wlp.mavenFeatures.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
	public LibertyFeature(String symbolicName, String shortName, String name, String description, Map<String, Collection<String>> requiredFeaturesWithTolerates, String productVersion, String mavenCoordinates, boolean isWebsphereLiberty) {
		super();
		this.symbolicName = symbolicName;
		this.shortName = shortName;
		this.name = name;
		this.description = description;
		this.requiredFeaturesWithTolerates = requiredFeaturesWithTolerates;
		this.productVersion = productVersion;
		if (mavenCoordinates != null) {
			this.mavenCoordinates = new MavenCoordinates(mavenCoordinates);
		} else {
			String artifactId = shortName != null ? shortName : symbolicName;
			this.mavenCoordinates = new MavenCoordinates(
					isWebsphereLiberty ? Constants.WEBSPHERE_LIBERTY_FEATURES_GROUP_ID : Constants.OPEN_LIBERTY_FEATURES_GROUP_ID,
					artifactId, productVersion);
		}
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
	
	/**
	 * Gets the list of features that this feature depends on.
	 * 
	 * @param allFeatures
	 *            The map of all features, mapping from symbolic name to
	 *            LibertyFeature object
	 * @return List of LibertyFeature objects that this feature depends on.
	 * @throws MavenRepoGeneratorException
	 *             If a required feature cannot be found in the map.
	 */
	public List<LibertyFeature> getRequiredFeatures(Map<String, LibertyFeature> allFeatures)
			throws MavenRepoGeneratorException {
		List<LibertyFeature> dependencies = new ArrayList<LibertyFeature>();
		if (requiredFeaturesWithTolerates != null) {
			for (String requireFeature : requiredFeaturesWithTolerates.keySet()) {
				Collection<String> toleratesVersions = null;
				if (allFeatures.containsKey(requireFeature)) {
					dependencies.add(allFeatures.get(requireFeature));
				} else if ((toleratesVersions = requiredFeaturesWithTolerates.get(requireFeature)) != null) {
					boolean tolerateFeatureFound = false;
					for (String version : toleratesVersions) {
						String tolerateFeatureAndVersion = requireFeature.substring(0, requireFeature.lastIndexOf("-")) + "-" + version;
						if (allFeatures.containsKey(tolerateFeatureAndVersion)) {
							dependencies.add(allFeatures.get(tolerateFeatureAndVersion));
							tolerateFeatureFound = true;
							break;
						}
					}
					if (!tolerateFeatureFound) {
						throw new MavenRepoGeneratorException(
								"For feature " + symbolicName + ", cannot find required feature " + requireFeature + " or any of its tolerated versions: " + toleratesVersions);
					}
				} else {
					throw new MavenRepoGeneratorException(
							"For feature " + symbolicName + ", cannot find required feature " + requireFeature);
				}
			}
		}
		return dependencies;
	}
	
}