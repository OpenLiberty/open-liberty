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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tra.work;

import javax.resource.spi.work.WorkContext;

@SuppressWarnings("serial")
public class TRAWorkContext implements WorkContext {

    public String getDescription() {
        return null;
    }

    public String getName() {
        return null;
    }

}
