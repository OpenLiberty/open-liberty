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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

@ApplicationPath("/app")
@Path("/multi") 
public class MultipartResource extends Application {

    @GET
    @Produces("multipart/form-data")
    public Response getListOfEntityParts() {
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

        
        
        System.out.println("getListOfAttachments - returning  " + parts);

        return Response.ok(new GenericEntity<List<EntityPart>>(parts) {
        }, MediaType.MULTIPART_FORM_DATA).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.MULTIPART_FORM_DATA)
    @Path("/test/form")
    public Response form(final List<EntityPart> parts) throws IOException {
        final List<EntityPart> multipart = List.of(
                EntityPart.withName("received-content")
                        .content(parts.get(0).getContent(byte[].class))
                        .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                        .build(),
                EntityPart.withName("added-content")
                        .content("test added content".getBytes(StandardCharsets.UTF_8))
                        .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                        .build()
        );
        return Response.ok(new GenericEntity<>(multipart) {
        }, MediaType.MULTIPART_FORM_DATA).build();
    }

    
    @POST
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public String postListOfAttachments(List<EntityPart> parts) {
        try {
            System.out.println("postListOfAttachments - got list of parts: " + parts);
            assertEquals(6, parts.size());

            EntityPart part = parts.get(0);
            assertEquals("file1", part.getName());
            assertEquals("some.xml", part.getFileName().get());
            assertEquals(MediaType.APPLICATION_XML_TYPE, part.getMediaType());
            assertEquals(Util.toString(Util.xmlFile()), Util.toString(part.getContent()));

            part = parts.get(1);
            assertEquals("file2", part.getName());
            assertEquals("mpRestClient2.0.asciidoc", part.getFileName().get());
            assertEquals("text", part.getMediaType().getType());
            assertEquals("asciidoc", part.getMediaType().getSubtype());
            assertEquals("[myContentId]", part.getHeaders().get("Content-ID").toString());
            assertEquals(Util.toString(Util.asciidocFile()), Util.toString(part.getContent()));
            assertEquals("[SomeValue]", part.getHeaders().get("MyCoolHeader").toString());

            part = parts.get(2);
            assertEquals("notAFile", part.getName());
             assertEquals("text", part.getMediaType().getType());
            assertEquals("asciidoc", part.getMediaType().getSubtype());
            String contentDisposition = part.getHeaders().get("Content-Disposition").get(0);
            assertFalse("Content-Disposition header contains filename attr, but should not: " + contentDisposition,
                        contentDisposition.contains("filename="));
            assertEquals("[Value1]", part.getHeaders().get("Header1").toString());
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

            assertEquals("noSpecifiedContentType", part.getName());
            contentDisposition = part.getHeaders().get("Content-Disposition").get(0);
             assertFalse("Content-Disposition header contains filename attr, but should not: " + contentDisposition,
                        contentDisposition.contains("filename="));
            // MediaType not specified ; should default to text/plain
            assertEquals("text", part.getMediaType().getType());
            assertEquals("plain", part.getMediaType().getSubtype());
            part = parts.get(4);
            assertEquals("empty-entity-part", Util.getPartName(part));
            assertEquals("empty-entity-part", part.getName());
            assertEquals("", part.getContent(String.class)); 

            part = parts.get(5);
            assertEquals("empty-input-stream-part", Util.getPartName(part));
            assertEquals("empty-input-stream-part", part.getName());
            assertEquals("", part.getContent(String.class));

        } catch (Throwable t) {
            fail("Test failed with exception:  " + t);
            t.printStackTrace();
            return t.toString();
        }
        return "SUCCESS";
    }

    // This tests a mixture of @FormParam (the specification supports EntityPart, String, and InputStream.
    // It also tests that parameters that are expected by the resource method, but not sent, will not cause a problem and will be null.
    @POST
    @Path("/asMixOfFormParams")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public String postMixedFormParams(@FormParam("file1") EntityPart part1,
                                      @FormParam("file2") String part2,
                                      @FormParam("notAFile") InputStream part3,
                                      @FormParam("missingEntityPart") EntityPart part4,
                                      @FormParam("missingString") String part5,
                                      @FormParam("missingStream") InputStream part6,
                                      @FormParam("noSpecifiedContentType") EntityPart part7) throws IOException {
         assertEquals("file1", part1.getName());
         assertEquals(Util.removeLineFeeds(Util.toString(Util.asciidocFile()).trim()), Util.removeLineFeeds(part2.trim()));
         assertEquals("This is not a file...", Util.toString(part3));
         assertNull(part4);  // Expected null value
         assertNull(part5);  // Expected null value
         assertNull(part6);  // Expected null value
         assertEquals("noSpecifiedContentType", part7.getName());
         return "SUCCESS";
    }

    @POST
    @Path("/asFormParamStrings")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public String postFormParamOfStrings(@FormParam("file1") String part1,
                                         @FormParam("file2") String part2,
                                         @FormParam("notAFile") String part3,
                                         @FormParam("noSpecifiedContentType") String part4) throws IOException {
        assertEquals(Util.removeLineFeeds(Util.toString(Util.xmlFile()).trim()), Util.removeLineFeeds(part1.trim()));
        assertEquals(Util.removeLineFeeds(Util.toString(Util.asciidocFile()).trim()), Util.removeLineFeeds(part2.trim()));
        assertEquals("This is not a file...", part3.trim());
        assertEquals("No content type specified", part4.trim());
        return "SUCCESS";
    }

    @PUT
    public String inputStreamClosed(List<EntityPart> parts) throws IOException {
        EntityPart part = parts.get(0);
        assertEquals("file1", Util.getPartName(part));
        assertEquals("some.xml", part.getFileName().get());
        assertEquals(MediaType.APPLICATION_XML_TYPE, part.getMediaType());
        assertEquals(Util.toString(Util.xmlFile()), Util.toString(part.getContent()));
        return "SUCCESS";
    }
    
