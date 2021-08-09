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
package org.apache.myfaces.view.facelets.util;

import java.util.ArrayList;
import javax.faces.FacesException;
import org.apache.myfaces.shared.util.ClassUtils;

/**
 * Utility methods used in FaceletsViewDeclarationLanguage
 *
 * @author Leonardo Uribe
 */
public class FaceletsViewDeclarationLanguageUtils
{
    public static Class getReturnType(String signature)
    {
        int endName = signature.indexOf('(');
        if (endName < 0)
        {
            throw new FacesException("Invalid method signature:" + signature);
        }
        int end = signature.lastIndexOf(' ', endName);
        if (end < 0)
        {
            throw new FacesException("Invalid method signature:" + signature);
        }
        try
        {
            return ClassUtils.javaDefaultTypeToClass(signature.substring(0, end));
        }
        catch (ClassNotFoundException e)
        {
            throw new FacesException("Invalid method signature:" + signature);
        }
    }

    /**
     * Get the parameters types from the function signature.
     *
     * @return An array of parameter class names
     */
    public static Class[] getParameters(String signature) throws FacesException
    {
        ArrayList<Class> params = new ArrayList<Class>();
        // Signature is of the form
        // <return-type> S <method-name S? '('
        // < <arg-type> ( ',' <arg-type> )* )? ')'
        int start = signature.indexOf('(') + 1;
        boolean lastArg = false;
        while (true)
        {
            int p = signature.indexOf(',', start);
            if (p < 0)
            {
                p = signature.indexOf(')', start);
                if (p < 0)
                {
                    throw new FacesException("Invalid method signature:" + signature);
                }
                lastArg = true;
            }
            String arg = signature.substring(start, p).trim();
            if (!"".equals(arg))
            {
                try
                {
                    params.add(ClassUtils.javaDefaultTypeToClass(arg));
                }
                catch (ClassNotFoundException e)
                {
                    throw new FacesException("Invalid method signature:" + signature);
                }
            }
            if (lastArg)
            {
                break;
            }
            start = p + 1;
        }
        return params.toArray(new Class[params.size()]);
    }


}
