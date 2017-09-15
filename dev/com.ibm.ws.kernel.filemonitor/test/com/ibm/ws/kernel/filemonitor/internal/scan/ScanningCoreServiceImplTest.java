/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.filemonitor.internal.scan;

import com.ibm.ws.kernel.filemonitor.internal.CoreServiceImpl;
import com.ibm.ws.kernel.filemonitor.internal.CoreServiceImplTestParent;

/**
 *
 */
public class ScanningCoreServiceImplTest extends CoreServiceImplTestParent {

    /**
     * @return
     */
    @Override
    protected CoreServiceImpl instantiateCoreService() {
        return new ScanningCoreServiceImpl();
    }

}
