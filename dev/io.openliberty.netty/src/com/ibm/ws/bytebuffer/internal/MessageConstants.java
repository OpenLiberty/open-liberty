/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bytebuffer.internal;

/**
 * Constants used by the ByteBuffer package for user-seen messages.
 */
public interface MessageConstants {
    // -------------------------------------------------------------------------
    // Public Constants
    // -------------------------------------------------------------------------

    /** NLS message bundle */
    String WSBB_BUNDLE = "com.ibm.ws.bytebuffer.internal.resources.ByteBufferMessages";
    /** RAS group name */
    String WSBB_TRACE_NAME = "WsByteBuffer";

    // -------------------------------------------------------------------------
    // NLS Messages
    // -------------------------------------------------------------------------

    /** Reference to the NLS message for an unrecognized custom property */
    String UNRECOGNIZED_CUSTOM_PROPERTY = "UNRECOGNIZED_CUSTOM_PROPERTY";
    /** Reference to the NLS message for an invalid property */
    String NOT_VALID_CUSTOM_PROPERTY = "NOT_VALID_CUSTOM_PROPERTY";
    /** Reference to the NLS message for an invalid numerical property */
    String CONFIG_VALUE_NUMBER_EXCEPTION = "CONFIG_VALUE_NUMBER_EXCEPTION";
    /** Reference to the NLS message for a pool config mismatch */
    String POOL_MISMATCH = "POOL_MISMATCH";

}
