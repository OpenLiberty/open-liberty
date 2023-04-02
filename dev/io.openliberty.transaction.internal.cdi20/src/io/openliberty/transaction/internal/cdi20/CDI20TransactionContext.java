/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package io.openliberty.transaction.internal.cdi20;

import java.lang.annotation.Annotation;

import javax.enterprise.inject.spi.BeanManager;

import com.ibm.tx.jta.cdi.AbstractTransactionContext;

/**
 * Implementation for the TransactionScoped annotation.
 */
public class CDI20TransactionContext extends AbstractTransactionContext {

    public CDI20TransactionContext(BeanManager beanManager) {
        super(beanManager);
    }

    @Override
    public void fireEvent(BeanManager beanManager, Object obj, Annotation... annotations) {
        beanManager.getEvent().select(annotations).fire(obj);
    }

}
