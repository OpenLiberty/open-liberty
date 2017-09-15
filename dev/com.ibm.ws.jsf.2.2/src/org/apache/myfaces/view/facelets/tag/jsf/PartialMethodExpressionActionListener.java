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
package org.apache.myfaces.view.facelets.tag.jsf;

import javax.el.MethodExpression;
import javax.faces.component.PartialStateHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.MethodExpressionActionListener;

public class PartialMethodExpressionActionListener extends 
    MethodExpressionActionListener implements PartialStateHolder
{
    private boolean _initialStateMarked;

    public PartialMethodExpressionActionListener()
    {
        super();
    }

    public PartialMethodExpressionActionListener(
            MethodExpression methodExpression1,
            MethodExpression methodExpression2)
    {
        super(methodExpression1, methodExpression2);
    }

    public PartialMethodExpressionActionListener(
            MethodExpression methodExpression)
    {
        super(methodExpression);
    }

    public void clearInitialState()
    {
        _initialStateMarked = false;
    }

    public boolean initialStateMarked()
    {
        return _initialStateMarked;
    }

    public void markInitialState()
    {
        _initialStateMarked = true;
    }

    public void restoreState(FacesContext context, Object state)
    {
        if (state == null)
        {
            return;
        }
        super.restoreState(context, state);
    }

    public Object saveState(FacesContext context)
    {
        if (initialStateMarked())
        {
            return null;
        }
        return super.saveState(context);
    }
}