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

package com.ibm.ws.security.wim.scim20.model;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.model.Meta;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class MetaImplTest {

    @Test
    public void serialize() throws Exception {

        MetaImpl meta = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"resourceType\":\"resourceType\",");
        expected.append("\"created\":0,");
        expected.append("\"lastModified\":0,");
        expected.append("\"location\":\"location\",");
        expected.append("\"version\":\"version\"");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(meta);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        Meta deserialized = SCIMUtil.deserialize(serialized, Meta.class);
        assertEquals(meta, deserialized);
    }

    public static MetaImpl getTestInstance() {
        MetaImpl meta = new MetaImpl();
        meta.setCreated(new Date(0));
        meta.setLastModified(new Date(0));
        meta.setLocation("location");
        meta.setResourceType("resourceType");
        meta.setVersion("version");
        return meta;
    }
}
