/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.apache.cxf.interceptor;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Releases the file set to hold when this interceptor is added to the interceptor chain
 */
public class ReleaseTempFileHoldInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = Logger.getLogger(ReleaseTempFileHoldInterceptor.class.getName());

    public ReleaseTempFileHoldInterceptor() {
        super(Phase.POST_INVOKE);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        // Liberty change begin
        try {
            // Mark attachment to hold for multiple reads if there is any
            AttachmentUtil.releaseTempFileHold(message);
        } catch (IOException e) {
            LOG.warning("Attempt of relasing the hold from temporary file and stream failed!");
        }
        // Liberty change end
    }

}
