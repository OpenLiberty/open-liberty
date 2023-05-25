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

public interface IMandatory {
    public void basicMandatory(TestContext tc, Throwable t) throws Throwable;

    public void mandatoryWithUTBegin(TestContext tc, Throwable t) throws Throwable;

    public void mandatoryWithUTCommit(TestContext tc, Throwable t) throws Throwable;

    public void mandatoryWithUTGetStatus(TestContext tc, Throwable t) throws Throwable;

    public void mandatoryWithUTRollback(TestContext tc, Throwable t) throws Throwable;

    public void mandatoryWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable;

    public void mandatoryWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable;

    public void mandatoryWithRunUnderUOW(TestContext tc, Throwable t) throws Throwable;

    public void basicMandatoryNoLists(TestContext tc, Throwable t) throws Throwable;
}