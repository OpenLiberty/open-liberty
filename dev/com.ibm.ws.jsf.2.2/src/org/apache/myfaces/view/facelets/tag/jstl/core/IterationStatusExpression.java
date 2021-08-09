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
package org.apache.myfaces.view.facelets.tag.jstl.core;

import javax.el.ELContext;
import javax.el.ValueExpression;

/**
 * @author Jacob Hookom
 * @version $Id: IterationStatusExpression.java 1187701 2011-10-22 12:21:54Z bommel $
 */
public final class IterationStatusExpression extends ValueExpression
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final IterationStatus status;

    /**
     * 
     */
    public IterationStatusExpression(IterationStatus status)
    {
        this.status = status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.ValueExpression#getValue(javax.el.ELContext)
     */
    public Object getValue(ELContext context)
    {
        return this.status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.ValueExpression#setValue(javax.el.ELContext, java.lang.Object)
     */
    public void setValue(ELContext context, Object value)
    {
        throw new UnsupportedOperationException("Cannot set IterationStatus");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.ValueExpression#isReadOnly(javax.el.ELContext)
     */
    public boolean isReadOnly(ELContext context)
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.ValueExpression#getType(javax.el.ELContext)
     */
    public Class getType(ELContext context)
    {
        return IterationStatus.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.ValueExpression#getExpectedType()
     */
    public Class getExpectedType()
    {
        return IterationStatus.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.Expression#getExpressionString()
     */
    public String getExpressionString()
    {
        return this.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.Expression#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        return this.status.equals(obj);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.Expression#hashCode()
     */
    public int hashCode()
    {
        return this.status.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.Expression#isLiteralText()
     */
    public boolean isLiteralText()
    {
        return true;
    }

    public String toString()
    {
        return this.status.toString();
    }

}
