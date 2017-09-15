/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.config;

public class Method {
    public String name;
    public Method method;
    public Field field;
    public int index = -1;

    //implementation methods
    public java.lang.reflect.Method methodImpl;

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(name).append("()");
        if (method != null) {
            sb.append(".").append(method);
        } else if (field != null) {
            sb.append(".").append(field);
        }
        if (index != -1) {
            sb.append(".").append(index);
        }
        return sb.toString();
    }

    @Override
    public Object clone() {
        Method m = new Method();
        m.name = name;
        if (method != null)
            m.method = (Method) method.clone();
        if (field != null)
            m.field = (Field) field.clone();
        m.index = index;
        return m;
    }
}
