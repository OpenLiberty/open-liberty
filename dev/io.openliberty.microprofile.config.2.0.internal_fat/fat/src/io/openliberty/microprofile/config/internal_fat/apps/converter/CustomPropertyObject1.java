/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.converter;

import java.io.IOException;

/**
 *
 */
public class CustomPropertyObject1 {

    String setting1;
    String setting2;

    // prevent default construction
    @SuppressWarnings("unused")
    private CustomPropertyObject1() {

    }

    /**
     * @param value
     * @throws IOException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public CustomPropertyObject1(String stringified) throws IOException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

        String[] settings = stringified.split(",");

        for (String setting : settings) {
            String[] keyValue = setting.split("=");
            String key = keyValue[0];
            String value = keyValue[1];
            if (key.equals("setting1")) {
                setting1 = value;
            }
            if (key.equals("setting2")) {
                setting2 = value;
            }
        }
    }

    /**
     * @param string
     * @param string2
     */
    public CustomPropertyObject1(String string1, String string2) {
        setting1 = string1;
        setting2 = string2;
    }

    /**
     * @param value
     * @return
     * @throws Exception
     */
    public static CustomPropertyObject1 create(String value) throws Exception {
        try {
            return new CustomPropertyObject1(value);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw e;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof CustomPropertyObject1))
            return false;
        CustomPropertyObject1 other = (CustomPropertyObject1) obj;
        if (!setting1.equals(other.setting1)) {
            return false;
        }
        return setting2.equals(other.setting2);
    }

}
