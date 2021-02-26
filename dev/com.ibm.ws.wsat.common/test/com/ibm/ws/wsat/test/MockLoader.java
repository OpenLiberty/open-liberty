/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.test;

/**
 * Mock class loader service. Doesn't need to do anything special for
 * unit tests.
 */
public class MockLoader extends MockProxy {
    public ClassLoader createThreadContextClassLoader(ClassLoader cl) {
        return cl;
    }
}
