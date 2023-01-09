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
package io.openliberty.jcache.internal.fat.loginmodule;

import java.security.Principal;

/**
 * Custom principal which isn't Serializable and is used to test serialization of Subjects to the authentication cache.
 */
public class NonSerializablePrincipal implements Principal {
    @Override
    public String getName() {
        return "NonSerializablePrincipalName";
    }

    @Override
    public String toString() {
        return NonSerializablePrincipal.class.getSimpleName();
    }
}
