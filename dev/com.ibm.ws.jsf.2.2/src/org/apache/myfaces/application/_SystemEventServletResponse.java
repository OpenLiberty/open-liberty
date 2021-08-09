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
package org.apache.myfaces.application;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * @author Werner Punz (latest modification by $Author: struberg $)
 * @version $Revision: 1188643 $ $Date: 2011-10-25 13:13:09 +0000 (Tue, 25 Oct 2011) $
 *
 * the problem with the system event listeners is that they
 * are triggered often outside of an existing request
 * hence we have to provide dummy objects
 */

public class _SystemEventServletResponse extends HttpServletResponseWrapper
{

    public _SystemEventServletResponse()
    {
        super( (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class[] { HttpServletResponse.class },
                new InvocationHandler()
                {
                    public Object invoke(Object proxy, Method m, Object[] args) 
                    {
                        throw new UnsupportedOperationException("This response class is an empty placeholder");
                    }
                }));
    }

    @Override
    public String getCharacterEncoding()
    {
        return null;
    }

    @Override
    public String getContentType()
    {
        return null;
    }

    @Override
    public void setCharacterEncoding(String charset)
    {
    }

    @Override
    public void setContentLength(int len)
    {
    }

    @Override
    public void setContentType(String type)
    {
    }

}
