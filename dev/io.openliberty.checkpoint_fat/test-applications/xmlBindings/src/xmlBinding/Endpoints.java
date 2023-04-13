/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package xmlBinding;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@ApplicationScoped
@Path("endpoint")
public class Endpoints {

    @GET
    @Path("properties")
    @Produces(MediaType.APPLICATION_XML)
    public XMLobject getProperties() throws Exception {
        XMLobject xmlObject = new XMLobject();
        xmlObject.setName("XML");
        xmlObject.setId(5);
        return xmlObject;
    }

}
