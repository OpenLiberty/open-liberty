/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package io.openliberty.microprofile.lra.test;

import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.eclipse.microprofile.lra.tck.service.spi.LRACallbackException;
import java.net.URI;
import javax.enterprise.context.ApplicationScoped;


/**
 * A non-implementation of the LRARecoveryService, just to get the LRA TCK running
 * (not passing!). Must be made available to the tests via META-INF/services. 
 */
@ApplicationScoped
public class LRARecoveryServiceImpl implements LRARecoveryService {
    
    public void waitForCallbacks(URI lraId) throws LRACallbackException {
        // do nothing
    }


    public boolean waitForEndPhaseReplay(URI lraId) throws LRACallbackException {
        return true;
    }

}
