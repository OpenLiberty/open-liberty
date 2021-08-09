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
import javax.ejb.RemoveException;

/**
 * The base class for wrapper proxies of local home views. This class must be
 * a subclass EJSLocalWrapper because internals expect that the home wrapper is
 * an EJBLocalObject, presumably for implementation convenience. Therefore,
 * this class uses the same rationale to extend EJSLocalWrapperProxy.
 * 
 * @see WrapperProxy
 */
public class EJSLocalHomeWrapperProxy
                extends EJSLocalWrapperProxy
                implements EJBLocalHome
{
    public EJSLocalHomeWrapperProxy(WrapperProxyState state)
    {
        super(state);
    }

    public void remove(Object primaryKey)
                    throws RemoveException
    {
        ((EJBLocalHome) EJSContainer.resolveWrapperProxy(this)).remove(primaryKey);
    }
}
