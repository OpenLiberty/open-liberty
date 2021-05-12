/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.UserTransaction;

@ApplicationScoped
public class UserTranBean {
    @Resource
    private UserTransaction userTran;

    public UserTransaction getUserTransaction() {
        return userTran;
    }
}
