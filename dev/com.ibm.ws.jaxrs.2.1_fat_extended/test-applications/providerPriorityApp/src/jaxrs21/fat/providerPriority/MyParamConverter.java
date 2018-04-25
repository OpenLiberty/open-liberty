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
package jaxrs21.fat.providerPriority;

import javax.ws.rs.ext.ParamConverter;

public class MyParamConverter implements ParamConverter<MyParam> {
    private final int version;

    MyParamConverter(int version) {
        this.version = version;
    }

    @Override
    public MyParam fromString(String value) {
        return new MyParam(value, version);
    }

    @Override
    public String toString(MyParam value) {
        return value.getName();
    }
}
