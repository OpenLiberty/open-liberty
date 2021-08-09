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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import javax.activation.DataHandler;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;
import com.ibm.websphere.jaxrs20.multipart.IMultipartBody;

@Path("/resource2")
public class MultipartServiceImpl {
    @POST
    @Path("/multipartbody")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response processMultiparts(IMultipartBody multipartBody) throws IOException {
        List<IAttachment> attachments = multipartBody.getAllAttachments();
        String responseString = null;
        InputStream stream = null;
        OutputStream out = null;
        for (Iterator<IAttachment> it = attachments.iterator(); it.hasNext();) {
            IAttachment attachment = it.next();
            if (attachment == null) {
                continue;
            }
            DataHandler dataHandler = attachment.getDataHandler();
            stream = dataHandler.getInputStream();
            MultivaluedMap<String, String> map = attachment.getHeaders();
            String filename = getFileName(map);
            System.out.println("fileName Here: " + filename);
            if (filename.equals("unknown")) {
                responseString = getString(stream);
                System.out.println(responseString);
            } else {
                out = new FileOutputStream(new File("./" + "2" + getFileName(map)));

                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = stream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
            }
        }
        if (stream != null) {
            stream.close();
        }
        if (out != null) {
            out.flush();
            out.close();
        }

        return Response.ok(responseString).build();
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

    private String getString(InputStream in) {
        StringBuffer sb = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }

        return sb.toString();

    }
}
