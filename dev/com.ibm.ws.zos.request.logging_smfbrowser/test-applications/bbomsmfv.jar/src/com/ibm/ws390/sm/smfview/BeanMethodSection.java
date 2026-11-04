/*******************************************************************************
 * Copyright (c) 2001 IBM Corporation and others.
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

//------------------------------------------------------------------------------
/** Data container for SMF data related to a Bean method section. */
public class BeanMethodSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2; // @MD17014 C

    /** Name of the bean method. */
    public String m_methodName;

    /** Private strings added by Hutch */ //@SUa
    private String my1_meth; //@SUa
    private String my2_meth; //@SUa

    /** Number of times the represented bean method was invoked. */
    public int m_numberOfInvocations;

    /** Average response time of the bean method. */
    public int m_averageResponseTime;

    /** Maximum repsonse time of the bean method. */
    public int m_maximumResponseTime;

    /** Transaction policy set for the bean method. */
    public int m_transactionPolicy;

    /** Invocation identity set for the bean method. */
    public int m_invocationIdentity;

    /** Persistance set for the bean method. */
    public int m_persistance;

    /** Roles allowd to invoke the bean method. */
    public String m_roles;

    /** Invocation locale for the bean method. */
    public int m_invocationLocale;

    /** User session policy for the bean method. */
    public int m_userSessionPolicy;

    //12@P0A
    /** Number of times ejbLoad was invoked for the associated bean. */
    public int m_ejbLoadInvocations;

    /** Average load time for the bean. */
    public int m_ejbLoadExecAvg;

    /** Maximum load time for the bean. */
    public int m_ejbLoadExecMax;

    /** Number of store invocation for the bean. */
    public int m_ejbStorInvocations;

    /** Average store time. */
    public int m_ejbStorExecAvg;

    /** Maximum store time. */
    public int m_ejbStorExecMax;

    /** Number of activate invocations for the bean. */
    public int m_ejbActiInvocations;

    /** Average execution time for activate. */
    public int m_ejbActiExecAvg;

    /** Maximum execution time for activate. */
    public int m_ejbActiExecMax;

    /** Number of passivate invocations for the bean. */
    public int m_ejbPassInvocations;

    /** Average execution time for passivate. */
    public int m_ejbPassExecAvg;

    /** Maximum execution time for passivate. */
    public int m_ejbPassExecMax;

    /** Average cpu time */
    public long m_avgCpuTime; //@MD17014 A

    /** Minimum cpu time */
    public long m_minCpuTime; //@MD17014 A

    /** Maximum cpu time */
    public long m_maxCpuTime; //@MD17014 A

    //----------------------------------------------------------------------------
    /**
     * BeanMethodSection constructor from a SmfStream.
     *
     * @param aSmfStream        SmfStream to be used to build this BeanMethodSection
     * @param aRequestedVersion Version as required by the SmfRecord.
     *                              The requested version is currently set in the product section.
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public BeanMethodSection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_methodName = aSmfStream.getString(1024, SmfUtil.UNICODE);

        m_numberOfInvocations = aSmfStream.getInteger(4); //   @L3C

        m_averageResponseTime = aSmfStream.getInteger(4); //   @L3C

        m_maximumResponseTime = aSmfStream.getInteger(4); //   @L3C

        m_transactionPolicy = aSmfStream.getInteger(4); //   @L3C

        m_invocationIdentity = aSmfStream.getInteger(4); //   @L3C

        m_persistance = aSmfStream.getInteger(4); //   @L3C

        m_roles = aSmfStream.getString(512, SmfUtil.UNICODE);

        m_invocationLocale = aSmfStream.getInteger(4); //   @L3C

        m_userSessionPolicy = aSmfStream.getInteger(4); //   @L3C

        //12@P0A
        m_ejbLoadInvocations = aSmfStream.getInteger(4); //   @L3C
        m_ejbLoadExecAvg = aSmfStream.getInteger(4); //   @L3C
        m_ejbLoadExecMax = aSmfStream.getInteger(4); //   @L3C
        m_ejbStorInvocations = aSmfStream.getInteger(4); //   @L3C
        m_ejbStorExecAvg = aSmfStream.getInteger(4); //   @L3C
        m_ejbStorExecMax = aSmfStream.getInteger(4); //   @L3C
        m_ejbActiInvocations = aSmfStream.getInteger(4); //   @L3C
        m_ejbActiExecAvg = aSmfStream.getInteger(4); //   @L3C
        m_ejbActiExecMax = aSmfStream.getInteger(4); //   @L3C
        m_ejbPassInvocations = aSmfStream.getInteger(4); //   @L3C
        m_ejbPassExecAvg = aSmfStream.getInteger(4); //   @L3C
        m_ejbPassExecMax = aSmfStream.getInteger(4); //   @L3C
        // @MD17014 5A
        if (version() >= 2) {
            m_avgCpuTime = aSmfStream.getLong();
            m_minCpuTime = aSmfStream.getLong();
            m_maxCpuTime = aSmfStream.getLong();
        }

    } // BeanMethodSection.BeanMethodSection(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the version supported by this class.
     *
     * @return Version supported by this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // BeanMethodSection.supportedVersion()

    //----------------------------------------------------------------------------
    /**
     * Returns the transaction policy as a string.
     *
     * @return Transaction policy as a string.
     */
    private String transactionPolicyToString() {
        switch (m_transactionPolicy) {
            case 0:
                return "TX_NOT_SUPPORTED";
            case 1:
                return "TX_BEAN_MANAGED";
            case 2:
                return "TX_REQUIRED";
            case 3:
                return "TX_SUPPORTS";
            case 4:
                return "TX_REQUIRES_NEW";
            case 5:
                return "TX_MANDATORY";
            case 6:
                return "TX_NEVER";
            default:
                return "unknown";
        }
    } // BeanMethodSection.transactionPolicyToString()

    //----------------------------------------------------------------------------
    /**
     * Returns the invocation identity as a string.
     *
     * @return Invocation identity as a string.
     */
    private String invocationIdentityToString() {
        switch (m_invocationIdentity) {
            case 0:
                return "Caller";
            case 1:
                return "Server";
            case 2:
                return "Specified";
            default:
                return "unknown";
        }
    } // BeanMethodSection.invocationIdentityToString()

    //----------------------------------------------------------------------------
    /**
     * Returns the persistance as a string.
     *
     * @return Persistance policy as a string.
     */
    private String persistanceToString() {
        switch (m_persistance) {
            case 0:
                return "Caller";
            case 1:
                return "Server";
            case 2:
                return "Specified";
            default:
                return "unknown";
        }
    } // BeanMethodSection.persistanceToString()

    //----------------------------------------------------------------------------
    /**
     * Returns the invocation locale as a string.
     *
     * @return Invocation locale as a string.
     */
    private String invocationLocaleToString() {
        switch (m_invocationLocale) {
            case 0:
                return "Caller";
            case 1:
                return "Server";
            default:;
        }
        return "unknown";
    } // BeanMethodSection.invocationLocaleToString()

    //----------------------------------------------------------------------------
    /**
     * Returns the user session policy as a string.
     *
     * @return User session policy as a string.
     */
    private String userSessionPolicyToString() {
        switch (m_userSessionPolicy) {
            case 0:
                return "Not supported";
            case 1:
                return "Required";
            case 2:
                return "Supports";
            case 3:
                return "Requires new";
            case 4:
                return "Mandatory";
            case 5:
                return "Never";
            case 6:
                return "BeanManaged";
            default:;
        }
        return "unknown";
    } // BeanMethodSection.userSessionPolicyToString()

    //----------------------------------------------------------------------------
    /**
     * Dumps the fields of this object to a print stream.
     *
     * @param aPrintStream       The stream to print to
     * @param aBaseTripletNumber The triplet number of the base item.
     * @param aTripletNumber     The triplet number of this BeanMethodInfo
     */
    public void dump(
                     SmfPrintStream aPrintStream,
                     int aBaseTripletNumber,
                     int aTripletNumber) {

        aPrintStream.println("");
        String tripletId = Integer.toString(aBaseTripletNumber) // @L2C
                           + "." + Integer.toString(aTripletNumber); // @L2C
        aPrintStream.printKeyValue("Triplet #", tripletId); // @L2C
        aPrintStream.printlnKeyValue("Type", "BeanMethodSection"); // @L2C

        aPrintStream.push();

        aPrintStream.printlnKeyValue("MethodName", m_methodName);
        aPrintStream.printlnKeyValue("# Invocations", m_numberOfInvocations);
        aPrintStream.printlnTimeMills("AverageResponseTime", m_averageResponseTime);
        aPrintStream.printlnTimeMills("MaximumResponseTime", m_maximumResponseTime);

        aPrintStream.printlnKeyValueString(
                                           "Transaction Policy ", m_transactionPolicy, transactionPolicyToString());
        aPrintStream.printlnKeyValue(
                                     "Roles              ", m_roles);

        //12@P0A
        aPrintStream.printKeyValue("ejbLoad      #Invocations", m_ejbLoadInvocations);
        aPrintStream.printTimeMills("Average", m_ejbLoadExecAvg);
        aPrintStream.printlnTimeMills("Maximum", m_ejbLoadExecMax);

        aPrintStream.printKeyValue("ejbStore     #Invocations", m_ejbStorInvocations);
        aPrintStream.printTimeMills("Average", m_ejbStorExecAvg);
        aPrintStream.printlnTimeMills("Maximum", m_ejbStorExecMax);

        aPrintStream.printKeyValue("ejbActivate  #Invocations", m_ejbActiInvocations);
        aPrintStream.printTimeMills("Average", m_ejbActiExecAvg);
        aPrintStream.printlnTimeMills("Maximum", m_ejbActiExecMax);

        aPrintStream.printKeyValue("ejbPassivate #Invocations", m_ejbPassInvocations);
        aPrintStream.printTimeMills("Average", m_ejbPassExecAvg);
        aPrintStream.printlnTimeMills("Maximum", m_ejbPassExecMax);
        // @MD17014 5 A
        if (version() >= 2) {
            aPrintStream.printlnKeyValue("Average CPU Time", m_avgCpuTime);
            aPrintStream.printlnKeyValue("Minimum CPU Time", m_minCpuTime);
            aPrintStream.printlnKeyValue("Maximum CPU Time", m_maxCpuTime);
        }

        aPrintStream.pop();

        // Write Method Name, #Invocations & Response Times           //@SUa
        //@SUa
        if (m_numberOfInvocations >= 0) { //@SUa
            PerformanceSummary.writeNewLine(); //@SVa
            PerformanceSummary.writeString("  ", 31); //@SVa PAD
            int offColon = m_methodName.indexOf(":"); //@SUa
            String my_methodName = m_methodName; //@SUa
            if (offColon >= 0) { //@SUa
                my1_meth = m_methodName.substring(0, offColon); //@SUa ****
                my2_meth = m_methodName.substring(offColon + 1, m_methodName.length());

                if (my1_meth.compareTo(my2_meth) == 0) {
                    my_methodName = my1_meth;
                } else {
                    my_methodName = m_methodName;
                }
            } //@SUa
//if (my_methodName.length() > 30)
//	   {PerformanceSummary.writeString(my1_meth, 30);
//    	PerformanceSummary.writeNewLine();
//    	PerformanceSummary.writeString(" ",31);
//    	PerformanceSummary.writeString(":" + my2_meth, 30);
//   }
//else {
            PerformanceSummary.writeString(my_methodName, 30); //@SUa
//  }
            PerformanceSummary.writeInt(m_numberOfInvocations, 5); //@SW9
            PerformanceSummary.writeString(" ", 2); //@SW9
            PerformanceSummary.writeInt(m_averageResponseTime, 7); //@SUa
// PerformanceSummary.writeInt(m_maximumResponseTime, 7); //@SW9
        } //@SUa                                                           //@SUa

        if (m_ejbLoadInvocations > 0) { //@SUa - ejbLoad
            PerformanceSummary.writeNewLine(); //@SVa
            PerformanceSummary.writeString("  ", 31); //@SVa PAD
            PerformanceSummary.writeString(" >ejbLoad", 30); //@SUa
            PerformanceSummary.writeInt(m_ejbLoadInvocations, 5); //@SW9
            PerformanceSummary.writeInt(m_ejbLoadExecAvg, 9); //@SUa
// PerformanceSummary.writeInt(m_ejbLoadExecMax, 7);      //@SW9
        } //@SUa
        if (m_ejbStorInvocations > 0) { //@SUa - ejbPassivate
            PerformanceSummary.writeNewLine(); //@SVa
            PerformanceSummary.writeString("  ", 31); //@SVa PAD
            PerformanceSummary.writeString(" >ejbStore", 30); //@SUa
            PerformanceSummary.writeInt(m_ejbStorInvocations, 5); //@SUa
            PerformanceSummary.writeInt(m_ejbStorExecAvg, 9); //@SUa
// PerformanceSummary.writeInt(m_ejbStorExecMax, 7);        //@SUa
        } //@SUa
        if (m_ejbActiInvocations > 0) { //@SUa - ejbActivate
            PerformanceSummary.writeNewLine(); //@SVa
            PerformanceSummary.writeString("  ", 31); //@SVa PAD
            PerformanceSummary.writeString(" >ejbActivate", 30); //@SUa
            PerformanceSummary.writeInt(m_ejbActiInvocations, 5); //@SW9
            PerformanceSummary.writeInt(m_ejbActiExecAvg, 9); //@SUa
// PerformanceSummary.writeInt(m_ejbActiExecMax, 7);        //@SW9
        } //@SUa
        if (m_ejbPassInvocations > 0) { //@SUa - ejbPassivate
            PerformanceSummary.writeNewLine(); //@SVa
            PerformanceSummary.writeString("  ", 31); //@SVa PAD
            PerformanceSummary.writeString(" >ejbPassivate", 30); //@SUa
            PerformanceSummary.writeInt(m_ejbPassInvocations, 5); //@SW9
            PerformanceSummary.writeInt(m_ejbPassExecAvg, 9); //@SUa
// PerformanceSummary.writeInt(m_ejbPassExecMax, 7);        //@SW9a
        } // endIf                                                   //@SUa

        if (version() >= 2) { //@SW
// PerformanceSummary.writeString(" ", 1); 					//@SW9

            PerformanceSummary.writeLong(m_avgCpuTime, 10); //@SW99
            PerformanceSummary.writeString(" -av", 4); //@SU99
// PerformanceSummary.writeLong(m_maxCpuTime, 9); 			//@SW9
// PerformanceSummary.writeLong(m_minCpuTime, 9); 			//@SW9
        } // endif													//@SUa

    } // BeanMethodSection.dump()

} // BeanMethodSection