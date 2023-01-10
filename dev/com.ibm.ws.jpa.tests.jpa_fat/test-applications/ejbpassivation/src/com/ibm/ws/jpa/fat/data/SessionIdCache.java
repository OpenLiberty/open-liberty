/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fat.data;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public class SessionIdCache {
    public static final CopyOnWriteArrayList<String> sessionList = new CopyOnWriteArrayList<String>();

    public static final CopyOnWriteArrayList<String> activateList = new CopyOnWriteArrayList<String>();
    public static final CopyOnWriteArrayList<String> passivateList = new CopyOnWriteArrayList<String>();

    public static void clearAll() {
        sessionList.clear();
        activateList.clear();
        passivateList.clear();
    }

    public static void removeActivatePassivate(String id) {
        activateList.remove(id);
        passivateList.remove(id);
    }
}
