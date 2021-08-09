/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.runtime;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.ContainerException;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfigurationProvider;
import com.ibm.wsspi.injectionengine.InjectionException;

public class ComponentNameSpaceConfigurationProviderImpl
                implements ComponentNameSpaceConfigurationProvider
{
    private final BeanMetaData ivBMD;
    private final AbstractEJBRuntime ivEJBRuntime;

    public ComponentNameSpaceConfigurationProviderImpl(BeanMetaData bmd, AbstractEJBRuntime ejbRuntime)
    {
        ivBMD = bmd;
        ivEJBRuntime = ejbRuntime;
    }

    @Override
    public String toString()
    {
        return super.toString() + '[' + ivBMD.j2eeName + ']';
    }

    public ComponentNameSpaceConfiguration getComponentNameSpaceConfiguration()
                    throws InjectionException
    {
        try
        {
            return ivEJBRuntime.finishBMDInitForReferenceContext(ivBMD);
        } catch (ContainerException ex)
        {
            throw new InjectionException(ex);
        } catch (EJBConfigurationException ex)
        {
            throw new InjectionException(ex);
        }
    }
}
