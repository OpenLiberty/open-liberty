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

public interface INever {
    public void basicNever(TestContext tc, Throwable t) throws Throwable;

    public void neverWithUTBegin(TestContext tc, Throwable t) throws Throwable;

    public void neverWithUTCommit(TestContext tc, Throwable t) throws Throwable;

    public void neverWithUTGetStatus(TestContext tc, Throwable t) throws Throwable;

    public void neverWithUTRollback(TestContext tc, Throwable t) throws Throwable;

    public void neverWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable;

    public void neverWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable;

    public void neverWithRunUnderUOW(TestContext tc, Throwable t) throws Throwable;

    public void basicNeverNoLists(TestContext tc, Throwable t) throws Throwable;
}