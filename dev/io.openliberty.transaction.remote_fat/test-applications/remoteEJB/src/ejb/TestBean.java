/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package ejb;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.SystemException;

import shared.TestBeanRemote;
import shared.Util;

@Stateless(name = "TestBean")
@Remote(TestBeanRemote.class)
public class TestBean implements TestBeanRemote {

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getProperty(String var) {
        return System.getProperty(var);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public String mandatory() throws SystemException {
        return Util.tranID();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String required() throws SystemException {
        return Util.tranID();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String requiresNew() throws SystemException {
        return Util.tranID();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String supports() throws SystemException {
        return Util.tranID();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String notSupported() throws SystemException {
        return Util.tranID();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public String never() throws SystemException {
        return Util.tranID();
    }
}