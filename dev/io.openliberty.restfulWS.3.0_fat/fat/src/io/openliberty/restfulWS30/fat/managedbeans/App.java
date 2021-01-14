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
package io.openliberty.restfulWS30.fat.managedbeans;

import jakarta.annotation.ManagedBean;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

@ManagedBean("application")
@ApplicationPath("/app")
public class App extends Application {


    static boolean uriInfoInjectedBeforePostConstruct;

    @Context
    UriInfo uriInfo;

    @PostConstruct
    public void init() {
        uriInfoInjectedBeforePostConstruct = uriInfo != null;
    }
}
