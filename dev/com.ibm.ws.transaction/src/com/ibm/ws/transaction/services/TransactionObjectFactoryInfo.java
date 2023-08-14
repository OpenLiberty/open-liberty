/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
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
package com.ibm.ws.transaction.services;

import java.lang.annotation.Annotation;

import javax.annotation.Resource;
import javax.naming.spi.ObjectFactory;
import javax.transaction.UserTransaction;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.injectionengine.ObjectFactoryInfo;

@Component(service = { ObjectFactoryInfo.class })
public class TransactionObjectFactoryInfo extends ObjectFactoryInfo {

    @Override
    @Trivial
    public Class<? extends Annotation> getAnnotationClass() {
        return Resource.class;
    }

    @Override
    @Trivial
    public Class<?> getType() {
        return UserTransaction.class;
    }

    @Override
    @Trivial
    public boolean isOverrideAllowed() {
        return false;
    }

    @Override
    @Trivial
    public Class<? extends ObjectFactory> getObjectFactoryClass() {
        return TransactionObjectFactory.class;
    }

}
