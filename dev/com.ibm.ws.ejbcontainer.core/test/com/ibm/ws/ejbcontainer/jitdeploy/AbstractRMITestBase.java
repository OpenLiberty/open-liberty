/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Assume;

public abstract class AbstractRMITestBase
{
    static
    {
        TestUtilDelegateImpl.initialize();
    }

    protected static int parseRMICCompatible(String name)
    {
        int value = com.ibm.wsspi.ejbcontainer.JITDeploy.parseRMICCompatible(name);
        if (value == RMIC_COMPATIBLE_NONE)
        {
            throw new IllegalArgumentException(name);
        }
        return value;
    }

    protected static String JAVA_RUNTIME_VERSION = System.getProperty("java.runtime.version");
    protected static final boolean JAVA_VENDOR_IBM = System.getProperty("java.vendor").contains("IBM");

    protected static final int RMIC_COMPATIBLE_NONE = com.ibm.wsspi.ejbcontainer.JITDeploy.parseRMICCompatible("none");
    protected static final int RMIC_COMPATIBLE_ALL = parseRMICCompatible("all");

    protected static final int[] JITDEPLOY_RMIC_COMPATIBLE = {
                                                              RMIC_COMPATIBLE_NONE,
                                                              parseRMICCompatible("values"),
                                                              parseRMICCompatible("exceptions"),
                                                              RMIC_COMPATIBLE_ALL,
    };

    protected static final int[] RMIC_RMIC_COMPATIBLE = { RMIC_COMPATIBLE_ALL };

    private static File RMIC = getRMIC();

    private static File getRMIC()
    {
        File jdkBinDir = new File(System.getProperty("java.home") + "/../bin/");

        File rmicFile = new File(jdkBinDir, "rmic");
        if (!rmicFile.exists())
        {
            rmicFile = new File(jdkBinDir, "rmic.exe");
            if (!rmicFile.exists())
            {
                return null;
            }
        }

        if (!rmicFile.canExecute())
        {
            return null;
        }

        return rmicFile;
    }

    protected static boolean isRMICEnabled()
    {
        return RMIC != null;
    }

