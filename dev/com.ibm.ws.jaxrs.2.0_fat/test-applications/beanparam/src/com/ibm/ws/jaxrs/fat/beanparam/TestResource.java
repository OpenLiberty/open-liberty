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
package com.ibm.ws.jaxrs.fat.beanparam;

import javax.ws.rs.BeanParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.TEXT_PLAIN)
@Path("/")
public class TestResource {

    @BeanParam
    BeanParamEntity fieldBeanParam;

    @POST
    @Path("formparam")
    public String formParam(String content, @BeanParam BeanParamEntity bean) {
        System.out.println("content=" + content);
        System.out.println("bean.form=" + bean.form);
        System.out.println("bean.inner.innerForm=" + bean.inner.innerForm);
        return content + "&" + bean.form + "&" + bean.inner.innerForm;
    }

    @POST
    @Path("cookieparam")
    public String cookieParam(String content, @BeanParam BeanParamEntity bean) {
        System.out.println("content=" + content);
        System.out.println("bean.cookie=" + bean.cookie);
        System.out.println("bean.inner.innerCookie=" + bean.inner.innerCookie);
        return content + "&" + bean.cookie + "&" + bean.inner.innerCookie;
    }
}
