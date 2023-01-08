/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests.basic.warlibs.war;

import javax.ejb.LocalBean;
import javax.enterprise.context.ApplicationScoped;

import com.ibm.ws.cdi.visibility.tests.basic.warlibs.maifestLibJar.WarBeanInterface2;
import com.ibm.ws.cdi.visibility.tests.basic.warlibs.webinfLibJar.WarBeanInterface;

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
