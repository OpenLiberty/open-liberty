/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package componenttest.exception;

import componenttest.custom.junit.runner.RepeatTestFilter;

public class TopologyException extends Exception {

    public TopologyException() {
        super();
    }

    public TopologyException(String message, Throwable cause) {
        super(getRepeatAction() + message, cause);
    }

    public TopologyException(String message) {
        super(getRepeatAction() + message);

    }

    public TopologyException(Throwable cause) {
        super(cause);
    }

    private static String getRepeatAction() {
        String repeatActionString = RepeatTestFilter.getRepeatActionsAsString();
        if (repeatActionString == null || repeatActionString.isEmpty()) {
            return "";
        } else {
            return "(On Repeat Action " + repeatActionString +") ";
        }
    }

    private static final long serialVersionUID = 1L;

}
