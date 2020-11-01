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
package jaxrs21.fat.jsonp;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/resource")
public class JsonpResource {

    @POST
    @Path("/number/incr")
    @Consumes("application/json")
    @Produces("application/json")
    public JsonNumber incrNumber(JsonNumber num) {
        int input = num.intValue();
        JsonNumber jsonNum = Json.createValue(input + 1);
        return jsonNum;
    }
}
