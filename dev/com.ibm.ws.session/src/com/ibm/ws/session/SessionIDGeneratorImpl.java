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
package com.ibm.ws.session;

import com.ibm.ws.session.utils.IDGeneratorImpl;

public class SessionIDGeneratorImpl extends IDGeneratorImpl {

    public SessionIDGeneratorImpl() {
        super();
    }

    public SessionIDGeneratorImpl(int sessionIDLength) {
        super(sessionIDLength);
    }

    public String getSessionID() {
        return getID();
    }

}