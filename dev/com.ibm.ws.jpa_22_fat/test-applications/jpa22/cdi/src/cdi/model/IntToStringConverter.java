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

package cdi.model;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Named
@Converter
public class IntToStringConverter implements AttributeConverter<Integer, String> {
    private LoggingService logger;

    public IntToStringConverter() {
        // Default logger to avoid NPEs if injection fails.
        logger = new LoggingService() {

            private final List<String> _messages = Arrays.asList(new String[] { "injection failed" });

            @Override
            public void log(String s) {
                System.out.println("Default logger - injection failed: " + s);
            }

            @Override
            public List<String> getAndClearMessages() {
                return _messages;
            }

        };
        System.out.println("IntToStringConverter <init>");
    }

    @Inject
    public void setLoggingService(LoggingService ls) {
        logger = ls;
        logger.log(msg("injection"));
    }

    private static String msg(String s) {
        return "IntToStringConverter-" + s;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.AttributeConverter#convertToDatabaseColumn(java.lang.Object)
     */
    @Override
    public String convertToDatabaseColumn(Integer i) {
        logger.log(msg("convertToDatabaseColumn:" + i));

        if (i == null) {
            return null;
        } else {
            return i.toString();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.AttributeConverter#convertToEntityAttribute(java.lang.Object)
     */
    @Override
    public Integer convertToEntityAttribute(String s) {
        logger.log(msg("convertToEntityAttribute:" + s));

        if (s == null) {
            return null;
        } else {
            return new Integer(s);
        }
    }

}
