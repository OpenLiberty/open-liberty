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
package com.ibm.ws.jaxrs.fat.getCgetS.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("res1")
public class TestResource1 {

    private static Object synObj = "synObj";
    private static int beanId = 0;
    private int id = -1;

    /**
     * per request instance
     */
    public TestResource1() {
        synchronized (synObj) {
            id = beanId;
            beanId += 1;
        }
    }

    /**
     * singleton instance
     *
     * @param cid
     */
    public TestResource1(int cid) {
        this.id = cid;
    }

    @GET
    @Path("testGetSingletonHigherThanGetClass")
    public String testGetSingletonHigherThanGetClass() {
        return "ID=" + String.valueOf(id);
    }
}
