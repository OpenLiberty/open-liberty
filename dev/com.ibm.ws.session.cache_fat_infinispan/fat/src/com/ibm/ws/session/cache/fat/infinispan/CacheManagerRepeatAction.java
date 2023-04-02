/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.session.cache.fat.infinispan;

import componenttest.rules.repeater.RepeatTestAction;

/**
 * Repeat test action to repeat the tests using the cacheManagerRef configuration attribute.
 */
public class CacheManagerRepeatAction implements RepeatTestAction {
    public static final String ID = "CacheManager";

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setup() throws Exception {
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String toString() {
        return ID;
    }
}
