/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tra.work;

import javax.resource.spi.work.WorkContext;

@SuppressWarnings("serial")
public class TRAWorkContext1 implements WorkContext {

    public String getDescription() {
        return "TRAWorkContext1";
    }

    public String getName() {
        return this.getClass().getName();
    }

}
