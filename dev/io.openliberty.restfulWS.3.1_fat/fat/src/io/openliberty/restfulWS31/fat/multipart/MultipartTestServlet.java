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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.resteasy.plugins.providers.multipart.ResteasyEntityPartBuilder;
import org.junit.After;
import org.junit.Test;

import com.ibm.websphere.jaxrs20.multipart.AttachmentBuilder;
import com.ibm.websphere.jaxrs20.multipart.IAttachment;

import componenttest.app.FATServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
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
    public void testMultipartRequestIAttachment() throws Exception {
        testIAttachmentMultipartRequest("/app/multi/iAttachments", false);
    }
    @Test
    public void testMultipartRequestIAttachmentWithDefaultMediaType() throws Exception {
        testIAttachmentMultipartRequest("/app/multi/iAttachmentsWithDefaults",true);
    }
    @Test
    public void testMultipartRequestEntityPart() throws Exception {
        testEntityPartMultipartRequest("/app/multi/entityParts",false);
    }
    @Test
    public void testMultipartRequestEntityPartWithDefaultMediaType() throws Exception {
        testEntityPartMultipartRequest("/app/multi/entityPartsWithDefaults",true);
    }

        
    private void testIAttachmentMultipartRequest(String path, boolean useDefault) {
        List<IAttachment> attachments = new ArrayList<>();

        // MediaType will be defaulted if not provided, so test both.
        try {
            if (useDefault) {
                attachments.add(AttachmentBuilder.newBuilder("fileid")
                                .inputStream(new ByteArrayInputStream("person1234".getBytes()))
                                .build());
                attachments.add(AttachmentBuilder.newBuilder("description")
                                .inputStream(new ByteArrayInputStream("XML file about person1234".getBytes()))
                                .build());
                
                File file = new File("./person.xml");
                attachments.add(AttachmentBuilder.newBuilder("thefile")
                                .inputStream("person.xml",new FileInputStream(file))
                                .build());


            } else {
                attachments.add(AttachmentBuilder.newBuilder("fileid")
                                .inputStream(new ByteArrayInputStream("person1234".getBytes()))
                                .contentType(MediaType.TEXT_PLAIN)
                                .build());
                attachments.add(AttachmentBuilder.newBuilder("description")
                                .inputStream(new ByteArrayInputStream("XML file about person1234".getBytes()))
                                .contentType(MediaType.WILDCARD)
                                .build());
                
                File file = new File("./person.xml");
                attachments.add(AttachmentBuilder.newBuilder("thefile")
                                .inputStream("person.xml",new FileInputStream(file))
                                .contentType(MediaType.APPLICATION_XML_TYPE)
                                .build());
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception caught: " + e);
        }
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