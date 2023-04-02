/*******************************************************************************
 * Copyright (c) 2010, 2021 IBM Corporation and others.
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

// Bean with an invalid @EJB reference

package com.ibm.bnd.lookupoverride.shared;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

@Stateless
@Interceptors(PostConstructInterceptor.class)
public class Bad4Bean implements Bad {

    // invalid combination of references, specifying an @EJB annotation with
    // lookup on a field of this bean and an @EJB annotation with beanName
    // on a field of this bean's interceptor.

    @EJB(name = "bad4combo", lookup = "ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean")
    TargetBean ivTarget1;

    // Not expected to succeed
    @Override
    public int boing() {

        return 59;

    }

}
