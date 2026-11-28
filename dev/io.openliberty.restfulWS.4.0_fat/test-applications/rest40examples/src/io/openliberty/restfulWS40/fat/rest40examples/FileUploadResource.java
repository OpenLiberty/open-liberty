/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.restfulWS40.fat.rest40examples;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.InputStream;
import java.io.IOException;

/**
 * Demonstrates multipart/form-data handling in Jakarta REST 4.0
 * using the standard EntityPart API (new in REST 4.0)
 */
@Path("/upload")
public class FileUploadResource {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(
            @FormParam("file") EntityPart filePart,
            @FormParam("description") String description) {

        if (filePart == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new UploadResponse(null, 0, "No file provided"))
                .build();
        }

        try {
            String fileName = filePart.getFileName().orElse("unknown");

            // Read the file content to get size
            long fileSize = 0;
            try (InputStream is = filePart.getContent()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fileSize += bytesRead;
                }
            }

            // Process the file
            System.out.println("Uploading file: " + fileName);
            System.out.println("File size: " + fileSize);
            System.out.println("Description: " + description);
            System.out.println("Content-Type: " + filePart.getMediaType());

            return Response.ok()
                .entity(new UploadResponse(fileName, fileSize, "Upload successful"))
                .build();

        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new UploadResponse(null, 0, "Error processing file: " + e.getMessage()))
                .build();
        }
    }
}

// Made with Bob
