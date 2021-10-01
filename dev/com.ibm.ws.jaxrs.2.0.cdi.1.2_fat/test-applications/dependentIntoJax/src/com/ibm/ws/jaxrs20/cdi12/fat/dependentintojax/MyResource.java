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
package com.ibm.ws.jaxrs20.cdi12.fat.dependentintojax;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/testDepPreDestroy")
public class MyResource {

    private static List<String> preDestroyMsgs = new LinkedList<String>();

    @Inject DependentBean db;
	
    @GET
    public String getRequest() {
        System.out.println("getRequest() - db = " + db);
        return "preDestroy was called " + preDestroyMsgs.size() + " times";
    }

    public static void registerPreDestroy(String s) {
        preDestroyMsgs.add(s);
    }
}
