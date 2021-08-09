/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ras;

import com.ibm.ws.logging.internal.TrSharedSecrets;

/**
 * Package-private class to expose functionality internal to logging. The
 * instance of this class is created by TrSharedSecrets.
 */
class TrSharedSecretsImpl extends TrSharedSecrets {
    @Override
    public void addGroup(TraceComponent tc, String group) {
        tc.addGroup(group);
    }
}
