/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.jakarta.data.inmemory.provider;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import jakarta.enterprise.lang.model.declarations.FieldInfo;

/**
 * A fake Jakarta Data provider extension that only produces a single repository class,
 * which is because it doesn't have a real implementation and is only for tests
 * that register a Jakarta Data provider as a CDI extension.
 */
public class CompositeBuildCompatibleExtension implements BuildCompatibleExtension {

    //@Trivial
    @Discovery
    public void discovery(ScannedClasses scan) {
        System.out.println("Discovery invoked");
    }

    @Enhancement(types = Object.class, withSubtypes = true)
    public void enhancement(FieldInfo info) {
        System.out.println("enhancement field info: " + info.toString());

        // Causes SecurityException "java.lang.RuntimePermission" "accessDeclaredMembers"
        // and there does not appear to be any doPriv in the stack:
        //System.out.println("  declaring class:      " + info.declaringClass());
    }
}
