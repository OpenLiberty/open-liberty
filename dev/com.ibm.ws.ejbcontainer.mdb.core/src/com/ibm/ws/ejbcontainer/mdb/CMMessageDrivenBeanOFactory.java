/*******************************************************************************
 * Copyright (c) 2001, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb;

import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.BeanOFactory;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;

/**
 * BeanOFactory that creates MessageDriven <code>BeanOs</code>. <p>
 */
public class CMMessageDrivenBeanOFactory
                extends BeanOFactory
{
    @Override
    protected BeanO newInstance(EJSContainer c, EJSHome h)
    {
        return new CMMessageDrivenBeanO(c, h);
    }
}
