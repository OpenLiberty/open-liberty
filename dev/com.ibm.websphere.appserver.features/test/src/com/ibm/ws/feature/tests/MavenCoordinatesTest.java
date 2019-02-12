/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.feature.tests;

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;

import com.ibm.ws.feature.utils.FeatureInfo;
import com.ibm.ws.feature.utils.FeatureInfo.Edition;
import com.ibm.ws.feature.utils.FeatureInfo.Kind;
import com.ibm.ws.feature.utils.FeatureMapFactory;

public class MavenCoordinatesTest {

    private static final String MAVEN_CENTRAL = "http://central.maven.org/maven2/";

    @Test
    public void validateMavenCoordinates() throws FileNotFoundException {
        // Required to run on older IBM JDK 8 versions
        System.setProperty("com.ibm.jsse2.overrideDefaultTLS", "true");

        HttpClient client = HttpClientBuilder.create().build();

        Set<String> failures = new HashSet<>();
        Set<String> checkedCoords = new HashSet<>();
        Map<String, FeatureInfo> features = FeatureMapFactory.getFeatureMapFromFile("./visibility/");
        for (FeatureInfo feature : features.values()) {
            if (feature.getKind() == Kind.NOSHIP || feature.getEdition() == Edition.FULL)
                continue;
            for (String mavenCoord : feature.getProvidedMavenCoords()) {
                if (checkedCoords.contains(mavenCoord))
                    continue;
                checkedCoords.add(mavenCoord);
                if (!checkCoordinates(client, mavenCoord)) {
                    String msg = "Feature " + feature.getName() + " included a mavenCoordinate that is not publicly available: " + mavenCoord;
                    failures.add(msg);
                    System.out.println(msg);
                }
            }
        }

        if (!failures.isEmpty())
            fail("Found mavenCoordinates in feature files that are not present in cnf/oss_dependencies.maven, " +
                 "be sure that mavenCoordinates refers to maven G:A:V coordinates available publicly in Maven Central.\n" +
                 failures.toString());
    }

    private boolean checkCoordinates(HttpClient client, String coordinates) {
        String[] bits = coordinates.split(":");
        if (bits.length != 3)
            return false;

        String group = bits[0].replaceAll("\\.", "/");
        String artifact = bits[1];
        String version = bits[2];
        String URL = MAVEN_CENTRAL + group + '/' + artifact + '/' + version;
        HttpHead ping = new HttpHead(URL);
        try {
            HttpResponse response = client.execute(ping);
            int rc = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK != rc) {
                System.out.println("Got rc=" + rc + " for URL=" + URL);
                return false;
            }
        } catch (Exception ignore) {
            // Don't fail the test if maven central cannot be reached for some reason
            ignore.printStackTrace();
        }
        return true;
    }

}
