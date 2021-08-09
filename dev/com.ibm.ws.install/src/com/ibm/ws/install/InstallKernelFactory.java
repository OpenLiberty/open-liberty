/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

import java.io.File;

import com.ibm.ws.install.internal.InstallKernelImpl;

/**
 * This class provides the APIs to get an instance of IInstallKernel.
 */
public class InstallKernelFactory {

    private static InstallKernel installKernel;

    /**
     * Return the singleton instance of InstallKernel. It should be used only by Feature Manager or LPM.
     *
     * @return the installKernel
     */
    public static InstallKernel getInstance() {
        if (installKernel == null) {
            installKernel = new InstallKernelImpl();
        }
        return installKernel;
    }

    /**
     * Return the singleton instance of InstallKernel.
     * The instance will be reset if the installRoot is changed.
     *
     * @param installRoot the liberty installation root.
     * @return the installKernel
     */
    public static InstallKernel getInstance(File installRoot) {
        if (installKernel == null) {
            installKernel = new InstallKernelImpl(installRoot);
        }
        return installKernel;
    }

    /**
     * Return a new instance of InstallKernelImpl
     *
     * @return the InstallKernelImpl instance
     */
    public static InstallKernelInteractive getInteractiveInstance() {
        return new InstallKernelImpl();
    }
}
