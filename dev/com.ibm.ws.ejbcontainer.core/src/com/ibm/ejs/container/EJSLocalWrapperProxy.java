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

import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.RemoveException;

import com.ibm.ejs.util.Util;

/**
 * The base class for wrapper proxies of local component views. This class
 * must be a subclass of EJSLocalWrapper due to the signature of {@link EJSHome#createWrapper_Local}.
 * 
 * @see WrapperProxy
 */
public class EJSLocalWrapperProxy
                extends EJSLocalWrapper
                implements WrapperProxy
{
    volatile WrapperProxyState ivState;

    public EJSLocalWrapperProxy(WrapperProxyState state)
    {
        ivState = state;
    }

    @Override
    public String toString()
    {
        return Util.identity(this) + '(' + ivState + ')';
    }

    @Override
    public int hashCode()
    {
        return ivState.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof EJSLocalWrapperProxy &&
               ivState.equals(((EJSLocalWrapperProxy) o).ivState);
    }

    @Override
    public EJBLocalHome getEJBLocalHome()
    {
        return ((EJBLocalObject) EJSContainer.resolveWrapperProxy(this)).getEJBLocalHome();
    }

    @Override
    public Object getPrimaryKey()
    {
        return ((EJBLocalObject) EJSContainer.resolveWrapperProxy(this)).getPrimaryKey();
    }

    @Override
    public void remove()
                    throws RemoveException
    {
        ((EJBLocalObject) EJSContainer.resolveWrapperProxy(this)).remove();
    }

    @Override
    public boolean isIdentical(EJBLocalObject o)
    {
        return equals(o);
    }
}
