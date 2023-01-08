/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
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

import java.lang.annotation.Annotation;

import javax.ejb.ApplicationException;

public class ApplicationExceptionImpl
                implements ApplicationException
{
    private final boolean ivRollback;
    private final boolean ivInherited;

    public ApplicationExceptionImpl(boolean rollback, boolean inherited)
    {
        ivRollback = rollback;
        ivInherited = inherited;
    }

    public Class<? extends Annotation> annotationType()
    {
        return ApplicationException.class;
    }

    public boolean rollback()
    {
        return ivRollback;
    }

    public boolean inherited()
    {
        return ivInherited;
    }
}
