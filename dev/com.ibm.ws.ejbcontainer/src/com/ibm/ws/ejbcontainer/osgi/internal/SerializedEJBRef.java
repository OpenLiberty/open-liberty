/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.io.Serializable;

public class SerializedEJBRef implements Serializable {

    private static final long serialVersionUID = 5178157581955211788L;

    private final byte[] bytes;

    public SerializedEJBRef(byte[] serializedEJB) {
        bytes = serializedEJB;
    }

    public byte[] getSerializedEJB() {
        return bytes;
    }

}
