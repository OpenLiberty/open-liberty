/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tra.ann;

import javax.jms.Message;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.spi.AdministeredObject;

import com.ibm.tra.inbound.base.TRAAdminObject;
import com.ibm.tra.inbound.base.TRAAdminObject1;

@AdministeredObject
public class AdminTestsAdministeredObjectFailure implements TRAAdminObject, TRAAdminObject1 {

    public Message getMsg() {
        return null;
    }

    public void putMsg(Message msg) {
    }

    public Reference getReference() throws NamingException {
        return null;
    }

    public int countMessages() {
        return 0;
    }

}
