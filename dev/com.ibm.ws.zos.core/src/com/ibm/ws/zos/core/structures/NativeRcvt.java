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

/**
 * Provides access to the System RCVT Control Block
 */
public interface NativeRcvt {

    /**
     * Get the RCVTID field of the RCVTID
     *
     * @return The RCVTID field
     */
    public String getRCVTID();

}
