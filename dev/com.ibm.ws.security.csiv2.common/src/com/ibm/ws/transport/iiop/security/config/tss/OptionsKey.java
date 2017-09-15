/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.security.config.tss;

public final class OptionsKey {
    public final short supports;
    public final short requires;

    /**
     * @param supports
     * @param requires
     */
    public OptionsKey(short supports, short requires) {
        this.supports = supports;
        this.requires = requires;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {

        return supports + requires << 16;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OptionsKey other = (OptionsKey) obj;
        if (requires != other.requires)
            return false;
        if (supports != other.supports)
            return false;
        return true;
    }
}