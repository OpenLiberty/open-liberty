package com.ibm.ws.ssl.core;

/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This list keeps track of all configured cryptographic token devices.
 * <p>
 * This class holds all of the configured cryptographic token devices for the
 * process.
 * </p>
 * 
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public final class WSPKCSInKeyStoreList {

    private static final TraceComponent tc = Tr.register(WSPKCSInKeyStoreList.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    private static Vector<WSPKCSInKeyStore> theV = new Vector<WSPKCSInKeyStore>();

    /**
     * Constructor.
     */
    public WSPKCSInKeyStoreList() {
        // do nothing
    }

    /**
     * Insert a new keystore into the list.
     * 
     * @param tokenType
     * @param tokenlib
     * @param tokenPwd
     * @param askeystore
     * @param contextProvider
     * @param pureAcceleration
     * @return WSPKCSInKeyStore
     * @throws Exception
     */
    public synchronized WSPKCSInKeyStore insert(String tokenType, String tokenlib, String tokenPwd, boolean askeystore, String keyStoreProvider) throws Exception {
        // check to see if the library has been initialized already;by comparing
        // the elements in the enumerations
        // perhaps a java 2 sec mgr
        if (tc.isEntryEnabled())
            Tr.entry(tc, "insert", new Object[] { tokenType, tokenlib, keyStoreProvider });

        WSPKCSInKeyStore pKS = insertedAlready(tokenlib);
        boolean already = false;

        // what is inserted already, but not as the askeystore specified. In
        // other words, askeystore indicates keystore, but
        // the pKS was inserted as truststore.

        // looks like we have not inserted anything yet.
        if (pKS == null) {
            pKS = new WSPKCSInKeyStore(tokenlib, keyStoreProvider);
        } else {
            already = true;
        }
        if (askeystore)
            pKS.asKeyStore(tokenType, tokenlib, tokenPwd);
        else
            pKS.asTrustStore(tokenType, tokenlib, tokenPwd);

        if (!already)
            theV.add(pKS);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "insert");
        }
        return pKS;
    }

    /**
     * Access the possible keystore object for the input token library. This
     * returns null if there is no match.
     * 
     * @param tokenlib
     * @return WSPKCSInKeyStore
     */
    public WSPKCSInKeyStore getListElement(String tokenlib) {
        return insertedAlready(tokenlib);
    }

    /**
     * Query whether the input token library matchs a keystore on this list.
     * 
     * @param tokenlib
     * @return boolean
     */
    public boolean contains(String tokenlib) {
        return (null != insertedAlready(tokenlib));
    }

    /**
     * Lookup the keystore object that may exist in the list for the input token
     * library value.
     * 
     * @param tokenlib
     * @return WSPKCSInKeyStore - null if not found
     */
    private WSPKCSInKeyStore insertedAlready(String tokenlib) {
        WSPKCSInKeyStore pKS = null;
        WSPKCSInKeyStore rc = null;
        Enumeration<WSPKCSInKeyStore> e = theV.elements();

        while (null == rc && e.hasMoreElements()) {
            pKS = e.nextElement();

            if (tokenlib.equalsIgnoreCase(pKS.getlibName_key())) {
                rc = pKS;
            } else if (tokenlib.equalsIgnoreCase(pKS.getlibName_trust())) {
                rc = pKS;
            }
        }

        return rc;
    }

    /**
     * Open the input filename as a stream.
     * 
     * @param fileName
     * @return InputStream
     * @throws MalformedURLException
     * @throws IOException
     */
    public InputStream openKeyStore(String fileName) throws MalformedURLException, IOException {
        InputStream fis = null;
        URL urlFile = null;
        File kfile = null;

        try {
            kfile = new File(fileName);
        } catch (NullPointerException e) {
            throw new IOException();
        }

        try {
            if (kfile.exists()) {

                // its a file that is there

                if (kfile.length() == 0) {

                    // the keystore file is empty
                    // debug
                    throw new IOException(fileName);
                }
                // get the url syntax for the fully-qualified filename
                urlFile = new URL("file:" + kfile.getCanonicalPath());
            } else {

                // otherwise, its a url or a file that doesn't exist
                try {
                    urlFile = new URL(fileName);
                } catch (MalformedURLException e) {

                    // not proper url syntax
                    // -or- a file that doesn't exist, we don't know which.

                    // error message
                    throw e;
                }
            }

        } catch (SecurityException e) {
            // error message
            throw new IOException(fileName);
        }

        // Attempt to open the keystore file

        try {
            fis = urlFile.openStream();

        } catch (IOException e) {
            // error message
            throw e;
        }

        return fis;
    }
}
