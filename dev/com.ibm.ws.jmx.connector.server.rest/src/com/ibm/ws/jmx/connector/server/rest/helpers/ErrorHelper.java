/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerJsonException;

/**
 * General exception and error handling
 */
public class ErrorHelper {
    /**
     * This method will recycle the given converter.
     */
    public static RESTHandlerJsonException createRESTHandlerJsonException(Throwable e, JSONConverter converter, int status) {
        try {
            //See if we need to fetch a converter
            if (converter == null) {
                converter = JSONConverter.getConverter();
            }
            //Create a new OutputStream to avoid any corrupted data
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            //Write the exception inside the output stream
            converter.writeThrowable(os, e);

            //Get the message from the output stream
            String exceptionMessage = os.toString("UTF-8");

            //return a Web exception with the new response
            return new RESTHandlerJsonException(exceptionMessage, status, true);

        } catch (IOException innerException) {
            //Since we got an exception while converting the error just write the actual exception text with an internal error code. This should never
            //happen because our JSONErrorOutputStream shouldn't ever throw an IOException
            return new RESTHandlerJsonException(e.getMessage(), status, true);

        } finally {
            JSONConverter.returnConverter(converter);
        }
    }
}
