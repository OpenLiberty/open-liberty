/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.service;

import java.io.File;

import com.ibm.ws.kernel.provisioning.ServiceFingerprint;

/**
 * Product extension locations are resolved in the OSGi framework, but the ServiceFingerprint
 * is managed in the bootstrap code (i.e. outside the OSGi framework).
 * 
 * This class is a bridge between the OSGi framework and the ServiceFingerprint loaded in the
 * bootstrap code. It is loaded in a classloader outside of the OSGi framework and delegated to
 * from inside the OSGi framework.
 */
public class ProductExtensionServiceFingerprint {

    public static void putProductExtension(String productExtensionName, String productExtensionInstallLocation) {
        ServiceFingerprint.putInstallDir(productExtensionName, new File(productExtensionInstallLocation));
    }

}
