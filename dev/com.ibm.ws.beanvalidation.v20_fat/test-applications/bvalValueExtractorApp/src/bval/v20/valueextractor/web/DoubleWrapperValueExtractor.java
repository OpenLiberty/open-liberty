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
package bval.v20.valueextractor.web;

import javax.inject.Inject;
import javax.validation.valueextraction.ExtractedValue;
import javax.validation.valueextraction.UnwrapByDefault;
import javax.validation.valueextraction.ValueExtractor;

import org.junit.Assert;

@UnwrapByDefault
public class DoubleWrapperValueExtractor implements ValueExtractor<@ExtractedValue(type = Double.class) DoubleWrapper> {

    // Used to verify DoubleWrapperValueExtractor is being used to extract values from lists.
    public static int counter = 0;

    @Inject
    ValueExtractorBean bean;

    @Override
    public void extractValues(DoubleWrapper doubleWrapper, ValueReceiver receiver) {
        counter++;

        // Verify that a CDI bean can be injected into a custom ValueExtractor to
        // confirm that the custom ValueExtractor was registered as a CDI managed object.
        System.out.println("Verifying that " + getClass() + " can have a CDI bean injected into it: " + bean);
        Assert.assertNotNull(bean);

        receiver.value(null, doubleWrapper.getValue());
    }
}
