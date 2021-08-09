/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.ut.decorator;

import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.osgi.service.component.annotations.Component;

import com.ibm.tx.jta.embeddable.UserTransactionDecorator;

/**
 *
 */
@Component(service = UserTransactionDecorator.class)
public class UserTransactionDecoratorImpl implements UserTransactionDecorator {

    @Override
    public UserTransaction decorateUserTransaction(UserTransaction ut, boolean injection, Object injectionContext) throws NamingException {
        return new UserTransactionWrapper(ut);
    }

    public class UserTransactionWrapper implements UserTransaction {

        private final UserTransaction _ut;

        /**
         * @param ut
         */
        public UserTransactionWrapper(UserTransaction ut) {
            _ut = ut;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.transaction.UserTransaction#begin()
         */
        @Override
        public void begin() throws NotSupportedException, SystemException {

            System.out.println(this.getClass().getCanonicalName() + ".begin()");
            _ut.begin();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.transaction.UserTransaction#commit()
         */
        @Override
        public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {
            _ut.commit();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.transaction.UserTransaction#getStatus()
         */
        @Override
        public int getStatus() throws SystemException {
            return _ut.getStatus();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.transaction.UserTransaction#rollback()
         */
        @Override
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            _ut.rollback();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.transaction.UserTransaction#setRollbackOnly()
         */
        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {
            _ut.setRollbackOnly();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.transaction.UserTransaction#setTransactionTimeout(int)
         */
        @Override
        public void setTransactionTimeout(int arg0) throws SystemException {
            _ut.setTransactionTimeout(arg0);
        }
    }
}