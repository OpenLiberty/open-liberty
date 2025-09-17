/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import java.util.Set;

public interface RecLogService {

    void initialize(String serverName);

    void unsetRecoveryLogFactory(RecoveryLogFactory fac);

    void startRecovery(RecoveryLogFactory fac) throws RecoveryFailedException, InternalLogException;

    void startPeerRecovery(RecoveryDirector director);

    void setPeerRecoverySupported(String recoveryIdentity);

    Set<String> getRecoveryIds();

    void addRecoveryId(String peerRecoveryIdentity);

    void removeRecoveryId(String peerRecoveryIdentity);
}