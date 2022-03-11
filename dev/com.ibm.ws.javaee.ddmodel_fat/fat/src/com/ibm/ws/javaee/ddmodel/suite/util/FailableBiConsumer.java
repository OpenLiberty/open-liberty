/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.suite.util;

// Note: Parameterizing on exception types does not work.
//       Catch blocks cannot use parameterized exception types.

@FunctionalInterface
public interface FailableBiConsumer<T1, T2> {
    void accept(T1 t1, T2 t2) throws Exception;
}    
