/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mpRestClient.fat.multipartClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MultipartClientTestServlet")
public class MultipartClientTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(MultipartClientTestServlet.class.getName());

    private RestClientBuilder builder;

    @Override
    public void init() throws ServletException {
        String baseUrlStr = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_secondary") + "/multipart";
        LOG.info("baseUrl = " + baseUrlStr);
        URL baseUrl;
        try {
            baseUrl = new URL(baseUrlStr);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
        builder = RestClientBuilder.newBuilder()
                        .baseUrl(baseUrl);
    }

    @Test
    public void testUploadSingleFile(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try (FileManagerClient client = builder.build(FileManagerClient.class)) {
            final byte[] content;
            try (InputStream in = MultipartClientTestServlet.class.getResourceAsStream("/multipart/test-file1.txt")) {
                assertNotNull("Could not find /multipart/test-file1.txt", in);
                content = in.readAllBytes();
            }
            
            // Send in an InputStream to ensure it works with an InputStream
            final List<EntityPart> files = List.of(EntityPart.withFileName("test-file1.txt")
                    .content(new ByteArrayInputStream(content))
                    .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .build());
                    
            try (Response response = client.uploadFile(files)) {
                assertEquals(201, response.getStatus());
                final JsonArray jsonArray = response.readEntity(JsonArray.class);
                assertNotNull(jsonArray);
                assertEquals(1, jsonArray.size());
                final JsonObject json = jsonArray.getJsonObject(0);
                assertEquals("test-file1.txt", json.getString("name"));
                assertEquals("test-file1.txt", json.getString("fileName"));
                assertEquals("This is a test file for file 1.", json.getString("content"));
            }
        }
    }

    @Test
    public void testUploadMultipleFiles(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try (FileManagerClient client = builder.build(FileManagerClient.class)) {
            final Map<String, byte[]> entityPartContent = new LinkedHashMap<>(2);
            try (InputStream in = MultipartClientTestServlet.class.getResourceAsStream("/multipart/test-file1.txt")) {
                assertNotNull("Could not find /multipart/test-file1.txt", in);
                entityPartContent.put("test-file1.txt", in.readAllBytes());
            }
            try (InputStream in = MultipartClientTestServlet.class.getResourceAsStream("/multipart/test-file2.txt")) {
                assertNotNull("Could not find /multipart/test-file2.txt", in);
                entityPartContent.put("test-file2.txt", in.readAllBytes());
            }
            
            final List<EntityPart> files = entityPartContent.entrySet()
                    .stream()
                    .map((entry) -> {
                        try {
                            return EntityPart.withName(entry.getKey())
                                    .fileName(entry.getKey())
                                    .content(entry.getValue())
                                    .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                                    .build();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .collect(Collectors.toList());

            try (Response response = client.uploadFile(files)) {
                assertEquals(201, response.getStatus());
                final JsonArray jsonArray = response.readEntity(JsonArray.class);
                assertNotNull(jsonArray);
                assertEquals(2, jsonArray.size());
                
                // Don't assume the results are in a specific order
                for (JsonValue value : jsonArray) {
                    final JsonObject json = value.asJsonObject();
                    if (json.getString("name").equals("test-file1.txt")) {
                        assertEquals("test-file1.txt", json.getString("fileName"));
                        assertEquals("This is a test file for file 1.", json.getString("content"));
                    } else if (json.getString("name").equals("test-file2.txt")) {
                        assertEquals("test-file2.txt", json.getString("fileName"));
                        assertEquals("This is a test file for file 2.", json.getString("content"));
                    } else {
                        fail(String.format("Unexpected entry %s in JSON response: %n%s", json, jsonArray));
                    }
                }
            }
        }
    }
}