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

public interface IRequiresNew {
    public void basicRequiresNew(TestContext tc, Throwable t) throws Throwable;

    public void requiresNewWithUTBegin(TestContext tc, Throwable t) throws Throwable;

    public void requiresNewWithUTCommit(TestContext tc, Throwable t) throws Throwable;

    public void requiresNewWithUTGetStatus(TestContext tc, Throwable t) throws Throwable;

    public void requiresNewWithUTRollback(TestContext tc, Throwable t) throws Throwable;

    public void requiresNewWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable;

    public void requiresNewWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable;

    public void requiresNewWithRunUnderUOW(TestContext tc, Throwable t) throws Throwable;

    public void basicRequiresNewNoLists(TestContext tc, Throwable t) throws Throwable;
}