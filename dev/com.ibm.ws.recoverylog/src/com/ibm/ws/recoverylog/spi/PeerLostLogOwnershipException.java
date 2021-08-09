/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

/**
 * This exception indicates that an operation on a peer recovery log has failed due to
 * a home server having reclaimed the log.
 */
public class PeerLostLogOwnershipException extends InternalLogException {
    String reason = null;

    public PeerLostLogOwnershipException() {
        this(null);
    }

    public PeerLostLogOwnershipException(Throwable cause) {
        super(cause);
    }

    public PeerLostLogOwnershipException(String s, Throwable cause) {
        super(s, cause);
        reason = s;
    }

    @Override
    public String toString() {
        if (reason != null)
            return reason + ", " + super.toString();
        else
            return super.toString();
    }
}