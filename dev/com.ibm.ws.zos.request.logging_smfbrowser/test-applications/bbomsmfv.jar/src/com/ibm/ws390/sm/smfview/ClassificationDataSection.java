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

//  ------------------------------------------------------------------------------
/** Data container for SMF data related to a Smf record product section. */
public class ClassificationDataSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;

    /** Version of the section */
    public int m_version;
    /** data type converted to a string */
    public String dataTypeString = "";
    /** data type of the classification data */
    public int m_dataType;
    /** length of the data */
    public int m_dataLength;
    /** is the data in ASCII? */
    public boolean m_dataIsAscii = false;
    /** charset of the data */
    public String m_dataCharSet = "        ";
    /** the classification data string itself */
    public String m_theData;

    /** For IIOP, the application name data */
    static public final int TypeApplicationName = 1;
    /** For IIOP, the Module name data */
    static public final int TypeModuleName = 2;
    /** For IIOP, the component name data */
    static public final int TypeComponentName = 3;
    /** For IIOP, the class name data */
    static public final int TypeClassName = 4;
    /** For IIOP, the method name data */
    static public final int TypeMethodName = 5;
    /** For HTTP, the URI */
    static public final int TypeURI = 6;
    /** For HTTP, the target host name */
    static public final int TypeHostname = 7;
    /** For HTTP, the target port number */
    static public final int TypePort = 8;
    /** For MDB-A, the Message Listener Port */
    static public final int TypeMLP = 9;
    /** For MDB-A, the Selector */
    static public final int TypeSelector = 10;
    /** For WOLA, the service name */
    static public final int TypeWolaServiceName = 11;
    /** For WOLA, the CICS Transaction name */
    static public final int TypeWolaCICSTranName = 12;

    //----------------------------------------------------------------------------
    /**
     * ClassificationDataSection constructor from a SmfStream.
     *
     * @param aSmfStream SmfStream to be used to build this ClassificationDataSection.
     *                       The requested version is currently set in the Platform Neutral Section
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public ClassificationDataSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);

        m_version = aSmfStream.getInteger(4);

        m_dataType = aSmfStream.getInteger(4);

        m_dataLength = aSmfStream.getInteger(4);

        // Append a user readable type string as a description
        // instead of just the number for the data types
        switch (m_dataType) {
            case 1:
                dataTypeString = "Application Name";
                m_dataIsAscii = false;
                break;
            case 2:
                dataTypeString = "Module Name";
                m_dataIsAscii = false;
                break;
            case 3:
                dataTypeString = "Component Name";
                m_dataIsAscii = false;
                break;
            case 4:
                dataTypeString = "Class Name";
                m_dataIsAscii = false;
                break;
            case 5:
                dataTypeString = "(mangled) Method Name";
                m_dataIsAscii = false;
                break;
            case 6:
                dataTypeString = "URI";
                m_dataIsAscii = false;
                break;
            case 7:
                dataTypeString = "Target Hostname";
                m_dataIsAscii = false;
                break;
            case 8:
                dataTypeString = "Target Port";
                m_dataIsAscii = false;
                break;
            case 9:
                dataTypeString = "Message Listener Port";
                break;
            case 10:
                dataTypeString = "Selector";
                break;
            case 11:
                dataTypeString = "WOLA Service Name";
                break;
            case 12:
                dataTypeString = "WOLA CICS Transaction Name";
                break;
        }

        if (m_dataIsAscii) {
            m_dataCharSet = "ASCII";
            m_theData = aSmfStream.getString(128, SmfUtil.ASCII);
        } else { // Data is EBCDIC
            m_dataCharSet = "EBCDIC";
            m_theData = aSmfStream.getString(128, SmfUtil.EBCDIC);
        }

    } // ClassificationDataSection(..)

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
     * @param aTripletNumber The triplet number of this ClassificationDataSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "ClassificationDataSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("Version    ", m_version);
        aPrintStream.printlnKeyValueString("Data Type  ", m_dataType, dataTypeString);
        aPrintStream.printlnKeyValue("Data Length", m_dataLength);

        aPrintStream.printlnKeyValueString("Data       ", m_theData, m_dataCharSet);

        aPrintStream.pop();

        // Write Classification info to Summary Viewer                      //@SU9
        if (m_dataType == 6) {
            PerformanceSummary.writeString(".9Cl", 4); //@SU9 Just let them know a Classification Section is present
            PerformanceSummary.writeString(m_theData, 13); //@SU99
        } // endif
          // Write Classification info to Summary Viewer (Not Now)  //@SU9
          // if (m_dataType == 6) { 
          //    PerformanceSummary.writeString("\n         .9Cl",14);       //@SU99
          // }                           // Write new Line for URI, & others? @SU99                     
          //    PerformanceSummary.writeString(";",1);                      //@SU99
          // PerformanceSummary.writeString(m_theData,13);                 //@SU99  
          //  ++PerformanceSummary.lineNumber;                // Increment Line #  //@SU9

    } // dump()

} // ClassificationDataSection
