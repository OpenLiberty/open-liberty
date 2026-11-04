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
/** Data container for SMF data related to a BOSS BO. */
public class ClassSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2;

    /** Name of the BO class. */
    public String m_className;

    /** Number of instances created. */
    public int m_numberOfInstancesCreated;

    /** Number of instances activated. */
    public int m_numberOfInstancesActivated;

    /** Number of instances removed. */
    public int m_numberOfInstancesRemoved;

    /** Number of instances passivated. */
    public int m_numberOfInstancesPassivated;

    /** Number of resource manager reads. */
    public int m_numberOfResourceManagerReads;

    /** Number of resource manager writes. */
    public int m_numberOfResourceManagerWrites;

    /** Number of triplets from the Smf record related to this class. */
    public int m_methodTripletN;

    /** Triplets associated with this set class method invocations. */
    public Triplet[] m_methodTriplets;

    /** Method section describing calls on this class. */
    public MethodSection[] m_methodSections;

    //----------------------------------------------------------------------------
    /**
     * ClassSection constructor from Smf stream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of ClassSection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public ClassSection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_className = aSmfStream.getString(256, SmfUtil.EBCDIC);

        m_numberOfInstancesCreated = aSmfStream.getInteger(4); //    @L3C

        m_numberOfInstancesActivated = aSmfStream.getInteger(4); //    @L3C

        m_numberOfInstancesRemoved = aSmfStream.getInteger(4); //    @L3C

        m_numberOfInstancesPassivated = aSmfStream.getInteger(4); //    @L3C

        m_numberOfResourceManagerReads = aSmfStream.getInteger(4); //    @L3C

        m_numberOfResourceManagerWrites = aSmfStream.getInteger(4); //    @L3C

        m_methodTripletN = aSmfStream.getInteger(4); //    @L3C

        m_methodTriplets = new Triplet[m_methodTripletN];
        for (int cX = 0; cX < m_methodTripletN; ++cX) {
            m_methodTriplets[cX] = new Triplet(aSmfStream);
        }

        m_methodSections = new MethodSection[m_methodTripletN];
        for (int cX = 0; cX < m_methodTripletN; ++cX) {
            m_methodSections[cX] = new MethodSection(aSmfStream, aRequestedVersion);
        }

    } // ClassSection.ClassSection(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // ClassSection.supportedVersion()

    //----------------------------------------------------------------------------
    /**
     * Dump the object to a print stream.
     * 
     * @param aPrintStream   print stream to dump to.
     * @param aTripletNumber number of the triplet
     *                           where this instance of ClassSection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "ClassSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("ClassName", m_className);
        aPrintStream.printKeyValue("Instances: #created", m_numberOfInstancesCreated);
        aPrintStream.printlnKeyValue("#activated", m_numberOfInstancesActivated);
        aPrintStream.printKeyValue("           #removed", m_numberOfInstancesRemoved);
        aPrintStream.printlnKeyValue("#passivated", m_numberOfInstancesPassivated);
        aPrintStream.printKeyValue("ResourceManager: #reads", m_numberOfResourceManagerReads);
        aPrintStream.printlnKeyValue("#writes", m_numberOfResourceManagerWrites);

        // Number of methods
        aPrintStream.println("");
        aPrintStream.printlnKeyValue("#Methods", m_methodTripletN);

        // Method triplets and methods
        for (int mX = 0; mX < m_methodTripletN; ++mX) {
            m_methodTriplets[mX].dump(aPrintStream, aTripletNumber, mX + 1); // @L2C
        }

        for (int mX = 0; mX < m_methodTripletN; ++mX) {
            m_methodSections[mX].dump(aPrintStream, aTripletNumber, mX + 1); // @L2C
        } // for

        aPrintStream.pop();

    } // ClassSection.dump()

} // ClassSection
