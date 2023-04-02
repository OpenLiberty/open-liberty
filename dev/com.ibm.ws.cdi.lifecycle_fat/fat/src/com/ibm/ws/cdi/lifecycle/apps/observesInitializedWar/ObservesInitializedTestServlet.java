/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.lifecycle.apps.observesInitializedWar;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.lifecycle.apps.observesInitializedManifestJar.ManifestAutostartObserver;
import com.ibm.ws.cdi.lifecycle.apps.observesInitializedWebInfJar.WebInfAutostartObserver;

import componenttest.app.FATServlet;

@WebServlet("/ObservesInitializedTestServlet")
public class ObservesInitializedTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;
    @Inject
    WebInfAutostartObserver webInfObserver;
    @Inject
    ManifestAutostartObserver manifestObserver;
    @Inject
    WarAutostartObserver warObserver;

    @Test
    public void testWebInfObservesInitialization() {
        assertTrue(webInfObserver.methodCalled());
    }

    @Test
    public void testManifestJarObservesInitialization() {
        assertTrue(manifestObserver.methodCalled());
    }

    @Test
    public void testWarObservesInitialization() {
        assertTrue(warObserver.methodCalled());
    }

}
