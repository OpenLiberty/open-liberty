/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin;

/**
 * Class representing the filestore object.
 *
 */
public interface SIBFileStore extends LWMConfig{

	/**
	 * Get the UUID of the filestore
	 * @return String
	 */
    public String getUuid();

    /**
     * Set the UUID
     * @param value
     */
    public void setUuid(String value);

    /**
     * Get the filestore path
     * @return String
     */
    public String getPath();

    /**
     * Set the filestore path
     * @param path
     * @throws InvalidFileStoreConfigurationException Will be thrown if the path is empth or null
     */
    public void setPath(String path)
                    throws InvalidFileStoreConfigurationException;

    /**
     * Get the log file size of the filestore
     * @return
     */
    public long getLogFileSize();

    /**
     * Set the logfile size of the filestore
     * @param logFileSize
     * @throws InvalidFileStoreConfigurationException
     */
    public void setLogFileSize(long logFileSize) throws InvalidFileStoreConfigurationException;
    
    /**
     * @return the logFileSize
     */
    public long getFileStoreSize() ;

    /**
     * @param logFileSize the logFileSize to set
     */
    public void setFileStoreSize(long fileStoreSize) throws InvalidFileStoreConfigurationException;
    
    /**
     * Get the Minimum permanent filestore size
     * @return long
     */
    public long getMinPermanentFileStoreSize();

    /**
     * Set the Minimum permanent filestore size
     * This method is not functionally supported for liberty, as this property is 
     * computed as half of fileStoreSize specified by user
     * @param minPermanentFileStoreSize
     */
    public void setMinPermanentFileStoreSize(long minPermanentFileStoreSize);

    /**
     * Get the maximum permanent filestore size
     * @return
     */
    public long getMaxPermanentFileStoreSize();

    /**
     * Set the maximum permanent filestore size
     * This method is not functionally supported for liberty, as this property is 
     * computed as half of fileStoreSize specified by user
     * @param maxPermanentFileStoreSize
     */
    public void setMaxPermanentFileStoreSize(long maxPermanentFileStoreSize);

    /**
     * Get the minimum temporary filestore size
     * @return long
     */
    public long getMinTemporaryFileStoreSize();

    /**
     * Set the minimum temporary filestore size
     * This method is not functionally supported for liberty, as this property is 
     * computed as half of fileStoreSize specified by user
     * @param minTemporaryFileStoreSize
     */
    public void setMinTemporaryFileStoreSize(long minTemporaryFileStoreSize);

    /**
     * Get the maximum temporary filestore size
     * @return
     */
    public long getMaxTemporaryFileStoreSize();

    /**
     * Set the maximum temporary filestore size
     * This method is not functionally supported for liberty, as this property is 
     * computed as half of fileStoreSize specified by user
     * @param maxTemporaryFileStoreSize
     */
    public void setMaxTemporaryFileStoreSize(long maxTemporaryFileStoreSize);

    /**
     * Indicates if Unlimited temporary filestore size is set
     * @return boolean
     */
    public boolean isUnlimitedTemporaryStoreSize();

    /**
     * Set unlimitedTemporaryStoreSize attribute
     * @param unlimitedTemporaryStoreSize
     */
    public void setUnlimitedTemporaryStoreSize(
                                               boolean unlimitedTemporaryStoreSize);

    /**
     * Indicates if Unlimited permanent filestore size is set
     * @return
     */
    public boolean isUnlimitedPermanentStoreSize();

    /**
     * Setter for unlimitedPermanentStoreSize
     * @param unlimitedPermanentStoreSize
     */
    public void setUnlimitedPermanentStoreSize(
                                               boolean unlimitedPermanentStoreSize);
      
    /**
     * validates the filestore settings
     * @throws InvalidFileStoreConfigurationException
     * 
     * Rules : 
     * 1) Validate that the logSize is not bigger than the half of the fileStoreSize
     */
    public void validateFileStoreSettings() throws InvalidFileStoreConfigurationException ;

}
