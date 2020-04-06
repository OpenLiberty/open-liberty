/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.certificateutil.keytool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;

/**
 *
 */
public class KeytoolCommandTest {
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final Process proc = mock.mock(Process.class);
    private final InputStream procIn = mock.mock(InputStream.class);
    private final Sequence readSequence = mock.sequence("readSequence");
    List<String> san = new ArrayList<String>();

    @Test
    public void getCommand_justArguments() {
        san.add("SAN=DNS:localhost");
        KeytoolCommand cmd = new KeytoolCommand("myLoc", "myPwd", 365, "CN=myName", 2048, "RSA", "SHA256withRSA", "PKCS12", san);
        List<String> cmdArgs = cmd.getCommandArgs();
        assertEquals("-genkey", cmdArgs.get(1));
        assertEquals("-keystore", cmdArgs.get(2));
        assertEquals("myLoc", cmdArgs.get(3));
        assertEquals("-storepass", cmdArgs.get(4));
        assertEquals("myPwd", cmdArgs.get(5));
        assertEquals("-keypass", cmdArgs.get(6));
        assertEquals("myPwd", cmdArgs.get(7));
        assertEquals("-validity", cmdArgs.get(8));
        assertEquals(Integer.toString(365), cmdArgs.get(9));
        assertEquals("-dname", cmdArgs.get(10));
        assertEquals("CN=myName", cmdArgs.get(11));
        assertEquals("-alias", cmdArgs.get(12));
        assertEquals(DefaultSSLCertificateCreator.ALIAS, cmdArgs.get(13));
        assertEquals("-sigalg", cmdArgs.get(14));
        assertEquals(DefaultSSLCertificateCreator.SIGALG, cmdArgs.get(15));
        assertEquals("-keyalg", cmdArgs.get(16));
        assertEquals(DefaultSSLCertificateCreator.KEYALG, cmdArgs.get(17));
        assertEquals("-keySize", cmdArgs.get(18));
        assertEquals(Integer.toString(2048), cmdArgs.get(19));
    }

