/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.classloading;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.library.ApplicationExtensionLibrary;
import com.ibm.wsspi.library.Library;

@Component(name = "com.ibm.ws.jsf.container.classloading",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true)
public class JSFLibraryIntegration implements ApplicationExtensionLibrary {

    @Reference(target = "(config.displayId=library[com.ibm.ws.jsfContainer.2.2])")
    protected Library lib;

    @Override
    public Library getReference() {
        return lib;
    }

}