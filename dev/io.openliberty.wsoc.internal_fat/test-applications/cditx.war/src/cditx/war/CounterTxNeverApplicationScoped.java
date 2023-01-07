/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package cditx.war;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

/**
 * Counter for the application scope.
 */
@ApplicationScoped
@Named
public class CounterTxNeverApplicationScoped extends Counter {

    private static final long serialVersionUID = 1L;

    @Override
    @Transactional(value = TxType.NEVER)
    public int getNext() {
        System.out.println("CounterTxNeverApplicationScoped getNext() called");
        return super.getNext();
    }

    @PreDestroy
    public void destruct() {}

}