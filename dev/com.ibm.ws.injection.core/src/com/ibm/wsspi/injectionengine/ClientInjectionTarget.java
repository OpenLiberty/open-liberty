/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import java.lang.reflect.Member;
import java.lang.reflect.Type;

import com.ibm.ejs.util.Util;

/**
 * This class temporarily holds additional injection target data required for
 * federated client module injection. Many of the operations on this class are
 * not expected to be called during reference processing and are unsupported.
 * After reference processing is finished, instances of this class are
 * translated to instances of {@link ClientInjection}.
 */
public class ClientInjectionTarget
                extends InjectionTarget
{
    private final String ivTargetClassName;
    private final String ivTargetName;

    public ClientInjectionTarget(String targetClassName, String targetName, InjectionBinding<?> binding)
    {
        ivTargetClassName = targetClassName;
        ivTargetName = targetName;
        setInjectionBinding(binding);
    }

    @Override
    public String toString()
    {
        return Util.identity(this) + '[' + ivTargetClassName + '.' + ivTargetName +
               ", " + ((ivFromXML) ? "XML" : "Annotation") + ']';
    }

    public String getTargetClassName()
    {
        return ivTargetClassName;
    }

    public String getTargetName()
    {
        return ivTargetName;
    }

    @Override
    public Member getMember()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getInjectionClassType()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getGenericType()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void injectMember(Object objectToInject, Object dependentObject)
    {
        throw new UnsupportedOperationException();
    }
}
