/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.fafRemote.ejb;

/**
 * Remote interface for Basic Container Managed Transaction Stateless
 * Session bean.
 **/
public interface StatelessRemote {

    public void test_fireAndForget();

    public boolean asyncWorkWasDone();

    public void setAsyncWorkNotDone();

    public long getThreadId();

    public void awaitBarrier();

}
