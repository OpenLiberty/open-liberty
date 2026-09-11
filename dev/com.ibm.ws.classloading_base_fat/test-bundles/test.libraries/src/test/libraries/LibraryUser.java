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
package test.libraries;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.library.Library;

/**
 *
 */
@Component(configurationPid = "test.library.user",
           configurationPolicy = REQUIRE,
           enabled = true,
           immediate = true)
public class LibraryUser {

    @Activate
    protected void activate() {
        System.out.println("LibraryUser00 - entered activate method");
    }

    @Deactivate
    protected void deactivate() {
        System.out.println("LibraryUser00 - entered deactivate method");
    }

    @Reference(cardinality = MANDATORY, updated = "updateLibrary")
    protected void setLibrary(Library lib) {
        System.out.println("LibraryUser01 - entered setLibrary method");
    }

    protected void unsetLibrary(Library lib) {
        System.out.println("LibraryUser02 - entered unsetLibrary method");
    }

    protected void updateLibrary(Library lib) {
        System.out.println("LibraryUser03 - entered updateLibrary method");
    }

}
