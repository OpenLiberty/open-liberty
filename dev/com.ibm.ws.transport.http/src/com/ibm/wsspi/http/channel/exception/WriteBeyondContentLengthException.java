/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel.exception;

import java.io.IOException;

public class WriteBeyondContentLengthException extends IOException {
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3258407331258708534L;

    public WriteBeyondContentLengthException() {
        super();
    }

    public WriteBeyondContentLengthException(String message) {
        super(message);
    }
}
