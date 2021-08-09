/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.resources.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.Test;

import com.ibm.ws.repository.common.utils.internal.HashUtils;

/**
 *
 */
public class HashUtilsMultiThreadedTest {

    private static final String SHA256 = "SHA-256";
    private static final String MD5 = "MD5";

    /**
     * Calculate the SHA256 hash of all files in a directory using multithreading
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void hashMultiFilesSHA256() throws IOException, InterruptedException {
        System.out.println("============ hashMultiFilesSHA256");
        hashMultiFiles(SHA256);
    }

    /**
     * Calculate the MD5 hash of all files in a directory using multithreading
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void hashMultiFilesMD5() throws IOException, InterruptedException {
        System.out.println("============ hashMultiFilesMD5");
        hashMultiFiles(MD5);
    }

    /**
     * Test that a directory of files can be hashed in parallel
     *
     * @param hashType - SHA256 or MD5
     * @throws InterruptedException
     */
    private void hashMultiFiles(final String hashType) throws IOException, InterruptedException {
        final File dir = new File("lib/LibertyFATTestFiles");
        final Hashtable<String, String> singleThread = new Hashtable<String, String>();
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isFile()) {
                if (hashType.equals(SHA256)) {
                    singleThread.put(f.getAbsolutePath(), HashUtils.getFileSHA256String(f));
                } else if (hashType.equals(MD5)) {
                    singleThread.put(f.getAbsolutePath(), HashUtils.getFileMD5String(f));
                } else {
                    fail("invalid hash type requested: " + hashType);
                }

            }
        }
        ArrayList<Thread> threads = new ArrayList<Thread>();
        for (final File f : files) {
            if (f.isFile()) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String single = singleThread.get(f.getAbsolutePath());
                            String multi = null;

                            if (hashType.equals(SHA256)) {
                                multi = HashUtils.getFileSHA256String(f);
                            } else if (hashType.equals(MD5)) {
                                multi = HashUtils.getFileMD5String(f);
                            } else {
                                fail("invalid hash type requested: " + hashType);
                            }

                            assertEquals(hashType + " hashcode produced for " + f.getName() + " does not match", multi, single);
                            System.out.println(f + " ok");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                Thread t = new Thread(runnable);
                threads.add(t);
                t.start();
            }
        }
        // Join the threads so that the output completes before the next test starts
        for (Thread t : threads) {
            t.join();
        }
    }

}
