/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.impl.probe;

import java.lang.reflect.Method;

/**
 * Probe a JVM instance for support of CRIU by calling the java method
 * <code>org.eclipse.openj9.criu.CRIUSupport.isCRIUSupported</code>.
 * <p>
 * Support will potentially be present on on certain IBM JVMs (>=1.8), running on linux platforms with criu installed
 * <p>
 * This class has its own java main() because to function correctly, it must be run in a JVM started with the
 * switch <code>--XX:+EnableCRIUSupport</code>.
 *
 */
public class CriuSupport {

    /**
     * Any data written to STDERR means criu is NOT supported of support could not be determined due to an error.
     * If there is no data written to STDERR then criu is supported.
     *
     * @param args
     */
    public static void main(String[] args) {
        boolean chkPtSupported = false;
        out("Testing for CRIU support");
        try {
            Class<?> criuSupport = Class.forName("org.eclipse.openj9.criu.CRIUSupport");
            out("The class 'org.eclipse.openj9.criu.CRIUSupport' is loaded.");
            try {
                // Check is supported. To be true, JVM must have been started with switch
                // --XX:+EnableCRIUSupport and criu shared libraries must be available to
                // be loaded by JVM
                Method supported = criuSupport.getDeclaredMethod("isCRIUSupportEnabled");
                chkPtSupported = ((Boolean) supported.invoke(criuSupport));
                if (chkPtSupported) {
                    out("isCRIUSupportEnabled() returned " + chkPtSupported);
                } else {
                    err("isCRIUSupportEnabled() returned " + chkPtSupported);
                }
            } catch (Exception e) {
                // An exception here probably means the JDK CRIU API is deprecated or some other incompatible change
                // has occurred. We can't reliably determine if this server should be targeted for criu tests.
                err("Exception invoking method org.eclipse.openj9.criu.CRIUSupport", e);
            }
        } catch (ClassNotFoundException e) {
            // Exception here implies we are running on a JVM lacking criu support
            err("Exception trying to load class org.eclipse.openj9.criu.CRIUSupport.", e);
        } catch (UnsatisfiedLinkError e) {
            //Running on a system where criu is not installed
            err("Unable to load class org.eclipse.openj9.criu.CRIUSupport.", e);
        }
        System.exit(0);
    }

    /**
     * Log to stderr
     *
     * @param string
     */
    private static void err(String string) {
        System.err.println("criuSupport> " + string);
    }

    /**
     * Log to stserr
     *
     * @param string
     * @param throwable exception to log
     */
    private static void err(String string, Throwable throwable) {
        System.err.println("criuSupport> " + string);
        //log the Throwable
        System.err.println(throwable);
        for (StackTraceElement ste : throwable.getStackTrace()) {
            System.err.println("  at " + ste.toString());
        }
    }

    /**
     * Log to stdout.
     *
     * @param string
     */
    private static void out(String string) {
        System.out.println("criuSupport> " + string);
    }

}
