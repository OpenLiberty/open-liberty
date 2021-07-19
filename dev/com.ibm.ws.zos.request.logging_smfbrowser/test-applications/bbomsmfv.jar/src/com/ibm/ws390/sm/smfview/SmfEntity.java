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

//------------------------------------------------------------------------------
/** Base class for versioned SMF entities. */
public abstract class SmfEntity {

    /** Version of this instance of SmfEntity. */
    private int m_version = 0;

    //----------------------------------------------------------------------------
    /**
     * Creates an instance of SmfEntity with the specified version.
     * 
     * @param aVersion Version of the entity as requested from the SmfRecord.
     * @throws UnsupportedVersionException Exception thrown when requested version does is higher than supported version.
     */
    public SmfEntity(int aVersion) throws UnsupportedVersionException {

        setVersion(aVersion);

    } // SmfEntity()

    //----------------------------------------------------------------------------
    /**
     * Returns the version of this SmfEntity.
     * 
     * @return Version of this SmfEntity.
     */
    public int version() {

        return m_version;

    } // version()

    //----------------------------------------------------------------------------
    /**
     * Returns the version supported by the implementation of SmfEntity as provided by a derived class.
     * 
     * @return version supported by the implementation of SmfEntity as given by a derived class.
     */
    public abstract int supportedVersion();

    //----------------------------------------------------------------------------
    /**
     * Sets the version as requested by the SmfRecord.
     * 
     * @param aVersion Version as requested for this instance of SmfEntity.
     * @throws UnsupportedVersionException Exception thrown when the requested version is not supported.
     */
    protected void setVersion(int aVersion) throws UnsupportedVersionException {

        int supportedVersion = supportedVersion();

        if (aVersion > supportedVersion) {
            throw new UnsupportedVersionException(getClass().getName(), supportedVersion, aVersion);
        }
        m_version = aVersion;

    } // setRecordVersion(...)

} // SmfEntity