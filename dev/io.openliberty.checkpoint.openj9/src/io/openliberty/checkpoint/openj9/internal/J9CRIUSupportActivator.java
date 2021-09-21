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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;

public class J9CRIUSupportActivator implements BundleActivator {

    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    public void start(BundleContext bc) {
        try {
            Class.forName("org.eclipse.openj9.criu.CRIUSupport");
            bc.registerService(ExecuteCRIU.class, new ExecuteCRIU_OpenJ9(), null);
        } catch (ClassNotFoundException e) {
            // do nothing; not on open j9 that supports CRIU
        }
    }

    @Override
    public void stop(BundleContext bc) {

    }

}
