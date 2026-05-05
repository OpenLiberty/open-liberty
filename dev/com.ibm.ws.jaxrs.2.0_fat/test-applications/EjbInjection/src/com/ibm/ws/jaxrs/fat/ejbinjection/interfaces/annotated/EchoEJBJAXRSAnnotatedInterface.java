/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.annotated;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

//@Path("/singlenonimplementedannotatedinterface")
public interface EchoEJBJAXRSAnnotatedInterface {

    @POST
    @Path("echo")
    public String echo(String message);

}
