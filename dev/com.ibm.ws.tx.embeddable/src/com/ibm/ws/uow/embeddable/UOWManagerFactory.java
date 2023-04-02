/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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

package com.ibm.ws.uow.embeddable;

public class UOWManagerFactory {
    private static UOWManager _clientuowManager;

    // uses the initialisation-on-demand holder idiom to provide safe and fast lazy loading
    private static class UOWManagerHolder {
        public static final UOWManager _INSTANCE = new EmbeddableUOWManagerImpl();
    }

    public static UOWManager getUOWManager() {
        return UOWManagerHolder._INSTANCE;
    }

    /**
     * @return
     */
    public static UOWManager getClientUOWManager() {
        if (_clientuowManager == null) {
            _clientuowManager = new ClientUOWManagerImpl();
        }

        return _clientuowManager;
    }

}