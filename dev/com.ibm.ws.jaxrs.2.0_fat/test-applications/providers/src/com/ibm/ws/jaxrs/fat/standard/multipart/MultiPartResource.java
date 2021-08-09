/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.standard.multipart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.wink.common.model.multipart.BufferedOutMultiPart;
import org.apache.wink.common.model.multipart.OutPart;

@Path("providers/standard/multipart")
public class MultiPartResource {

    @POST
    @Consumes("multipart/form-data")
    @Produces("multipart/form-data")
    public Response postFormData(@FormParam("file1") File file1,
                                 @FormParam("first") String first,
                                 @FormParam("last") String last,
                                 @FormParam("file2") File file2,
                                 @FormParam("int") int i) {
        BufferedOutMultiPart bomp = new BufferedOutMultiPart();
        OutPart op = new OutPart();
        op.setBody(file2);
        op.setContentType(MediaType.TEXT_PLAIN);
        op.setLocationHeader("file2");
        bomp.addPart(op);
        op = new OutPart();
        op.setBody(first);
        op.setContentType(MediaType.TEXT_PLAIN);
        op.setLocationHeader("first");
        bomp.addPart(op);
        op = new OutPart();
        op.setBody(last);
        op.setContentType(MediaType.TEXT_PLAIN);
        op.setLocationHeader("last");
        bomp.addPart(op);
        op = new OutPart();
        op.setBody(file1);
        op.setContentType(MediaType.TEXT_PLAIN);
        op.setLocationHeader("file1");
        bomp.addPart(op);
        op = new OutPart();
        op.setBody(i + "");
        op.setContentType(MediaType.TEXT_PLAIN);
        op.setLocationHeader("int");
        bomp.addPart(op);
        return Response.ok(bomp, "multipart/form-data").build();
    }

    @POST
    @Path("/decoded")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public String decodedParameter(@FormParam("string") String string, @FormParam("file") File file)
                    throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String fileContents = reader.readLine();
        reader.close();
        return string + " " + fileContents;
    }

    @POST
    @Path("/encoded")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public String encodedParameter(@Encoded @FormParam("string") String string,
                                   @Encoded @FormParam("file") File file) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String fileContents = reader.readLine();
        reader.close();
        return string + " " + fileContents;
    }
}
