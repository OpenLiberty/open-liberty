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

/**
 * Remote interface for the AppInstallChgDefBndTest client container test
 * to drive work in the server process. <p>
 **/
public interface LookupNameBndDriver {

    public static String APP_NAME = "LookupOverrideTestApp";
    public static String APP_MODULE_NAME = "LookupOverrideEJB.jar";

    /////////////////////////////////////////////////////////
    ///////////   @EJB variations    ///////////////////
    /////////////////////////////////////////////////////////

    // test E1
    public String verifyE1LookupWithNoOtherBindings();

    // test E2
    public String verifyE2BindingNameOverLookup();

    // test E3
    public String verifyE3BindingNameOverLookupName();

    // test E3.5
    public String verifyE35BindingNameOverLookupNameRemote();

    // test E4.5
    public String verifyE45BindingNameOverLookupAndBeanName();

    // test E5
    public String verifyE5ErrorLookupAndBeanNameMultiFields();

    // test E7
    public String verifyE7ErrorLookupNameAndEjbLinkInterceptorAndBean();

    // test E7.5
    public String verifyE75ErrorLookupAndBeanNameInterceptorAndBean();

    // test E8
    public String verifyE8LookupNameOverLookup();

    // test E10
    public String verifyE10LookupNameOverBeanName();

    // test E11
    public String verifyE11EjbLinkOverBeanName();

    // test E12
    public String verifyE12MissingClassThwartsInjection(); // 641396

    /////////////////////////////////////////////////////////
    ///////////   @Resource variations    ///////////////////
    /////////////////////////////////////////////////////////

    // test R0
    public String verifyR0EnvEntry();

    // test R1
    public String verifyR1EnvEntryLookup();

    // test R3
    public String verifyR3ResourceRefLookup();

    // test R4
    public String verifyR4MessageDestinationRefLookup();

    // test R5
    public String verifyR5ResourceRefBindingNameOverLookup();

    // test R6
    public String verifyR6ResourceEnvRefBindingNameOverLookup();

    // test R7
    public String verifyR7MessageDestinationRefBindingNameOverLookup();

    // test R8
    public String verifyR8ResourceRefBindingNameOverLookupName();

    // test R9
    public String verifyR9ResourceEnvRefBindingNameOverLookupName();

    // test R10
    public String verifyR10MessageDestinationRefBindingNameOverLookupName();

    // test R11
    public String verifyR11ResourceRefLookupNameOverLookup();

    // test R12
    public String verifyR12ResourceEnvRefLookupNameOverLookup();

    // test R13
    public String verifyR13MessageDestinationLookupNameOverLookup();

    // test R14
    public String verifyR14NameAndLookupOnEnvEntry();

}
