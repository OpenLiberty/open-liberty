/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.transaction.internal.cdi12;

import java.lang.annotation.Annotation;

import javax.enterprise.inject.spi.BeanManager;

import com.ibm.tx.jta.cdi.AbstractTransactionContext;

/**
 * Implementation for the TransactionScoped annotation.
 */
public class CDI12TransactionContext extends AbstractTransactionContext {

    public CDI12TransactionContext(BeanManager beanManager) {
        super(beanManager);
    }

    @Override
    public void fireEvent(BeanManager beanManager, Object obj, Annotation... annotations) {
        beanManager.fireEvent(obj, annotations);
    }

}
