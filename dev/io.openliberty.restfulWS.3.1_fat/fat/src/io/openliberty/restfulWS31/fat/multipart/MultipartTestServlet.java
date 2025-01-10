/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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
package io.openliberty.restfulWS31.fat.multipart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.After;
import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;
import io.openliberty.restfulWS31.fat.multipart.Util.CheckableInputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

@WebServlet(urlPatterns = "/MultipartTestServlet")
public class MultipartTestServlet extends FATServlet {
    private static final long serialVersionUID = 4563445834756294836L;

    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/multipart/";

    private Client client;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @After
    private void teardown() {
        client.close();
    }
    

    @Test 
    public void testMultipartResponse() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                                  .path("/app/multi")
                                  .request(MediaType.MULTIPART_FORM_DATA)
                                  .get();

        assertEquals(200, response.getStatus());

        List<EntityPart> parts = response.readEntity(new GenericType<List<EntityPart>>() {});
        System.out.println("testMultipartResponse - got list of parts: " + parts);
        assertEquals(6, parts.size());

        EntityPart part = parts.get(0);
        assertEquals("file1", Util.getPartName(part));
        assertEquals("some.xml", Util.getFileName(part));
        assertEquals(MediaType.APPLICATION_XML_TYPE, part.getMediaType());

        part = parts.get(1);
        assertEquals("file2", Util.getPartName(part));
        assertEquals("mpRestClient2.0.asciidoc", part.getFileName().get());
        assertEquals("mpRestClient2.0.asciidoc", Util.getFileName(part));
        assertEquals("text/asciidoc", part.getMediaType().getType() + "/" + part.getMediaType().getSubtype());
        assertEquals("myContentId", part.getHeaders().get("Content-ID").get(0));
        assertEquals("SomeValue", part.getHeaders().get("MyCoolHeader").get(0));

        part = parts.get(2);
        assertEquals("notAFile", Util.getPartName(part));

        assertEquals("notAFile", part.getName());
        // no fileName specified, so filename attr should not exist on header
        try {
            String fileName = part.getFileName().get(); 
            fail("There should not be a filename but was " + fileName);
        } catch (NoSuchElementException e ){
            // Caught expected exception
        }
        String contentDisposition = part.getHeaders().get("Content-Disposition").get(0);
        assertFalse("Content-Disposition header contains filename attr, but should not: " + contentDisposition,
                    contentDisposition.contains("filename="));
        assertEquals("text/asciidoc", part.getMediaType().getType() + "/" + part.getMediaType().getSubtype());
        MultivaluedMap<String, String> headers = part.getHeaders();
        assertEquals("Value1", headers.getFirst("Header1").toString());
        List<String> header2ValuesList = headers.get("Header2");
        assertNotNull(header2ValuesList);
        
        if (header2ValuesList.size() == 3) {
            assertEquals("[Value2, Value3, Value4]", header2ValuesList.toString());
        } else {
            fail("Unexpected number of header values for Header2: " + header2ValuesList.toString());
       }

        part = parts.get(3);
        assertEquals("noSpecifiedContentType", Util.getPartName(part));
        assertEquals("noSpecifiedContentType", part.getName());
        contentDisposition = part.getHeaders().get("Content-Disposition").get(0);
        assertFalse("Content-Disposition header contains filename attr, but should not: " + contentDisposition,
                    contentDisposition.contains("filename="));
        assertEquals("text/plain", part.getMediaType().getType() + "/" + part.getMediaType().getSubtype()); // not specified ; should default to text/plain
        
        part = parts.get(4);
        assertEquals("empty-entity-part", Util.getPartName(part));
        assertEquals("empty-entity-part", part.getName());
        assertEquals("", part.getContent(String.class)); 

