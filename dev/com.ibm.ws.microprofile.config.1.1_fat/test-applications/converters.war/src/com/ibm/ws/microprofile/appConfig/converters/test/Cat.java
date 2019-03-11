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
package com.ibm.ws.microprofile.appConfig.converters.test;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.log.Log;

/**
 *
 */
public class Cat {

    public String sound;

    // prevent default construction
    @SuppressWarnings("unused")
    private Cat() {}

    /**
     * @param value
     * @throws IOException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public Cat(String stringified) throws IOException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

        Pattern p = Pattern.compile(("(\\w*)=(\\w*)"));
        String[] settings = stringified.split(",");

        for (int i = 0; i < settings.length; i++) {
            Matcher m = p.matcher(settings[i]);
            while (m.find()) {
                String propertyName = m.group(1);
                String propertyValue = m.group(2);
                try {
                    this.getClass().getField(propertyName).set(this, propertyValue);
                } catch (NoSuchFieldException e) {
                    Log.error(this.getClass(), "Data read from config source has invalid Field name: " + propertyName, e);
                    throw e;
                }
            }
        }

    }
}
