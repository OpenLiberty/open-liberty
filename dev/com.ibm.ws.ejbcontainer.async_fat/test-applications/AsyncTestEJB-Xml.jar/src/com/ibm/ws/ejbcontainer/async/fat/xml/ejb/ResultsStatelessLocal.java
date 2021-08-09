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
package com.ibm.ws.ejbcontainer.async.fat.xml.ejb;

import java.util.concurrent.Future;

/**
 * Local interface for Container Managed Transaction Stateless
 * Session bean that returns results in a Future object.
 **/
public interface ResultsStatelessLocal {
    public Future<String> test_fireAndReturnResults();

    public Future<String> test_fireAndReturnResults_null();

    public Future<String> test_fireAndReturnResults_await();
}