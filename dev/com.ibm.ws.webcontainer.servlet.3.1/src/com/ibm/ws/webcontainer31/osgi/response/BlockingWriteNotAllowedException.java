/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.webcontainer31.osgi.response;

import java.io.IOException;

/**
 * This custom exception will be thrown when non-blocking i/o is started but an blocking write is attempted.
 */
public class BlockingWriteNotAllowedException extends IOException {

    /**
     * @param formatMessage
     */
    public BlockingWriteNotAllowedException(String formatMessage) {
        super(formatMessage);
    }

    /**  */
    private static final long serialVersionUID = 1L;

}
