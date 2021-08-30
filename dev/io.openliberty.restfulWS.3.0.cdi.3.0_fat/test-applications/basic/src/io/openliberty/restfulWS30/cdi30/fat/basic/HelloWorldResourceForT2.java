/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.cdi30.fat.basic;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

@Path("/helloworldt2")
public class HelloWorldResourceForT2 extends HelloWorldResource {

    public HelloWorldResourceForT2(@Context UriInfo uriinfoForC) {
        System.out.println(uriinfoForC.toString());
        super.setType(uriinfoForC.getPath());
    }
}