/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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

package com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.ApplicationScoped.Instance;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Path;

import com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjectionInstanceMicroProfileApp;

// http://localhost:<nonSecurePort>/microProfileApp/rest/ClaimInjectionApplicationScopedInstance/MicroProfileApp
// allow the same methods to invoke GET, POST, PUT, ... invocation type determines which is invoked.

@Path("ClaimInjectionApplicationScopedInstance")
@ApplicationScoped
public class MicroProfileApp extends ClaimInjectionInstanceMicroProfileApp {

}
