/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics30;

public class BaseMetricConstants {
    public static final String GC_OBJECT_TYPE = "java.lang:.*type=GarbageCollector.*";

    public static final String GC_OBJECT_TYPE_NAME = "java.lang:type=GarbageCollector,name=";

    public static final String MEMORY_OBJECT_TYPE = "java.lang:type=Memory";

    public static final String RUNTIME_OBJECT_TYPE = "java.lang:type=Runtime";

    public static final String THREAD_OBJECT_TYPE = "java.lang:type=Threading";

    public static final String CLASSLOADING_OBJECT_TYPE = "java.lang:type=ClassLoading";

    public static final String OS_OBJECT_TYPE = "java.lang:type=OperatingSystem";
}
