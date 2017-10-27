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
package com.ibm.ws.cdi12.test.warLibAccessBeansInWar;

import javax.ejb.LocalBean;
import javax.enterprise.context.ApplicationScoped;

import com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar.WarBeanInterface;
import com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar2.WarBeanInterface2;

/**
 *
 */
@LocalBean
@ApplicationScoped
public class WarBean implements WarBeanInterface, WarBeanInterface2 {

    @Override
    public String getBeanMessage() {
        return "WarBean";
    }

}
