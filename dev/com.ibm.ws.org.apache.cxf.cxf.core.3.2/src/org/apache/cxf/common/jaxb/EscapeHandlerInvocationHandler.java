/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.common.jaxb;

import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public final class EscapeHandlerInvocationHandler implements InvocationHandler {

    private Object target; 
    public EscapeHandlerInvocationHandler(Object obj) {
        target = obj;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        if ("escape".equals(method.getName()) && args.length == 5) {
            if ((Integer)args[1] == 0 && (Integer)args[2] == 0) {
                Writer writer = (Writer)args[4];
                writer.write("");
                return null;
            }
            result =  method.invoke(target, args);
        } 
        return result;
    }
    
}
