/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.interop;

import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.ibm.ws.jaxws.test.wsr.server.stub.People;
import com.ibm.ws.jaxws.test.wsr.server.stub.PeopleService;

@Path("/ep1")
public class InteropStartResource {

    @GET
    @Path("jaxwsEP1")
    public String jaxwsEP1Client() throws MalformedURLException {
//        Call JAX-WS WS with passOver parameter and return result
        final String WSDL_URL = new StringBuilder().append("http://localhost:")
                        .append(Integer.getInteger("bvt.prop.HTTP_default"))
                        .append("/helloServer/PeopleService?wsdl")
                        .toString();
        PeopleService service = new PeopleService(new URL(WSDL_URL));
        People bill = service.getBillPort();
        String result = bill.hello();
        return result;
    }
}
