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
package web;

import java.util.concurrent.Callable;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class DerbyRABean {
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public <T> T runInNewGlobalTran(Callable<T> callable) {
        try {
            return callable.call();
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new EJBException(x);
        }
    }
}
