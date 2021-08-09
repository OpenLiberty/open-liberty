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
package com.ibm.ws.ejbcontainer.async.fat.farRemote.ejb;

import java.util.concurrent.Future;

/**
 * Remote interface for Container Managed Transaction Singleton
 * Session bean that returns results in a Future object.
 **/
public interface ResultsSingletonRemote {
    public void test_SynchTimeout(int waitTime);

    public Future<String> test_fireAndReturnResults();

    public Future<String> test_fireAndReturnResults(int watiTime);

    public Future<String> test_fireAndReturnResults_null();
}