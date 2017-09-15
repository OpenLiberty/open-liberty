/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.ra.inbound;

/**
 * Type-safe enumeration for read ahead optimization.
 */
public final class SibRaReadAhead {

    /**
     * Always perform read ahead.
     */
    public static final SibRaReadAhead ON = new SibRaReadAhead("ON");

    /**
     * Never perform read ahead.
     */
    public static final SibRaReadAhead OFF = new SibRaReadAhead("OFF");

    /**
     * Only perform read ahead for non-durable subscriptions and unshared
     * durable subscriptions.
     */
    public static final SibRaReadAhead DEFAULT = new SibRaReadAhead("DEFAULT");

    /**
     * String representation of the shareability.
     */
    private final String _name;

    /**
     * Private constructor to prevent instantiation.
     * 
     * @param name
     *            a string representation of the shareability
     */
    private SibRaReadAhead(final String name) {

        _name = name;

    }

    /**
     * Returns a string representation of this object.
     * 
     * @return a string representation of this object
     */
    public String toString() {

        return _name;

    }

}
