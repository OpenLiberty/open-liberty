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
package com.ibm.ws.zos.core.utils;

/**
 * A wrapper to call the z/OS SMF service
 */
public interface Smf {

    /**
     * Write a z/OS SMF record type 120, subtype 11
     *
     * @param data The record data
     * @return return code from SMF
     */
    public int smfRecordT120S11Write(byte[] data);

    /**
     * @param data
     * @return
     */
    int smfRecordT120S12Write(byte[] data);

}
