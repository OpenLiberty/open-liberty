/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.impl;

import java.util.concurrent.atomic.AtomicBoolean;

public class QuiesceState {
    private static final AtomicBoolean quiesceInProgress = new AtomicBoolean(false);

    public static boolean isQuiesceInProgress(){
        return quiesceInProgress.get();
    }

    public static void startQuiesce(){
        quiesceInProgress.set(true);
    }

    public static void stopQuiesce(){
        quiesceInProgress.set(false);
    }
}