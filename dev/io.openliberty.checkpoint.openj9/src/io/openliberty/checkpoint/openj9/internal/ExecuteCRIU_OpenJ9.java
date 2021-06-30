/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.openj9.internal;

import java.io.File;
import java.io.IOException;

import org.eclipse.openj9.criu.CRIUSupport;
import org.eclipse.openj9.criu.CRIUSupport.CRIUResult;
import org.eclipse.openj9.criu.CRIUSupport.CRIUResultType;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;

public class ExecuteCRIU_OpenJ9 implements BundleActivator, ExecuteCRIU {

    @Override

    public void start(BundleContext bc) {
        try {
            Class.forName("org.eclipse.openj9.criu.CRIUSupport");
            bc.registerService(ExecuteCRIU.class, this, null);
        } catch (ClassNotFoundException e) {
            // do nothing; not on open j9 that supports CRIU
        }
    }

    @Override
    public void stop(BundleContext bc) {

    }

    @Override
    public int dump(File directory) throws IOException {
        if (!CRIUSupport.isCRIUSupportEnabled()) {
            // TODO log appropriate message
            System.out.println("Must set the JVM option: -XX:+EnableCRIUSupport");
            return -50;
        }
        CRIUResult result = CRIUSupport.checkPointJVM(directory.toPath());
        if (result.getType() == CRIUResultType.SUCCESS) {
            return 1;
        }
        return -50;
    }

}
