/*******************************************************************************
 * Copyright (c) 2006, 2025 IBM Corporation and others.
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

package singleton.property.startup.ejb;

import singleton.property.shared.StartupHelperSingleton;

//@Singleton
public class StartupHelperSingletonBean implements StartupHelperSingleton {
    private boolean ivStarted = false;
    private boolean ivWarStarted = false;
    private boolean ivListenerStarted = false;

    @Override
    public void setPostConstructRun(boolean started) {
        ivStarted = started;
    }

    @Override
    public boolean isPostConstructRun() {
        return ivStarted;
    }

    @Override
    public void setWarPostConstructRun(boolean started) {
        ivWarStarted = started;
    }

    @Override
    public boolean isWarPostConstructRun() {
        return ivWarStarted;
    }

    @Override
    public void setListenerStarted(boolean started) {
        ivListenerStarted = started;
    }

    @Override
    public boolean isListenerStarted() {
        return ivListenerStarted;
    }

}
