/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package concurrent.cdi.ejb;

import java.util.concurrent.Callable;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

@Local(Invoker.class)
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class InvokerEJB implements Invoker {

    @Override
    public <T> T runInEJB(Callable<T> testCode) throws Exception {
        return testCode.call();
    }

}
