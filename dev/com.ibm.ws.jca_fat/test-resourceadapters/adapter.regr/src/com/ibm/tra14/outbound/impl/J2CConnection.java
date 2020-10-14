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

package com.ibm.tra14.outbound.impl;

import com.ibm.tra14.outbound.base.ConnectionBase;
import com.ibm.tra14.outbound.base.ConnectionRequestInfoBase;
import com.ibm.tra14.outbound.base.ManagedConnectionBase;

public class J2CConnection extends ConnectionBase {

    public J2CConnection() {
        super();
    }

    public J2CConnection(ManagedConnectionBase mc, ConnectionRequestInfoBase cxReq) {
        super(mc, cxReq);
    }
}