/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authentication.filter.internal;

public interface ICondition {
    /*
     * What key from the HTTP request is this being compared with?
     */
    public String getKey();

    /*
     * actually check the condition against the test value. This is the value actually found in the HTTP
     * request.
     */
    public boolean checkCondition(IValue test) throws FilterException;

    /*
     * We allow requestHeader attribute name without value, so we will check the attribute name instead of value.
     */
    public boolean isNoAttrValue();

    @Override
    public String toString();
}
