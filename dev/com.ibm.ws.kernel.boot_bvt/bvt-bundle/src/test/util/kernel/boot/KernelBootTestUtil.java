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
package test.util.kernel.boot;

import java.io.File;
import java.io.IOException;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Block the deactivate method if a known file exists
 */
public class KernelBootTestUtil implements BundleActivator {

    /** {@inheritDoc} */
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("BVT BUNDLE: Starting " + this);
    }

    /** {@inheritDoc} */
    @Override
    public void stop(BundleContext context) throws Exception {
        File file = new File("TestBundleDeactivate.txt").getAbsoluteFile();
        System.out.println("BVT BUNDLE: Stopping " + this + " and creating " + file);

        try {
            if (!file.exists() && !file.createNewFile())
                throw new IllegalStateException("Failed to create " + file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println("Created " + file);

        File f = new File("TestBundleDeactivatePrevented.txt").getAbsoluteFile();
        for (int i = 0; i < 60; i++) {
            try {
                if (f.exists()) {
                    Thread.sleep(500);
                    System.out.println("Waiting for file " + f + " to be removed");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
