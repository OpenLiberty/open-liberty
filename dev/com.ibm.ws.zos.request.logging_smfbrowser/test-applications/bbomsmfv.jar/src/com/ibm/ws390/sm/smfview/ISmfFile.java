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
/**
 * Interface to make the SmfFile interpreter RMI capable.
 * This enables processing of records on the local system
 * which were read on another system and then transferred
 * by RMI to the local system.
 * Using that procedure enables interactive debug of the
 * Smf record interpreter with the help of PC tools.
 */
public interface ISmfFile extends java.rmi.Remote {

    /**
     * Opens a SmfFile as specified by a name.
     * 
     * @param aSmfFilename name of the Smf file.
     * @throws java.io.IOException      in case of IO errors.
     * @throws java.rmi.RemoteException as all RMI code.
     */
    public void open(String aSmfFilename) throws java.io.IOException, java.rmi.RemoteException;

    /**
     * Close the SmfFile.
     * 
     * @throws java.io.IOException      in case of IO errors.
     * @throws java.rmi.RemoteException as all RMI code.
     */
    public void close() throws java.io.IOException, java.rmi.RemoteException;

    /**
     * Read an array of bytes from the SmfFile.
     * 
     * @return array of bytes.
     * @throws java.io.IOException      in case of IO errors.
     * @throws java.rmi.RemoteException as all RMI code.
     */
    public byte[] read() throws java.io.IOException, java.rmi.RemoteException;

} // ISmfFile
