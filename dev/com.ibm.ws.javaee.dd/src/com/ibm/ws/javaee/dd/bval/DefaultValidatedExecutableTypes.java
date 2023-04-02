/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.javaee.dd.bval;

import java.util.List;

public interface DefaultValidatedExecutableTypes {
    enum ExecutableTypeEnum {
        NONE,
        CONSTRUCTORS,
        NON_GETTER_METHODS,
        GETTER_METHODS,
        ALL
    }

    List<ExecutableTypeEnum> getExecutableTypes();
}
