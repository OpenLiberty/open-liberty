/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.zos.command.processing;

import java.util.List;

/**
 *
 * Command Responses interface.
 *
 */
public interface CommandResponses {
    /**
     * Get a list of responses for a command.
     *
     * @param command   The command.
     * @param responses A list that the responses get added to.
     * @return A return code. non zero indicates an error happened.
     */
    public int getCommandResponses(String command, List<String> responses);
}