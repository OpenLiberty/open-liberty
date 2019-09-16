/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.mix.ejb;

import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless(name = "NoInterfaceBean4")
@LocalBean
@Local(LocalInterfaceForNoInterfaceBean4.class)
public class NoInterfaceBean4 {
    // Like the NoInterfaceBean, this guy is defined with both a @LocalBean and @Local annotation, so this bean definition doesn't
    // buy us anything in terms of testing different ways to declare a no-interface style view.  However, this bean does have
    // testing value because we need to verify some scenarios using a *stateless* bean that has both local-interface and no-interface
    // views exposed...and we can't do that using the NoInterfaceBean, because that guys is *stateful*.
    public int methodNotExposedOnInterface(int originalValue) {
        // We just want to ensure that we can call a method not exposed on an interface.
        int newValue = originalValue + 1;
        return newValue;
    }
}