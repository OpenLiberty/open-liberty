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

package com.ibm.tra.ann;

import java.util.LinkedList;

import javax.naming.NamingException;
import javax.naming.Reference;

import com.ibm.tra.inbound.base.TRAAdminObject2;

@SuppressWarnings("serial")
public class AdminObjectAnnSuperClass3 implements TRAAdminObject2 {

    protected Reference _ref = null;
    @SuppressWarnings("unchecked")
    protected LinkedList _queue;
    protected String _className = this.getClass().getName();
    protected String _factoryName = "com.ibm.tra.inbound.impl.TRAObjectFactory";

    public AdminObjectAnnSuperClass3() {
        super();
    }

    @Override
    public Reference getReference() throws NamingException {
        if (_ref == null) {
            _ref = new Reference(this.getClass().getName(), _factoryName, null);
        }
        return _ref;
    }

    @Override
    public void deleteMsgs() {}

}
