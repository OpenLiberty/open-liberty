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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.withbeanname;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Stateless(name = "MyStatelessWithBeanNameEJBResource")
@Path("/statelessWithBeanNameEJBResource")
public class StatelessWithBeanNameEJBResource {

    @Resource(name = "injectedString")
    private String injectedString;

    @GET
    public String get() {
        return StatelessWithBeanNameEJBResource.class.getName() + injectedString;
    }
}
