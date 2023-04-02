/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.async.fat.farRemote.ejb;

import java.util.concurrent.Future;

/**
 * Remote interface for Container Managed Transaction Stateless
 * Session bean that returns results in a Future object.
 **/
public interface ResultsStatelessRemote {
    public Future<String> test_fireAndReturnResults();

    public Future<String> test_fireAndReturnResults_null();

    public Future<String> test_fireAndReturnResults_await();

    public Future<Boolean> test_fireAndReturnResults_classloader();
}