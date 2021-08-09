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
package com.ibm.ws.jaxrs.fat.jacksonJsonIgnore;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {
    @GET
    public TestPojo get() throws JsonProcessingException {
        TestPojo n = new TestPojo();
        ObjectMapper om = new ObjectMapper();
        String v = om.writeValueAsString(n);
        TestPojo t = new TestPojo();
        t.setFish("fish::" + v + "::\n");
        return t;
    }
}