/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.scim20.model.serviceprovider;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class SortImplTest {
    @Test
    public void serialize() throws Exception {
        SortImpl sort = new SortImpl();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"supported\":true");
        expected.append("}");

        /*
         * Serialize.
         */
        String serializedResponse = SCIMUtil.serialize(sort);
        assertEquals(expected.toString(), serializedResponse);

        /*
         * No need to test deserialization as this is a response only. The
         * customer would never send an instance into us.
         */
    }
}
