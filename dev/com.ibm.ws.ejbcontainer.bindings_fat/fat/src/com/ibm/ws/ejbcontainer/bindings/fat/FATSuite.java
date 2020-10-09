/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.bindings.fat.tests.AmbiguousBindingsTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.BindToJavaGlobalTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.BindToServerRootTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.BindingNameTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.BndErrorTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.ComponentIDTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.CustomBindingsTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.DefaultBindingsTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.DisableShortBndTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.EJBinWARBindingsTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.HomeBindingNameTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.JNDINameTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.NoInterfaceBindingsTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.ServerXMLBindingTest;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.SimpleBindingNameTest;

@RunWith(Suite.class)
@SuiteClasses({
                AmbiguousBindingsTest.class,
                BindingNameTest.class,
                BindToJavaGlobalTest.class,
                BindToServerRootTest.class,
                BndErrorTest.class,
                ComponentIDTest.class,
                CustomBindingsTest.class,
                DefaultBindingsTest.class,
                DisableShortBndTest.class,
                EJBinWARBindingsTest.class,
                HomeBindingNameTest.class,
                JNDINameTest.class,
                NoInterfaceBindingsTest.class,
                ServerXMLBindingTest.class,
                SimpleBindingNameTest.class

})

public class FATSuite {}
