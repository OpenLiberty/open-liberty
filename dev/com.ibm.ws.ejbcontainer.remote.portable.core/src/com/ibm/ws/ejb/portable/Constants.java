/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejb.portable;

/**
 * Provides a single definition of constants used by the classes
 * in the com.ibm.ws.ejb.portable java package.
 */
public interface Constants
{
    /**
     * Length of eye catcher constants.
     */
    int EYE_CATCHER_LENGTH = 4;

    /**
     * Eye catcher constants
     */
    byte HANDLE_EYE_CATCHER[] = { 'W', 'S', 'E', 'H' };
    byte HOME_HANDLE_EYE_CATCHER[] = { 'W', 'S', 'H', 'H' };
    byte EJB_META_DATA_EYE_CATCHER[] = { 'W', 'S', 'M', 'D' };
    byte FINDER_COLLECTION_EYE_CATCHER[] = { 'W', 'S', 'F', 'C' };
    byte FINDER_ENUMERATION_EYE_CATCHER[] = { 'W', 'S', 'F', 'E' };
    byte FINDER_ENUMERATOR_EYE_CATCHER[] = { 'W', 'S', 'R', 'E' };
    byte FINDER_RESULT_CLIENT_BASE_EYE_CATCHER[] = { 'W', 'S', 'C', 'B' };
    byte FINDER_RESULT_CLIENT_ENUMERATION_EYE_CATCHER[] = { 'W', 'S', 'C', 'E' };
    byte FINDER_RESULT_CLIENT_ITERATOR_EYE_CATCHER[] = { 'W', 'S', 'C', 'I' };
    byte FINDER_RESULT_CLIENT_COLLECTION_EYE_CATCHER[] = { 'W', 'S', 'C', 'C' };
    byte TIMER_HANDLE_EYE_CATCHER[] = { 'W', 'S', 'T', 'H' }; // LI2281.07
    byte TIMER_TASK_EYE_CATCHER[] = { 'W', 'S', 'T', 'T' }; // LI2281.11
    byte CLIENT_ASYNC_RESULT_EYE_CATCHER[] = { 'W', 'S', 'C', 'A' }; // F743-4593

    /**
     * The following constants are used to indicate which
     * websphere platform an object originated from.
     */
    short PLATFORM_DISTRIBUTED = 1; // distributed platform
    short PLATFORM_ZOS = 2; // z/OS platform

    /**
     * The following constants are for the version ID's of
     * portable implementation for EJBMetaData interface. Version ID
     * indicates the format of the serialized output stream. Version
     * changes only when format of output stream changes.
     */
    short EJBMETADATA_V1 = 1; // 1st portable implementation.

    /**
     * The following constants are for the version ID's of the portable
     * implementation of Handle interface. Version ID indicates the format
     * format of the serialized output stream. Version changes
     * only when format of output stream changes.
     * <p>
     * Note, a "robust" handle is defined to be either one that contains an
     * indirect IOR for the EJBObject or a direct IOR is used and a
     * fixed TCPIP address and port is used for the application server.
     */
    short HANDLE_V1 = 1; // Used for Entity bean only. EJBObject is written
                         // to output stream followed by additional data
                         // since a non-robust IOR is used to refer to Entity bean.

    short HANDLE_V2 = 2; // Used for either a robust Handle to a Entity bean
                         // or to a Session bean (which does not need to be robust).

    /**
     * The following constants are for the version ID's of
     * implementation for HomeHandle interface. Version ID indicates
     * the format of the serialized output stream. Version changes
     * only when format of output stream changes.
     * <p>
     * Note, a "robust" handle is defined to be either one that contains an
     * indirect IOR for the EJBHome or a direct IOR is used and a
     * fixed TCPIP address and port is used for the application server.
     */
    short HOME_HANDLE_V1 = 1; // A non-robust handle to EJBHome object.  EJBHome
                              // is written to output stream followed by
                              // additional data since non-robust IOR is used.

    short HOME_HANDLE_V2 = 2; // A robust handle to EJBHome object.  Only the
                              // EJBHome object is written to output stream.

    /**
     * Version ID for finder related objects. Version ID indicates
     * the format of the serialized output stream. Version changes
     * only when format of output stream changes.
     */
    short FINDER_ENUMERATION_V1 = 1; // 1st version

    short FINDER_ENUMERATOR_V1 = 1; // 1st version

    short FINDER_COLLECTION_V1 = 1; // 1st version
    short FINDER_RESULT_CLIENT_BASE_V1 = 1;
    short FINDER_RESULT_CLIENT_ENUMERATION_V1 = 1;
    short FINDER_RESULT_CLIENT_COLLECTION_V1 = 1;
    short FINDER_RESULT_CLIENT_ITERATOR_V1 = 1;//113380.7

    /**
     * The following constants are for the version ID's of portable
     * implementations for the TimerHandle interface. Version ID
     * indicates the format of the serialized output stream. Version
     * changes only when format of output stream changes.
     */
    // LI2281.07
    short TIMER_HANDLE_V1 = 1; // 1st portable implementation.

    /**
     * The following constants are for the version ID's of portable
     * implementations for the TimerTaskHandler interface. Version ID
     * indicates the format of the serialized output stream. Version
     * changes only when format of output stream changes.
     */
    // LI2281.11
    short TIMER_TASK_V1 = 1; // 1st portable implementation.

    // A timer with a ScheduleExpression.
    short TIMER_TASK_V2 = 2; // F743-500

    /**
     * Version ID for client-side asynchronous results. Version ID
     * indicates the format of the serialized output stream. Version
     * changes only when format of output stream changes.
     */
    short CLIENT_ASYNC_RESULT_V1 = 1; // F743-4593

}
