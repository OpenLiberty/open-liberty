/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.event.internal;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReservedKeysTest {

    @Test
    public void testReserveSlot() {
        int startingIndex = ReservedKeys.reserveSlot("STARTING_POINT_FOR_TEST") + 1;
        List<String> names = Arrays.asList("foo", "bar", "baz");
        for (int i = 0; i < 5; i++) {
            for (String s : names) {
                assertEquals(startingIndex + names.indexOf(s), ReservedKeys.reserveSlot(s));
            }
        }
    }

}
