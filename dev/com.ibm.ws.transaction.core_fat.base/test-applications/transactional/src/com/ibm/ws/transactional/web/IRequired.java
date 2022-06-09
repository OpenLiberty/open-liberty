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

public interface IRequired {
    public void basicRequired(TestContext tc, Throwable t) throws Throwable;

    public void requiredWithUTBegin(TestContext tc, Throwable t) throws Throwable;

    public void requiredWithUTCommit(TestContext tc, Throwable t) throws Throwable;

    public void requiredWithUTGetStatus(TestContext tc, Throwable t) throws Throwable;

    public void requiredWithUTRollback(TestContext tc, Throwable t) throws Throwable;

    public void requiredWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable;

    public void requiredWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable;

    public void requiredWithRunUnderUOW(TestContext tc, Throwable t) throws Throwable;

    public void basicRequiredNoLists(TestContext tc, Throwable t) throws Throwable;

    public void basicRequiredAlternativeExceptions(TestContext tc, Throwable t) throws Throwable;
}