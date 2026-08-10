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
package com.ibm.ws.jaxrs.fat.securitylimits;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;

/**
 * A simple JAX-RS resource used by all three application paths to test
 * security limit enforcement for attachment headers and form parameters.
 */
@Path("/limits")
public class SecurityLimitsResource {

    /**
     * Accepts a multipart/form-data POST. The attachment deserializer will
     * enforce the attachment-headers-max-count limit when parsing each part.
     */
    @POST
    @Path("/multipart")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String acceptMultipart(List<IAttachment> parts) {
        return "OK:" + parts.size();
    }

    /**
     * Accepts a URL-encoded form POST. FormUtils will enforce the
     * maxFormParameterCount limit when parsing the body.
     */
    @POST
    @Path("/form")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String acceptForm(@FormParam("p1") String p1) {
        return "OK";
    }
}
