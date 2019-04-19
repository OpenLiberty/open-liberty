/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.store;

import java.util.HashMap;
import java.util.Map;
/**
 * This class is used to store the object of ValidatorFactory and Validators if ObjectInputStream has already loaded when
 * the java agent code is injecting extra fields to ObjectInputStream class. When this condition happens, there is no way to add
 * additional fields to store these two objects, since current JDK does not support adding new fields if the class was already loaded.
 * Therefore, this class is used to store these two objects. 
 * This condition happens only when non-IBM JDK is used and some unidentified condition(s) had happened before loading java agent code.
 * This class will be loaded by using the bootstrap classloader in order to maintain class visibility from anywhere.
 *
 */
public class Holder {
    public static final String CLASS_PATH_NAME = "com.ibm.ws.kernel.instrument.serialfilter.store.Holder";
    public static final String CLASS_NAME = "com/ibm/ws/kernel/instrument/serialfilter/store/Holder";
    public static final String FACTORY_FIELD = "serializationValidatorFactory";
    public static final String FACTORY_DESC = "Ljava/util/Map;";
    public static final String VALIDATOR_FIELD = "serializationValidator";
    public static final String VALIDATOR_DESC = "Ljava/util/Map;";

    public static Map<?, ?> serializationValidatorFactory;
    public static final Map<Object, Map<Object, Object>> serializationValidator = new HashMap<Object, Map<Object, Object>>();
}
