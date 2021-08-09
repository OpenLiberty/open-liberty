/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.internal;

import org.osgi.framework.BundleContext;

import com.ibm.ws.kernel.boot.BootstrapConfig;

/**
 * Internal interface to allow for testing via mock
 */
public interface Provisioner {
    void initialProvisioning(BundleContext systemBundleCtx, BootstrapConfig config) throws InvalidBundleContextException;

    final static class InvalidBundleContextException extends Exception {
        private static final long serialVersionUID = 1L;
        // no-op marker class
    }
}
