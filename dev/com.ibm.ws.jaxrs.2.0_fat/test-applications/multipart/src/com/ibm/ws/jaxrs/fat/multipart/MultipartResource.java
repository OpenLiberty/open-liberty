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
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.ibm.websphere.jaxrs20.multipart.AttachmentBuilder;
import com.ibm.websphere.jaxrs20.multipart.IAttachment;

@ApplicationPath("/app")
@Path("/multi")
public class MultipartResource extends Application {

    @GET
    @Produces("multipart/form-data")
    public List<IAttachment> getListOfAttachments() {
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
        System.out.println("getListOfAttachments - returning  " + attachments);

        return attachments;
    }

    @POST
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public String postListOfAttachments(List<IAttachment> parts) {
        try {
            System.out.println("postListOfAttachments - got list of parts: " + parts);
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
        } catch (Throwable t) {
            return t.toString();
        }
        return "SUCCESS";
    }

    @POST
    @Path("/asFormParams")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public String postFormParamOfAttachments(@FormParam("file1") IAttachment part1,
                                             @FormParam("file2") IAttachment part2,
                                             @FormParam("notAFile") IAttachment part3,
                                             @FormParam("noSpecifiedContentType") IAttachment part4) {
        return postListOfAttachments(Arrays.asList(part1, part2, part3, part4));
    }

    @POST
    @Path("/asFormParamStrings")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public String postFormParamOfStrings(@FormParam("file1") String part1,
                                         @FormParam("file2") String part2,
                                         @FormParam("notAFile") String part3,
                                         @FormParam("noSpecifiedContentType") String part4) throws IOException {
        assertEquals(Util.toString(Util.xmlFile()).trim(), part1.trim());
        assertEquals(Util.toString(Util.asciidocFile()).trim(), part2.trim());
        assertEquals("This is not a file...", part3.trim());
        assertEquals("No content type specified", part4.trim());
        return "SUCCESS";
    }

    @PUT
    public String inputStreamClosed(List<IAttachment> parts) throws IOException {
        IAttachment part = parts.get(0);
        assertEquals("file1", Util.getPartName(part));
        assertEquals("some.xml", part.getDataHandler().getName());
        assertEquals("some.xml", Util.getFileName(part));
        assertEquals(MediaType.APPLICATION_XML_TYPE, part.getContentType());
        assertEquals(Util.toString(Util.xmlFile()), Util.toString(part.getDataHandler().getInputStream()));
        return "SUCCESS";
    }
}
