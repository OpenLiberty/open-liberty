/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.parsers.internal;

import static com.ibm.ws.repository.base.JsonAnyOrderMatcher.matchesJsonInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.ResourceType;

public class CreateJsonRepositoryFilesTest {

    public CreateJsonRepositoryFiles cjrf;
    File baseDir = new File("../com.ibm.ws.repository.parsers");

    /**
     * Test that the JSON created for the same asset dosn't change with other product or parser changes.
     * The metadata file used is explicitly specified. We will only test the specified metadata option
     * for feature, the rest will only test co-located as if this works they should all do so.
     *
     * @throws IOException
     */
    @Test

    public void testJsonWithSpecifiedMetadata() throws IOException {
        String name = "com.ibm.websphere.appserver.anno-1.0.esa";

        File assetFile = new File(baseDir, "resources/esa/esaAssetOnly/" + name);
        File metadataFile = new File(baseDir, "resources/esa/esaMetadataOnly/" + name + ".metadata.zip");
        String assetType = ResourceType.FEATURE.toString();
        String outputLocation = new File(baseDir, "/build/json").getAbsolutePath();
        runAsIfCalledFromXml(assetFile, metadataFile, assetType, outputLocation);

        // compare the output json with the reference json
        File createdFile = new File(baseDir, "/build/json/" + name + ".json");
        File referenceFile = new File(baseDir, "/resources/reference/" + name + ".json");
        assertThat(readJsonObject(createdFile), matchesJsonInAnyOrder(readJsonObject(referenceFile)));

        // tidy up just in case tests next tests fails to produce one and assumes this is its output
        createdFile.delete();
    }

    /**
     * Test that the JSON created for the same asset dosn't change with other product or parser changes.
     * The metadata file used is explicitly specified. We will only test the specified metadata option
     * for feature, the rest will only test co-located as if this works they should all do so.
     *
     * @throws IOException
     */
    @Test
    public void testJspJsonWithSpecifiedMetadata() throws IOException {
        String name = "com.ibm.websphere.appserver.jsp-2.3.esa";

        File assetFile = new File(baseDir, "resources/esa/esaAssetOnly/" + name);
        File metadataFile = new File(baseDir, "resources/esa/esaMetadataOnly/" + name + ".metadata.zip");
        String assetType = ResourceType.FEATURE.toString();
        String outputLocation = new File(baseDir, "/build/json").getAbsolutePath();
        runAsIfCalledFromXml(assetFile, metadataFile, assetType, outputLocation);

        // compare the output json with the reference json
        File createdFile = new File(baseDir, "/build/json/" + name + ".json");
        File referenceFile = new File(baseDir, "/resources/reference/" + name + ".json");
        assertThat(readJsonObject(createdFile), matchesJsonInAnyOrder(readJsonObject(referenceFile)));

        // tidy up just in case tests next tests fails to produce one and assumes this is its output
        createdFile.delete();
    }

    /**
     * Test that the JSON created for the same asset dosn't change with other product or parser changes.
     * The metadata file used is not specified but there is one co-located with the esa.
     *
     * @throws IOException
     */
    @Test

    public void testEsaJson() throws IOException {
        testSomeJson("com.ibm.websphere.appserver.anno-1.0.esa", "esa", ResourceType.FEATURE.toString());
    }

    /**
     * Test creating JSON for a parameterised call
     *
     * @throws IOException
     */
    private void testSomeJson(String name, String srcDir, String assetType) throws IOException {

        File assetFile = new File(baseDir, "/resources/" + srcDir + "/" + name);
        File metadataFile = new File("null"); // <<<<<< co-located metadata file
        String outputLocation = new File(baseDir, "/build/json").getAbsolutePath();
        runAsIfCalledFromXml(assetFile, metadataFile, assetType, outputLocation);

        // compare the output json with the reference json
        File createdFile = new File(baseDir, "/build/json/" + name + ".json");
        File referenceFile = new File(baseDir, "/resources/reference/" + name + ".json");
        assertThat(readJsonObject(createdFile), matchesJsonInAnyOrder(readJsonObject(referenceFile)));

        // tidy up just in case tests next tests fails to produce one and assumes this is its output
        createdFile.delete();
    }

    private void runAsIfCalledFromXml(File assetFile, File metadataFile, String assetType, String outputLocation) {
        cjrf = new CreateJsonRepositoryFiles();
        cjrf.setAssetFile(assetFile);
        cjrf.setMetadataFile(metadataFile);
        cjrf.setAssetType(assetType);
        cjrf.setOutputLocation(outputLocation);

        // check that the output location exists
        File outputDir = new File(outputLocation);
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                fail("unable to create directory " + outputDir);
            }
        }
        cjrf.execute();
    }

    /**
     * Read a JsonObject from a file.
     */
    private JsonObject readJsonObject(File f) throws FileNotFoundException {
        JsonReader reader = Json.createReader(new FileReader(f));
        return reader.readObject();
    }

}
