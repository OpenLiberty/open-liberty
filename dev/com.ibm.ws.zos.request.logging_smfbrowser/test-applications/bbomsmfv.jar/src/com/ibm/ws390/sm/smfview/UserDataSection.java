/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.sm.smfview;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;

//  ------------------------------------------------------------------------------
/** Data container for SMF data related to a Smf record product section. */
public class UserDataSection extends SmfEntity {

    /** Supported version of this class. */

    public final static int s_supportedVersion = 1;
    /** Version of this section */
    public int m_version;
    /** data type for this section (less than 65535 reserved for IBM use) */
    public int m_dataType;
    /** length of data in this section */
    public int m_dataLength;
    /** user data itself */
    public byte m_data[];

    private static final boolean debug = Boolean.getBoolean("com.ibm.ws390.sm.smfview.UserDataSection.debug");

    //----------------------------------------------------------------------------
    /**
     * UserDataSection constructor from a SmfStream.
     * 
     * @param aSmfStream SmfStream to be used to build this UserDataSection.
     *                       The requested version is currently set in the Platform Neutral Section
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public UserDataSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);

        m_version = aSmfStream.getInteger(4);

        m_dataType = aSmfStream.getInteger(4);

        m_dataLength = aSmfStream.getInteger(4);

        m_data = aSmfStream.getByteBuffer(2048);

    } // UserDataSection(..)

    /**
     * Copy CTOR, called by custom formatters that extend this class.
     * 
     * @param uds The user data section
     * @throws UnsupportedVersionException  bad version
     * @throws UnsupportedEncodingException bad encoding
     */
    public UserDataSection(UserDataSection uds) throws UnsupportedVersionException, UnsupportedEncodingException {
        super(s_supportedVersion);

        m_version = uds.m_version;
        m_dataType = uds.m_dataType;
        m_dataLength = uds.m_dataLength;
        m_data = uds.m_data;
    }

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // supportedVersion()

    //----------------------------------------------------------------------------
    /**
     * Dumps the fields of this object to a print stream.
     * 
     * @param aPrintStream   The stream to print to.
     * @param aTripletNumber The triplet number of this UserDataSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "UserDataSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("Version     ", m_version);
        aPrintStream.printlnKeyValue("Data Type   ", m_dataType);
        aPrintStream.printlnKeyValue("Data Length ", m_dataLength);
        aPrintStream.printlnKeyValue("Data        ", m_data, null);

        aPrintStream.pop();

        PerformanceSummary.writeString(".9Usr", 5); //@SU9 Just let them know a User Section is present
        // Write User data to Summary Report		                      //@SU9   
        //PerformanceSummary.writeString("\n          9Usr: ", 60);     //@SU9 
        //PerformanceSummary.writeInt(m_dataType, 2);                   //@SU9 
        //String m_dataString = new String(m_data);                     //@SU9
        //PerformanceSummary.writeString(m_dataString, 60);             //@SU9 

    } // dump()

    /**
     * Attempt to load a formatter for the given UserDataSection type.
     * <p>
     * Custom formatter classes are named using the following pattern:
     * com.ibm.ws390.smf.formatters.SMFType120SubType9UserDataTypexxx.class
     * where xxx is the user data type value, in decimal.
     * <p>
     * The custom formatter class must:
     * <br>(1) extend com.ibm.ws390.sm.smfview.UserDataSection
     * <br>(2) define a CTOR that takes a com.ibm.ws390.sm.smfview.UserDataSection object
     * as its only argument. (Note: It is highly recommended that this CTOR call
     * super(UserDataSection);)
     * 
     * @param aSmfStream    SmfStream to be used to build this UserDataSection.
     * @param recordSubType the 120 subtype we're adding user data to (e.g. 9)
     * @return UserDataSection formatter
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public static UserDataSection loadUserDataFormatter(SmfStream aSmfStream, int recordSubType) throws UnsupportedVersionException, UnsupportedEncodingException {
        UserDataSection uds = new UserDataSection(aSmfStream);

        int type = 120;
        int subtype = recordSubType;
        int udstype = uds.m_dataType;
        String formatterPackage = "com.ibm.ws390.smf.formatters.";
        String newClassName = formatterPackage + "SMFType" + type + "SubType" + subtype + "UserDataType" + udstype;

        UserDataSection newUds = uds; // initialize with base UDS object

        try {
            // Take a shot at finding a class that implements the right UserData type.
            // If it blows up in any way, just return the base UserDataSection object,
            // which will format the raw data.
            Class parameterTypes[] = new Class[] { UserDataSection.class };
            Class newClass = Class.forName(newClassName);
            Constructor ctor = newClass.getConstructor(parameterTypes);
            Object parms[] = new Object[] { uds };
            newUds = (UserDataSection) ctor.newInstance(parms);
        } catch (Throwable t) // just catch everything
        {
            if (debug) {
                System.err.println("Failed to load class " + newClassName + " due to exception: " + t);
                t.printStackTrace();
            }
        }

        return newUds;
    }

} // UserDataSection
