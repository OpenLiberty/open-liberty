/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.internal.openj9;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.checkpoint.internal.criu.CheckpointFailedException;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException.Type;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;

public class J9CRIUSupport {

    @FFDCIgnore({ ClassNotFoundException.class, InternalError.class })
    public static ExecuteCRIU create() {
        try {
            Class<?> criuSupport = Class.forName("org.eclipse.openj9.criu.CRIUSupport");
            try {
                Method supported = criuSupport.getDeclaredMethod("isCRIUSupportEnabled");
                if (!(Boolean) supported.invoke(criuSupport)) {
                    return createCRIUNotSupported(Type.UNSUPPORTED_DISABLED_IN_JVM,
                                                  "Error: Must set the jvm option: -XX:+EnableCRIUSupport", null, 0);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException hopefullyVeryRareException) {
                return createCRIUNotSupported(Type.UNSUPPORTED,
                                              "Error: Exception while invoking OpenJ9 method " +
                                                                "org.eclipse.openj9.criu.CRIUSupport.isCRIUSupportEnabled ",
                                              hopefullyVeryRareException, 0);
            }
            // return a fully functional implementation. Yay!
            return new ExecuteCRIU_OpenJ9();
        } catch (ClassNotFoundException e) {
            return createCRIUNotSupported(Type.UNSUPPORTED_IN_JVM, "There is no CRIU support in this JVM.", null, 0);
        } catch (java.lang.InternalError ie) {
            if (ie.getCause() instanceof UnsatisfiedLinkError) {
                return createCRIUNotSupported(Type.UNSUPPORTED_CRIU_NOT_INSTALLED,
                                              "Error: CRIU is not installed on the platform", ie, 0);
            }
            return createCRIUNotSupported(Type.UNSUPPORTED, "An internal error was encountered: " + ie, ie, 0);
        }
    }

    /**
     * Called when CRIU support is not present. Returns a ExecuteCRIU service which throws a variation of
     * CheckpointFailedException from all public methods. The exception can be examined to get more info on why CRIU
     * support is missing,
     *
     * @param type      contains an enum with a more specific error reason.
     * @param msg       exception error message
     * @param cause
     * @param errorCode
     */
    private static ExecuteCRIU createCRIUNotSupported(CheckpointFailedException.Type type,
                                                      String msg,
                                                      Throwable cause,
                                                      int errorCode) {
        final CheckpointFailedException criuSupportException = new CheckpointFailedException(type, msg, cause, errorCode);
        return new ExecuteCRIU() {
            @Override
            public void dump(File imageDir, String logFileName, File workDir) throws CheckpointFailedException {
                throw criuSupportException;
            }

            @Override
            public void checkpointSupported() throws CheckpointFailedException {
                throw criuSupportException;
            }

        };
    }

}
