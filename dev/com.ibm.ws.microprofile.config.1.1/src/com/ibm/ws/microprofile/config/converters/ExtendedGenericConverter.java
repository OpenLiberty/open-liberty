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
package com.ibm.ws.microprofile.config.converters;

import com.ibm.ws.microprofile.config.impl.ConversionManager;

/**
 * A converter with an explicit Type and priority
 */
public interface ExtendedGenericConverter {

    /**
     * @param rawString
     * @return
     */
    public abstract <T> Object convert(String rawString, Class<T> genericType, ConversionManager conversionManager, ClassLoader classLoader);
}