    @POST
    @Path("/entityPartsAsFormParams")
    @Consumes("multipart/form-data")
    public Response postMultipartEntityPartsUsingFormParam(@FormParam("fileid") EntityPart part1,
                                                           @FormParam("description") EntityPart part2,
                                                           @FormParam("thefile") EntityPart part3) throws IOException {
        List<EntityPart> parts = Arrays.asList(part1, part2, part3);
        String[] contentTypeArray = {"text/plain","*/*","application/xml"};
        String[] nameArray =  {"fileid","description","thefile"};
        String[] fileArray =  {null,null,"person.xml"};
        String[] contentArray = {"person1234","XML file about person1234",
                                 "<person>    <name>Chris</name>    <position>laying</position>    <temperature>99.9</temperature></person>"};
        InputStream stream = null;

        for (EntityPart part : parts) {
            if (part == null) {
                fail("No EntityPart objects sent to POST method.");
            }
            String name = part.getName();
            int i = getIndex(name, nameArray);
            
            assertEquals(nameArray[i], part.getName());
            assertEquals(contentTypeArray[i], part.getMediaType().toString());
  
            Optional<String> fileName = part.getFileName();
            
            if (fileName.isPresent()) {
                File tempFile = new File(fileName.get());
                assertEquals(fileArray[i],tempFile.getPath());
            } else {
                assertEquals(fileArray[i],null);
            }
            
            stream = part.getContent();
            if (stream != null) {
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                String line = null;
                try {
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                } catch (IOException e) {
                    fail("Unexpected exception: " + e);
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            fail("Unexpected exception: " + e);
                        }
                    }
                }
                assertEquals(contentArray[i],sb.toString());

                stream.close();
            } else {
               fail("Expected content, " + contentArray[i] + ", not present."); 
            }
            i++;
        }
        return Response.ok("SUCCESS").build();
    }
    
    @POST
    @Path("/entityParts")
    @Consumes("multipart/form-data")
    public Response postMultipartEntityParts(List <EntityPart> parts) throws IOException{
        String[] contentTypeArray = {"text/plain;charset=us-ascii","*/*","application/xml"};
        String[] nameArray =  {"fileid","description","thefile"};
        String[] fileArray =  {null,null,"person.xml"};
        String[] contentArray = {"person1234","XML file about person1234",
                                 "<person>    <name>Chris</name>    <position>laying</position>    <temperature>99.9</temperature></person>"};
        InputStream stream = null;

        for (EntityPart part : parts) {
            if (part == null) {
                fail("No EntityPart objects sent to POST method.");
            }
            String name = part.getName();
            int i = getIndex(name, nameArray);
            
            assertEquals(nameArray[i], part.getName());
            assertEquals(contentTypeArray[i], part.getMediaType().toString());
  
            Optional<String> fileName = part.getFileName();
            
            if (fileName.isPresent()) {
                File tempFile = new File(fileName.get());
                assertEquals(fileArray[i],tempFile.getPath());
            } else {
                assertEquals(fileArray[i],null);
            }
            
            stream = part.getContent();
            if (stream != null) {
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                String line = null;
                try {
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                } catch (IOException e) {
                    fail("Unexpected exception: " + e);
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            fail("Unexpected exception: " + e);
                        }
                    }
                }
                assertEquals(contentArray[i],sb.toString());

                stream.close();
            } else {
               fail("Expected content, " + contentArray[i] + ", not present."); 
            }
            i++;
        }
        return Response.ok("SUCCESS").build();
    }
    
    @POST
    @Path("/entityPartsWithDefaults")
    @Consumes("multipart/form-data")
    public Response postMultipartEntityPartsWithDefaultMediaType(List <EntityPart> parts) throws IOException{
        String[] contentTypeArray = {"text/plain;charset=us-ascii","text/plain;charset=us-ascii","application/octet-stream"};
        String[] nameArray =  {"fileid","description","thefile"};
        String[] fileArray =  {null,null,"person.xml"};
        String[] contentArray = {"person1234","XML file about person1234",
                                 "<person>    <name>Chris</name>    <position>laying</position>    <temperature>99.9</temperature></person>"};
        InputStream stream = null;

        for (EntityPart part : parts) {
            if (part == null) {
                fail("No EntityPart objects sent to POST method.");
            }
            String name = part.getName();
            int i = getIndex(name, nameArray);
            
            assertEquals(nameArray[i], part.getName());
            assertEquals(contentTypeArray[i], part.getMediaType().toString());
  
            Optional<String> fileName = part.getFileName();
            
            if (fileName.isPresent()) {
                File tempFile = new File(fileName.get());
                assertEquals(fileArray[i],tempFile.getPath());
            } else {
                assertEquals(fileArray[i],null);
            }
            
            stream = part.getContent();
            if (stream != null) {
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                String line = null;
                try {
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                } catch (IOException e) {
                    fail("Unexpected exception: " + e);
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            fail("Unexpected exception: " + e);
                        }
                    }
                }
                assertEquals(contentArray[i],sb.toString());

                stream.close();
            } else {
               fail("Expected content, " + contentArray[i] + ", not present."); 
            }
            i++;
        }
        return Response.ok("SUCCESS").build();
    }
    
    private int getIndex(String name, String[] nameArray) {
        int returnVal = -1;
        for (int i = 0; i < nameArray.length; i++) {
            if (name.equals(nameArray[i])) {
                returnVal = i;
                break;
            } 
        }
        if (returnVal == -1) {
            fail("Part/Attachment not found: " + name);
        }
        return returnVal;
    }  
}


