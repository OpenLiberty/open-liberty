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

package ejbpassivation.data;

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
