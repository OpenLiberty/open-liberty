/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.beans;

import java.net.URL;

import com.ibm.ws.microprofile.appConfig.cdi.test.Animal;
import com.ibm.ws.microprofile.appConfig.cdi.test.Dog;
import com.ibm.ws.microprofile.appConfig.cdi.test.MyObject;
import com.ibm.ws.microprofile.appConfig.cdi.test.Parent;
import com.ibm.ws.microprofile.appConfig.cdi.test.Pizza;

public interface ConfigFieldInjectionInterface {

    public String getSIMPLE_KEY1();

    public MyObject getGENERIC_INSTANCE_KEY();

    public MyObject getGENERIC_PROVIDER_KEY();

    public String getSIMPLE_KEY2();

    public String getSIMPLE_KEY3();

    public Parent getPARENT_KEY();

    public String getDISCOVERED_KEY();

    public String getNULL_WITH_DEFAULT_KEY();

    public Dog getDOG_KEY();

    public Animal getANIMAL_KEY();

    public String getSYS_PROP();

    public URL getOPTIONAL_KEY();

    public URL getOPTIONAL_NULL_KEY();

    public URL getOPTIONAL_MISSING_KEY();

    public String getMISSING_KEY();

    public Pizza getPIZZA_KEY();

    public Pizza getPIZZA_MISSING_KEY();

    public Pizza getPIZZA_GOOD_KEY();

}
