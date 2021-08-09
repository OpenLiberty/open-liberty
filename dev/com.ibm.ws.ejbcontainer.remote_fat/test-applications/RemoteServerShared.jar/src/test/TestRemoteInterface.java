/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test;

import java.util.concurrent.Future;

/**
 * EJB remote interface with a single period (.) in the package name
 * and methods for testing remote asynchronous methods and remote bean state.
 */
public interface TestRemoteInterface {

    /**
     * Simple method that returns the bean name
     */
    String getBeanName();

    /**
     * Increments bean state, returning new value.
     */
    int increment(int value);

    /**
     * Verifies the passed remote bean has the same bean name.
     */
    boolean verifyRemoteBean(TestRemoteInterface remoteBean);

    /**
     * Asynchronous methods that returns the bean name.
     */
    Future<String> asynchMethodReturn();

}
