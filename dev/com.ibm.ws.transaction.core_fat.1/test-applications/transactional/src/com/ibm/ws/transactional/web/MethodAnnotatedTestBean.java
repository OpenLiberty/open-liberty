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
package com.ibm.ws.transactional.web;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

public class MethodAnnotatedTestBean extends POJO {

    @Override
    @Transactional(value = TxType.MANDATORY,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void basicMandatory(TestContext tc, Throwable t) throws Throwable {
        super.basicMandatory(tc, t);
    }

    @Override
    @Transactional(value = TxType.MANDATORY,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void mandatoryWithUTBegin(TestContext tc, Throwable t) throws Throwable {
        super.mandatoryWithUTBegin(tc, t);;
    }

    @Override
    @Transactional(value = TxType.MANDATORY,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void mandatoryWithUTCommit(TestContext tc, Throwable t) throws Throwable {
        super.mandatoryWithUTCommit(tc, t);;
    }

    @Override
    @Transactional(value = TxType.MANDATORY,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void mandatoryWithUTGetStatus(TestContext tc, Throwable t) throws Throwable {
        super.mandatoryWithUTGetStatus(tc, t);;
    }

    @Override
    @Transactional(value = TxType.MANDATORY,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void mandatoryWithUTRollback(TestContext tc, Throwable t) throws Throwable {
        super.mandatoryWithUTRollback(tc, t);;
    }

    @Override
    @Transactional(value = TxType.MANDATORY,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void mandatoryWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable {
        super.mandatoryWithUTSetRollbackOnly(tc, t);;
    }

    @Override
    @Transactional(value = TxType.MANDATORY,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void mandatoryWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable {
        super.mandatoryWithUTSetTransactionTimeout(tc, t);;
    }

    @Override
    @Transactional(value = TxType.MANDATORY,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void mandatoryWithRunUnderUOW(TestContext tc, Throwable t) throws Throwable {
        super.mandatoryWithRunUnderUOW(tc, t);;
    }

    @Override
    @Transactional(value = TxType.MANDATORY)
    public void basicMandatoryNoLists(TestContext tc, Throwable t) throws Throwable {
        super.basicMandatoryNoLists(tc, t);
    }

    @Override
    @Transactional(value = TxType.NEVER,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void basicNever(TestContext tc, Throwable t) throws Throwable {
        super.basicNever(tc, t);
    }

    @Override
    @Transactional(value = TxType.NEVER,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void neverWithUTBegin(TestContext tc, Throwable t) throws Throwable {
        super.neverWithUTBegin(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NEVER,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void neverWithUTCommit(TestContext tc, Throwable t) throws Throwable {
        super.neverWithUTCommit(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NEVER,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void neverWithUTGetStatus(TestContext tc, Throwable t) throws Throwable {
        super.neverWithUTGetStatus(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NEVER,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void neverWithUTRollback(TestContext tc, Throwable t) throws Throwable {
        super.neverWithUTRollback(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NEVER,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void neverWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable {
        super.neverWithUTSetRollbackOnly(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NEVER,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void neverWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable {
        super.neverWithUTSetTransactionTimeout(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NEVER,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void neverWithRunUnderUOW(TestContext tc, Throwable t) throws Throwable {
        super.neverWithRunUnderUOW(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NEVER)
    public void basicNeverNoLists(TestContext tc, Throwable t) throws Throwable {
        super.basicNeverNoLists(tc, t);
    }

    @Override
    @Transactional(value = TxType.NOT_SUPPORTED,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void basicNotSupported(TestContext tc, Throwable t) throws Throwable {
        super.basicNotSupported(tc, t);
    }

    @Override
    @Transactional(value = TxType.NOT_SUPPORTED,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void notSupportedWithUTBegin(TestContext tc, Throwable t) throws Throwable {
        super.notSupportedWithUTBegin(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NOT_SUPPORTED,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void notSupportedWithUTCommit(TestContext tc, Throwable t) throws Throwable {
        super.notSupportedWithUTCommit(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NOT_SUPPORTED,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void notSupportedWithUTGetStatus(TestContext tc, Throwable t) throws Throwable {
        super.notSupportedWithUTGetStatus(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NOT_SUPPORTED,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void notSupportedWithUTRollback(TestContext tc, Throwable t) throws Throwable {
        super.notSupportedWithUTRollback(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NOT_SUPPORTED,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void notSupportedWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable {
        super.notSupportedWithUTSetRollbackOnly(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NOT_SUPPORTED,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void notSupportedWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable {
        super.notSupportedWithUTSetTransactionTimeout(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NOT_SUPPORTED,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void notSupportedWithRunUnderUOW(TestContext tc, Throwable t) throws Throwable {
        super.notSupportedWithRunUnderUOW(tc, t);;
    }

    @Override
    @Transactional(value = TxType.NOT_SUPPORTED)
    public void basicNotSupportedNoLists(TestContext tc, Throwable t) throws Throwable {
        super.basicNotSupportedNoLists(tc, t);
    }

    @Override
    @Transactional(rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void basicRequired(TestContext tc, Throwable t) throws Throwable {
        super.basicRequired(tc, t);
    }

    @Override
    @Transactional(rollbackOn = { Exception.class, },
                   dontRollbackOn = { IllegalArgumentException.class })
    public void basicRequiredAlternativeExceptions(TestContext tc, Throwable t) throws Throwable {
        super.basicRequiredAlternativeExceptions(tc, t);
    }

    @Override
    @Transactional(rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiredWithUTBegin(TestContext tc, Throwable t) throws Throwable {
        super.requiredWithUTBegin(tc, t);;
    }

    @Override
    @Transactional(rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiredWithUTCommit(TestContext tc, Throwable t) throws Throwable {
        super.requiredWithUTCommit(tc, t);;
    }

    @Override
    @Transactional(rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiredWithUTGetStatus(TestContext tc, Throwable t) throws Throwable {
        super.requiredWithUTGetStatus(tc, t);;
    }

    @Override
    @Transactional(rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiredWithUTRollback(TestContext tc, Throwable t) throws Throwable {
        super.requiredWithUTRollback(tc, t);;
    }

    @Override
    @Transactional(rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiredWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable {
        super.requiredWithUTSetRollbackOnly(tc, t);;
    }

    @Override
    @Transactional(rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiredWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable {
        super.requiredWithUTSetTransactionTimeout(tc, t);;
    }

    @Override
    @Transactional(rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiredWithRunUnderUOW(TestContext tc, Throwable t) throws Throwable {
        super.requiredWithRunUnderUOW(tc, t);;
    }

    @Override
    @Transactional
    public void basicRequiredNoLists(TestContext tc, Throwable t) throws Throwable {
        super.basicRequiredNoLists(tc, t);
    }

    @Override
    @Transactional(value = TxType.REQUIRES_NEW,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void basicRequiresNew(TestContext tc, Throwable t) throws Throwable {
        super.basicRequiresNew(tc, t);
    }

    @Override
    @Transactional(value = TxType.REQUIRES_NEW,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiresNewWithUTBegin(TestContext tc, Throwable t) throws Throwable {
        super.requiresNewWithUTBegin(tc, t);;
    }

    @Override
    @Transactional(value = TxType.REQUIRES_NEW,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiresNewWithUTCommit(TestContext tc, Throwable t) throws Throwable {
        super.requiresNewWithUTCommit(tc, t);;
    }

    @Override
    @Transactional(value = TxType.REQUIRES_NEW,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiresNewWithUTGetStatus(TestContext tc, Throwable t) throws Throwable {
        super.requiresNewWithUTGetStatus(tc, t);;
    }

    @Override
    @Transactional(value = TxType.REQUIRES_NEW,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiresNewWithUTRollback(TestContext tc, Throwable t) throws Throwable {
        super.requiresNewWithUTRollback(tc, t);;
    }

    @Override
    @Transactional(value = TxType.REQUIRES_NEW,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiresNewWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable {
        super.requiresNewWithUTSetRollbackOnly(tc, t);;
    }

    @Override
    @Transactional(value = TxType.REQUIRES_NEW,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiresNewWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable {
        super.requiresNewWithUTSetTransactionTimeout(tc, t);;
    }

    @Override
    @Transactional(value = TxType.REQUIRES_NEW,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void requiresNewWithRunUnderUOW(TestContext tc, Throwable t) throws Throwable {
        super.requiresNewWithRunUnderUOW(tc, t);;
    }

    @Override
    @Transactional(value = TxType.REQUIRES_NEW)
    public void basicRequiresNewNoLists(TestContext tc, Throwable t) throws Throwable {
        super.basicRequiresNewNoLists(tc, t);
    }

    @Override
    @Transactional(value = TxType.SUPPORTS,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void basicSupports(TestContext tc, Throwable t) throws Throwable {
        super.basicSupports(tc, t);
    }

    @Override
    @Transactional(value = TxType.SUPPORTS,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void supportsWithUTBegin(TestContext tc, Throwable t) throws Throwable {
        super.supportsWithUTBegin(tc, t);
    }

    @Override
    @Transactional(value = TxType.SUPPORTS,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void supportsWithUTCommit(TestContext tc, Throwable t) throws Throwable {
        super.supportsWithUTCommit(tc, t);;
    }

    @Override
    @Transactional(value = TxType.SUPPORTS,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void supportsWithUTGetStatus(TestContext tc, Throwable t) throws Throwable {
        super.supportsWithUTGetStatus(tc, t);;
    }

    @Override
    @Transactional(value = TxType.SUPPORTS,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void supportsWithUTRollback(TestContext tc, Throwable t) throws Throwable {
        super.supportsWithUTRollback(tc, t);;
    }

    @Override
    @Transactional(value = TxType.SUPPORTS,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void supportsWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable {
        super.supportsWithUTSetRollbackOnly(tc, t);;
    }

    @Override
    @Transactional(value = TxType.SUPPORTS,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void supportsWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable {
        super.supportsWithUTSetTransactionTimeout(tc, t);;
    }

    @Override
    @Transactional(value = TxType.SUPPORTS,
                   rollbackOn = { IllegalAccessException.class, InterruptedException.class },
                   dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
    public void supportsWithRunUnderUOW(TestContext tc, Throwable t) throws Throwable {
        super.supportsWithRunUnderUOW(tc, t);
    }

    @Override
    @Transactional(value = TxType.SUPPORTS)
    public void basicSupportsNoLists(TestContext tc, Throwable t) throws Throwable {
        super.basicSupportsNoLists(tc, t);
    }
}