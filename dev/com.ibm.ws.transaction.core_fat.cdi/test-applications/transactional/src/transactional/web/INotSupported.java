/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package transactional.web;

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