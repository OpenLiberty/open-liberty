/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.multipart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Test;

import com.ibm.websphere.jaxrs20.multipart.AttachmentBuilder;
import com.ibm.websphere.jaxrs20.multipart.IAttachment;
import com.ibm.ws.jaxrs.fat.multipart.Util.CheckableInputStream;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

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

        List<IAttachment> parts = response.readEntity(new GenericType<List<IAttachment>>() {});
        System.out.println("testMultipartResponse - got list of parts: " + parts);
        assertEquals(4, parts.size());

        IAttachment part = parts.get(0);
        assertEquals("file1", Util.getPartName(part));
        assertEquals("some.xml", part.getDataHandler().getName());
        assertEquals("some.xml", Util.getFileName(part));
        assertEquals(MediaType.APPLICATION_XML_TYPE, part.getContentType());
        assertEquals(Util.toString(Util.xmlFile()), Util.toString(part.getDataHandler().getInputStream()));

        part = parts.get(1);
        assertEquals("file2", Util.getPartName(part));
        assertEquals("mpRestClient2.0.asciidoc", part.getDataHandler().getName());
        assertEquals("mpRestClient2.0.asciidoc", Util.getFileName(part));
        assertEquals("text/asciidoc", part.getContentType().toString());
        assertEquals("myContentId", part.getContentId());
        assertEquals("myContentId", part.getHeader("Content-ID"));
        assertEquals(Util.toString(Util.asciidocFile()), Util.toString(part.getDataHandler().getInputStream()));
        assertEquals("SomeValue", part.getHeader("MyCoolHeader"));

        part = parts.get(2);
        assertEquals("notAFile", Util.getPartName(part));
        assertEquals("notAFile", part.getDataHandler().getName()); // no fileName specified, should default to part name
        //assertNull(Util.getFileName(part)); // no fileName specified, so filename attr should not exist on header
        String contentDisposition = part.getHeader("Content-Disposition");
        assertFalse("Content-Disposition header contains filename attr, but should not: " + contentDisposition,
                    contentDisposition.contains("filename="));
        assertEquals("text/asciidoc", part.getContentType().toString());
        assertEquals("This is not a file...",
                     Util.toString(part.getDataHandler().getInputStream()));
        assertEquals("Value1", part.getHeader("Header1"));
        MultivaluedMap<String, String> headers = part.getHeaders();
        assertEquals("Value1", headers.getFirst("Header1"));
        // there is a behavior difference between CXF and RESTEasy
        List<String> header2ValuesList = headers.get("Header2");
        assertNotNull(header2ValuesList);
        if (header2ValuesList.size() == 1) {
            // CXF returns a single-entry list of one comma-separated string
            String header2Values = headers.getFirst("Header2");
            assertNotNull(header2Values);
            String[] header2ValuesArr = header2Values.split(",");
            assertEquals(3, header2ValuesArr.length);
            assertEquals("Value2", header2ValuesArr[0]);
            assertEquals("Value3", header2ValuesArr[1]);
            assertEquals("Value4", header2ValuesArr[2]);
        } else if (header2ValuesList.size() == 3) {
            // RESTEasy returns a list of 3 strings
            assertEquals("Value2", header2ValuesList.get(0));
            assertEquals("Value3", header2ValuesList.get(1));
            assertEquals("Value4", header2ValuesList.get(2));
        } else {
            fail("unexpected number of header values for Header2");
        }
        part = parts.get(3);
        assertEquals("noSpecifiedContentType", Util.getPartName(part));
        assertEquals("noSpecifiedContentType", part.getDataHandler().getName());
        contentDisposition = part.getHeader("Content-Disposition");
        assertFalse("Content-Disposition header contains filename attr, but should not: " + contentDisposition,
                    contentDisposition.contains("filename="));
        assertEquals("text/plain", part.getContentType().toString()); // not specified ; should default to text/plain
        assertEquals("No content type specified", Util.toString(part.getDataHandler().getInputStream()));
    }

    @Test
    public void testMultipartRequestListOfAttachments() throws Exception {
        testMultipartRequest("/app/multi");
    }

    @Test
    public void testMultipartRequestFormParams() throws Exception {
        testMultipartRequest("/app/multi/asFormParams");
    }

    @Test
    public void testMultipartRequestFormParams_Strings() throws Exception {
        testMultipartRequest("/app/multi/asFormParamStrings");
    }

    @Test
    public void testInputStreamIsClosedByJAXRS() throws Exception {
        List<IAttachment> attachments = new ArrayList<>();

        CheckableInputStream inputStream = Util.wrapStream(Util.xmlFile());
        attachments.add(AttachmentBuilder.newBuilder("file1")
                                         .inputStream("some.xml", inputStream)
                                         .contentType(MediaType.APPLICATION_XML)
                                         .build());
        assertFalse("InputStream has been closed before sending multipart request", inputStream.isClosed());

        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/multi")
                        .request(MediaType.TEXT_PLAIN)
                        .put(Entity.entity(attachments, MediaType.MULTIPART_FORM_DATA));

        assertTrue("InputStream was not closed after sending multipart request", inputStream.isClosed());
        assertEquals(200, response.getStatus());
        assertEquals("SUCCESS", response.readEntity(String.class));
    }

    @Test
    @AllowedFFDC("java.io.IOException")
    public void testInputStreamIsClosedByUserBeforeSending() throws Exception {
        List<IAttachment> attachments = new ArrayList<>();

        CheckableInputStream inputStream = Util.wrapStream(Util.xmlFile());
        attachments.add(AttachmentBuilder.newBuilder("file1")
                                         .inputStream("some.xml", inputStream)
                                         .contentType(MediaType.APPLICATION_XML)
                                         .build());
        inputStream.close();
        assertTrue("InputStream was closed but is reporting as unclosed...", inputStream.isClosed());

        try {
            client.target(URI_CONTEXT_ROOT)
                  .path("/app/multi")
                  .request(MediaType.TEXT_PLAIN)
                  .put(Entity.entity(attachments, MediaType.MULTIPART_FORM_DATA));
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
}