/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient12.jsonbContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.json.bind.config.PropertyVisibilityStrategy;

public class PrivateVisibilityStrategy implements PropertyVisibilityStrategy {

    /* (non-Javadoc)
     * @see javax.json.bind.config.PropertyVisibilityStrategy#isVisible(java.lang.reflect.Field)
     */
    @Override
    public boolean isVisible(Field f) {
        return true;
    }

    /* (non-Javadoc)
     * @see javax.json.bind.config.PropertyVisibilityStrategy#isVisible(java.lang.reflect.Method)
     */
    @Override
    public boolean isVisible(Method m) {
        return false;
    }
}
