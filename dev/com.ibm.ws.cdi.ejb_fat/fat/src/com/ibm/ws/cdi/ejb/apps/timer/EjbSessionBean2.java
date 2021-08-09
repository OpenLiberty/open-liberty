/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.timer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ibm.ws.cdi.ejb.apps.timer.view.EjbSessionBean2Local;

/**
 * Session Bean implementation class EjbSessionCounter
 */
@Stateless
@Local(EjbSessionBean2Local.class)
@LocalBean
public class EjbSessionBean2 implements EjbSessionBean2Local {
    @Inject
    RequestScopedCounter counter;

    /**
     * Default constructor.
     */
    public EjbSessionBean2() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public int getCount() {
        return counter.get();
    }

    @PostConstruct
    void postConstruct() {
        System.out.println(String.format("%s@%08x created", this.getClass().getSimpleName(), System.identityHashCode(this)));
    }

    @PreDestroy
    void preDestroy() {
        System.out.println(String.format("%s@%08x destroyed", this.getClass().getSimpleName(), System.identityHashCode(this)));
    }

    @Override
    public void incCount() {
        counter.increment();
    }
}
