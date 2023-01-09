/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package concurrent.mp.fat.jaxrs.web;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/testapp")
public class MPContextJAXRSTestApp extends Application {
    // TODO switching to singleton is one way to demonstrate that JAX-RS context is not propagated to other threads.
    // Another way would be switching MPContextJAXRSURIInfo to CDI @ApplicationScoped.  If the injected URIInfo could be
    // made into a CDI bean that is request scoped, then CDI context propagation should in theory allow the JAX-RS context to propagate.
    public Set<Object> getSingletons_disabled() {
        return Collections.singleton(new MPContextJAXRSURIInfo());
    }
}