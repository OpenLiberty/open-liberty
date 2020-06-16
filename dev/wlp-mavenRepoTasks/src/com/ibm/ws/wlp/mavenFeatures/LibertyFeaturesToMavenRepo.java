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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.apache.aries.util.manifest.ManifestProcessor;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.LogLevel;

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
	private String releaseVersion;

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

			// parse Open Liberty features JSON
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
			// parse WebSphere Liberty features JSON
			if (websphereLibertyJson != null) {
				File websphereLibertyJsonFile = new File(websphereLibertyJson);
				try {
					modifiedWebsphereLibertyJsonFile = File.createTempFile(websphereLibertyJsonFile.getName(), Constants.ArtifactType.JSON.getLibertyFileExtension());
				} catch (IOException e) {
					throw new MavenRepoGeneratorException("Could not create temporary file", e);
				}

				allFeatures.putAll(parseFeaturesAndCreateModifiedJson(websphereLibertyJsonFile, modifiedWebsphereLibertyJsonFile, true));

			}

			// for each LibertyFeature
			for (LibertyFeature feature : allFeatures.values()) {
				if (websphereLibertyJson != null && !feature.isWebsphereLiberty()) {
					// if WebSphere Liberty build, only output WebSphere Liberty artifacts
					continue;
				}

				// copy ESA to target dirs
				copyArtifact(inputDir, outputDir, feature, Constants.ArtifactType.ESA);

				List<MavenCoordinates> featureCompileDependencies = getFeatureCompileDependencies(inputDir, feature);

				// generate POM in target dir with list of dependencies
				generatePom(feature, featureCompileDependencies, allFeatures, outputDir, Constants.ArtifactType.ESA);
			}

			String version;
			if (releaseVersion == null) {
				// Use the first found feature version to determine product version
				LibertyFeature firstFeature = allFeatures.values().iterator().next();
				version = firstFeature.getProductVersion();

				// Sanity check: ensure all of the feature versions are the same
				for (LibertyFeature feature : allFeatures.values()) {
					if (!version.equals(feature.getProductVersion())) {
						log("Product versions do not match for features " + firstFeature.getSymbolicName() + ":" + version + " and " + feature.getSymbolicName() + ":" + feature.getProductVersion(), LogLevel.WARN.getLevel());
					}
				}
			} else {
				version = releaseVersion;

				// Sanity check: ensure all of the feature versions are the same
				for (LibertyFeature feature : allFeatures.values()) {
					if (!version.equals(feature.getProductVersion())) {
						log("Product versions do not match. Expected release version " + version + ", actual feature and version " + feature.getSymbolicName() + ":" + feature.getProductVersion(), LogLevel.WARN.getLevel());
					}
				}
			}


			// Copy JSON artifacts and generate POMs for either Open or WebSphere Liberty	
			//generate Bill of Materials for either Open or WebSphere Liberty
			if (websphereLibertyJson == null) {
				copyJsonArtifact(modifiedOpenLibertyJsonFile, outputDir, version, false);
				generateJsonPom(outputDir, version, false);
				generateBOM(false,version,outputDir,allFeatures,Constants.ArtifactType.ESA);

			} else {
				copyJsonArtifact(modifiedWebsphereLibertyJsonFile, outputDir, version, true);
				generateJsonPom(outputDir, version, true);
				generateBOM(true,version,outputDir,allFeatures,Constants.ArtifactType.ESA);

			}

			// generate pom.xml for license packages (wlp-base-license and wlp-nd-license)
			if(websphereLibertyJson != null){
				generateLicensePom(outputDir, Constants.BASE_LICENSE_ARTIFACT_ID, version, Constants.BASE_LICENSE_NAME,true);
				generateLicensePom(outputDir, Constants.ND_LICENSE_ARTIFACT_ID, version, Constants.ND_LICENSE_NAME,true);
			}


			System.out.println("Successfully generated Maven artifacts from Liberty features.");
		} catch (MavenRepoGeneratorException e) {
			System.out.println("Failed to generate Maven artifacts from Liberty features. Exception: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Failed to generate BOM from Liberty features. Exception: " + e.getMessage());
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
	 * Set the expected release version.
	 */
	public void setReleaseVersion(String releaseVersion) {
		this.releaseVersion = releaseVersion;
	}

	/**
	 * Generate POM file.
	 *
	 * @param feature The Liberty feature to generate POM for.
	 * @param featureCompileDependencies List of compile dependencies that the feature should provide.
	 * @param allFeatures The map of feature symbolic names to LibertyFeature objects.
	 * @param outputDir The root directory of the target Maven repository.
	 * @param type The type of artifact.
	 * @throws MavenRepoGeneratorException If the POM file could not be written.
	 */
	private void generatePom(LibertyFeature feature, List<MavenCoordinates> featureCompileDependencies, Map<String, LibertyFeature> allFeatures, File outputDir,
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
		setLicense(model, coordinates.getVersion(), true, feature.isRestrictedLicense(), Constants.WEBSPHERE_LIBERTY_FEATURES_GROUP_ID.equals(coordinates.getGroupId()));
		boolean isWebsphereLiberty = Constants.WEBSPHERE_LIBERTY_FEATURES_GROUP_ID.equals(coordinates.getGroupId());
		if (!isWebsphereLiberty){
			setScmDevUrl(model);
		}

		List<Dependency> dependencies = new ArrayList<Dependency>();
		model.setDependencies(dependencies);

		// ESA depends on other ESAs
		List<LibertyFeature> requiredFeatures = getRequiredFeatures(feature, allFeatures);
		if (!requiredFeatures.isEmpty()) {
			for (LibertyFeature requiredFeature : requiredFeatures) {
				MavenCoordinates requiredArtifact = requiredFeature.getMavenCoordinates();
				addDependency(dependencies, requiredArtifact, type,null);
			}
		}

		if (featureCompileDependencies != null) {
			for (MavenCoordinates requiredArtifact : featureCompileDependencies) {
				addDependency(dependencies, requiredArtifact, null,null);
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

	private static void generateBOM(boolean isWebsphereLiberty, String version,File outputDir, Map<String, LibertyFeature> allFeatures,Constants.ArtifactType type) throws MavenRepoGeneratorException, IOException {

		String groupId = isWebsphereLiberty ? Constants.WEBSPHERE_LIBERTY_FEATURES_GROUP_ID : Constants.OPEN_LIBERTY_FEATURES_GROUP_ID;
		MavenCoordinates coordinates = new MavenCoordinates(groupId, Constants.BOM_ARTIFACT_ID, version);
		Model model = new Model();
		model.setModelVersion(Constants.MAVEN_MODEL_VERSION);
		model.setGroupId(coordinates.getGroupId());
		model.setArtifactId(coordinates.getArtifactId());
		model.setVersion(coordinates.getVersion());
		model.setPackaging(Constants.ArtifactType.POM.getType());
		setLicense(model,version, false, false,isWebsphereLiberty);



		List<Dependency> dependencies = new ArrayList<Dependency>();
		DependencyManagement dependencyManagement = new DependencyManagement();
		model.setDependencyManagement(dependencyManagement);
		dependencyManagement.setDependencies(dependencies);

		for (LibertyFeature feature : allFeatures.values()) {
			MavenCoordinates requiredArtifact = feature.getMavenCoordinates();
			if(requiredArtifact.getGroupId()==coordinates.getGroupId()){
				addDependency(dependencies, requiredArtifact,type,"provided");
			}

		}

		if(isWebsphereLiberty){
			MavenCoordinates openLibertyCoordinates = new MavenCoordinates(Constants.OPEN_LIBERTY_FEATURES_GROUP_ID, Constants.BOM_ARTIFACT_ID, version);
			addDependency(dependencies,openLibertyCoordinates, Constants.ArtifactType.POM,"import");
			model.setName(Constants.WEBSPHERE_LIBERTY_BOM);
			model.setDescription(Constants.WEBSPHERE_LIBERTY_BOM);
		} else{
			model.setName(Constants.OPEN_LIBERTY_BOM);
			model.setDescription(Constants.OPEN_LIBERTY_BOM);
			setScmDevUrl(model);
		}

		File artifactDir = new File(outputDir, Utils.getRepositorySubpath(coordinates));
		artifactDir.mkdirs();
		File targetFile = new File(artifactDir, Utils.getFileName(coordinates, Constants.ArtifactType.POM));

		if(!targetFile.exists()){
			targetFile.createNewFile();
		}

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
		setLicense(model, version, false, false, isWebsphereLiberty);


		List<Dependency> dependencies = new ArrayList<Dependency>();
		model.setDependencies(dependencies);

		// WL JSON POM depends on OL JSON POM
		if (isWebsphereLiberty && openLibertyJson != null) {
			MavenCoordinates openLibertyCoordinates = new MavenCoordinates(Constants.OPEN_LIBERTY_FEATURES_GROUP_ID, Constants.JSON_ARTIFACT_ID, version);
			addDependency(dependencies, openLibertyCoordinates, Constants.ArtifactType.JSON,null);
			model.setName(Constants.WEBSPHERE_LIBERTY_JSON);
			model.setDescription(Constants.WEBSPHERE_LIBERTY_JSON);
		} else {
			model.setName(Constants.OPEN_LIBERTY_JSON);
			model.setDescription(Constants.OPEN_LIBERTY_JSON);
			setScmDevUrl(model);
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
	 * Generates the pom.xml files for the wlp-base-license and wlp-nd-license artifacts
	 * @param outputDir
	 * @param version
	 * @param isWebsphereLiberty
	 * @throws MavenRepoGeneratorException
	 */
	private void generateLicensePom(File outputDir, String artifactId, String version, String name, boolean isWebsphereLiberty) throws MavenRepoGeneratorException {
		String groupId = Constants.WEBSPHERE_LIBERTY_FEATURES_GROUP_ID;
		MavenCoordinates coordinates = new MavenCoordinates(groupId, artifactId, version);
		Model model = new Model();
		model.setModelVersion(Constants.MAVEN_MODEL_VERSION);
		model.setGroupId(coordinates.getGroupId());
		model.setArtifactId(coordinates.getArtifactId());
		model.setVersion(coordinates.getVersion());
		model.setPackaging(Constants.ArtifactType.ZIP.getType());
		setLicense(model, version, false, false, isWebsphereLiberty);

		model.setName(name);
		model.setDescription(name);


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



	private static void setLicense(Model model, String version, boolean feature, boolean restrictedLicense, boolean isWebsphereLiberty) {
		License license = new License();
		if (!isWebsphereLiberty) {
			license.setName(Constants.LICENSE_NAME_EPL);
			license.setUrl(Constants.LICENSE_URL_EPL);
			license.setDistribution(Constants.LICENSE_DISTRIBUTION_REPO);
		} else if (feature) {
			license.setName(Constants.LICENSE_NAME_FEATURE_TERMS);
			license.setUrl(Constants.LICENSE_URL_FEATURE_TERMS_PREFIX + version + (restrictedLicense ? Constants.LICENSE_URL_FEATURE_TERMS_RESTRICTED_SUFFIX : Constants.LICENSE_URL_FEATURE_TERMS_SUFFIX));
			license.setDistribution(Constants.LICENSE_DISTRIBUTION_REPO);
		} else {
			license.setName(Constants.LICENSE_NAME_MAVEN);
			license.setUrl(Constants.LICENSE_URL_MAVEN);
			license.setDistribution(Constants.LICENSE_DISTRIBUTION_REPO);
			license.setComments(Constants.LICENSE_COMMENTS_MAVEN);
		}
		model.addLicense(license);
	}

	/**
	 * Add dependency to the list of Maven Dependencies.
	 *
	 * @param dependencies The list of dependencies to append to.
	 * @param requiredArtifact The required artifact to add as a dependency.
	 * @param type The type of artifact, or null if jar.
	 */
	private static void addDependency(List<Dependency> dependencies, MavenCoordinates requiredArtifact, Constants.ArtifactType type, String scope) {
		Dependency dependency = new Dependency();
		dependency.setGroupId(requiredArtifact.getGroupId());
		dependency.setArtifactId(requiredArtifact.getArtifactId());
		dependency.setVersion(requiredArtifact.getVersion());

		if(scope!=null){
			dependency.setScope(scope);
		}
		if (type != null) {
			dependency.setType(type.getType());
		}
		dependencies.add(dependency);
	}

	/**
	 * Get list of compile dependencies for each feature by looking in its ESA file.
	 *
	 * @param inputDir Input dir with all ESAs.
	 * @param feature The feature to get dependencies for.
	 * @return List of MavenCoordinates for compile dependencies
	 * @throws MavenRepoGeneratorException If the ESA file could not be read
	 */
	private static List<MavenCoordinates> getFeatureCompileDependencies(File inputDir, LibertyFeature feature) throws MavenRepoGeneratorException {
		List<MavenCoordinates> compileDependencies = new ArrayList<MavenCoordinates>();

		File esa = new File(inputDir, feature.getSymbolicName() + Constants.ArtifactType.ESA.getLibertyFileExtension());
		if (!esa.exists()) {
			throw new MavenRepoGeneratorException("ESA " + esa + " does not exist for feature " + feature.getSymbolicName());
		}

		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(esa);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				// find API dependencies in zip
				MavenCoordinates c = findCompileDependency(zipEntry.getName(), Constants.API_DEPENDENCIES_GROUP_ID);
				if (c != null) {
					compileDependencies.add(c);
					continue;
				}
				// find SPI dependencies in zip
				c = findCompileDependency(zipEntry.getName(), Constants.SPI_DEPENDENCIES_GROUP_ID);
				if (c != null) {
					compileDependencies.add(c);
					continue;
				}
			}
			// find Java spec dependencies from manifest
			ZipEntry manifest = zipFile.getEntry(Constants.MANIFEST_ZIP_ENTRY);
			if (manifest != null) {
				compileDependencies.addAll(findCompileDependenciesFromManifest(zipFile, manifest));
			} else {
				throw new MavenRepoGeneratorException(
						"Could not find manifest file " + Constants.MANIFEST_ZIP_ENTRY + " within the ESA file " + esa);
			}
		} catch (IOException e) {
			throw new MavenRepoGeneratorException("ESA " + esa + " could not be read as a zip file.");
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
					// nothing to do
				}
			}
		}

		return compileDependencies;
	}

	/**
	 * Find compile dependency from a zip entry in the ESA
	 *
	 * @param zipEntryPath The entry path in the zip
	 * @param groupId The group ID of the dependency to look for
	 * @return Maven coordinates corresponding to the entry
	 */
	private static MavenCoordinates findCompileDependency(String zipEntryPath, String groupId) {
		int apiNameIndex = zipEntryPath.indexOf(groupId);
		int extensionIndex = zipEntryPath.lastIndexOf(".jar");
		if (apiNameIndex >= 0 && extensionIndex >= 0) {
			String fileNameWithoutExtension = zipEntryPath.substring(apiNameIndex, extensionIndex);

			String artifactId = fileNameWithoutExtension.substring(0, fileNameWithoutExtension.lastIndexOf("_"));
			String versionId = fileNameWithoutExtension.substring(fileNameWithoutExtension.lastIndexOf("_")+1, fileNameWithoutExtension.length());
			MavenCoordinates coordinates = new MavenCoordinates(groupId, artifactId, versionId);
			System.out.println("Found compile dependency: " + coordinates);
			return coordinates;
		}
		return null;
	}

	/**
	 * Find compile dependencies from the manifest file in the ESA
	 *
	 * @param zipFile The ESA file
	 * @param zipEntry The manifest entry in the zip
	 * @return List of Maven coordinates for compile dependencies found in the manifest, or empty list if none found
	 * @throws MavenRepoGeneratorException If the manifest could not be parsed
	 */
	private static List<MavenCoordinates> findCompileDependenciesFromManifest(ZipFile zipFile, ZipEntry zipEntry) throws MavenRepoGeneratorException {
		List<MavenCoordinates> result = new ArrayList<MavenCoordinates>();
		InputStream inputStream = null;
		try {
			inputStream = zipFile.getInputStream(zipEntry);
			Manifest manifest = new Manifest(inputStream);
			Attributes mainAttributes = manifest.getMainAttributes();
			String subsystemContent = mainAttributes.getValue(Constants.SUBSYSTEM_CONTENT);
			List<String> lines = ManifestProcessor.split(subsystemContent, ",");
			for (String line : lines) {
				if (line.contains(Constants.SUBSYSTEM_MAVEN_COORDINATES)){
					List<String> components = ManifestProcessor.split(line, ";");
					String requiredFeature = components.get(0);
					String mavenCoordinates = null;
					for (String component : components) {
						if (component.startsWith(Constants.SUBSYSTEM_MAVEN_COORDINATES)) {
							mavenCoordinates = component.substring(Constants.SUBSYSTEM_MAVEN_COORDINATES.length() + 1, component.length());
							if (mavenCoordinates.startsWith("\"") && mavenCoordinates.endsWith("\"")) {
								mavenCoordinates = mavenCoordinates.substring(1, mavenCoordinates.length() - 1);
							}
							break;
						}
					}
					if (mavenCoordinates != null) {
						try {
							result.add(new MavenCoordinates(mavenCoordinates));
							System.out.println("Found compile dependency for subsystem content " + requiredFeature + ": " + mavenCoordinates);
						} catch (IllegalArgumentException e) {
							throw new MavenRepoGeneratorException(
									"Invalid Maven coordinates defined in subsystem content " + requiredFeature + " in the manifest for ESA file " + zipFile.getName(), e);
						}
					} else {
						throw new MavenRepoGeneratorException(
								"For ESA " + zipFile.getName() + ", found " + Constants.SUBSYSTEM_MAVEN_COORDINATES + " key in manifest but failed to parse it from the string: " + line);
					}
				}
			}
		} catch (IOException e) {
			throw new MavenRepoGeneratorException("Could not read manifest file " + zipEntry.getName()
					+ " from ESA file " + zipFile.getName(), e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
			}
		}
		return result;
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
		System.out.println("Source file: " +sourceFile);
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
				final String productVersion = parseProductVersion(appliesTo);
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

				String licenseId = json.getString(Constants.LICENSE_ID_KEY);
				boolean restrictedLicense = licenseId.contains(Constants.LICENSE_ID_RESTRICTED_SUBSTRING);

				String mavenCoordinates = null;
				if (wlpInfo.containsKey(Constants.MAVEN_COORDINATES_KEY)) {
					mavenCoordinates = wlpInfo.getString(Constants.MAVEN_COORDINATES_KEY);
				}

				HashMap<String, String> licenseMap = new HashMap<String, String>(){
					{
						put("Base", String.format("%s:%s:%s", Constants.WEBSPHERE_LIBERTY_FEATURES_GROUP_ID, Constants.BASE_LICENSE_ARTIFACT_ID, productVersion));
						put("ND", String.format("%s:%s:%s", Constants.WEBSPHERE_LIBERTY_FEATURES_GROUP_ID, Constants.ND_LICENSE_ARTIFACT_ID, productVersion));
					}
				};


				String minimumLicenseMavenCoordinate = null;
				if(isWebsphereLiberty) {
					if (wlpInfo.containsKey("appliesToFilterInfo")) {
						JsonArray editions = wlpInfo.getJsonArray("appliesToFilterInfo").getJsonObject(0).getJsonArray("editions");

						boolean licenseFound = false;
						for (String license : Constants.LICENSE_PRIORITY) {
							for (int ind = 0; ind < editions.size(); ind++) {
								if (editions.getString(ind).equals(license)) {
									minimumLicenseMavenCoordinate = licenseMap.get(license);
									licenseFound = true;
									break;
								}
							}
							if (licenseFound) {
								break;
							}
						}
					}
				}


				LibertyFeature feature = minimumLicenseMavenCoordinate == null ? new LibertyFeature(symbolicName, shortName, name, description, requireFeaturesWithTolerates, productVersion, mavenCoordinates, isWebsphereLiberty, restrictedLicense)
						: new LibertyFeature(symbolicName, shortName, name, description, requireFeaturesWithTolerates, productVersion, mavenCoordinates, isWebsphereLiberty, restrictedLicense, minimumLicenseMavenCoordinate) ;

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

			if(features.get(symbolicName).getMinimumLicenseMavenCoordinate() != null){
				wlpInfoBuilder.add("licenseMavenCoordinate", features.get(symbolicName).getMinimumLicenseMavenCoordinate());
			}

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

	/**
	 * Gets the list of features that this feature depends on.
	 *
	 * @param feature This feature.
	 * @param allFeatures
	 *            The map of all features, mapping from symbolic name to
	 *            LibertyFeature object
	 * @return List of LibertyFeature objects that this feature depends on.
	 * @throws MavenRepoGeneratorException
	 *             If a required feature cannot be found in the map.
	 */
	public List<LibertyFeature> getRequiredFeatures(LibertyFeature feature, Map<String, LibertyFeature> allFeatures)
			throws MavenRepoGeneratorException {
		List<LibertyFeature> dependencies = new ArrayList<LibertyFeature>();
		Map<String, Collection<String>> requiredFeaturesWithTolerates = feature.getRequiredFeaturesWithTolerates();
		if (requiredFeaturesWithTolerates != null) {
			for (String requireFeature : requiredFeaturesWithTolerates.keySet()) {
				Collection<String> toleratesVersions = null;
				if (allFeatures.containsKey(requireFeature)) {
					dependencies.add(allFeatures.get(requireFeature));
				} else if ((toleratesVersions = requiredFeaturesWithTolerates.get(requireFeature)) != null) {
					log("For feature " + feature.getSymbolicName() + ", cannot find direct dependency to required feature " + requireFeature + " so it must use tolerates list: " + toleratesVersions, LogLevel.WARN.getLevel());
					boolean tolerateFeatureFound = false;
					for (String version : toleratesVersions) {
						String tolerateFeatureAndVersion = requireFeature.substring(0, requireFeature.lastIndexOf("-")) + "-" + version;
						if (allFeatures.containsKey(tolerateFeatureAndVersion)) {
							dependencies.add(allFeatures.get(tolerateFeatureAndVersion));
							log("For feature " + feature.getSymbolicName() + ", found tolerated dependency " + tolerateFeatureAndVersion, LogLevel.DEBUG.getLevel());
							tolerateFeatureFound = true;
							break;
						}
					}
					if (!tolerateFeatureFound) {
						throw new MavenRepoGeneratorException(
								"For feature " + feature.getSymbolicName() + ", cannot find required feature " + requireFeature + " or any of its tolerated versions: " + toleratesVersions);
					}
				} else {
					throw new MavenRepoGeneratorException(
							"For feature " + feature.getSymbolicName() + ", cannot find required feature " + requireFeature);
				}
			}
		}
		return dependencies;
	}

	/**
	 *
	 * @param model
	 */
	private static void setScmDevUrl(Model model){
		model.setScm(new Scm());
		model.getScm().setConnection(Constants.OPEN_LIBERTY_SCM_CONNECTION);
		model.getScm().setDeveloperConnection(Constants.OPEN_LIBERTY_SCM_CONNECTION);
		model.getScm().setUrl(Constants.OPEN_LIBRETY_SCM_URL);
		model.getScm().setTag(Constants.OPEN_LIBERTY_SCM_TAG);
		model.setUrl(Constants.OPEN_LIBERTY_URL);

		Developer dev = new Developer();
		dev.setId(Constants.DEV_ID);
		dev.setName(Constants.DEV_NAME);
		dev.setEmail(Constants.DEV_EMAIL);
		List<Developer> developers = new ArrayList<Developer>();
		developers.add(dev);
		model.setDevelopers(developers);
	}

}