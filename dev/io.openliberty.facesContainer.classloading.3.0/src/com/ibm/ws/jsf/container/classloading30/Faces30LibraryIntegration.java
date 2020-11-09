/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.classloading30;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.library.ApplicationExtensionLibrary;
import com.ibm.wsspi.library.Library;

@Component(name = "com.ibm.ws.jsf.container.classloading.3.0",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true)
public class Faces30LibraryIntegration implements ApplicationExtensionLibrary {

    // Defines a required dependency on the <library> config element with the
    // id="com.ibm.ws.facesContainer.3.0" (provided by defaultInstances.xml in this same bundle)
    // in order to have the referenced Library be automatically applied to app classloaders
    @Reference(target = "(config.displayId=library[com.ibm.ws.facesContainer.3.0])")
    protected Library lib;

    @Activate
    protected void activate() {
        // Pass the JSF spec level to the application extension via system property
        // since the application cannot access other registries such as OSGi

        //Keep com.ibm.ws.jsfContainer.JSF_SPEC_LEVEL the same as the variable is also used
        // for 2.2 & 2.3 container features
        System.setProperty("com.ibm.ws.jsfContainer.JSF_SPEC_LEVEL", "3.0");
    }

    @Override
    public Library getReference() {
        return lib;
    }

}
