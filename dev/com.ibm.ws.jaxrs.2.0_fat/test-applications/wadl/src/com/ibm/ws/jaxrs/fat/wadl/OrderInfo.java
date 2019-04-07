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
package com.ibm.ws.jaxrs.fat.wadl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/Order")
public interface OrderInfo {

    @GET
    @Produces("application/xml")
    @Path("{orderId}")
    public Order getOrder(@PathParam("orderId") int officeId);

    @GET
    @Produces("application/xml")
    @Path("All")
    public OrderList getAllOrders();

}