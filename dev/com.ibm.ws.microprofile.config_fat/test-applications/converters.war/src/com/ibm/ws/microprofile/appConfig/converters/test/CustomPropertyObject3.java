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
package com.ibm.ws.microprofile.appConfig.converters.test;

import java.io.IOException;

/**
 *
 */
public class CustomPropertyObject3 {

    String attr1;
    String attr2;

    // prevent default construction
    @SuppressWarnings("unused")
    private CustomPropertyObject3() {

    }

    /**
     * @param value
     * @throws IOException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public CustomPropertyObject3(String stringified) throws IOException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

        String[] settings = stringified.split(",");

        for (String setting : settings) {
            String[] keyValue = setting.split("=");
            String key = keyValue[0];
            String value = keyValue[1];
            if (key.equals("attr1")) {
                attr1 = value;
            }
            if (key.equals("attr2")) {
                attr2 = value;
            }
        }
    }

    /**
     * @param string
     * @param string2
     */
    public CustomPropertyObject3(String string1, String string2) {
        attr1 = string1;
        attr2 = string2;
    }

    /**
     * @param value
     * @return
     * @throws Exception
     */
    public static CustomPropertyObject3 create(String value) throws Exception {
        try {
            return new CustomPropertyObject3(value);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw e;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof CustomPropertyObject3))
            return false;
        CustomPropertyObject3 other = (CustomPropertyObject3) obj;
        if (!attr1.equals(other.attr1)) {
            return false;
        }
        return attr2.equals(other.attr2);
    }

}
