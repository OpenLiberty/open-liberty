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
package org.apache.myfaces.shared.util;

import java.lang.reflect.Method;

/**
 *
 */
public final class ServletSpecifications
{
    private static volatile Boolean servlet30Available;
    
    public static boolean isServlet30Available()
    {
        if (servlet30Available == null)
        {
            Class clazz = ClassUtils.simpleClassForName("javax.servlet.http.Cookie");
            try
            {
                Method m = clazz.getMethod("setHttpOnly", boolean.class);
                if (m != null)
                {
                    servlet30Available = Boolean.TRUE;
                }
                else
                {
                    servlet30Available = Boolean.FALSE;
                }
            }
            catch (NoSuchMethodException ex)
            {
                servlet30Available = Boolean.FALSE;
            }
            catch (SecurityException ex)
            {
                // Don't assume servlet 2.5 if a SecurityException is thrown,
                // assume always servlet 3.0
                servlet30Available = Boolean.TRUE;
            }
        }
        return servlet30Available;
    }

}
