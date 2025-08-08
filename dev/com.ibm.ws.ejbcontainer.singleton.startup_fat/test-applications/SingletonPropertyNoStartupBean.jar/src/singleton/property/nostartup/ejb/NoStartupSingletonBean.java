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

package singleton.property.nostartup.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import singleton.property.shared.BasicSingleton2;
import singleton.property.shared.StartupHelperSingleton;

@Singleton
public class NoStartupSingletonBean implements BasicSingleton2 {
    @EJB
    private StartupHelperSingleton ivHelper;

    @PostConstruct
    public void postConstruct() {
        // Indicate that Singleton instance created.
        ivHelper.setPostConstructRun(true);

    }

}
