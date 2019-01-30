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
package com.ibm.ws.jaxrs.fat.contextresolver;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

@Path(value = "/user")
public class UserAccount {

    private static Map<String, User> users = new HashMap<String, User>();

    @POST
    @Consumes(value = "text/xml")
    public void createUser(JAXBElement<User> element) {
        User user = element.getValue();
        users.put(user.getUserName(), user);
    }

    @GET
    @Path(value = "/{userName}")
    @Produces(value = "text/xml")
    public JAXBElement<User> getUser(@PathParam(value = "userName") String userName) {
        User user = users.get(userName);
        return new JAXBElement<User>(new QName("http://jaxb.context.tests", "user"), User.class, user);
    }

}
