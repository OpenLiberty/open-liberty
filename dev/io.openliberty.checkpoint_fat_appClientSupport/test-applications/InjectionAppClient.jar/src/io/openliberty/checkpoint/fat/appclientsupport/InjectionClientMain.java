/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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

package io.openliberty.checkpoint.fat.appclientsupport;

import javax.ejb.EJB;
import javax.inject.Inject;

import io.openliberty.checkpoint.fat.appclientsupport.view.SimpleCDIInjection;
import io.openliberty.checkpoint.fat.appclientsupport.view.SimpleEJBInjectionBeanRemote;
import io.openliberty.checkpoint.fat.appclientsupport.view.SimpleGlobalEJBInjectionBeanRemote;

public class InjectionClientMain {

    private final static String NAME_EJB_GLOBAL = "java:global/InjectionApp/InjectionAppEJB/SimpleGlobalEJBInjectionBean!io.openliberty.checkpoint.fat.appclientsupport.view.SimpleGlobalEJBInjectionBeanRemote";

    @EJB(lookup = NAME_EJB_GLOBAL)
    public static SimpleGlobalEJBInjectionBeanRemote injectedGlobalEjb;

    @EJB
    public static SimpleEJBInjectionBeanRemote injectedEjb;

    @Inject
    public static SimpleCDIInjection cdiBean;

    public static void main(String[] args) {
        System.out.println("main - entry");

        if (checkEJBWithLookup(injectedGlobalEjb))
            System.out.println("injectGlobal_EJB-PASSED");

        if (checkEJB(injectedEjb))
            System.out.println("inject_EJB-PASSED");

        if (checkCDI(cdiBean))
            System.out.println("inject_CDI-PASSED");

        System.out.println("main - exit");
    }

    private static boolean checkEJBWithLookup(SimpleGlobalEJBInjectionBeanRemote ejb) {
        boolean b;
        try {
            b = ejb != null && ejb.add(4, 8) == 12;
        } catch (Exception ex) {
            ex.printStackTrace();
            b = false;
        }
        return b;
    }

    private static boolean checkEJB(SimpleEJBInjectionBeanRemote ejb) {
        boolean b;
        try {
            b = ejb != null && ejb.subtract(12, 4) == 8;
        } catch (Exception ex) {
            ex.printStackTrace();
            b = false;
        }
        return b;
    }

    private static boolean checkCDI(SimpleCDIInjection cdiBean) {
        boolean b;
        try {
            b = cdiBean != null && cdiBean.multiply(2, 4) == 8;
        } catch (Exception ex) {
            ex.printStackTrace();
            b = false;
        }
        return b;
    }

}
