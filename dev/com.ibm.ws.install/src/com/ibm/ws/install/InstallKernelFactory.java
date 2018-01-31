/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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
