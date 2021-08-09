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
package org.apache.myfaces.view.facelets.el;

import javax.faces.view.Location;

/**
 * Default implementation of {@link ContextAwareExceptionWrapper}, used for delegation
 *
 * @author martinkoci
 */
public class DefaultContextAwareELException implements ContextAwareExceptionWrapper
{

    private Location _location;

    private String _expressionString;

    private String _qName;

    private Throwable _wrapped;

    public DefaultContextAwareELException(Location location,
                                          String expressionString, String qName,
                                          Throwable wrapped)
    {
        _location = location;
        _expressionString = expressionString;
        _qName = qName;
        _wrapped = wrapped;
    }

    public Location getLocation()
    {
        return _location;
    }

    public String getExpressionString()
    {
        return _expressionString;
    }

    public String getQName()
    {
        return _qName;
    }

    public Throwable getWrapped()
    {
        return _wrapped;
    }
}
