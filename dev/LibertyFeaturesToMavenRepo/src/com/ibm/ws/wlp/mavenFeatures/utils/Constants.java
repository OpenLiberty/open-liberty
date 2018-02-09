package com.ibm.ws.wlp.mavenFeatures.utils;

public class Constants {
		
	public static final String WLP_INFORMATION_KEY = "wlpInformation";
	public static final String WLP_INFORMATION_2_KEY = "wlpInformation2";
	public static final String PROVIDE_FEATURE_KEY = "provideFeature";
	public static final String NAME_KEY = "name";
	public static final String SHORT_DESCRIPTION_KEY = "shortDescription";
	public static final String APPLIES_TO_KEY = "appliesTo";
	public static final String APPLIES_TO_VALUE_PRODUCT_VERSION = "productVersion";
	public static final String REQUIRE_FEATURE_KEY = "requireFeature";
	public static final String INSTALL_POLICY_KEY = "installPolicy";
	public static final String VISIBILITY_KEY = "visibility";
	public static final String VISIBILITY_VALUE_PUBLIC = "PUBLIC";
	public static final String VISIBILITY_VALUE_INSTALL = "INSTALL";
	public static final String MAVEN_COORDINATES_KEY = "mavenCoordinates";
	
	public static final String WEBSPHERE_LIBERTY_FEATURES_GROUP_ID = "com.ibm.websphere.appserver.features";
	public static final String OPEN_LIBERTY_FEATURES_GROUP_ID = "io.openliberty.features";
	public static final String JSON_ARTIFACT_ID = "features";

	public static final String SHORT_NAME_KEY = "shortName";
	
	public static final String MAVEN_MODEL_VERSION = "4.0.0";

	public enum ArtifactType {
	    ESA,
	    JSON,
	    POM;
		
		public String getType() {
			switch (this) {
			case ESA:
				return "esa";
			case JSON:
				return "json";
			case POM:
				return "pom";
			default:
				throw new AssertionError("Unexpected artifact type " + this);
			}
		}
		
		public String getLibertyFileExtension() {
			switch (this) {
			case ESA:
				return ".esa";
			case JSON:
				return ".json";
			case POM:
			default:
				throw new AssertionError("Unexpected artifact type " + this);
			}
		}
		
		public String getMavenFileExtension() {
			switch (this) {
			case ESA:
				return ".esa";
			case JSON:
				return ".json";
			case POM:
				return ".pom";
			default:
				throw new AssertionError("Unexpected artifact type " + this);
			}
		}
		
	}
	
}
