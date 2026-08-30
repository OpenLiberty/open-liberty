/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.library.packages.importer2;

import java.util.List;
import java.util.concurrent.Callable;

import org.osgi.framework.BundleReference;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Component;

@Component(service=Callable.class,
                property="test.library.packages=true")
public class TestImporter implements Callable<String>{
    private static final String[] libraryPackages = {"test.library.packages.exporter1", "test.library.packages.exporter2"};
    private static final String libraryClassName = ".TestLibraryClass";
    private static final String doesNotExist = "/DoesNotExistResource";


    @Override
    public String call() throws Exception {
        StringBuilder result = new StringBuilder();
        for (String packageName : libraryPackages) {
            result.append(checkPackage(packageName));
        }
        return result.toString();
    }

    /**
     * @param packageName
     */
    private String checkPackage(String packageName) throws Exception {
        if (checkLibraryWire(packageName)) {
            return "PASSED." + Class.forName(packageName + libraryClassName).newInstance() + ";";
        } else {
            return "FAILED." + packageName + ";";
        }

    }

    private boolean checkLibraryWire(String packageName) {
        BundleReference bundleReference = (BundleReference) getClass().getClassLoader();
        BundleWiring wiring = bundleReference.getBundle().adapt(BundleWiring.class);
        wiring.getClassLoader().getResource(packageName.replace('.', '/') + doesNotExist);
        List<BundleWire> packageWires = wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
        for (BundleWire bundleWire : packageWires) {
            if (packageName.equals(bundleWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))) {
                return true;
            }
        }
        return false;
    }

}
