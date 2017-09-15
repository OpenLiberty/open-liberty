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

import java.lang.reflect.Method;

import javax.el.FunctionMapper;

/**
 * Composite FunctionMapper that attempts to load the Method from the first FunctionMapper, then the second if
 * <code>null</code>.
 * 
 * @see javax.el.FunctionMapper
 * @see java.lang.reflect.Method
 * 
 * @author Jacob Hookom
 * @version $Id: CompositeFunctionMapper.java 1187701 2011-10-22 12:21:54Z bommel $
 */
public final class CompositeFunctionMapper extends FunctionMapper
{

    private final FunctionMapper fn0;

    private final FunctionMapper fn1;

    public CompositeFunctionMapper(FunctionMapper fn0, FunctionMapper fn1)
    {
        this.fn0 = fn0;
        this.fn1 = fn1; // this one can be null
    }

    /**
     * @see javax.el.FunctionMapper#resolveFunction(java.lang.String, java.lang.String)
     */
    public Method resolveFunction(String prefix, String name)
    {
        Method m = this.fn0.resolveFunction(prefix, name);
        if (m == null && this.fn1 != null)
        {
            return this.fn1.resolveFunction(prefix, name);
        }
        return m;
    }

}
