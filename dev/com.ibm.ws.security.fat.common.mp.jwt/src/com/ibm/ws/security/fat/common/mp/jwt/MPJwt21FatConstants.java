/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.mp.jwt;

public class MPJwt21FatConstants extends MPJwtFatConstants {

    public static final String DEFAULT_TOKEN_AGE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileDefaultTokenAgeInMP-ConfigInMETA-INF";
    public static final String DEFAULT_TOKEN_AGE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileDefaultTokenAgeInMP-ConfigUnderWEB-INF";
    public static final String LONG_TOKEN_AGE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileLongTokenAgeInMP-ConfigInMETA-INF";
    public static final String LONG_TOKEN_AGE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileLongTokenAgeInMP-ConfigUnderWEB-INF";
    public static final String SHORT_TOKEN_AGE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileShortTokenAgeInMP-ConfigInMETA-INF";
    public static final String SHORT_TOKEN_AGE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileShortTokenAgeInMP-ConfigUnderWEB-INF";

    public static final String DEFAULT_CLOCK_SKEW_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileDefaultClockSkewInMP-ConfigInMETA-INF";
    public static final String DEFAULT_CLOCK_SKEW_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileDefaultClockSkewInMP-ConfigUnderWEB-INF";
    public static final String LONG_CLOCK_SKEW_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileLongClockSkewInMP-ConfigInMETA-INF";
    public static final String LONG_CLOCK_SKEW_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileLongClockSkewInMP-ConfigUnderWEB-INF";
    public static final String SHORT_CLOCK_SKEW_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileShortClockSkewInMP-ConfigInMETA-INF";
    public static final String SHORT_CLOCK_SKEW_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileShortClockSkewInMP-ConfigUnderWEB-INF";

    public static final String DEFAULT_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileDefaultKeyMgmtKeyAlgInMP-ConfigInMETA-INF";
    public static final String DEFAULT_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileDefaultKeyMgmtKeyAlgInMP-ConfigUnderWEB-INF";
    public static final String MATCH_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileMatchKeyMgmtKeyAlgInMP-ConfigInMETA-INF";
    public static final String MATCH_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileMatchKeyMgmtKeyAlgInMP-ConfigUnderWEB-INF";
    public static final String MISMATCH_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileMismatchKeyMgmtKeyAlgInMP-ConfigInMETA-INF";
    public static final String MISMATCH_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileMismatchKeyMgmtKeyAlgInMP-ConfigUnderWEB-INF";
    public static final String INVALID_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT = "microProfileInvalidKeyMgmtKeyAlgInMP-ConfigInMETA-INF";
    public static final String INVALID_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT = "microProfileInvalidKeyMgmtKeyAlgInMP-ConfigUnderWEB-INF";

    public static final String TOKEN_AGE_KEY = "mp.jwt.verify.token.age";
    public static final String CLOCK_SKEW_KEY = "mp.jwt.verify.clock.skew";
    public static final String DECRYPT_KEY_ALG_KEY = "mp.jwt.decrypt.key.algorithm";
}
