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
package com.ibm.ejs.container;

import javax.ejb.EnterpriseBean;

/**
 * This class exists for ABI compatibility with ejbdeploy'ed home beans.
 */
public class BeanManagedBeanO
{
    final EntityBeanO ivDelegate;

    BeanManagedBeanO(EntityBeanO delegate)
    {
        ivDelegate = delegate;
    }

    /**
     * Return the underlying bean instance. This method exists for ABI
     * compatibility with ejbdeploy'ed home beans.
     */
    public EnterpriseBean getEnterpriseBean()
    {
        return (EnterpriseBean) ivDelegate.getBeanInstance();
    }
}
