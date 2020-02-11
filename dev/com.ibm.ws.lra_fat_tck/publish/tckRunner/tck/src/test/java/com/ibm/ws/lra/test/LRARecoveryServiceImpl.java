/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.lra.test;

import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.eclipse.microprofile.lra.tck.service.spi.LRACallbackException;
import java.net.URI;
import javax.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class LRARecoveryServiceImpl implements LRARecoveryService {
    
    public void waitForCallbacks(URI lraId) throws LRACallbackException {
        // do nothing
    }


    public boolean waitForEndPhaseReplay(URI lraId) throws LRACallbackException {
        return false;
    }

}
