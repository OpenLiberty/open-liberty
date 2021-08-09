/*******************************************************************************
 * Copyright (c) 1997, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.sql.SQLException;
import java.util.Hashtable; 

import javax.resource.ResourceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

/**
 * Collection of misc. utility methods for the J2CCode.
 * 
 * NOTE: This class is NOT part of j2cClient.jar. If you need to add something
 * that is referenced by a class that is in the client jar, consider adding instead
 * to J2CConstants.java
 * 
 */
public final class J2CUtilityClass {
    public final static String _defaultThrowableDelimiter = ":"; 

    private static final TraceComponent tc =
                    Tr.register(
                                J2CUtilityClass.class,
                                J2CConstants.traceSpec,
                                J2CConstants.messageFile); 

    // map of pmiName to component-managed auth alias
    // used to check for possible alias-gone-bade in exception path of MCWrapper.getConnection
    public static final Hashtable<String, Object> pmiNameToCompAlias = new Hashtable<String, Object>(); 

    /**
     * Generates the exception string using the default delimiter.
     * 
     * @see generateExceptionString(Throwable t, String delim)
     */
    public static String generateExceptionString(Throwable t) {
        return generateExceptionString(t, _defaultThrowableDelimiter);
    }

    /**
     * <P> Generates a string which consists of the toStrings() of the provided Exception and any linked or chained
     * exceptions and initial causes.
     * 
     * @param t The first exception in the chain
     * @param delim The character which will be used to deliminate exceptions.
     * 
     * @return a string which includes all the toStrings() of the linked exceptions.
     */
    public static String generateExceptionString(Throwable t, String delim) {

        StringBuffer sb = new StringBuffer();

        if (t != null) {

            sb.append(t.toString());

            Throwable nextThrowable = getNextThrowable(t);

            if (nextThrowable != null) {
                sb.append(delim);
                sb.append(generateExceptionString(getNextThrowable(t), delim));
            }
        }

        return sb.toString();
    }

    /**
     * Finds the next Throwable object from the one provided.
     * 
     * @param t The throwable to start with
     * @return The next or linked, or initial cause.
     */
    public static Throwable getNextThrowable(Throwable t) {

        Throwable nextThrowable = null;
        if (t != null) {

            // try getCause first.
            nextThrowable = t.getCause();

            if (nextThrowable == null) {
                // if getCause returns null, look for the JDBC and JCA specific chained exceptions
                // in case the resource adapter or database has not implemented getCause and initCause yet.
                if (t instanceof SQLException) {
                    nextThrowable = ((SQLException) t).getNextException();
                }

                else if (t instanceof ResourceException) {
                    nextThrowable = ((ResourceException) t).getCause(); 
                }
            }
        }
        return nextThrowable;
    }

    /**
     * The <b>res-resolution-control</b> local transaction property specifies for relational
     * resource adapters the resolution control to be used by the backend database.
     * 
     * @param tm transaction manager
     * @return True if resolution control is "ContainerAtBoundary". False if "Application".
     */
    public static final boolean isContainerAtBoundary(EmbeddableWebSphereTransactionManager tm) {
        boolean isContainerResolved = false; // RESOLVER_APPLICATION - the default
        UOWCurrent uowCurrent = (UOWCurrent) tm;
        UOWCoordinator uowCoord = uowCurrent == null ? null : uowCurrent.getUOWCoord();

        if (uowCoord != null) {
            if (!uowCoord.isGlobal()) { 
                isContainerResolved = ((LocalTransactionCoordinator) uowCoord).isContainerResolved();
            } else { 
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "isContainerAtBoundary unexpectedly called within a global tran and the value is defaulted to APPLICATION");
                }
            } 
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "isContainerAtBoundary=" + isContainerResolved); 

        return isContainerResolved;

    }
}