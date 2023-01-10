/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
package com.ibm.ejs.csi;

import com.ibm.websphere.csi.EJBModuleConfigData;

/**
 * Wrapper for all the bean config data passed across the CSI.
 */
public class EJBModuleConfigDataImpl
                implements EJBModuleConfigData
{
    private static final long serialVersionUID = -5249702119132194143L;

    private Object ivEJBJar;
    private Object ivBindings;
    private Object ivExtensions;

    public EJBModuleConfigDataImpl(Object ejbJar, Object bindings, Object extensions)
    {
        ivEJBJar = ejbJar;
        ivBindings = bindings;
        ivExtensions = extensions;
    }

    public Object getModule()
    {
        return ivEJBJar;
    }

    public Object getModuleBinding()
    {
        return ivBindings;
    }

    public Object getModuleExtension()
    {
        return ivExtensions;
    }
}
