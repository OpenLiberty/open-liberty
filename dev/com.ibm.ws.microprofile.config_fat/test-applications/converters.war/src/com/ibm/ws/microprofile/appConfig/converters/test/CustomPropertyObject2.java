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
public class CustomPropertyObject2 {

    String value1;
    String value2;

    // prevent default construction
    @SuppressWarnings("unused")
    private CustomPropertyObject2() {

    }

    /**
     * @param value
     * @throws IOException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public CustomPropertyObject2(String stringified) throws IOException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

        String[] settings = stringified.split(",");

        for (String setting : settings) {
            String[] keyValue = setting.split("=");
            String key = keyValue[0];
            String value = keyValue[1];
            if (key.equals("value1")) {
                value1 = value;
            }
            if (key.equals("value2")) {
                value2 = value;
            }
        }
    }

    /**
     * @param string
     * @param string2
     */
    public CustomPropertyObject2(String string1, String string2) {
        value1 = string1;
        value2 = string2;
    }

    /**
     * @param value
     * @return
     * @throws Exception
     */
    public static CustomPropertyObject2 create(String value) throws Exception {
        try {
            return new CustomPropertyObject2(value);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw e;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof CustomPropertyObject2))
            return false;
        CustomPropertyObject2 other = (CustomPropertyObject2) obj;
        if (!value1.equals(other.value1)) {
            return false;
        }
        return value2.equals(other.value2);
    }

}