    public static byte[] getRMICBytes(Class<?> inputClass, String outputClassName)
    {
        Assume.assumeTrue(isRMICEnabled());

        URL url = inputClass.getProtectionDomain().getCodeSource().getLocation();

        URI classesURI;
        try
        {
            classesURI = url.toURI();
        } catch (URISyntaxException ex)
        {
            throw new Error(ex);
        }

        File classesDir = new File(classesURI);

        File inputFile = new File(classesDir, inputClass.getName().replace('.', '/') + ".class");
        if (!inputFile.exists())
        {
            throw new Error(inputFile.toString());
        }

        File outputFile = new File(classesDir, outputClassName.replace('.', '/') + ".class");
        File versionOutputFile = new File(outputFile.getAbsolutePath() + ".jrv");
        if (!outputFile.exists() ||
            outputFile.lastModified() < inputFile.lastModified() ||
            !JAVA_RUNTIME_VERSION.equals(readLine(versionOutputFile)))
        {
            // rmic won't overwrite up-to-date .class files, which we want if
            // we've switched to a new Java.
            Assert.assertTrue(outputFile.delete() || !outputFile.exists());

            exec(new String[] { RMIC.toString(),
                               "-iiop",
                               "-keep",
                               "-g",
                               "-d",
                               classesDir.getAbsolutePath(),
                               "-classpath",
                               classesDir.getAbsolutePath() + File.pathSeparator + System.getProperty("java.class.path"),
                               inputClass.getName() });
            if (!outputFile.exists())
            {
                throw new Error(outputFile.toString());
            }

            writeLine(versionOutputFile, JAVA_RUNTIME_VERSION);
        }

        InputStream input = null;
        try
        {
            input = new FileInputStream(outputFile);
            input = new BufferedInputStream(input);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int c; (c = input.read()) != -1;)
            {
                baos.write(c);
            }

            return baos.toByteArray();
        } catch (IOException ex)
        {
            throw new Error(ex);
        } finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                } catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static String readLine(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            return reader.readLine();
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            throw new IOError(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new IOError(e);
                }
            }
        }
    }

    private static void writeLine(File file, String line) {
        PrintStream out;
        try {
            out = new PrintStream(new FileOutputStream(file), true, "UTF-8");
            out.println(line);
            out.close();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static void exec(String[] command)
    {
        try
        {
            Process process = Runtime.getRuntime().exec(command);

            Thread stdoutThread = new Thread(new StreamCopier(process.getInputStream(), System.out));
            Thread stderrThread = new Thread(new StreamCopier(process.getErrorStream(), System.err));

            stdoutThread.start();
            stderrThread.start();

            stdoutThread.join();
            stderrThread.join();

            process.waitFor();

            if (process.exitValue() != 0)
            {
                throw new Error("exit=" + process.exitValue() + "; " + Arrays.asList(command));
            }
        } catch (IOException ex)
        {
            throw new IOError(ex);
        } catch (InterruptedException ex)
        {
            throw new Error(ex);
        }
    }

    private static class StreamCopier
                    implements Runnable
    {
        private final InputStream ivInput;
        private final OutputStream ivOutput;

        StreamCopier(InputStream input, OutputStream output)
        {
            ivInput = new BufferedInputStream(input);
            ivOutput = output;
        }

        @Override
        public void run()
        {
            try
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(ivInput));
                PrintWriter writer = new PrintWriter(ivOutput, true);
                for (String line; (line = reader.readLine()) != null;)
                {
                    writer.println(line);
                }
            } catch (IOException ex)
            {
                throw new IOError(ex);
            }
        }
    }

    protected static class TestClassLoader
                    extends ClassLoader
    {
        TestClassLoader()
        {
            super(TestClassLoader.class.getClassLoader());
        }

        public Class<?> defineClass(String name, byte[] bytes)
        {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    protected boolean isRMIC() {
        return false;
    }

    protected abstract int[] getRMICCompatible();

    private static class WriteMethodCall
                    implements TestMethodCall
    {
        private final String ivMethodName;
        private final Object ivValue;

        WriteMethodCall(String methodName, Object value)
        {
            ivMethodName = methodName;
            ivValue = value;
        }

        @Override
        public Object invoke(String methodName, Object[] args)
        {
            Assert.assertEquals(ivMethodName, methodName);
            Assert.assertEquals(ivValue, args[0]);
            return null;
        }
    }

    private static class ReadMethodCall
                    implements TestMethodCall
    {
        private final String ivMethodName;
        private final Object ivValue;

        ReadMethodCall(String methodName, Object value)
        {
            ivMethodName = methodName;
            ivValue = value;
        }

        @Override
        public Object invoke(String methodName, Object[] args)
        {
            Assert.assertEquals(ivMethodName, methodName);
            return ivValue;
        }
    }

    protected boolean isKeywordOperationMangled() {
        // rmic from IBM JRE does not mangle method names that are keywords, but
        // rmic from HotSpot does.  JITDeploy is compatible with the IBM JRE.
        return isRMIC() && !JAVA_VENDOR_IBM;
    }

    protected TestMethodCall read(String methodName, Object value)
    {
        return new ReadMethodCall("read_" + methodName, value);
    }

    protected TestMethodCall utilRead(String delegateMethodName, String methodName, Object value)
    {
        return new ReadMethodCall("read" + delegateMethodName + "+read_" + methodName, value);
    }

    protected TestMethodCall write(String methodName, Object value)
    {
        return new WriteMethodCall("write_" + methodName, value);
    }

    protected TestMethodCall utilWrite(String delegateMethodName, String methodName, Object value)
    {
        return new WriteMethodCall("write" + delegateMethodName + "+write_" + methodName, value);
    }
}
