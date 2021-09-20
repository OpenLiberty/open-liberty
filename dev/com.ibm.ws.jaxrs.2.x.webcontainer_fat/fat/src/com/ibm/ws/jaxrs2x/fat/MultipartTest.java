/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs2x.fat;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class MultipartTest {

    @Server("com.ibm.ws.jaxrs2x.fat.thirdpartylib")
    public static LibertyServer server;

    private static final String thirdpartylibwar = "thirdpartylib";

    private static String MULTIPART_URI = null;
    private static String MULTIPARTBODY_URI = null;
    private static String MULTIPART_URI2 = null;

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive app = ShrinkHelper.buildDefaultApp(thirdpartylibwar, "com.ibm.ws.jaxrs2x.fat.thirdparty.multipart");
        ShrinkHelper.exportAppToServer(server, app);
        if (JakartaEE9Action.isActive()) {
            Path someArchive = Paths.get("publish/servers/" + server.getServerName() + "/apps/thirdpartylib.war");
            JakartaEE9Action.transformApp(someArchive);
        }
        server.addInstalledAppForValidation(thirdpartylibwar);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        MULTIPART_URI = getBaseTestUri(thirdpartylibwar, "multipart", "resource/uploadFile");
        MULTIPARTBODY_URI = getBaseTestUri(thirdpartylibwar, "multipart", "resource2/multipartbody");
        MULTIPART_URI2 = getBaseTestUri(thirdpartylibwar, "multipart", "resource/uploadFile2");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    private static int getPort() {
        return server.getHttpDefaultPort();
    }

    /**
     * Utility for constructing the resource URL needed for
     * test client to call service.
     *
     * @param contextRoot Tests assume this is the WAR file name
     * @param urlPattern  Specified in web.xml's url-pattern
     * @param path        Value of resource's @Path annotation
     * @return
     */
    public static String getBaseTestUri(String contextRoot, String urlPattern, String resourcePath) {

        // If any of the parameters are null, return empty; usage error
        if (contextRoot == null || urlPattern == null || resourcePath == null) {
            System.out.println("getBaseTestUri(contextRoot, urlPattern, resourcePath) returning empty string");
            return "";
        }

        // For tests, service will always be in the same machine
        StringBuffer sb = new StringBuffer("http://localhost:");
        sb.append(getPort());
        sb.append("/");
        sb.append(contextRoot);
        sb.append("/");
        sb.append(urlPattern);
        sb.append("/");
        sb.append(resourcePath);
        return sb.toString();
    }

    @Test
    public void testUploadMultipart() throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        String srcPath = server.getServerRoot() + "/upload.jsp";
        builder.addPart("my_file", new FileBody(new File(srcPath)));
        HttpPost request = new HttpPost(MULTIPART_URI);
        request.setEntity(builder.build());
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testMultipartBody() throws ClientProtocolException, IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        String srcPath = server.getServerRoot() + "/upload.jsp";
        builder.addPart("my_file", new FileBody(new File(srcPath)));
        String expected = "Project summary";
        builder.addPart("comment", new StringBody(expected, ContentType.TEXT_PLAIN));

        HttpPost request = new HttpPost(MULTIPARTBODY_URI);
        request.setEntity(builder.build());
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String returned = EntityUtils.toString(response.getEntity());
        assertEquals(expected, returned);
    }

    @Test
    public void testUploadMultipart2() throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        String srcPath = server.getServerRoot() + "/testInput.csv";
        builder.addPart("my_file", new FileBody(new File(srcPath)));
        HttpPost request = new HttpPost(MULTIPART_URI2);
        request.setEntity(builder.build());
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
    }

}