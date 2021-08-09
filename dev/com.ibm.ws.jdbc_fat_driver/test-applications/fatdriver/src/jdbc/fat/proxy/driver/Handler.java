/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.proxy.driver;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

public class Handler implements InvocationHandler {
    private Properties props;
    private final Class<?> type;

    Handler(Class<?> type, Properties props) {
        this.type = type;
        this.props = props;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?> returnType = method.getReturnType();
        if ("hashCode".equals(methodName))
            return System.identityHashCode(proxy);
        if ("toString".equals(methodName))
            return "jdbc.fat.proxy.driver." + type.getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(proxy));
        String s = methodName.startsWith("get") ? props.getProperty(methodName.substring(3)) : null;
        if (boolean.class.equals(returnType))
            return s == null ? true : Boolean.parseBoolean(s);
        if (long.class.equals(returnType))
            return s == null ? 0 : Long.parseLong(s);
        if (int.class.equals(returnType))
            return s == null ? 0 : Integer.parseInt(s);
        if (short.class.equals(returnType))
            return s == null ? 0 : Short.parseShort(s);
        if (String.class.equals(returnType))
            return s == null ? null : s;
        if (returnType.isInterface())
            return Proxy.newProxyInstance(Handler.class.getClassLoader(), new Class[] { returnType }, new Handler(returnType, props));
        return null;
    }
}
