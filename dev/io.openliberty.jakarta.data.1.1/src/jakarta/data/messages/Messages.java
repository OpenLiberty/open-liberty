/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package jakarta.data.messages;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Method signatures are copied from Jakarta Data.
 */
public class Messages {
    private static final ResourceBundle messages = //
                    ResourceBundle.getBundle("jakarta.data.messages.DataMessages");

    private Messages() {
    }

    public static String get(String messageId,
                             Object... messageArgs) {
        return MessageFormat.format(messages.getString(messageId),
                                    messageArgs);
    }

    public static void requireNonNull(Object value,
                                      String methodArg) {
        if (value == null)
            throw new NullPointerException(get("001.arg.required",
                                               methodArg));
    }
}
