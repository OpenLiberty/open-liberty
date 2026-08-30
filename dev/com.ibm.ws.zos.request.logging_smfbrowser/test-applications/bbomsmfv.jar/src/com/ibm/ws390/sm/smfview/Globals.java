/*******************************************************************************
 * Copyright (c) 2001 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.sm.smfview;

//------------------------------------------------------------------------------
/** Various global constants. */
public interface Globals {

    /** WebSphere for z/OS Smf record id. */
    public static final int CB390 = 120;

    /** Name length of containers. */
    public static final int NAME_LENGTH_CONTAINER = 256;

    /** Name length of classes. */
    public static final int NAME_LENGTH_CLASS = 256;

    /** Name length of methods. */
    public static final int NAME_LENGTH_METHOD = 256;

    /** Name length of hosts. */
    public static final int NAME_LENGTH_HOSTNAME = 64;

} // Globals