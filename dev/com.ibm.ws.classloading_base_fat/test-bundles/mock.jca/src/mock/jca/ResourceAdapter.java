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
package mock.jca;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.classloading.ClassProvider;
import com.ibm.ws.classloading.LibertyClassLoader;
import com.ibm.wsspi.library.Library;

@Component(configurationPid = "com.ibm.ws.jca.resourceAdapter",
           configurationPolicy = REQUIRE)
public class ResourceAdapter implements ClassProvider {

    private Library lib;

    @Reference(name = "library", target = "(id=unbound)")
    protected void setLibrary(Library lib) {
        System.out.println("###setLibrary(" + lib.id() + ")");
        this.lib = lib;
    }

    protected void unsetLibrary(Library lib) {
        System.out.println("###unsetLibrary(" + lib.id() + ")");
        this.lib = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <CL extends ClassLoader & LibertyClassLoader> CL getDelegateLoader() {
        System.out.println("###getDelegateLoader()");
        return (CL) lib.getClassLoader();
    }

}
