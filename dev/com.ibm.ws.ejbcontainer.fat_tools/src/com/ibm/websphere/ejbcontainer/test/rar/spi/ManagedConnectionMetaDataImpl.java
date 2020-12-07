// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested 
// of its trade secrets, irrespective of what has been deposited with the 
// U.S. Copyright Office.
//
// Change Log:
//  Date       pgmr       reason   Description
//  --------   -------    ------   ---------------------------------
//  01/07/03   jitang	  d155877  create
//  03/10/03   jitang     d159967  Fix some java doc problem
//  ----------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.spi;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;

/**
 * Implementation class for ManagedConnectionMetaData.
 */
public class ManagedConnectionMetaDataImpl implements ManagedConnectionMetaData {
    /**
     * @see javax.resource.spi.ManagedConnectionMetaData#getEISProductName()
     */
    @Override
    public String getEISProductName() throws ResourceException {
        return null;
    }

    /**
     * @see javax.resource.spi.ManagedConnectionMetaData#getEISProductVersion()
     */
    @Override
    public String getEISProductVersion() throws ResourceException {
        return null;
    }

    /**
     * @see javax.resource.spi.ManagedConnectionMetaData#getMaxConnections()
     */
    @Override
    public int getMaxConnections() throws ResourceException {
        return 0;
    }

    /**
     * @see javax.resource.spi.ManagedConnectionMetaData#getUserName()
     */
    @Override
    public String getUserName() throws ResourceException {
        return null;
    }
}