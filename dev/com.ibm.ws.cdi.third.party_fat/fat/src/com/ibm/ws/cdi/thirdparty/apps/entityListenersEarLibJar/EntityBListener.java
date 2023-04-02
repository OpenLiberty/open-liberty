/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

package com.ibm.ws.cdi.thirdparty.apps.entityListenersEarLibJar;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.PrePersist;

public class EntityBListener {

    @Inject 
    BeanManager bm;
   
    @PrePersist
    public void prePersist(Object object) {
        if (bm != null) {
            System.out.println("prePersist" + object.toString() + " " + bm.toString());
            System.out.println("testInjectWorksInsideEntityListeners passed!");
        }
    }
   
    @PostPersist
    public void postPersist(Object object){
        if (bm != null) {
            System.out.println("postPersist" + object.toString() + " " + bm.toString());
            System.out.println("testInjectWorksInsideEntityListeners passed!");
        }
    }
   
}
