/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter;

/**
 * This class represents the connection handle states. A connection handle can
 * be in one of the following three states: active, inactive and closed.<p>
 */
public interface HandleStates {
    /** Connection handle state constants. */
    public static final int ACTIVE = 0,
                    INACTIVE = 1,
                    CLOSED = 2;

    /** List of Connection handle state names. */
    public static final String[] STATE_STRINGS = new String[] { "ACTIVE", "INACTIVE", "CLOSED" };

}
