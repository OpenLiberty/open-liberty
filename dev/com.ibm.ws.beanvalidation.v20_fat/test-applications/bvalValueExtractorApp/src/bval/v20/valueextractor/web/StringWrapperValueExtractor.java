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

import javax.validation.valueextraction.ExtractedValue;
import javax.validation.valueextraction.UnwrapByDefault;
import javax.validation.valueextraction.ValueExtractor;

@UnwrapByDefault
public class StringWrapperValueExtractor implements ValueExtractor<@ExtractedValue(type = String.class) StringWrapper> {

    // Used to verify StringWrapperValueExtractor is being used to extract values from lists.
    public static int counter = 0;

    @Override
    public void extractValues(StringWrapper stringWrapper, ValueReceiver receiver) {
        counter++;
        receiver.value(null, stringWrapper.getValue());
    }
}
