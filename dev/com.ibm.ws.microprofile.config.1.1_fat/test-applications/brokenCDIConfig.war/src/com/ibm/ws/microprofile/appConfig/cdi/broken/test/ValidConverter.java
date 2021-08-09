/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.broken.test;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Assert;

public class ValidConverter implements Converter<TypeWithValidConverter> {

    /** {@inheritDoc} */
    @Override
    public TypeWithValidConverter convert(String value) {
        Assert.fail("TypeWithValidConverter Converter should not be used- Config Property value is not defined.");
        return null;
    }

}
