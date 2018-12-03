/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.utils;

public abstract class MySkipRule implements ConditionalIgnoreRule.IgnoreCondition {

    public abstract Boolean callSpecificCheck();

    @Override
    public boolean isSatisfied() {

        return callSpecificCheck();
    }
}
