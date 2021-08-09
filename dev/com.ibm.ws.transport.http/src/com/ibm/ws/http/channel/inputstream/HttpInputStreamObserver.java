/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.inputstream;

/**
 * The need of these APIs from channel to webcontainer to support Multiread
 *
 */
public interface HttpInputStreamObserver {

    /*
     * Indicates that the input stream, obtained using either getInputStream
     * or getReader has been closed.
     */
    public void alertISOpen();

    /*
     * Indicates that the input stream, obtained using either getInputStream
     * or getReader has been closed.
     */
    public void alertISClose();

}
