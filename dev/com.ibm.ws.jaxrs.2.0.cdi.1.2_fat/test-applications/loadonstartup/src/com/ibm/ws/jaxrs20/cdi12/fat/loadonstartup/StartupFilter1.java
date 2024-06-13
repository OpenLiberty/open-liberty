/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.cdi12.fat.loadonstartup;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

// Change from ApplicationScoped to RequestScoped to verify the fix for issue 9976
@RequestScoped
public class StartupFilter1 implements ContainerRequestFilter {

    @Inject
    private StartupBean1 bean;

    @PostConstruct
    public void init() {
        System.out.println("StartupFilter1.PostConstruct.init: bean = " + bean);
    }

    public void filter(ContainerRequestContext requestContext) throws IOException {
        bean.run();
    }
}
