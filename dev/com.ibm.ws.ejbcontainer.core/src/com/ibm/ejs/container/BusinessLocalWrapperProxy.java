/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

/**
 * The base class for wrapper proxies of local business interface.
 * 
 * @see WrapperProxy
 */
public class BusinessLocalWrapperProxy
                implements WrapperProxy
{
    volatile WrapperProxyState ivState;

    public BusinessLocalWrapperProxy(WrapperProxyState state)
    {
        ivState = state;
    }

    @Override
    public String toString()
    {
        return super.toString() + '(' + ivState + ')';
    }

    @Override
    public int hashCode()
    {
        return ivState.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof BusinessLocalWrapperProxy &&
               ivState.equals(((BusinessLocalWrapperProxy) o).ivState);
    }
}
