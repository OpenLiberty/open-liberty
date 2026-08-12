/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
 package io.openliberty.microprofile.reactive.messaging.fat.apps.startup.extension;

import jakarta.annotation.Priority; 
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.BeanManager;

public class DelayingCDIExtension implements Extension {

    protected void afterDeploymentValidation(@Priority(Integer.MAX_VALUE) @Observes AfterDeploymentValidation done, BeanManager beanManager) {
        //Delay until kafka messagse emitted before app startup risk being lost
        System.out.println("afterDeploymentValidation - beginning delay");
        
        try {
            Thread.sleep(3000);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    
    }

}
