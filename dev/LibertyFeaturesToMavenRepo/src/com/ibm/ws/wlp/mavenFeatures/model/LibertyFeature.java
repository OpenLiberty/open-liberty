package com.ibm.ws.wlp.mavenFeatures.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.ws.wlp.mavenFeatures.utils.Constants;

public class LibertyFeature {

	private final String symbolicName;
	private final String shortName;
	private final String name;
	private final String description;
	private final List<String> requiredFeatures;
	private final String productVersion;
	private final MavenCoordinates mavenCoordinates;

	/**
	 * Construct LibertyFeature
	 * @param symbolicName symbolic name
	 * @param shortName short name (if this is a public feature or a bundle)
	 * @param name readable name
	 * @param description readable description
	 * @param requiredFeatures list of required features
	 * @param productVersion Liberty version
	 * @param mavenCoordinates Maven coordinates
	 * @param isWebsphereLiberty If true then it is WebSphere Liberty, else Open Liberty
	 */
	public LibertyFeature(String symbolicName, String shortName, String name, String description, List<String> requiredFeatures, String productVersion, String mavenCoordinates, boolean isWebsphereLiberty) {
		super();
		this.symbolicName = symbolicName;
		this.shortName = shortName;
		this.name = name;
		this.description = description;
		this.requiredFeatures = requiredFeatures;
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
		if (requiredFeatures != null) {
			for (String requireFeature : requiredFeatures) {
				if (allFeatures.containsKey(requireFeature)) {
					dependencies.add(allFeatures.get(requireFeature));
				} else {
					throw new MavenRepoGeneratorException(
							"Cannot find feature " + requireFeature + " which is required by " + symbolicName);
				}
			}
		}
		return dependencies;
	}
	
}