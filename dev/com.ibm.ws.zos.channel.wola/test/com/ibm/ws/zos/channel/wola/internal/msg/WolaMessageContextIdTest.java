/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal.msg;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 */
public class WolaMessageContextIdTest {

    @Test
    public void verifyForNativeValue() {
        for (WolaMessageContextId contextId : WolaMessageContextId.class.getEnumConstants()) {
            assertEquals(contextId, WolaMessageContextId.forNativeValue(contextId.nativeValue));
        }
    }
}
