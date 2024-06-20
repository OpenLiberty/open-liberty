/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.thread.term.internal;

import org.osgi.framework.BundleContext;

/**
 * Class which is notified when the thread termination manager bundle is
 * started and stopped.
 */
public class TerminationManagerBundleActivator implements org.osgi.framework.BundleActivator {

    /** {@inheritDoc} */
    @Override
    public void start(BundleContext context) throws Exception {
        // ------------------------------------------------------------------
        // Nothing to do on bundle start.
        // ------------------------------------------------------------------
    }

    /** {@inheritDoc} */
    @Override
    public void stop(BundleContext context) throws Exception {
        // ------------------------------------------------------------------
        // On bundle stop, notify the thread termination manager that the
        // native code must be re-loaded if the termination manager is
        // re-activated.
        // ------------------------------------------------------------------
        TerminationManagerImpl.nativeRegistrationCompleted = false;
    }

}
