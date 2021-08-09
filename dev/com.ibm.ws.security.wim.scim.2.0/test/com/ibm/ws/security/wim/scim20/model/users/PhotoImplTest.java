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

import com.ibm.websphere.security.wim.scim20.model.users.Photo;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class PhotoImplTest {

    @Test
    public void serialize() throws Exception {
        PhotoImpl photo = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"value\":\"value\",");
        expected.append("\"display\":\"display\",");
        expected.append("\"type\":\"type\",");
        expected.append("\"primary\":true");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(photo);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        Photo deserialized = SCIMUtil.deserialize(serialized, Photo.class);
        assertEquals(photo, deserialized);
    }

    public static PhotoImpl getTestInstance() {
        PhotoImpl photo = new PhotoImpl();
        photo.setDisplay("display");
        photo.setPrimary(true);
        photo.setType("type");
        photo.setValue("value");
        return photo;
    }
}
