/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.fat.versionwarning;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static java.util.function.Predicate.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;
import org.hamcrest.Matcher;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.openapi40.fat.versionwarning.app.VersionWarningApplication;
import io.openliberty.microprofile.openapi40.fat.versionwarning.app.VersionWarningDataObject;

/**
 * Test that we warn if the user uses OpenAPI 3.1 features when they have OpenAPI 3.0 configured
 */
@RunWith(FATRunner.class)
public class VersionWarningTest extends FATServletClient {

    @Server("OpenAPIVersionWarningServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWWKO1687W", // Warning for ignored parameter on OpenAPI 3.0
                          "CWWKO1650E"); // Error for invalid PathItem reference on OpenAPI 3.0
    }

    @After
    public void cleanupApps() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testVersionWarning() throws Exception {
        setOpenAPIVersion("3.0");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "testWarning.war")
                                   .addPackage(VersionWarningApplication.class.getPackage());

        server.setMarkToEndOfLog();
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        // Assert that the correct warnings are produced
        assertThat(server.findStringsInLogsUsingMark("CWWKO1687W", server.getDefaultLogFile()),
                   containsInAnyOrder(warningFor(Info.class, "summary", VersionWarningApplication.class),
                                      warningFor(License.class, "identifier", VersionWarningApplication.class),
                                      warningFor(Components.class, "pathItems", VersionWarningApplication.class),
                                      warningFor(OpenAPIDefinition.class, "webhooks", VersionWarningApplication.class),
                                      warningFor(Schema.class, "comment", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "ifSchema", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "thenSchema", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "elseSchema", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "dependentSchemas", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "patternProperties", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "dependentRequired", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "propertyNames", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "contains", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "minContains", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "maxContains", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "prefixItems", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "contentEncoding", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "contentMediaType", VersionWarningDataObject.class),
                                      warningFor(Schema.class, "contentSchema", VersionWarningDataObject.class),
                                      warningFor(SchemaProperty.class, "comment", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "ifSchema", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "thenSchema", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "elseSchema", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "dependentSchemas", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "patternProperties", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "dependentRequired", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "propertyNames", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "contains", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "minContains", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "maxContains", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "prefixItems", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "contentEncoding", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "contentMediaType", VersionWarningApplication.class),
                                      warningFor(SchemaProperty.class, "contentSchema", VersionWarningApplication.class)));

        // Assert that the fields are not included in the output OpenAPI document
        JsonObject openapiDoc = new HttpRequest(server, "/openapi").requestProp("Accept", "application/json")
                                                                   .run(JsonObject.class);
        assertJsonPathMissing(openapiDoc, "info/summary");
        assertJsonPathMissing(openapiDoc, "info/license/identifier");
        assertJsonPathMissing(openapiDoc, "components/pathItems");
        assertJsonPathMissing(openapiDoc, "webhooks");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/$comment");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/const");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/enum"); // constValue should have been converted to enum, so it should be present
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/if");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/then");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/else");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/dependentSchemas");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/patternProperties");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/dependentRequired");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/propertyNames");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/properties/testList/contains");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/properties/testList/minContains");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/properties/testList/maxContains");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/properties/testList/prefixItems");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/properties/encodedTest/contentEncoding");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/properties/encodedTest/contentMediaType");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/properties/encodedTest/contentSchema");

    }

    @Test
    public void testNoVersionWarning() throws Exception {
        setOpenAPIVersion("3.1");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "testWarning.war")
                                   .addPackage(VersionWarningApplication.class.getPackage());

        server.setMarkToEndOfLog();
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        // Assert that we don't get any warnings about ignored properties
        assertThat(server.findStringsInLogsUsingMark("CWWKO1687W", server.getDefaultLogFile()), empty());

        // Assert present fields
        JsonObject openapiDoc = new HttpRequest(server, "/openapi").requestProp("Accept", "application/json")
                                                                   .run(JsonObject.class);
        assertJsonPathPresent(openapiDoc, "info/summary");
        assertJsonPathPresent(openapiDoc, "info/license/identifier");
        assertJsonPathPresent(openapiDoc, "components/pathItems");
        assertJsonPathPresent(openapiDoc, "webhooks");
        assertJsonPathPresent(openapiDoc, "paths/~1/get/callbacks/test/~1callback~1test/$ref");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/$comment");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/const");
        assertJsonPathMissing(openapiDoc, "components/schemas/VersionWarningDataObject/enum"); // constValue should not be converted to enum, so it should be missing
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/if");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/then");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/else");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/dependentSchemas");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/patternProperties");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/dependentRequired");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/propertyNames");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/properties/testList/contains");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/properties/testList/minContains");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/properties/testList/maxContains");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/properties/testList/prefixItems");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/properties/encodedTest/contentEncoding");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/properties/encodedTest/contentMediaType");
        assertJsonPathPresent(openapiDoc, "components/schemas/VersionWarningDataObject/properties/encodedTest/contentSchema");
    }

    private Matcher<String> warningFor(Class<?> annotationClass, String parameter, Class<?> containingClass) {
        return containsString("CWWKO1687W: The " + annotationClass.getSimpleName() + "." + parameter
                              + " annotation element in the "
                              + containingClass.getName()
                              + " class is ignored because it requires OpenAPI version 3.1 but the current OpenAPI version is 3.0.");
    }

    /**
     * Set the OpenAPI version in server.xml and wait for the server to process any update
     *
     * @param openAPIVersion the new OpenAPI version
     */
    private void setOpenAPIVersion(String openAPIVersion) throws Exception {
        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setOpenApiVersion(openAPIVersion);
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    private void assertJsonPathPresent(JsonObject root, String path) {
        StringWriter output = new StringWriter();
        try (JsonWriter writer = Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true)).createWriter(output)) {
            writer.write(root);
        }
        assertThat("Path not present: " + path + "\nfull doc: " + output.toString() + "\n", findJson(root, path), hasProperty("present", is(true)));
    }

    private void assertJsonPathMissing(JsonObject root, String path) {
        StringWriter output = new StringWriter();
        try (JsonWriter writer = Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true)).createWriter(output)) {
            writer.write(root);
        }
        assertThat("Path not missing: " + path + "\nfull doc: " + output.toString() + "\n", findJson(root, path), hasProperty("present", is(false)));
    }

    private Optional<JsonValue> findJson(JsonValue root, String path) {
        List<String> pathParts = Pattern.compile("/")
                                        .splitAsStream(path)
                                        .filter(not(String::isBlank))
                                        .map(s -> s.replace("~1", "/"))
                                        .map(s -> s.replace("~0", "~"))
                                        .collect(Collectors.toUnmodifiableList());
        return findJson(root, pathParts);
    }

    private Optional<JsonValue> findJson(JsonValue root, List<String> pathParts) {
        if (root == null) {
            return Optional.empty();
        }
        if (pathParts.isEmpty()) {
            return Optional.of(root);
        }

        String head = pathParts.get(0);
        List<String> tail = pathParts.subList(1, pathParts.size());
        switch (root.getValueType()) {
            case ARRAY:
                try {
                    return findJson(root.asJsonArray().get(Integer.parseInt(head)), tail);
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    return Optional.empty();
                }
            case OBJECT:
                return findJson(root.asJsonObject().get(head), tail);
            default:
                return Optional.empty();
        }
    }
}
