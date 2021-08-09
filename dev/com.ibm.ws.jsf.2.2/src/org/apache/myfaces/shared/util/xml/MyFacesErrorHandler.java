/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.shared.util.xml;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Convenient error handler for xml sax parsing.
 */
public class MyFacesErrorHandler
        implements ErrorHandler
{
    private Logger _log;

    public MyFacesErrorHandler(Logger log)
    {
        _log = log;
    }

    public void warning(SAXParseException exception)
    {
        if (_log.isLoggable(Level.WARNING))
        {
            _log.log(Level.WARNING, getMessage(exception), exception);
        }
    }

    public void error(SAXParseException exception)
    {
        _log.log(Level.SEVERE, getMessage(exception), exception);
    }

    public void fatalError(SAXParseException exception)
    {
        _log.log(Level.SEVERE, getMessage(exception), exception);
    }

    private String getMessage(SAXParseException exception)
    {
        StringBuilder buf = new StringBuilder();
        buf.append("SAXParseException at");
        buf.append(" URI=");
        buf.append(exception.getSystemId());
        buf.append(" Line=");
        buf.append(exception.getLineNumber());
        buf.append(" Column=");
        buf.append(exception.getColumnNumber());
        return buf.toString();
    }

}
