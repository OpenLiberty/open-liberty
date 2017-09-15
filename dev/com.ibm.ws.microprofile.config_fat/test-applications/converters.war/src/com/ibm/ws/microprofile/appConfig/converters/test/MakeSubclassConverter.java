/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.converters.test;

import org.eclipse.microprofile.config.spi.Converter;

public class MakeSubclassConverter<T extends Parent> implements Converter<T> {

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public T convert(String value) {
        return (T) Child.newChild(value);
    }

}
