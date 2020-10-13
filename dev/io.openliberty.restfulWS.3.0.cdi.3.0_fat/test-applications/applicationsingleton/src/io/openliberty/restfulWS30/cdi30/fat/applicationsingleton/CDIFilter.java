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
package io.openliberty.restfulWS30.cdi30.fat.applicationsingleton;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CDIFilter implements ClientRequestFilter, ContainerRequestFilter {

    SimpleBean simpleBean;

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        System.out.println(simpleBean.getMessage());
    }

    @Override
    public void filter(ClientRequestContext context) throws IOException {
        System.out.println(simpleBean.getMessage());
    }

    @Inject
    public void setSimpleBean(SimpleBean simpleBean) {
        this.simpleBean = simpleBean;
    }
}