    @Test
    public void getAbsoluteKeytoolPath() {
        san.add("SAN=DNS:localhost");
        KeytoolCommand cmd = new KeytoolCommand("myLoc", "myPwd", 365, "CN=myName", 2048, "RSA", "SHA256withRSA", "PKCS12", san);
        String path = cmd.getAbsoluteKeytoolPath();
        assertTrue("keytool path does not start with this JVM's java.home directory",
                   path.startsWith(System.getProperty("java.home")));
        // If this is a Windows system, expect a .exe to follow
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            assertEquals("The full path to keytool was not what we expecpeted",
                         System.getProperty("java.home") + "/bin/keytool.exe",
                         path);
        } else {
            assertEquals("The full path to keytool was not what we expecpeted",
                         System.getProperty("java.home") + "/bin/keytool",
                         path);
        }
    }

    /**
     * Ensure that there are no passwords displayed by toString and that the
     * format and content are as we expect them to be.
     */
    @Test
    public void test_toString() {
        san.add("SAN=DNS:localhost");
        KeytoolCommand cmd = new KeytoolCommand("myLoc", "myPwd", 365, "CN=myName", 2048, "RSA", "SHA256withRSA", "PKCS12", san);
        assertFalse("The command line contained the password in plain text",
                    cmd.toString().contains("myPwd"));

        StringBuffer cmdArgs = new StringBuffer();
        cmdArgs.append(cmd.getAbsoluteKeytoolPath());
        cmdArgs.append(" ");
        cmdArgs.append("-genkey");
        cmdArgs.append(" ");
        cmdArgs.append("-keystore");
        cmdArgs.append(" ");
        cmdArgs.append("myLoc");
        cmdArgs.append(" ");
        cmdArgs.append("-storepass");
        cmdArgs.append(" ");
        cmdArgs.append("***");
        cmdArgs.append(" ");
        cmdArgs.append("-keypass");
        cmdArgs.append(" ");
        cmdArgs.append("***");
        cmdArgs.append(" ");
        cmdArgs.append("-validity");
        cmdArgs.append(" ");
        cmdArgs.append(Integer.toString(365));
        cmdArgs.append(" ");
        cmdArgs.append("-dname");
        cmdArgs.append(" ");
        cmdArgs.append("CN=myName");
        cmdArgs.append(" ");
        cmdArgs.append("-alias");
        cmdArgs.append(" ");
        cmdArgs.append(DefaultSSLCertificateCreator.ALIAS);
        cmdArgs.append(" ");
        cmdArgs.append("-sigalg");
        cmdArgs.append(" ");
        cmdArgs.append(DefaultSSLCertificateCreator.SIGALG);
        cmdArgs.append(" ");
        cmdArgs.append("-keyalg");
        cmdArgs.append(" ");
        cmdArgs.append(DefaultSSLCertificateCreator.KEYALG);
        cmdArgs.append(" ");
        cmdArgs.append("-keySize");
        cmdArgs.append(" ");
        cmdArgs.append(Integer.toString(DefaultSSLCertificateCreator.DEFAULT_SIZE));
        cmdArgs.append(" ");
        cmdArgs.append("-storetype");
        cmdArgs.append(" ");
        cmdArgs.append("PKCS12");
        cmdArgs.append(" ");
        cmdArgs.append("-ext");
        cmdArgs.append(" ");
        cmdArgs.append("SAN=DNS:localhost");
        assertEquals("The command line was not in the format expected",
                     cmdArgs.toString(),
                     cmd.toString());
    }

    @Test
    public void getProcErrorOutput_readOnceEOF() throws Exception {
        mock.checking(new Expectations() {
            {
                one(proc).getInputStream();
                will(returnValue(procIn));
                one(procIn).read(with(any(byte[].class)));
                inSequence(readSequence);
                will(returnValue(-1));
            }
        });
        san.add("SAN=DNS:localhost");
        KeytoolCommand cmd = new KeytoolCommand("myLoc", "myPwd", 365, "CN=myName", 2048, "RSA", "SHA256withRSA", "PKCS12", san);
        cmd.getProcErrorOutput(proc);

        mock.assertIsSatisfied();
    }

    @Test
    public void getProcErrorOutput_readOnceEmpty() throws Exception {
        mock.checking(new Expectations() {
            {
                one(proc).getInputStream();
                will(returnValue(procIn));
                one(procIn).read(with(any(byte[].class)));
                inSequence(readSequence);
                will(returnValue(0));
            }
        });
        san.add("SAN=DNS:localhost");
        KeytoolCommand cmd = new KeytoolCommand("myLoc", "myPwd", 365, "CN=myName", 2048, "RSA", "SHA256withRSA", "PKCS12", san);
        cmd.getProcErrorOutput(proc);

        mock.assertIsSatisfied();
    }

    @Test
    public void getProcErrorOutput_readOnce() throws Exception {
        mock.checking(new Expectations() {
            {
                one(proc).getInputStream();
                will(returnValue(procIn));
                one(procIn).read(with(any(byte[].class)));
                inSequence(readSequence);
                will(returnValue(50));
            }
        });
        san.add("SAN=DNS:localhost");
        KeytoolCommand cmd = new KeytoolCommand("myLoc", "myPwd", 365, "CN=myName", 2048, "RSA", "SHA256withRSA", "PKCS12", san);
        cmd.getProcErrorOutput(proc);

        mock.assertIsSatisfied();
    }

    @Test
    public void getProcErrorOutput_readOnceFull() throws Exception {
        mock.checking(new Expectations() {
            {
                one(proc).getInputStream();
                will(returnValue(procIn));
                one(procIn).read(with(any(byte[].class)));
                inSequence(readSequence);
                will(returnValue(4096));
                one(procIn).read(with(any(byte[].class)));
                inSequence(readSequence);
                will(returnValue(-1));
            }
        });
        san.add("SAN=DNS:localhost");
        KeytoolCommand cmd = new KeytoolCommand("myLoc", "myPwd", 365, "CN=myName", 2048, "RSA", "SHA256withRSA", "PKCS12", san);
        cmd.getProcErrorOutput(proc);

        mock.assertIsSatisfied();
    }

    @Test
    public void getProcErrorOutput_readTwice() throws Exception {
        mock.checking(new Expectations() {
            {
                one(proc).getInputStream();
                will(returnValue(procIn));
                one(procIn).read(with(any(byte[].class)));
                inSequence(readSequence);
                will(returnValue(4096));
                one(procIn).read(with(any(byte[].class)));
                inSequence(readSequence);
                will(returnValue(50));
            }
        });
        san.add("SAN=DNS:localhost");
        KeytoolCommand cmd = new KeytoolCommand("myLoc", "myPwd", 365, "CN=myName", 2048, "RSA", "SHA256withRSA", "PKCS12", san);
        cmd.getProcErrorOutput(proc);

        mock.assertIsSatisfied();
    }

    @Test
    public void getProcErrorOutput_readTwiceFull() throws Exception {
        mock.checking(new Expectations() {
            {
                one(proc).getInputStream();
                will(returnValue(procIn));
                one(procIn).read(with(any(byte[].class)));
                inSequence(readSequence);
                will(returnValue(4096));
                one(procIn).read(with(any(byte[].class)));
                inSequence(readSequence);
                will(returnValue(4096));
                one(procIn).read(with(any(byte[].class)));
                inSequence(readSequence);
                will(returnValue(-1));
            }
        });
        san.add("SAN=DNS:localhost");
        KeytoolCommand cmd = new KeytoolCommand("myLoc", "myPwd", 365, "CN=myName", 2048, "RSA", "SHA256withRSA", "PKCS12", san);
        cmd.getProcErrorOutput(proc);

        mock.assertIsSatisfied();
    }
}
