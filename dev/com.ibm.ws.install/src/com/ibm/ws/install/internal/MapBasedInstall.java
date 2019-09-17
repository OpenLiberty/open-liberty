/*******************************************************************************n * Copyright (c) 2019 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License v1.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-v10.htmln *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.install.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 *
 */
public class MapBasedInstall {

    private final InstallKernelMap map;
    private final File fromDir;
	private final String OPEN_LIBERTY_MAVEN_COORDINATES = "io.openliberty.features";
	private final String CLOSED_LIBERTY_MAVEN_COORDINATES = "com.ibm.websphere.appserver.features";
	private String openLibertyVersion = "19.0.0.8";

    /**
     * Initialize a map based install kernel with a local maven directory
     *
     * @param featuresToInstall
     * @param fromDir
     * @throws IOException
     */
    public MapBasedInstall(Collection<String> featuresToInstall, File fromDir) throws IOException {
        map = new InstallKernelMap();
        this.fromDir = fromDir;
		System.out.println("given" + featuresToInstall);

        System.out.println(Utils.getInstallDir().getName().toString());
        map.put("runtime.install.dir", Utils.getInstallDir());
        map.put("target.user.directory", new File(Utils.getInstallDir(), "usr/tmp"));
        map.put("install.local.esa", true);
        map.put("single.json.file", (getSingleJsonPaths(fromDir)));
		map.put("features.to.resolve", new ArrayList<>(featuresToInstall));
        map.put("license.accept", true); // TODO: discuss later
		map.get("install.kernel.init.code");
    }

    /**
     * Resolves and installs the features
     *
     * @throws InstallException
     * @throws IOException
     */
    public void installFeatures() throws InstallException, IOException {
        Collection<String> resolvedFeatures = (Collection<String>) map.get("action.result");
        if (resolvedFeatures == null) {
            throw new InstallException((String) map.get("action.error.message")); // TODO: appropriate message
        } else if (resolvedFeatures.isEmpty()) {
            System.out.println("resolved features empty");
//            String exceptionMessage = (String) map.get("action.error.message");
//            if (exceptionMessage == null) {
//                debug("resolvedFeatures was empty but the install kernel did not issue any messages");
//                info("The features are already installed, so no action is needed.");
//                return;
//            } else if (exceptionMessage.contains("CWWKF1250I")) {
//                info(exceptionMessage);
//                info("The features are already installed, so no action is needed.");
//                return;
//            } else {
//                throw new PluginExecutionException(exceptionMessage);
        }


		HashMap<String, List<String>> resolvedFeaturesMap = parseResolvedFeatures(resolvedFeatures);
		Collection<File> artifacts = new ArrayList<File>();
		artifacts.addAll(retrieveEsas(resolvedFeaturesMap.get("openLiberty")));
		artifacts.addAll(retrieveEsas(resolvedFeaturesMap.get("closedLiberty")));
		
		
		Collection<String> actionReturnResult = new ArrayList<String>();
        for (File esaFile : artifacts) {
			System.out.println("ESA file: " + esaFile.getName());
			map.put("license.accept", true);
            map.put("action.install", esaFile);
            // TODO: implement to
			Integer ac = (Integer) map.get("action.result");
			if (map.get("action.error.message") != null) {
				System.out.println("exeption found, : " + map.get("action.error.message"));
			} else if (map.get("action.install.result") != null) {
				actionReturnResult.addAll((Collection<String>) map.get("action.install.result"));
			}
        }




		System.out.println("The following features have been installed");
        for (String result : actionReturnResult) {
            System.out.println(result);
        }
    }

	/**
	 * Return a hashmap containing the individual filenames for open and closed
	 * liberty features
	 * 
	 * @param resolvedFeatures
	 * @return
	 */
	private HashMap<String, List<String>> parseResolvedFeatures(Collection<String> resolvedFeatures) {
		List<String> openLibertyFeatures = new ArrayList<String>();
		List<String> closedLibertyFeatures = new ArrayList<String>();

		HashMap<String, List<String>> featureMap = new HashMap<>();

		for (String feature : resolvedFeatures) {
			String mavenCoordinate = feature.split(":")[0];
			String featureEsa = feature.split(":")[1];

			if (mavenCoordinate.equals(OPEN_LIBERTY_MAVEN_COORDINATES)) {
				openLibertyFeatures.add(featureEsa);
			} else if (mavenCoordinate.equals(CLOSED_LIBERTY_MAVEN_COORDINATES)) {
				closedLibertyFeatures.add(featureEsa);
			}
		}

		featureMap.put("openLiberty", openLibertyFeatures);
		featureMap.put("closedLiberty", closedLibertyFeatures);
		return featureMap;
	}

    /**
     * Retrieves the esa files from a local maven repo given the list of features of to resolve
     *
     * @param resolvedFeatures
     * @return
     * @throws IOException
     */
    private Collection<File> retrieveEsas(Collection<String> resolvedFeatures) throws IOException {
        Collection<File> foundEsas = new HashSet<>();
		System.out.println("Given ");
		System.out.println(resolvedFeatures);
        try (Stream<Path> files = Files.walk(Paths.get(fromDir.toURI()))) {
            //   foundEsas.addAll(files.filter(f -> f.getFileName().toString().endsWith("esa")).filter(f -> resolvedFeatures.contains(extractFeatureName(f.getFileName().toString()))).map(f -> f.toFile()).collect(Collectors.toList()));
            foundEsas.addAll(files.filter(f -> f.getFileName().toString().endsWith("esa")).filter(f -> resolvedFeatures.removeIf(featureName -> featureName.equals(extractFeatureName(f.getFileName().toString())))).map(f -> f.toFile()).collect(Collectors.toList()));

        }
        System.out.println("ESAs not found:");
        System.out.println(resolvedFeatures);
        return foundEsas;
    }

    /**
	 * Extracts the feature name and version from an ESA filepath, such as
	 * appSecurity-3.0-19.0.0.8.esa
	 * 
	 * TODO: extract runtime version
	 * 
	 * @param filename
	 * @return
	 */
    private String extractFeatureName(String filename) {
		String[] split = filename.split("-");

        return split[0] + "-" + split[1];

    }

	private ArrayList<File> getSingleJsonPaths(File dir) throws IOException {
		ArrayList<File> jsonFilepaths = new ArrayList<>();
        File openLibertyfeatureDir = new File(dir, "io/openliberty/features/features/");
        File closedLibertyfeatureDir = new File(dir, "com/ibm/websphere/appserver/features/features");
        try (Stream<Path> files = Files.walk(Paths.get(openLibertyfeatureDir.toURI()))) {
            jsonFilepaths.addAll(files.filter(f -> f.getFileName().toString().endsWith(".json")).map(f -> f.toFile()).collect(Collectors.toList()));
        }
        try (Stream<Path> files = Files.walk(Paths.get(closedLibertyfeatureDir.toURI()))) {
            jsonFilepaths.addAll(files.filter(f -> f.getFileName().toString().endsWith(".json")).map(f -> f.toFile()).collect(Collectors.toList()));
        }
        return jsonFilepaths;
    }

}
