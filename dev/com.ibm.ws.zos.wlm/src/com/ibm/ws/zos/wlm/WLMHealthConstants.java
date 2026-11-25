/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm;

public class WLMHealthConstants {
    /* Constant of call period to WLM health API */
    public static final long INTERVAL_DEFAULT = 0;

    /* Constant of Max and Default increased percentage of WLM health */
    public static final Integer INCREMENT_DEFAULT = 100;

    /* Constant of Minimum increased percentage of WLM health */
    public static final Integer INCREMENT_MIN = 1;

    /* Constant name of metatype property for WLM health API call interval */
    public static final String zosHealthInterval = "interval";

    /* Constant name of metatype property for WLM health increment */
    public static final String zosHealthIncrement = "increment";

    /* Constant name of metatype property for WLM health range */
    public static final int HEALTH_MAX = 100;

    public static final int HEALTH_MIN = 1;
}
