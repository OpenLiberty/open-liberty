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
package com.ibm.ws.security.backchannelLogout.fat.utils;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.MySkipRule;

public class SkipIfSocialClient extends MySkipRule {

    protected static Class<?> thisClass = SkipIfSocialClient.class;
    public static Boolean socialClient = false;

    @Override
    public Boolean callSpecificCheck() {
        Log.info(thisClass, "callSpecificCheck", "Uses a Social Client: " + Boolean.toString(socialClient));
        return socialClient;
    }
}
