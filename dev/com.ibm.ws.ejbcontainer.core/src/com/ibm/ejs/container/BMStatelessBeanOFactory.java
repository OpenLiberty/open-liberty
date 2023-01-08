/*******************************************************************************
 * Copyright (c) 1998, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

/**
 * BeanOFactory that creates stateless session <code>BeanOs</code>. <p>
 */
public class BMStatelessBeanOFactory
                extends BeanOFactory
{
    @Override
    protected BeanO newInstance(EJSContainer c, EJSHome h)
    {
        return new BMStatelessBeanO(c, h);
    }
}
