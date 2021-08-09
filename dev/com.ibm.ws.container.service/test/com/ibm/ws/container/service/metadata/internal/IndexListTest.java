/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.metadata.internal;

import org.junit.Assert;
import org.junit.Test;

public class IndexListTest {
    @Test
    public void testReserve() {
        IndexList list = new IndexList();
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(i, list.reserve());
        }
    }

    @Test
    public void testUnreserveForward() {
        IndexList list = new IndexList();
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(i, list.reserve());
        }

        for (int i = 0; i < 10; i++) {
            list.unreserve(i);
        }

        for (int i = 10; --i >= 0;) {
            Assert.assertEquals(i, list.reserve());
        }
        for (int i = 10; i < 20; i++) {
            Assert.assertEquals(i, list.reserve());
        }
    }

    @Test
    public void testUnreserveBackward() {
        IndexList list = new IndexList();
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(i, list.reserve());
        }

        for (int i = 10; --i >= 0;) {
            list.unreserve(i);
        }

        for (int i = 0; i < 20; i++) {
            Assert.assertEquals(i, list.reserve());
        }
    }
}
