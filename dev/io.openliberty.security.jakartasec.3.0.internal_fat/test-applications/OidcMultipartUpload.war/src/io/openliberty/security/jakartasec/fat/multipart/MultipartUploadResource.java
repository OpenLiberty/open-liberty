/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.fat.multipart;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;

/**
 * The multipart upload endpoint from issue 35585. It is intentionally unprotected
 * (no {@code @RolesAllowed} / {@code @DenyAll}) so that an anonymous upload is allowed
 * through while the OpenID Connect HAM is still invoked for the request.
 */
@Path("/api/upload")
public class MultipartUploadResource {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String upload(List<EntityPart> parts) {
        String details = parts.stream()
                              .map(part -> part.getName() + ":" + part.getFileName().orElse("<no-filename>"))
                              .collect(Collectors.joining(","));
        return "parts=" + parts.size() + ";details=" + details;
    }
}
