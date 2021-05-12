/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs2x.fat.thirdparty.multipart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.activation.DataHandler;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;

@Path("/resource")
public class UploadServiceImpl {

    @GET
    @Path("{id}")
    public Response getUserById(@PathParam("id") String id) {

        return Response.status(200).entity("SingleParameter2 - getUserById is called, id : " + id).build();

    }

    @POST
    @Path("/uploadFile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(List<IAttachment> attachments, @Context HttpServletRequest request) {
        for (IAttachment attachment : attachments) {
            DataHandler handler = attachment.getDataHandler();
            try {
                InputStream stream = handler.getInputStream();
                MultivaluedMap<String, String> map = attachment.getHeaders();
                System.out.println("fileName Here " + getFileName(map));
                OutputStream out = new FileOutputStream(new File("./" + getFileName(map)));

                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = stream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                stream.close();
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return Response.ok("file uploaded").build();
    }

    @POST
    @Path("/uploadFile2")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile2(List<IAttachment> attachments, @Context HttpServletRequest request) {
        try {
            for (IAttachment attachment : attachments) {
                DataHandler handler = attachment.getDataHandler();
                InputStream stream = null;
                OutputStream out = null;
                try {
                    stream = handler.getInputStream();
                    MultivaluedMap<String, String> map = attachment.getHeaders();
                    System.out.println("fileName2 Here " + getFileName(map));
                    out = new FileOutputStream(new File("./" + getFileName(map)));
                    StringBuilder stringBuilder = new StringBuilder();
                    Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
                    char[] chars = new char[1024];
                    int charsRead;
                    while((charsRead = in.read(chars, 0, chars.length)) > 0) {
                        stringBuilder.append(chars, 0, charsRead);
                        out.write(new String(chars).getBytes("UTF-8"), 0, charsRead);
                    }
                    System.out.println("uploadFile2 stringBuilder.toString(): " + stringBuilder.toString());
                    if (!(stringBuilder.toString().contains("0123456789"))) {
                        throw new RuntimeException("uploadFile2: missing uploaded data");
                    }
                } finally {
                    if (stream != null) {
                        stream.close();
                    }
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                }
            }
            return Response.ok("file uploaded").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("caught exception: " + e).build();
        }
    }



    private String getFileName(MultivaluedMap<String, String> header) {
        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                String[] name = filename.split("=");
                String exactFileName = name[1].trim().replaceAll("\"", "");
                return exactFileName;
            }
        }
        return "unknown";
    }
}