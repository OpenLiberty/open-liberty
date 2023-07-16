/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.container.service.state;

public class ApplicationStateListenerConstants {

    //This class stores all the service rankings for application state listeners in one place alongside the reasons for them to have that rank.

    public static final int DEFAULT_SERVICE_RANK = 0;

    //EJB must shut down after CDI (see below).
    public static final int EJB_SERVICE_RANK = DEFAULT_SERVICE_RANK;

    //CDI must shut down before EJB as EJBs can have a cdi application scope and according to the CDI spec "jakarta.enterprise.event.Shutdown is not after @BeforeDestroyed(ApplicationScoped.class)"
    public static final int CDI_SERVICE_RANK = 1;

}
