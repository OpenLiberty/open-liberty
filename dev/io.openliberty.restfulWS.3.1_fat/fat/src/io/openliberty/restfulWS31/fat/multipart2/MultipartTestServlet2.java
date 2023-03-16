/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.restfulWS31.fat.multipart2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import com.ibm.websphere.jaxrs20.multipart.AttachmentBuilder;
import com.ibm.websphere.jaxrs20.multipart.IAttachment;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;
import io.openliberty.restfulWS31.fat.multipart2.Util.CheckableInputStream;
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

@WebServlet(urlPatterns = "/MultipartTestServlet2")
public class MultipartTestServlet2 extends FATServlet {
    private static final long serialVersionUID = 4563445834756294836L;

    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/multipart2/";

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
        assertEquals(4, parts.size());
//        assertEquals(2, parts.size());

        EntityPart part = parts.get(3);
        assertEquals("file1", Util2.getPartName(part));
 //       assertEquals("some.xml", part.getDataHandler().getName());
        assertEquals("some.xml", Util2.getFileName(part));
        assertEquals(MediaType.APPLICATION_XML_TYPE, part.getMediaType());
//        assertEquals(Util.toString(Util.xmlFile()), Util.toString(part.getDataHandler().getInputStream()));

        part = parts.get(2);
        assertEquals("file2", Util2.getPartName(part));
        assertEquals("mpRestClient2.0.asciidoc", part.getFileName().get());
        assertEquals("mpRestClient2.0.asciidoc", Util2.getFileName(part));
        assertEquals("text/asciidoc", part.getMediaType().getType() + "/" + part.getMediaType().getSubtype());
        assertEquals("[myContentId]", part.getHeaders().get("Content-ID").get(0));
//        assertEquals("myContentId", part.getHeader("Content-ID"));
//        assertEquals(Util.toString(Util.asciidocFile()), Util.toString(part.getDataHandler().getInputStream()));
        assertEquals("[SomeValue]", part.getHeaders().get("MyCoolHeader").get(0));

        part = parts.get(1);
        assertEquals("notAFile", Util2.getPartName(part));

        assertEquals("notAFile", part.getName()); 
        //assertNull(Util.getFileName(part)); // no fileName specified, so filename attr should not exist on header
//        String contentDisposition = part.getHeaders().get("Content-Disposition"));
//        assertFalse("Content-Disposition header contains filename attr, but should not: " + contentDisposition,
//                    contentDisposition.contains("filename="));
//        assertEquals("text/asciidoc", part.getContentType().toString());
 //       assertEquals("This is not a file...",
//                     Util.toString(part.getDataHandler().getInputStream()));
//        assertEquals("Value1", part.getHeader("Header1"));
        MultivaluedMap<String, String> headers = part.getHeaders();
        assertEquals("[Value1]", headers.getFirst("Header1"));
        // there is a behavior difference between CXF and RESTEasy
        List<String> header2ValuesList = headers.get("Header2");
        assertNotNull(header2ValuesList);
        if (header2ValuesList.size() == 1) {
            // CXF returns a single-entry list of one comma-separated string
            String header2Values = headers.getFirst("Header2");
            assertNotNull(header2Values);
            header2Values = header2Values.substring(1, header2Values.length()-1);
            String[] header2ValuesArr = header2Values.split(",");
            assertEquals(3, header2ValuesArr.length);
            System.out.println("header2ValuesArr[0] = " + header2ValuesArr[0]);
            System.out.println("header2ValuesArr[1] = " + header2ValuesArr[1]);
            System.out.println("header2ValuesArr[2] = " + header2ValuesArr[2]);
            assertEquals("Value2", header2ValuesArr[0].strip());
            assertEquals("Value3", header2ValuesArr[1].strip());
            assertEquals("Value4", header2ValuesArr[2].strip());
        } else if (header2ValuesList.size() == 3) {
            // RESTEasy returns a list of 3 strings
            assertEquals("[Value2]", header2ValuesList.get(0));
            assertEquals("[Value3]", header2ValuesList.get(1));
            assertEquals("[Value4]", header2ValuesList.get(2));
        } else {
            fail("unexpected number of header values for Header2");
        }
        
        part = parts.get(0);
        assertEquals("noSpecifiedContentType", Util2.getPartName(part));
        assertEquals("noSpecifiedContentType", part.getName());
        String contentDisposition = part.getHeaders().get("Content-Disposition").get(0);
        assertFalse("Content-Disposition header contains filename attr, but should not: " + contentDisposition,
                    contentDisposition.contains("filename="));
        assertEquals("text/plain", part.getMediaType().getType() + "/" + part.getMediaType().getSubtype()); // not specified ; should default to text/plain
    }

    @Test
    public void testMultipartRequestListOfEntityParts() throws Exception {
        testMultipartRequest2("/app/multi");
    }

    @Test
    public void testMultipartRequestListOfEntityPartsFormParams() throws Exception {
        testEntityPartMultipartRequest("/app/multi/entityPartsAsFormParams", false);
    }

//    @Test skip for now
    public void testMultipartRequestFormParams() throws Exception {
        testMultipartRequest2("/app/multi/asFormParams");
    }

    @Test
    public void testMultipartRequestFormParams_Strings() throws Exception {
        testMultipartRequest2("/app/multi/asFormParamStrings");
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

    private void testMultipartRequest2(String path) {
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

        
    private void testMultipartRequest(String path) {

        
        List<IAttachment> attachments = new ArrayList<>();

        attachments.add(AttachmentBuilder.newBuilder("file1")
                                         .inputStream("some.xml", Util.xmlFile())
                                         .contentType(MediaType.APPLICATION_XML)
                                         .build());

        attachments.add(AttachmentBuilder.newBuilder("file2")
                                         .inputStream(Util.asciidocFile())
                                         .fileName("mpRestClient2.0.asciidoc")
                                         .contentType("text/asciidoc")
                                         .contentId("myContentId")
                                         .header("MyCoolHeader", "SomeValue")
                                         .build());

        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle("Header1", "Value1");
        map.put("Header2", Arrays.asList("Value2", "Value3", "Value4"));
        attachments.add(AttachmentBuilder.newBuilder("notAFile")
                                         .inputStream(new ByteArrayInputStream("This is not a file...".getBytes()))
                                         .contentType("text/asciidoc")
                                         .headers(map)
                                         .build());

        attachments.add(AttachmentBuilder.newBuilder("noSpecifiedContentType")
                                         .inputStream(new ByteArrayInputStream("No content type specified".getBytes()))
                                         .build());

        System.out.println("testMultipartRequest - sending  " + attachments);
        Response response = client.target(URI_CONTEXT_ROOT)
                                  .path(path)
                                  .request(MediaType.TEXT_PLAIN)
                                  .post(Entity.entity(attachments, MediaType.MULTIPART_FORM_DATA));
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