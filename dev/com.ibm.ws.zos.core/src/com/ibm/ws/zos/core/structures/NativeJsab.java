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
package com.ibm.ws.zos.core.structures;

import java.nio.ByteBuffer;

/**
 * Access the z/OS JSAB
 */
public interface NativeJsab {
    /**
     * Get a {@code DirectByteBuffer} that maps the z/OS JSAB
     */
    public ByteBuffer mapMyJsab();

    /**
     * Get the JSAB Jobname
     *
     * @return The jobname name, in EBCDIC
     */
    public byte[] getJSABJBNM();

    /**
     * Get the jobid
     *
     * @return The jobid in EBCDIC
     */
    public byte[] getJSABJBID();
}
