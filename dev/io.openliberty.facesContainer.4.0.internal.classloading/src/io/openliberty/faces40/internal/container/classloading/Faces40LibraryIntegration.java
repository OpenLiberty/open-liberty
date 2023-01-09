/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.faces40.internal.container.classloading;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.library.ApplicationExtensionLibrary;
import com.ibm.wsspi.library.Library;

@Component(name = "com.ibm.ws.jsf.container.classloading.4.0",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true)
public class Faces40LibraryIntegration implements ApplicationExtensionLibrary {

    // Defines a required dependency on the <library> config element with the
    // id="io.openliberty.facesContainer.4.0" (provided by defaultInstances.xml in this same bundle)
    // in order to have the referenced Library be automatically applied to app classloaders
    @Reference(target = "(config.displayId=library[io.openliberty.facesContainer.4.0])")
    protected Library lib;

    @Activate
    protected void activate() {
        // Pass the JSF spec level to the application extension via system property
        // since the application cannot access other registries such as OSGi

        //Keep com.ibm.ws.jsfContainer.JSF_SPEC_LEVEL the same as the variable is also used
        // for 2.2 & 2.3 container features
        System.setProperty("com.ibm.ws.jsfContainer.JSF_SPEC_LEVEL", "4.0");
    }

    @Override
    public Library getReference() {
        return lib;
    }

}
