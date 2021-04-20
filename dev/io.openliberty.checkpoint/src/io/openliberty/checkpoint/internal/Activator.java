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
package io.openliberty.checkpoint.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
    private CheckpointImpl checkpoint;

    @Override
    public void start(BundleContext context) throws Exception {
        checkpoint = new CheckpointImpl();
        checkpoint.register(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        checkpoint.unregister();
    }

}
