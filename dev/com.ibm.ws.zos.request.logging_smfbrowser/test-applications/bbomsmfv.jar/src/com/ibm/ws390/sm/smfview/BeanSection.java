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
/** Data container for SMF data related to a Enterprise Java Bean. */
public class BeanSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2; // @MD17014 C

    /** Full AMC (Application-Module-Container) Name of the bean in plain text. */
    public String m_amcName;

    /** UUID associated with the instance of the EJB. */
    public byte[] m_amcUuid = new byte[60];

    /** type as defined by the EJB spec (mainly SessionBean or Entity Bean). */
    public int m_type;

    /** activation policy customized for the EJB. */
    public int m_activationPolicy;

    /** Load policy custmized for the EJB. */
    public int m_loadPolicy;

    /** Pin policy customized for the EJB. */
    public int m_pinPolicy;

    /** Reentrance policy customized for the EJB. */
    public int m_reentrancePolicy;

    /** Local transaction containmment customized for the EJB. */
    public int m_localTransactionContainment;

    /** Access control customized for the EJB. */
    public int m_accessControl;

    /** Local transaction outcome experienced by this bean. */
    public int m_localTransactionOutcome;

    /** Number of triplets from the Smf record related to this bean. */
    public int m_beanMethodTripletN;

    /** Triplets associated with this set of EJB invocations. */
    public Triplet[] m_beanMethodTriplets;

    /** Bean method sections describing calls on this EJB. */
    public BeanMethodSection[] m_beanMethodSections;

    /**
     * BeanSection constructor from Smf stream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of BeanSection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public BeanSection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_amcName = aSmfStream.getString(512, SmfUtil.UNICODE);

        m_amcUuid = aSmfStream.getByteBuffer(60);

        m_type = aSmfStream.getInteger(4); //    @L3C

        m_activationPolicy = aSmfStream.getInteger(4); //    @L3C

        m_loadPolicy = aSmfStream.getInteger(4); //    @L3C

        m_pinPolicy = aSmfStream.getInteger(4); //    @L3C

        m_reentrancePolicy = aSmfStream.getInteger(4); //    @L3C

        m_localTransactionContainment = aSmfStream.getInteger(4); //    @L3C

        m_accessControl = aSmfStream.getInteger(4); //    @L3C

        m_localTransactionOutcome = aSmfStream.getInteger(4); //    @L3C

        m_beanMethodTripletN = aSmfStream.getInteger(4); //    @L3C

        m_beanMethodTriplets = new Triplet[m_beanMethodTripletN];
        for (int mX = 0; mX < m_beanMethodTripletN; ++mX) {
            m_beanMethodTriplets[mX] = new Triplet(aSmfStream);
        }

        m_beanMethodSections = new BeanMethodSection[m_beanMethodTripletN];
        for (int mX = 0; mX < m_beanMethodTripletN; ++mX) {
            m_beanMethodSections[mX] = new BeanMethodSection(aSmfStream, aRequestedVersion);
        }

    } // BeanSection(...)

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
     * Returns the activity type as a string.
     * 
     * @return activity type as a string.
     */
    private String typeToString() {
        switch (m_type) {
            case 2:
                return "Stateless session bean";
            case 3:
                return "Stateful session bean";
            case 4:
                return "BMP entity bean"; // @414940A
            case 5:
                return "CMP entity bean"; // @414940A
            case 6:
                return "Message-driven bean"; // @414940A
            default:
                return "unknown";
        }
    } // typeToString()

    //----------------------------------------------------------------------------
    /**
     * Returns the activation policy as a string.
     * 
     * @return activationPolicy as a string.
     */
    private String activationPolicyToString() {
        switch (m_activationPolicy) {
            case 0:
                return "Once";
            case 1:
                return "At each session";
            case 2:
                return "At each transaction";
            default:
                return "unknown";
        }
    } // activationPolicyToString()

    //----------------------------------------------------------------------------
    /**
     * Returns the load policy as a string.
     * 
     * @return load policy as a string.
     */
    private String loadPolicyToString() {
        switch (m_loadPolicy) {
            case 0:
                return "At activation";
            case 1:
                return "At transaction";
            default:
                return "unknown";
        }
    } // loadPolicyToString()

    //----------------------------------------------------------------------------
    /**
     * Returns the pin policy as a string.
     * 
     * @return pin policy as a string.
     */
    private String pinPolicyToString() {
        switch (m_pinPolicy) {
            case 0:
                return "For activation period";
            case 1:
                return "For session";
            case 2:
                return "For transaction";
            case 3:
                return "For business method only";
            default:
                return "unknown";
        }
    } // pinPolicyToString()

    //----------------------------------------------------------------------------
    /**
     * Returns the reentrance policy as a string.
     * 
     * @return reentrance policy as a string.
     */
    private String reentrancePolicyToString() {
        switch (m_reentrancePolicy) {
            case 0:
                return "Not reentrant with transaction";
            case 1:
                return "Reentrant within transaction";
            default:
                return "unknown";
        }
    } // reentrancePolicyToString()

    //----------------------------------------------------------------------------
    /**
     * Returns the local transaction containment as a string.
     * 
     * @return local transaction containment as a string.
     */
    private String localTransactionContainmentToString() {
        switch (m_localTransactionContainment) {
            case 0:
                return "Session";
            case 1:
                return "Business method";
            default:
                return "unknown";
        }
    } // localTransactionContainmentToString()

    //----------------------------------------------------------------------------
    /**
     * Returns the access control as a string.
     * 
     * @return access control as a string.
     */
    private String accessControlToString() {
        switch (m_accessControl) {
            case 0:
                return "Container managed";
            case 1:
                return "Bean managed";
            default:
                return "unknown";
        }
    } // accessControlToString()

    //----------------------------------------------------------------------------
    /**
     * Returns the local transaction outcome as a string.
     * 
     * @return local transaction outcome as a string.
     */
    private String localTransactionOutcomeToString() {
        switch (m_localTransactionOutcome) {
            case 0:
                return "Rollback";
            case 1:
                return "Commit";
            default:
                return "unknown";
        }
    } // localTransactionOutcomeToString()

    //----------------------------------------------------------------------------
    /**
     * Dumps the object to a print stream.
     * 
     * @param aPrintStream   print stream to dump to.
     * @param aTripletNumber number of the triplet
     *                           where this instance of BeanSection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "BeanSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("AMCName", m_amcName);
        aPrintStream.printlnKeyValue("AMCUuid", m_amcUuid, null);
        aPrintStream.printlnKeyValueString(
                                           "Type            ", m_type, typeToString());
        aPrintStream.printlnKeyValueString(
                                           "ReentrancePolicy", m_reentrancePolicy, reentrancePolicyToString());

        aPrintStream.println("");
        aPrintStream.printlnKeyValue("# Methods", m_beanMethodTripletN);

        // Write AMC Component (Bean) Name out to summaryReport file.         //@SUa   

        int lengthAMC = m_amcName.length(); //@SUa
        int colon1 = m_amcName.indexOf("::"); //@SUa
        // Following changes get rid of 1st 2 chars truncation.				//@SU502
        String cName = m_amcName; //@SU502
        if (colon1 > 1) /* If no colon-deliminated name, use it */ //@SU502 
        { //@SU502
            colon1 = colon1 + 2; /* increment past 1st double colon to get 2nd */ //@SUa
            String mcName = m_amcName.substring(colon1, lengthAMC); //@SUa
            int colon2 = mcName.indexOf("::"); //@SUa
            colon2 = colon1 + colon2 + 2; /* Get past 2nd set of double :'s */ //@SUa
            cName = m_amcName.substring(colon2, lengthAMC); //@SU502
        } //@SU502
        if (aTripletNumber > 3) {
            PerformanceSummary.writeNewLine(); //@SVa
            PerformanceSummary.writeString("  ", 29); //@SVa PAD
        }
        PerformanceSummary.writeString(" ", 1); //@SUa  (was 19 chars)
        PerformanceSummary.writeString(cName, cName.length()); //@SUa

        // Method triplets and methods
        for (int mX = 0; mX < m_beanMethodTripletN; ++mX) {
            m_beanMethodTriplets[mX].dump(aPrintStream, aTripletNumber, mX + 1); // @L2C
        } // for

        for (int mX = 0; mX < m_beanMethodTripletN; ++mX) {
            m_beanMethodSections[mX].dump(aPrintStream, aTripletNumber, mX + 1); // @L2C
        } // for

        aPrintStream.pop();

    } // dump()

} // BeanSection