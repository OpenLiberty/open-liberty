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
package com.ibm.ws.wlp.mavenFeatures;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.wlp.mavenFeatures.model.LibertyFeature;
import com.ibm.ws.wlp.mavenFeatures.model.MavenCoordinates;
import com.ibm.ws.wlp.mavenFeatures.model.MavenRepoGeneratorException;
import com.ibm.ws.wlp.mavenFeatures.utils.Constants;
import com.ibm.ws.wlp.mavenFeatures.utils.Utils;

public class LibertyFeaturesToMavenRepo extends Task {
	
	/**
	 * inputDirPath-  the source directory with existing ESA and JSON files.
	 * outputDirPath- the target directory for the Maven repository to be generated.
	 */
	private String inputDirPath;
	private String outputDirPath;
	private String openLibertyJson;
	private String websphereLibertyJson;
	
	/**
	 * Generates a Maven repository from a given directory of existing Liberty
	 * assets in ESA and JSON format.   
	 */	
	@Override
	public void execute() throws BuildException {
	
	      
		if (inputDirPath==null || outputDirPath==null) {
			System.err.println("Usage: inputDir outputDir");
			System.err.println("  inputDir is the source directory with existing ESA and JSON files.");
			System.err.println("  outputDir is the target directory for the Maven repository to be generated.");
			System.exit(1);
		}

		File inputDir = new File(inputDirPath);
		File outputDir = new File(outputDirPath);
		
		System.out.println("Input dir: " + inputDir.getAbsolutePath());
		System.out.println("Output dir: " + outputDir.getAbsolutePath());

		File modifiedOpenLibertyJsonFile = null;
		File modifiedWebsphereLibertyJsonFile = null;
		try {
			// initialize output dir
			outputDir.mkdirs();
			if (!outputDir.isDirectory()) {
				throw new MavenRepoGeneratorException("Output dir " + outputDir + " could not be created or is not a directory.");
			}
			
			// parse OL features JSON
			Map<String, LibertyFeature> allFeatures = new HashMap<String, LibertyFeature>();
			if (openLibertyJson != null) {
				File openLibertyJsonFile = new File(openLibertyJson);
				try {
					modifiedOpenLibertyJsonFile = File.createTempFile(openLibertyJsonFile.getName(), Constants.ArtifactType.JSON.getLibertyFileExtension());
				} catch (IOException e) {
				 	throw new MavenRepoGeneratorException("Could not create temporary file", e);
				}
				allFeatures.putAll(parseFeaturesAndCreateModifiedJson(openLibertyJsonFile, modifiedOpenLibertyJsonFile, false));
				  		
			}
			// parse CL features JSON
			if (websphereLibertyJson != null) {
				File websphereLibertyJsonFile = new File(websphereLibertyJson);
				try {
					modifiedWebsphereLibertyJsonFile = File.createTempFile(websphereLibertyJsonFile.getName(), Constants.ArtifactType.JSON.getLibertyFileExtension());
				} catch (IOException e) {
					throw new MavenRepoGeneratorException("Could not create temporary file", e);
				}
				
				allFeatures.putAll(parseFeaturesAndCreateModifiedJson(websphereLibertyJsonFile, modifiedWebsphereLibertyJsonFile, true));
				  			
			}
			
			// parse additional dependencies from XML
			Map<String, List<MavenCoordinates>> additionalDependencies = parseAdditionalDependencies();
			
			// for each LibertyFeature
			for (LibertyFeature feature : allFeatures.values()) {
				
				// copy ESA to target dirs
				System.out.println("This is the feature: "+ feature.getSymbolicName());
				copyArtifact(inputDir, outputDir, feature, Constants.ArtifactType.ESA);

				// generate POM in target dir with list of dependencies
				generatePom(feature, additionalDependencies, allFeatures, outputDir, Constants.ArtifactType.ESA);
			}
			
			// Use the first found feature version to determine product version
			LibertyFeature firstFeature = allFeatures.values().iterator().next();
			String version = firstFeature.getProductVersion();
			// Sanity check: ensure all of the feature versions are the same
			for (LibertyFeature feature : allFeatures.values()) {
				if (!version.equals(feature.getProductVersion())) {
					throw new MavenRepoGeneratorException("Product versions do not match for features " + firstFeature.getSymbolicName() + ":" + version + " and " + feature.getSymbolicName() + ":" + feature.getProductVersion());
				}
			}
			
			
			// Copy JSON artifacts and generate POMs
			if (openLibertyJson != null) {
				copyJsonArtifact(modifiedOpenLibertyJsonFile, outputDir, version, false);
				generateJsonPom(outputDir, version, false);
			}
			if (websphereLibertyJson != null) {
				copyJsonArtifact(modifiedWebsphereLibertyJsonFile, outputDir, version, true);
				generateJsonPom(outputDir, version, true);
			}
			
			System.out.println("Successfully generated Maven artifacts from Liberty features.");
		} catch (MavenRepoGeneratorException e) {
			System.out.println("Failed to generate Maven artifacts from Liberty features. Exception: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

	}
	/**
	 * Set value of the source directory with existing ESA and JSON files.
	 */
	public void setInputDirPath(String inputDirPath) {
		this.inputDirPath = inputDirPath;
	 }
	
	/**
	 * 	Set the value of the target directory for the Maven repository to be
	 *  generated.
	 */
	public void setOutputDirPath(String outputDirPath) {
		this.outputDirPath = outputDirPath;
	 }

	/**
	 * Set the path to the single JSON for Open Liberty features
	 */
	public void setOpenLibertyJson(String openLibertyJson) {
		this.openLibertyJson = openLibertyJson;
	}
	
	/**
	 * Set the path to the single JSON for WebSphere Liberty features
	 */	
	public void setWebsphereLibertyJson(String websphereLibertyJson) {
		this.websphereLibertyJson = websphereLibertyJson;
	}
	
	/**
	 * Generate POM file.
	 * 
	 * @param feature The Liberty feature to generate POM for.
	 * @param additionalDependencies Map of each feature's symbolic name to additional dependencies.
	 * @param allFeatures The map of feature symbolic names to LibertyFeature objects.
	 * @param outputDir The root directory of the target Maven repository.
	 * @param type The type of artifact.
	 * @throws MavenRepoGeneratorException If the POM file could not be written.
	 */
	private static void generatePom(LibertyFeature feature, Map<String, List<MavenCoordinates>> additionalDependencies, Map<String, LibertyFeature> allFeatures, File outputDir,
			Constants.ArtifactType type) throws MavenRepoGeneratorException {
		MavenCoordinates coordinates = feature.getMavenCoordinates();
		Model model = new Model();
		model.setModelVersion(Constants.MAVEN_MODEL_VERSION);
		model.setGroupId(coordinates.getGroupId());
		model.setArtifactId(coordinates.getArtifactId());
		model.setVersion(coordinates.getVersion());
		model.setName(feature.getName());
		model.setDescription(feature.getDescription());
		model.setPackaging(type.getType());
		
		List<Dependency> dependencies = new ArrayList<Dependency>();
		model.setDependencies(dependencies);
					
		// ESA depends on other ESAs
		List<LibertyFeature> requiredFeatures = feature.getRequiredFeatures(allFeatures);
		if (!requiredFeatures.isEmpty()) {
			for (LibertyFeature requiredFeature : requiredFeatures) {
				MavenCoordinates requiredArtifact = requiredFeature.getMavenCoordinates();
				addDependency(dependencies, requiredArtifact, type);
			}
		}
		
		// add additional dependencies that it should provide
		if (additionalDependencies != null && additionalDependencies.containsKey(feature.getSymbolicName())) {
			List<MavenCoordinates> artifacts = additionalDependencies.get(feature.getSymbolicName());
			for (MavenCoordinates requiredArtifact : artifacts) {
				addDependency(dependencies, requiredArtifact, null);
			}
		}
		
		File artifactDir = new File(outputDir, Utils.getRepositorySubpath(coordinates));
		File targetFile = new File(artifactDir, Utils.getFileName(coordinates, Constants.ArtifactType.POM));
		
		try {
			Writer writer = new FileWriter(targetFile);
			new MavenXpp3Writer().write( writer, model );
			writer.close();
		} catch (IOException e) {
			throw new MavenRepoGeneratorException("Could not write POM file " + targetFile, e);
		}
		
	}
	
	private void generateJsonPom(File outputDir, String version, boolean isWebsphereLiberty) throws MavenRepoGeneratorException {		
		String groupId = isWebsphereLiberty ? Constants.WEBSPHERE_LIBERTY_FEATURES_GROUP_ID : Constants.OPEN_LIBERTY_FEATURES_GROUP_ID;
		MavenCoordinates coordinates = new MavenCoordinates(groupId, Constants.JSON_ARTIFACT_ID, version);
		Model model = new Model();
		model.setModelVersion(Constants.MAVEN_MODEL_VERSION);
		model.setGroupId(coordinates.getGroupId());			
		model.setArtifactId(coordinates.getArtifactId());
		model.setVersion(coordinates.getVersion());
		model.setPackaging(Constants.ArtifactType.JSON.getType());
		
		List<Dependency> dependencies = new ArrayList<Dependency>();
		model.setDependencies(dependencies);
					
		// WL JSON POM depends on OL JSON POM
		if (isWebsphereLiberty && openLibertyJson != null) {
			MavenCoordinates openLibertyCoordinates = new MavenCoordinates(Constants.OPEN_LIBERTY_FEATURES_GROUP_ID, Constants.JSON_ARTIFACT_ID, version);
			addDependency(dependencies, openLibertyCoordinates, Constants.ArtifactType.JSON);
		}
				
		File artifactDir = new File(outputDir, Utils.getRepositorySubpath(coordinates));
		File targetFile = new File(artifactDir, Utils.getFileName(coordinates, Constants.ArtifactType.POM));
		
		try {
			Writer writer = new FileWriter(targetFile);
			new MavenXpp3Writer().write( writer, model );
			writer.close();
		} catch (IOException e) {
			throw new MavenRepoGeneratorException("Could not write POM file " + targetFile, e);
		}
		
	}

	/**
	 * Add dependency to the list of Maven Dependencies.
	 * 
	 * @param dependencies The list of dependencies to append to.
	 * @param requiredArtifact The required artifact to add as a dependency.
	 * @param type The type of artifact, or null if jar.
	 */
	private static void addDependency(List<Dependency> dependencies, MavenCoordinates requiredArtifact, Constants.ArtifactType type) {
		Dependency dependency = new Dependency();
		dependency.setGroupId(requiredArtifact.getGroupId());
		dependency.setArtifactId(requiredArtifact.getArtifactId());
		dependency.setVersion(requiredArtifact.getVersion());
		if (type != null) {
			dependency.setType(type.getType());
		}
		dependencies.add(dependency);
	}

	/**
	 * Copy artifact file from inputDir to outputDir.
	 * 
	 * @param inputDir The input directory containing feature files.
	 * @param outputDir The root directory of the target Maven repository.
	 * @param feature The feature that you want to copy.
	 * @param type The type of artifact.
	 * @throws MavenRepoGeneratorException If the artifact could not be copied.
	 */
	private static void copyArtifact(File inputDir, File outputDir, LibertyFeature feature, Constants.ArtifactType type) throws MavenRepoGeneratorException {
		MavenCoordinates artifact = feature.getMavenCoordinates();
		File artifactDir = new File(outputDir, Utils.getRepositorySubpath(artifact));
		
		artifactDir.mkdirs();
		if (!artifactDir.isDirectory()) {
			throw new MavenRepoGeneratorException("Artifact directory " + artifactDir + " could not be created.");
		}

		File sourceFile = new File(inputDir, feature.getSymbolicName() + type.getLibertyFileExtension());
		System.out.println("This is the source file:" +sourceFile);
		System.out.println("This is the inputDir:"+inputDir);
		System.out.println("This is the name:" +feature.getSymbolicName() + type.getLibertyFileExtension() );
		if (!sourceFile.exists()) {
			throw new MavenRepoGeneratorException("Artifact source file " + sourceFile + " does not exist.");
		}

		File targetFile = new File(artifactDir, Utils.getFileName(artifact, type));
		
		try {
			Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new MavenRepoGeneratorException("Could not copy artifact from " + sourceFile.getAbsolutePath() + " to " + targetFile.getAbsolutePath(), e);
		}
	}
	
	private static void copyJsonArtifact(File jsonFile, File outputDir, String version, boolean isWebsphereLiberty) throws MavenRepoGeneratorException {
		String groupId = isWebsphereLiberty ? Constants.WEBSPHERE_LIBERTY_FEATURES_GROUP_ID : Constants.OPEN_LIBERTY_FEATURES_GROUP_ID;
		MavenCoordinates coordinates = new MavenCoordinates(groupId, Constants.JSON_ARTIFACT_ID, version);
		File artifactDir = new File(outputDir, Utils.getRepositorySubpath(coordinates));
		
		artifactDir.mkdirs();
		if (!artifactDir.isDirectory()) {
			throw new MavenRepoGeneratorException("Artifact directory " + artifactDir + " could not be created.");
		}

		File targetFile = new File(artifactDir, Utils.getFileName(coordinates, Constants.ArtifactType.JSON));
		
		try {
			Files.copy(jsonFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new MavenRepoGeneratorException("Could not copy artifact from " + jsonFile.getAbsolutePath() + " to " + targetFile.getAbsolutePath(), e);
		}
	}

	/**
	 * Parse JSON to create map of feature symbolic names to LibertyFeature objects, which includes all required metadata for each feature.
	 * 
	 * @param jsonFile The JSON file describing all features
	 * @param isWebsphereLiberty If true then the JSON is for WebSphere Liberty, else it is Open Liberty
	 * @return Map of feature symbolic names to LibertyFeature objects
	 * @throws MavenRepoGeneratorException If there was an error with parsing a file.
	 */
	private static Map<String, LibertyFeature> parseFeaturesAndCreateModifiedJson(File jsonFile, File modifiedJsonFile, boolean isWebsphereLiberty) throws MavenRepoGeneratorException {
		Map<String, LibertyFeature> features = new HashMap<String, LibertyFeature>();

		try {
			InputStream is = new FileInputStream(jsonFile);
			String jsonTxt = IOUtils.toString(is, (Charset) null);
			JsonReader jsonReader = Json.createReader(new StringReader(jsonTxt));
			JsonArray jsonArray = jsonReader.readArray();
			jsonReader.close();

			for (int i = 0; i < jsonArray.size(); i++) {
				JsonObject json = jsonArray.getJsonObject(i);

				JsonObject wlpInfo = json.getJsonObject(Constants.WLP_INFORMATION_KEY);

				JsonArray provideFeatureArray = wlpInfo.getJsonArray(Constants.PROVIDE_FEATURE_KEY);
				String symbolicName = provideFeatureArray.getString(0);

				String appliesTo = wlpInfo.getString(Constants.APPLIES_TO_KEY);
				String productVersion = parseProductVersion(appliesTo);
				if (productVersion == null) {
					throw new MavenRepoGeneratorException("Cannot parse product version from " + Constants.APPLIES_TO_KEY
							+ " key for feature " + symbolicName + " in file " + jsonFile.getAbsolutePath());
				}

				JsonObject wlpInfo2 = json.containsKey(Constants.WLP_INFORMATION_2_KEY) ? json.getJsonObject(Constants.WLP_INFORMATION_2_KEY) : null;
				boolean isBundle = (wlpInfo2 != null) && wlpInfo2.containsKey(Constants.VISIBILITY_KEY) && Constants.VISIBILITY_VALUE_INSTALL.equals(wlpInfo2.getString(Constants.VISIBILITY_KEY));
				boolean isPublicFeature = wlpInfo.containsKey(Constants.VISIBILITY_KEY) && Constants.VISIBILITY_VALUE_PUBLIC.equals(wlpInfo.getString(Constants.VISIBILITY_KEY));
				String shortName = null;
				if (isBundle || isPublicFeature) {
					if (wlpInfo.containsKey(Constants.SHORT_NAME_KEY)) {
						shortName = wlpInfo.getString(Constants.SHORT_NAME_KEY);
					}
				}

				// parse requireFeature
				Map<String, Collection<String>> requireFeaturesWithTolerates = new HashMap<String, Collection<String>>();
				if (wlpInfo.containsKey(Constants.REQUIRE_FEATURE_KEY)) {
					JsonArray requireFeatureArray = wlpInfo.getJsonArray(Constants.REQUIRE_FEATURE_KEY);
					if (requireFeatureArray != null && requireFeatureArray.size() > 0) {
						for (int j = 0; j < requireFeatureArray.size(); j++) {
							requireFeaturesWithTolerates.put(requireFeatureArray.getString(j), null);
						}
					}
				}
				
				// parse requireFeatureWithTolerates and add to the above map (overwriting any existing null value)
				if (wlpInfo.containsKey(Constants.REQUIRE_FEATURE_WITH_TOLERATES_KEY)) {
					JsonArray requireFeatureWithToleratesArray = wlpInfo.getJsonArray(Constants.REQUIRE_FEATURE_WITH_TOLERATES_KEY);
					if (requireFeatureWithToleratesArray != null && requireFeatureWithToleratesArray.size() > 0) {
						for (int j = 0; j < requireFeatureWithToleratesArray.size(); j++) {
							JsonObject requireFeatureWithToleratesObject = requireFeatureWithToleratesArray.getJsonObject(j);
							String requireFeature = requireFeatureWithToleratesObject.getString(Constants.FEATURE_KEY);
							if (requireFeatureWithToleratesObject.containsKey(Constants.TOLERATES_KEY)) {
								JsonArray tolerateArray = requireFeatureWithToleratesObject.getJsonArray(Constants.TOLERATES_KEY);
								Collection<String> tolerateVersions = new ArrayList<String>();
								for (int k = 0; k < tolerateArray.size(); k++) {
									tolerateVersions.add(tolerateArray.getString(k));
								}
								requireFeaturesWithTolerates.put(requireFeature, tolerateVersions);
							} else {
								requireFeaturesWithTolerates.put(requireFeature, null);
							}
						}
					}
				}

				String name = json.getString(Constants.NAME_KEY);
				String description = json.getString(Constants.SHORT_DESCRIPTION_KEY);
				if (description.equals("%description")){
					description = name;
				}
				
				String mavenCoordinates = null;
				if (wlpInfo.containsKey(Constants.MAVEN_COORDINATES_KEY)) {
					mavenCoordinates = wlpInfo.getString(Constants.MAVEN_COORDINATES_KEY);
					String[] tokens = mavenCoordinates.split(":");
					if (tokens.length != 3) {
						throw new MavenRepoGeneratorException("String " + mavenCoordinates + " is not a valid Maven coordinate string. Expected format is groupId:artifactId:version");
					}
				}

				LibertyFeature feature = new LibertyFeature(symbolicName, shortName, name, description, requireFeaturesWithTolerates, productVersion, mavenCoordinates, isWebsphereLiberty);
				
				features.put(symbolicName, feature);
			}
			
			// Add Maven coordinates into a modified copy of the JSON
			try {
				addMavenCoordinates(modifiedJsonFile, jsonArray, features);
			} catch (IOException e) {
				throw new MavenRepoGeneratorException(
						"Could not write modified JSON to temporary file " + modifiedJsonFile, e);
			}
		} catch (FileNotFoundException e) {
			throw new MavenRepoGeneratorException("JSON file " + jsonFile + " does not exist.", e);
		} catch (IOException e) {
			throw new MavenRepoGeneratorException("Could not parse JSON file " + jsonFile, e);
		}
		return features;
	}
	
	/**
	 * Add Maven coordinates into the modified JSON file.
	 * 
	 * @param modifiedJsonFile
	 *            The location to write the modified JSON file.
	 * @param jsonArray
	 *            The original JSON array of all features.
	 * @param features
	 *            The map of symbolic names to LibertyFeature objects which has
	 *            Maven coordinates.
	 * @throws IOException
	 */
	private static void addMavenCoordinates(File modifiedJsonFile, JsonArray jsonArray,
			Map<String, LibertyFeature> features) throws IOException {
		JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

		for (int i = 0; i < jsonArray.size(); i++) {
			JsonObject jsonObject = jsonArray.getJsonObject(i);
			JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder(jsonObject);

			JsonObject wlpInfo = jsonObject.getJsonObject(Constants.WLP_INFORMATION_KEY);
			JsonObjectBuilder wlpInfoBuilder = Json.createObjectBuilder(wlpInfo);

			JsonArray provideFeatureArray = wlpInfo.getJsonArray(Constants.PROVIDE_FEATURE_KEY);
			String symbolicName = provideFeatureArray.getString(0);

			wlpInfoBuilder.add(Constants.MAVEN_COORDINATES_KEY,
					features.get(symbolicName).getMavenCoordinates().toString());
			jsonObjectBuilder.add(Constants.WLP_INFORMATION_KEY, wlpInfoBuilder);
			jsonArrayBuilder.add(jsonObjectBuilder);
		}

		// Write JSON to the modified file
		FileOutputStream out = null;
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(JsonGenerator.PRETTY_PRINTING, true);
			JsonWriterFactory writerFactory = Json.createWriterFactory(config);
			out = new FileOutputStream(modifiedJsonFile);
			JsonWriter streamWriter = writerFactory.createWriter(out);
			streamWriter.write(jsonArrayBuilder.build());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/**
	 * Parse additional dependencies from XML file in project resources.
	 * 
	 * @return Map of each feature's symbolic name to a list of Maven coordinates for its dependencies
	 * @throws MavenRepoGeneratorException If the XML file could not be parsed.
	 */
	private Map<String, List<MavenCoordinates>> parseAdditionalDependencies() throws MavenRepoGeneratorException {
		Map<String, List<MavenCoordinates>> dependenciesMap = new HashMap<String, List<MavenCoordinates>>();
		
		InputStream in = getClass().getResourceAsStream("/featureMavenDependency.xml"); 		
		if (in == null) {
			throw new MavenRepoGeneratorException("Cannot find resource featureMavenDependency.xml");
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			Document document = db.parse(in);
			document.getDocumentElement().normalize();
			NodeList featureNodeList = document.getElementsByTagName("feature");
			for (int i = 0; i < featureNodeList.getLength(); i++) {
				Node featureNode = featureNodeList.item(i);
				if (featureNode.getNodeType() == Node.ELEMENT_NODE) {
					Element featureElement = (Element) featureNode;
					String featureName = featureElement.getAttribute("name");
					NodeList mavenDependenciesNodeList = featureElement.getElementsByTagName("mavendependencies");
					Element mavenDependenciesElement = (Element) mavenDependenciesNodeList.item(0);
					NodeList dependenciesNodeList = mavenDependenciesElement.getElementsByTagName("dependency");
					List<MavenCoordinates> artifacts = new ArrayList<MavenCoordinates>();
					for (int j = 0; j < dependenciesNodeList.getLength(); j++) {
						Node dependencyNode = dependenciesNodeList.item(j);
						if (dependencyNode.getNodeType() == Node.ELEMENT_NODE) {
							Element dependencyElement = (Element) dependencyNode;
							String groupId = dependencyElement.getElementsByTagName("groupId").item(0).getTextContent();
							String artifactId = dependencyElement.getElementsByTagName("artifactId").item(0).getTextContent();
							String version = dependencyElement.getElementsByTagName("version").item(0).getTextContent();
							artifacts.add(new MavenCoordinates(groupId, artifactId, version));
						}
					}
					dependenciesMap.put(featureName, artifacts);
				}
			}
		} catch (Exception e) {
			throw new MavenRepoGeneratorException("Failed to parse featureMavenDependency.xml", e);
		}
		
		return dependenciesMap;
	}
	
	private static String parseProductVersion(String appliesTo) {
		String productVersion = null;
		String[] tokens = appliesTo.split(";");
		for (String token : tokens) {
			String trimmed = token.trim();
			if (trimmed.startsWith(Constants.APPLIES_TO_VALUE_PRODUCT_VERSION)) {
				productVersion = trimmed.substring(Constants.APPLIES_TO_VALUE_PRODUCT_VERSION.length() + 1);
				// remove quote characters
				productVersion = productVersion.replace("\"", "").replace("'", "");
			}
		}
		return productVersion;
	}

}