/*******************************************************************************
 * Copyright (c) 2010, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.bnd.lookupoverride.shared;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.InvocationContext;

public class PostConstructInterceptor {

    private final static String CLASSNAME = PostConstructInterceptor.class.getName();

    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // test E7.5
    // invalid combination of references, specifying an @EJB annotation with
    // lookup on a field of this bean and an @EJB annotation with beanName
    // on a field of this bean's interceptor.

    @EJB(name = "bad4combo", beanName = "fooBean")
    TargetBean ivTarget1;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @PostConstruct
    public void postConstruct(InvocationContext ctx) {

        try {
            ctx.proceed();
        } catch (Exception ex) {
            svLogger.logp(Level.SEVERE, CLASSNAME, "PostConstruct", "Failed to proceed", ex);
        }
    }

}