        part = parts.get(5);
        assertEquals("empty-input-stream-part", Util.getPartName(part));
        assertEquals("empty-input-stream-part", part.getName());
        assertEquals("", part.getContent(String.class));

    }

    @Test
    public void testMultipartRequestListOfEntityParts() throws Exception {
        testMultipartRequest("/app/multi");
    }

    @Test
    public void testMultipartRequestListOfEntityPartsFormParams() throws Exception {
        testEntityPartMultipartRequest("/app/multi/entityPartsAsFormParams", false);
    }

    @Test
    public void testMultipartRequestListOfEntityPartsWithDefaults() throws Exception {
        testEntityPartMultipartRequest("/app/multi/entityPartsWithDefaults", true);
    }

    @Test
    public void testMultipartRequestMixedFormParams() throws Exception {
        testMultipartRequest("/app/multi/asMixOfFormParams");
    }

    @Test
    public void testMultipartRequestFormParams_Strings() throws Exception {
        testMultipartRequest("/app/multi/asFormParamStrings");
    }

    @Test
    public void testInputStreamIsClosedByJAXRS() throws Exception {
        List<EntityPart> parts = null;
        
        CheckableInputStream inputStream = Util.wrapStream(Util.xmlFile());
        
        try {
            parts = List.of(
                            EntityPart.withName("file1")
                                    .content("some.xml", inputStream)
                                    .mediaType(MediaType.APPLICATION_XML)
                                    .build());
         } catch (IOException e) {
            e.printStackTrace();
            fail("Exception caught: " + e);
        }

        assertFalse("InputStream has been closed before sending multipart request", inputStream.isClosed());

        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/multi")
                        .request(MediaType.TEXT_PLAIN)
                        .put(Entity.entity(new GenericEntity<List<EntityPart>>(parts){}, MediaType.MULTIPART_FORM_DATA));

        assertTrue("InputStream was not closed after sending multipart request", inputStream.isClosed());
        assertEquals(200, response.getStatus());
        assertEquals("SUCCESS", response.readEntity(String.class));
    }

    @Test
    @AllowedFFDC("java.io.IOException")
    public void testInputStreamIsClosedByUserBeforeSending() throws Exception {
        List<EntityPart> parts = null;
        
        CheckableInputStream inputStream = Util.wrapStream(Util.xmlFile());
        
        try {
            parts = List.of(
                            EntityPart.withName("file1")
                                    .content("some.xml", inputStream)
                                    .mediaType(MediaType.APPLICATION_XML)
                                    .build());
         } catch (IOException e) {
            e.printStackTrace();
            fail("Exception caught: " + e);
        }
        inputStream.close();
        assertTrue("InputStream was closed but is reporting as unclosed...", inputStream.isClosed());

        try {
            client.target(URI_CONTEXT_ROOT)
                  .path("/app/multi")
                  .request(MediaType.TEXT_PLAIN)
                  .put(Entity.entity(new GenericEntity<List<EntityPart>>(parts){}, MediaType.MULTIPART_FORM_DATA));
            fail("Did not throw expected ProcessingException when sending a previously-closed input stream");
        } catch (ProcessingException ex) {
            //expected
        } catch (Throwable t) {
            System.out.println("Caught unexcepted exception: " + t);
            t.printStackTrace();
            fail("Caught unexcepted exception: " + t);
        }
    }

    private void testMultipartRequest(String path) {
        List<EntityPart> parts = null;

        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("Header1", "Value1");
        map.put("Header2", Arrays.asList("Value2", "Value3", "Value4"));

        
        try {
            parts = List.of(
                            EntityPart.withName("file1")
                                    .content("some.xml", Util.xmlFile())
                                    .mediaType(MediaType.APPLICATION_XML)
                                    .build(),
                            EntityPart.withName("file2")
                                    .content(Util.asciidocFile())
                                    .fileName("mpRestClient2.0.asciidoc")
                                    .mediaType("text/asciidoc")
                                    .header("Content-ID","myContentId")
                                    .header("MyCoolHeader", "SomeValue")
                                    .build(),
                            EntityPart.withName("notAFile")
                                    .content(new ByteArrayInputStream("This is not a file...".getBytes()))
                                    .mediaType("text/asciidoc")
                                    .headers(map)
                                    .build(),
                            EntityPart.withName("noSpecifiedContentType")
                                    .content(new ByteArrayInputStream("No content type specified".getBytes()))
                                    .build(),
                            EntityPart.withName("empty-entity-part")
                                    .content("empty.txt", new ByteArrayInputStream(new byte[0]))
                                    .mediaType(MediaType.TEXT_PLAIN_TYPE)
                                    .build(),
                            EntityPart.withName("empty-input-stream-part")
                                    .content("test.csv", new ByteArrayInputStream(new byte[0]))
                                    .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                                    .build()
                        );
         } catch (IOException e) {
            e.printStackTrace();
            fail("Exception caught: " + e);
        }
        
        System.out.println("testMultipartRequest - sending  " + parts);
        Response response = client.target(URI_CONTEXT_ROOT)
                                  .path(path)
                                  .request(MediaType.TEXT_PLAIN)
                                  .post(Entity.entity(new GenericEntity<List<EntityPart>>(parts){}, MediaType.MULTIPART_FORM_DATA));
        assertEquals(200, response.getStatus());
        assertEquals("SUCCESS", response.readEntity(String.class));
    }    
    
    private void testEntityPartMultipartRequest(String path, boolean useDefault) {

        List<EntityPart> parts = null;
        try {
             File file = new File("./person.xml");
            if (useDefault) {
                parts = List.of(
                        EntityPart.withName("fileid")
                                .content(new ByteArrayInputStream("person1234".getBytes()))
                                .build(),
                        EntityPart.withName("description")
                                .content(new ByteArrayInputStream("XML file about person1234".getBytes()))
                                .build(),
                        EntityPart.withName("thefile")
                                .content("person.xml",new FileInputStream(file))
                                .build()
                 );               
                
            } else {
                parts = List.of(
                        EntityPart.withName("fileid")
                                .content(new ByteArrayInputStream("person1234".getBytes()))
                                .mediaType(MediaType.TEXT_PLAIN)
                                .build(),
                        EntityPart.withName("description")
                                .content(new ByteArrayInputStream("XML file about person1234".getBytes()))
                                .mediaType(MediaType.WILDCARD)
                                .build(),
                        EntityPart.withName("thefile")
                                .content("person.xml",new FileInputStream(file))
                                .mediaType(MediaType.APPLICATION_XML_TYPE)
                                .build()
                 );
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception caught: " + e);
        }
        
        System.out.println("testMultipartRequest - sending  " + parts);
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path(path)
                        .request(MediaType.TEXT_PLAIN)
                        .post(Entity.entity(new GenericEntity<List<EntityPart>>(parts){}, MediaType.MULTIPART_FORM_DATA));
        
        assertEquals(200, response.getStatus());
        assertEquals("SUCCESS", response.readEntity(String.class));
    }
}