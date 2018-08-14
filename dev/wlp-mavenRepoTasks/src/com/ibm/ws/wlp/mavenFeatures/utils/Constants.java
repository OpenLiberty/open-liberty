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
	public static final String REQUIRE_FEATURE_WITH_TOLERATES_KEY = "requireFeatureWithTolerates";
	public static final String FEATURE_KEY = "feature";
	public static final String TOLERATES_KEY = "tolerates";
	public static final String INSTALL_POLICY_KEY = "installPolicy";
	public static final String VISIBILITY_KEY = "visibility";
	public static final String VISIBILITY_VALUE_PUBLIC = "PUBLIC";
	public static final String VISIBILITY_VALUE_INSTALL = "INSTALL";
	public static final String MAVEN_COORDINATES_KEY = "mavenCoordinates";
	public static final String LICENSE_ID_KEY = "licenseId";
	public static final String LICENSE_ID_RESTRICTED_SUBSTRING = "restricted";
	
	public static final String API_DEPENDENCIES_GROUP_ID = "com.ibm.websphere.appserver.api";
	public static final String SPI_DEPENDENCIES_GROUP_ID = "com.ibm.websphere.appserver.spi";
	public static final String MANIFEST_ZIP_ENTRY = "OSGI-INF/SUBSYSTEM.MF";
	public static final String SUBSYSTEM_CONTENT = "Subsystem-Content";
	public static final String SUBSYSTEM_MAVEN_COORDINATES = "mavenCoordinates";

	
	public static final String WEBSPHERE_LIBERTY_FEATURES_GROUP_ID = "com.ibm.websphere.appserver.features";
	public static final String OPEN_LIBERTY_FEATURES_GROUP_ID = "io.openliberty.features";
	public static final String JSON_ARTIFACT_ID = "features";
	public static final String BOM_ARTIFACT_ID = "features-bom";

	
	public static final String LICENSE_DISTRIBUTION_REPO = "repo";
	public static final String LICENSE_NAME_EPL = "Eclipse Public License";
	public static final String LICENSE_URL_EPL = "https://www.eclipse.org/legal/epl-v10.html";
	public static final String LICENSE_NAME_FEATURE_TERMS = "Additional Features Terms & Conditions";
	public static final String LICENSE_URL_FEATURE_TERMS_PREFIX = "http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/";
	public static final String LICENSE_URL_FEATURE_TERMS_SUFFIX = "/lafiles/featureTerms/";
	public static final String LICENSE_URL_FEATURE_TERMS_RESTRICTED_SUFFIX = "/lafiles/featureTerms-restricted/";
	public static final String LICENSE_NAME_MAVEN = "IBM International License Agreement for Non-Warranted Programs";
	public static final String LICENSE_URL_MAVEN = "http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/maven/licenses/L-JTHS-8SZMHX/HTML/";
	public static final String LICENSE_COMMENTS_MAVEN = "Additional notices http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/maven/licenses/L-JTHS-8SZMHX/HTML/notices.html";
	
	public static final String WEBSPHERE_LIBERTY_BOM = "WebSphere Liberty features bill of materials";
	public static final String WEBSPHERE_LIBERTY_JSON = "WebSphere Liberty features JSON";
	public static final String OPEN_LIBERTY_BOM = "Open Liberty features bill of materials";
	public static final String OPEN_LIBERTY_JSON = "Open Liberty features JSON";

	public static final String OPEN_LIBERTY_SCM_CONNECTION = "scm:git:git@github.com:OpenLiberty/open-liberty.git";
	public static final String OPEN_LIBRETY_SCM_URL = "git@github.com:OpenLiberty/open-liberty.git";
	public static final String OPEN_LIBERTY_SCM_TAG = "HEAD";
	public static final String OPEN_LIBERTY_URL = "https://openliberty.io/";
	
	public static final String DEV_ID = "ericglau";
	public static final String DEV_NAME = "Eric Lau";
	public static final String DEV_EMAIL = "ericglau@ca.ibm.com"; 
	
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
