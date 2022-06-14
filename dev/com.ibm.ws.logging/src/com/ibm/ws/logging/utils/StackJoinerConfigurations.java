/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.utils;

import com.ibm.ws.logging.internal.impl.BaseTraceService;
import com.ibm.ws.logging.internal.impl.LoggingConstants;

/**
 * Stack Joiner functionality references the configuration which
 * enables the merger or joining of the multiple log records created
 * from a printStackTrace() call that results in each line of the stack
 * trace being processed as a new and unique log record.
 *
 * The configuration key follows the template "stackTraceSingleEntry" for
 * server.xml, env var and bootstrap properties. However, the functionality
 * is referred to as stack joiner.
 *
 * There-in also exists additional configuration (only env var) for the buffer size
 * of which how large a stack trace would be merge/joined. See the {@link LoggingConstants} fields
 * for more information .
 */
public class StackJoinerConfigurations {
    public static boolean stackJoinerEnabled() {
        return BaseTraceService.isStackTraceSingleEntryEnabled;
    }
}
