/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence.cdi;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Namespace prefix of a Repository dataStore value.
 */
@Trivial
enum Namespace {
    GLOBAL, // java:global
    APP, // java:app
    MODULE, // java:module
    COMP; // java:comp

    /**
     * Returns true if the granularity of this namespace exceeds
     * that of the specified namespace.
     *
     * @param ns namespace to compare with. Null is compared as java:global.
     * @return true if more granular. Otherwise false.
     */
    final boolean isMoreGranularThan(Namespace ns) {
        return ordinal() > (ns == null ? GLOBAL : ns).ordinal();
    }

    /**
     * Returns the Namespace enumeration constant given the prefix of the JNDI name.
     *
     * @param jndiName JNDI name.
     * @return enumeration value, of if non-matching then NULL.
     */
    static Namespace of(String jndiName) {
        if (jndiName.startsWith("java:"))
            if (jndiName.regionMatches(5, "app/", 0, 4))
                return APP;
            else if (jndiName.regionMatches(5, "module/", 0, 7))
                return MODULE;
            else if (jndiName.regionMatches(5, "comp/", 0, 5))
                return COMP;
            else if (jndiName.regionMatches(5, "global/", 0, 7))
                return GLOBAL;

        return null;
    }

    /**
     * Returns a textual representation of the namespace prefix.
     *
     * @return a textual representation of the namespace prefix.
     */
    @Override
    public String toString() {
        switch (this) {
            case GLOBAL:
                return "java:global";
            case APP:
                return "java:app";
            case MODULE:
                return "java:module";
            case COMP:
                return "java:comp";
            default:
                throw new IllegalStateException(); // unreachable
        }
    }
}