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

public interface INotSupported {
    public void basicNotSupported(TestContext tc, Throwable t) throws Throwable;

    public void notSupportedWithUTBegin(TestContext tc, Throwable t) throws Throwable;

    public void notSupportedWithUTCommit(TestContext tc, Throwable t) throws Throwable;

    public void notSupportedWithUTGetStatus(TestContext tc, Throwable t) throws Throwable;

    public void notSupportedWithUTRollback(TestContext tc, Throwable t) throws Throwable;

    public void notSupportedWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable;

    public void notSupportedWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable;

    public void notSupportedWithRunUnderUOW(TestContext tc, Throwable t) throws Throwable;

    public void basicNotSupportedNoLists(TestContext tc, Throwable t) throws Throwable;
}