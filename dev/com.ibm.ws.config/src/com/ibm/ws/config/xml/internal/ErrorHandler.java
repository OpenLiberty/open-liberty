/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 *
 */
public enum ErrorHandler {

    INSTANCE;

    /** Whether or not Liberty should continue if config error is detected */
    private OnError onError = OnError.WARN;

    /**
     * @param onError
     */
    private ErrorHandler() {
    }

    /**
     * @param onError
     */
    public void setOnError(OnError onError) {
        this.onError = onError;
    }

    public OnError getOnError() {
        return this.onError;
    }

    public String toTraceString(ExtendedConfiguration config, ConfigElement configElement) {
        if (config.getFactoryPid() == null) {
            return config.getPid();
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(config.getFactoryPid());

            if (configElement != null) {
                builder.append("-");
                builder.append(configElement.getId());
            }

            builder.append(" (");
            builder.append(config.getPid());
            builder.append(")");

            return builder.toString();
        }
    }

    /**
     * @return
     */
    public boolean fail() {
        return onError.equals(OnError.FAIL);
    }

}
