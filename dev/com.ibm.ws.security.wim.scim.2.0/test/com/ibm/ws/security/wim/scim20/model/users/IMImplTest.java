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

package com.ibm.ws.security.wim.scim20.model.users;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.model.users.IM;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class IMImplTest {

    @Test
    public void serialize() throws Exception {

        IMImpl im = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"value\":\"value\",");
        expected.append("\"display\":\"display\",");
        expected.append("\"type\":\"type\",");
        expected.append("\"primary\":false");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(im);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        IM deserialized = SCIMUtil.deserialize(serialized, IM.class);
        assertEquals(im, deserialized);
    }

    public static IMImpl getTestInstance() {

        IMImpl im = new IMImpl();
        im.setDisplay("display");
        im.setPrimary(false);
        im.setType("type");
        im.setValue("value");
        return im;
    }
}
