/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.loadonstartup;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

@ApplicationScoped
public class StartupFilter3 implements ContainerRequestFilter {

    @Inject
    private StartupBean3 bean;

    @PostConstruct
    public void init() {
        System.out.println("StartupFilter3.PostConstruct.init: bean = " + bean);
    }

    public void filter(ContainerRequestContext requestContext) throws IOException {
        bean.run();
     }
}
