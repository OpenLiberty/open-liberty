/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api.attributes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;

public class AttributeTest {

    private final String SECRET = "s3cr3t";

    @Test
    public void maskClientSecret() {
        Attribute attribute = new Attribute("client_secret", OAuth20Constants.ATTRTYPE_PARAM_BODY, SECRET);
        String attributeAsString = attribute.toString();
        assertFalse("The client_secret value must not be in the attribute string.", attributeAsString.contains(SECRET));
        assertTrue("The client_secret value must be masked.", attributeAsString.contains("*****"));
    }

}
