/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.cdi.lifecycle.apps.observesInitializedManifestJar;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class ManifestAutostartObserver {

    private boolean onInitCalled = false;

    public boolean methodCalled() {
        return onInitCalled;
    }

    public void onInitialize(@Observes @Initialized(ApplicationScoped.class) Object o) {
        onInitCalled = true;
    }

}