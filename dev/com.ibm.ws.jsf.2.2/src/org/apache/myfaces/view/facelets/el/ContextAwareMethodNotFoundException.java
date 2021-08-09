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

import javax.el.MethodNotFoundException;
import javax.faces.view.Location;

/**
 * Implementation of types {@link MethodNotFoundException}, {@link ContextAware}
 * and {@link javax.faces.FacesWrapper}
 *
 * @author martinkoci
 *
 * @see ContextAware
 */
public class ContextAwareMethodNotFoundException extends MethodNotFoundException implements ContextAwareExceptionWrapper
{

    /**
     *
     */
    private static final long serialVersionUID = -8172862923048615707L;

    private ContextAwareExceptionWrapper _delegate;
    //private Throwable _wrappedException;
    //private String _localizedMessage;

    public ContextAwareMethodNotFoundException(Location location,
                                               String expressionString, String qName, Throwable wrapped)
    {
        super(wrapped);
        //super(wrapped.getMessage());
        //_localizedMessage = wrapped.getLocalizedMessage();
        //_wrappedException = wrapped;
        _delegate = new DefaultContextAwareELException(location, expressionString, qName, wrapped);
    }

    public Throwable getWrapped()
    {
        return _delegate.getWrapped();
    }

    public Location getLocation()
    {
        return _delegate.getLocation();
    }

    public String getExpressionString()
    {
        return _delegate.getExpressionString();
    }

    public String getQName()
    {
        return _delegate.getQName();
    }

    /*
    @Override
    public String getLocalizedMessage()
    {
        return _localizedMessage;
    }

    @Override
    public Throwable getCause()
    {
        return _wrappedException.getCause();
    }

    @Override
    public synchronized Throwable initCause(Throwable cause)
    {
        return _wrappedException.initCause(cause);
    }*/
}
