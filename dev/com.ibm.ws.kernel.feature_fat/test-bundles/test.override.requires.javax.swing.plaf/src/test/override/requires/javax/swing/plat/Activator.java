/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.override.requires.javax.swing.plat;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWiring;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        BundleWiring wiring = context.getBundle().adapt(BundleWiring.class);

        wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE).stream() //
                        .filter((w) -> "javax.swing.plaf".equals(w.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))) //
                        .filter((w) -> {
                            System.out.println("SUCCESS: wire found to -> " + w.getProvider().getSymbolicName());
                            return true;
                        }) //
                        .findFirst() //
                        .orElseThrow(NoSuchFieldException::new);
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }

}
