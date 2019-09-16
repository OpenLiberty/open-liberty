/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import javax.ejb.Remote;

@Remote
public interface RemoteInterface1 {

    /**
     *
     * @return String "Used RemoteInterface1"
     */
    public String remoteBizInterface1();

    /**
     * Clean up the bean if it is a SFSB
     */
    public void finish();
}
