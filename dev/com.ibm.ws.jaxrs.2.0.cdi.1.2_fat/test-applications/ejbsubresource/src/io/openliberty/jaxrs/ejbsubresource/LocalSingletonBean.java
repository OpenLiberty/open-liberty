/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxrs.ejbsubresource;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Stateless(name = "LocalSingletonBean")
@Local({ ILocalSingletonBean.class })
public class LocalSingletonBean implements ILocalSingletonBean {

    public LocalSingletonBean() {}

    @Override
    public void remove() {}

    @Context
    private UriInfo ui;

    @Override
    @GET
    public String get() {
        return ui.getRequestUri().toASCIIString();
    }
}
