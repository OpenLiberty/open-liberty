/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.basic;

import java.util.List;
import java.util.concurrent.Future;

public interface BusinessRemote {
    void test();

    void testAppException() throws TestAppException;

    void testSystemException();

    void testTransactionException();

    List<?> testWriteValue(List<?> list);

    void setupAsyncVoid();

    void testAsyncVoid();

    long awaitAsyncVoidThreadId();

    void setupAsyncFuture(int asyncCount);

    Future<Long> testAsyncFuture();

    void awaitAsyncFuture();
}
