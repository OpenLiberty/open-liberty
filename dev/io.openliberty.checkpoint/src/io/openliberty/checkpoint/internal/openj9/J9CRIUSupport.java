/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.checkpoint.internal.openj9;

import java.io.File;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.checkpoint.internal.CheckpointImpl;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException.Type;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;

public class J9CRIUSupport {

    @FFDCIgnore(ClassNotFoundException.class)
    public static ExecuteCRIU create(CheckpointImpl checkpointImpl) {
        try {
            Class.forName("org.eclipse.openj9.criu.CRIUSupport");
            // return a fully functional implementation. Yay!
            return new ExecuteCRIU_OpenJ9(checkpointImpl);
        } catch (ClassNotFoundException e) {
            return createCRIUNotSupported(Type.UNSUPPORTED_IN_JVM, checkpointImpl.getMessage("CHECKPOINT_FAILED_UNSUPPORTED_JVM_CWWKC0459E"), e);
        }
    }

    /**
     * Called when CRIU support is not present. Returns a ExecuteCRIU service which throws a variation of
     * CheckpointFailedException from all public methods. The exception can be examined to get more info on why CRIU
     * support is missing,
     *
     * @param type contains an enum with a more specific error reason.
     * @param msg  exception error message
     */
    private static ExecuteCRIU createCRIUNotSupported(CheckpointFailedException.Type type,
                                                      String msg,
                                                      Throwable cause) {
        final CheckpointFailedException criuSupportException = new CheckpointFailedException(type, msg, null);
        return new ExecuteCRIU() {
            @Override
            public void dump(Runnable prepare, Runnable restore, File imageDir, String logFileName, File workDir, File envProps,
                             boolean unprivileged) throws CheckpointFailedException {
                throw criuSupportException;
            }

            @Override
            public void checkpointSupported() throws CheckpointFailedException {
                throw criuSupportException;
            }

        };
    }

}
