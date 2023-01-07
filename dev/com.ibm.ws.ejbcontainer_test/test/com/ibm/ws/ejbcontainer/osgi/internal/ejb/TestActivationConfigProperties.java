/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.osgi.internal.ejb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

@MessageDriven(activationConfig = { @ActivationConfigProperty(propertyName = TestActivationConfigProperties.NAME1, propertyValue = TestActivationConfigProperties.VALUE1),
                                   @ActivationConfigProperty(propertyName = TestActivationConfigProperties.NAME2, propertyValue = TestActivationConfigProperties.VALUE2) })
public class TestActivationConfigProperties {
    public static final String NAME1 = "name1";
    public static final String VALUE1 = "value1";
    public static final String NAME2 = "name2";
    public static final String VALUE2 = "value2";
}
