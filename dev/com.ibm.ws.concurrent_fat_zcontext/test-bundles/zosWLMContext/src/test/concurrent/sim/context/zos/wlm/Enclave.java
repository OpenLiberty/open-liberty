/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.concurrent.sim.context.zos.wlm;

/**
 * This a fake context that we made up for testing purposes.
 */
public class Enclave {
    private static ThreadLocal<String> context = new ThreadLocal<String>();

    public static void clear() {
        context.remove();
    }

    public static String getTransactionClass() {
        return context.get();
    }

    public static void setTransactionClass(String txClass) {
        context.set(txClass);
    }
}