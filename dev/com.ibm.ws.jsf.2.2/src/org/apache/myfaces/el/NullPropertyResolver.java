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
package org.apache.myfaces.el;

import javax.el.ELContext;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.PropertyNotFoundException;
import javax.faces.el.PropertyResolver;

/**
 * Default PropertyResolver.  See JSF 1.2 spec section 5.8.2
 *
 * @author Stan Silvert
 */
public final class NullPropertyResolver extends PropertyResolver
{

    /** Creates a new instance of NullPropertyResolver */
    public NullPropertyResolver()
    {
    }

    @Override
    public boolean isReadOnly(Object base, int index) throws EvaluationException, PropertyNotFoundException
    {
        elContext().setPropertyResolved(false);
        return false;
    }

    @Override
    public boolean isReadOnly(Object base, Object property) throws EvaluationException, PropertyNotFoundException
    {
        elContext().setPropertyResolved(false);
        return false;
    }

    @Override
    public Object getValue(Object base, int index) throws EvaluationException, PropertyNotFoundException
    {
        elContext().setPropertyResolved(false);
        return null;
    }

    @Override
    public Object getValue(Object base, Object property) throws EvaluationException, PropertyNotFoundException
    {
        elContext().setPropertyResolved(false);
        return null;
    }

    @Override
    public Class getType(Object base, int index) throws EvaluationException, PropertyNotFoundException
    {
        elContext().setPropertyResolved(false);
        return null;
    }

    @Override
    public Class getType(Object base, Object property) throws EvaluationException, PropertyNotFoundException
    {
        elContext().setPropertyResolved(false);
        return null;
    }

    @Override
    public void setValue(Object base, Object property, Object value)
            throws EvaluationException, PropertyNotFoundException
    {
        elContext().setPropertyResolved(false);
    }

    @Override
    public void setValue(Object base, int index, Object value) throws EvaluationException, PropertyNotFoundException
    {
        elContext().setPropertyResolved(false);
    }

    private ELContext elContext()
    {
        return FacesContext.getCurrentInstance().getELContext();
    }

}
