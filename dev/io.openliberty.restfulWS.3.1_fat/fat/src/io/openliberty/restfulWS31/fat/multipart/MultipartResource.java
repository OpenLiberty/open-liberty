/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.Response;

@ApplicationPath("/app")
@Path("/multi")
public class MultipartResource extends Application {
   
    @POST
    @Path("/iAttachments")
    @Consumes("multipart/form-data")
    public Response postMultipartIAttachments(List <IAttachment> attachments) throws IOException{
        String[] contentTypeArray = {"text/plain","*/*","application/xml"};
        String[] fileArray =  {"fileid","description","person.xml"};
        String[] contentArray = {"person1234","XML file about person1234",
                                 "<person>    <name>Chris</name>    <position>laying</position>    <temperature>99.9</temperature></person>"};
        InputStream stream = null;
        for (IAttachment attachment : attachments) {
            if (attachment == null) {
                fail("No IAttachments sent to Post method.");
            }
            int i = getIndex(attachment.getDataHandler().getName(), fileArray);
            String contentType = attachment.getDataHandler().getContentType();;
            assertEquals(contentTypeArray[i], contentType);
  
            String name = attachment.getDataHandler().getName();
            
            if (name != null) {
                File tempFile = new File(name);
                assertEquals(fileArray[i],tempFile.getPath());
            } else {
                fail("Expected fileName, " + fileArray[i] + ", not present.");
            }
            
            stream = attachment.getDataHandler().getInputStream();
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
    @Path("/iAttachmentsWithDefaults")
    @Consumes("multipart/form-data")
    public Response postMultipartIAttachmentsWithDefaultMediaType(List <IAttachment> attachments) throws IOException{
        String[] contentTypeArray = {"text/plain","text/plain","application/octet-stream"};
        String[] fileArray =  {"fileid","description","person.xml"};
        String[] contentArray = {"person1234","XML file about person1234",
                                 "<person>    <name>Chris</name>    <position>laying</position>    <temperature>99.9</temperature></person>"};
        InputStream stream = null;
        for (IAttachment attachment : attachments) {
            if (attachment == null) {
                fail("No IAttachments sent to Post method.");
            }
            int i = getIndex(attachment.getDataHandler().getName(), fileArray);
            String contentType = attachment.getDataHandler().getContentType();;
            assertEquals(contentTypeArray[i], contentType);
  
            String name = attachment.getDataHandler().getName();
            
            if (name != null) {
                File tempFile = new File(name);
                assertEquals(fileArray[i],tempFile.getPath());
            } else {
                fail("Expected fileName, " + fileArray[i] + ", not present.");
            }
            
            stream = attachment.getDataHandler().getInputStream();
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
