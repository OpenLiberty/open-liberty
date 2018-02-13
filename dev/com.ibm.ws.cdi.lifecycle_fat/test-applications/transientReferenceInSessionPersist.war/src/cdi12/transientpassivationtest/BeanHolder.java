/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi12.transientpassivationtest;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.TransientReference;
import javax.inject.Inject;

@ApplicationScoped
public class BeanHolder {

    public void doNothing() {
        int i = 1;
        i++;
    }

    @Inject
    public void transientVisit(@TransientReference MyStatefulSessionBean bean) {
        bean.doNothing();
    }
}
