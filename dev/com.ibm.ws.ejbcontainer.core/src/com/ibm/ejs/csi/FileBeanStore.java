/*******************************************************************************
 * Copyright (c) 1998, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.csi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.StatefulBeanReaper;
import com.ibm.ejs.container.activator.StatefulSessionActivationStrategy;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.SessionBeanStore;
import com.ibm.websphere.csi.StreamUnavailableException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * A <code>FileBeanStore</code> provides a <code>SessionBeanStore</code>
 * implementation that provides input/output streams to files for
 * reading/writing stateful session bean state. <p>
 */
public class FileBeanStore
                implements SessionBeanStore
{
    private static final TraceComponent tc = Tr.register(FileBeanStore.class, "EJBContainer", "com.ibm.ejs.container.container");
    private static final String CLASS_NAME = FileBeanStore.class.getName();

    /**
     * The prefix for all files. This must be kept in sync with
     * StatefulBeanFileReaper.
     */
    private static final String FILENAME_PREFIX = "BeanId_";

    /**
     * Directory to store/retrieve stateful session bean state from. <p>
     */
    private String passivationDir;

    private String serverName; // LIDB2775-23.4
    private String clusterName; // LIDB2775-23.4

    //LIDB2018-1 begins
    private static class WSFileOutputStream
                    extends FileOutputStream
                    implements PrivilegedAction<Object>
    {
        long beanTimeoutTime;
        File beanFileObj;

        public WSFileOutputStream(File fileObj, long TimeoutTime)
            throws java.io.IOException
        {
            super(fileObj);
            beanFileObj = fileObj;
            beanTimeoutTime = TimeoutTime;
        }

        public void close() throws java.io.IOException
        {
            super.close();
            AccessController.doPrivileged(this); // d651126
        }

        public Object run() // d651126
        {
            beanFileObj.setLastModified(beanTimeoutTime);
            return null;
        }
    }

    // LIDB2018-1 ends

    /**
     * Create a new <code>FileBeanStore</code> instance that reads/writes
     * session bean state to/from the specified directory. <p>
     */
    // LIDB2775-23.4 Begins
    public FileBeanStore(String passivationDir)
    {
        this(passivationDir, null, null);
    }

    public FileBeanStore(String passivationDir, String serverName, String clusterName)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init>", new Object[] { passivationDir, serverName, clusterName });

        if (passivationDir != null && new File(passivationDir).isDirectory())
        {
            this.passivationDir = passivationDir;
        }
        else
        {
            this.passivationDir = null;
            if (passivationDir != null)
            {
                Tr.warning(tc, "PASSIVATION_DIRECTORY_DOES_NOT_EXIST_CNTR0023W",
                           passivationDir); //p111002.3
            }
        }

        this.serverName = serverName;
        this.clusterName = clusterName;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init>");
    }

    // LIDB2775-23.4 Ends

    /**
     * Discard all conversational state that this <code>FileBeanStore</code>
     * might have stored for any stateful session beans. <p>
     */
    // RTC102568
    public void removeAll()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "removeAll");

        FileBeanFilter theFilter = new FileBeanFilter();
        File[] files = new File(passivationDir == null ? "." : passivationDir).listFiles(theFilter);
        if (files != null)
        {
            for (File file : files)
            {
                final String name = file.getName();
                if (name.startsWith(FILENAME_PREFIX))
                {
                    if (file.delete())
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "deleted " + name);
                    }
                    else
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "failed to delete " + name);
                    }
                }
                else
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "skipping " + name);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "removeAll");
    }

    /**
     * Return the File for a file name prefix.
     *
     * @param checkAll true if all files should be checked if a preferred file
     *            name does not exist
     */
    // RTC102568
    private File getStatefulBeanFile(String fileNamePrefix, boolean checkAll)
    {
        File statefulBeanFile = null;

        if (clusterName != null)
        {
            statefulBeanFile = new File(passivationDir, fileNamePrefix + clusterName);
            if (checkAll && statefulBeanFile.exists())
            {
                return statefulBeanFile;
            }
        }

        statefulBeanFile = new File(passivationDir, fileNamePrefix + serverName);
        if (checkAll && statefulBeanFile.exists())
        {
            return statefulBeanFile;
        }

        return new File(passivationDir, fileNamePrefix);
    }

    /**
     * Get input stream for specified key.
     */
    public GZIPInputStream getGZIPInputStream(BeanId beanId)
                    throws CSIException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getGZIPInputStream", beanId);

        final String fileName = getPortableFilename(beanId);
        GZIPInputStream result = null;

        try
        {
            result = (GZIPInputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<GZIPInputStream>() // 126586.1
            {
                public GZIPInputStream run()
                                throws FileNotFoundException, IOException
                {
                    File statefulBeanFile = getStatefulBeanFile(fileName, true);
                    FileInputStream fis = new FileInputStream(statefulBeanFile);
                    return new GZIPInputStream(fis);
                }
            });
        } catch (PrivilegedActionException ex)
        {
            Exception ex2 = ex.getException();
            if (ex2 instanceof FileNotFoundException)
            {
                FFDCFilter.processException(ex2, CLASS_NAME + ".getGZIPInputStream", "91", this);
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(tc, "No file found while trying to activate passivated stateful session bean", fileName);
                throw new StreamUnavailableException("");
            }

            if (ex2 instanceof IOException)
            {
                FFDCFilter.processException(ex2, CLASS_NAME + ".getGZIPInputStream", "98", this);
                Tr.warning(tc, "IOEXCEPTION_READING_FILE_FOR_STATEFUL_SESSION_BEAN_CNTR0024W",
                           new Object[] { fileName, this, ex2 }); //p111002.3
                throw new CSIException("IOException reading input stream for stateful session bean", (IOException) ex2);
            }

            throw new CSIException("Unexpected exception reading input stream for stateful session bean", ex2);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getGZIPInputStream");
        return result;
    }

    private long getBeanTimeoutTime(BeanId beanId)
    {
        if (EJSPlatformHelper.isZOS())
        {
            StatefulSessionActivationStrategy as = (StatefulSessionActivationStrategy) beanId.getActivationStrategy();
            StatefulBeanReaper reaper = as.getReaper();
            return reaper.getBeanTimeoutTime(beanId);
        }

        return 0;
    }

    /**
     * Get object ouput stream suitable for reading persistent state
     * associated with given key.
     */
    public GZIPOutputStream getGZIPOutputStream(BeanId beanId)
                    throws CSIException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getOutputStream", beanId);

        final String fileName = getPortableFilename(beanId);
        final long beanTimeoutTime = getBeanTimeoutTime(beanId);
        GZIPOutputStream result = null;

        try
        {
            result = (GZIPOutputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<GZIPOutputStream>()
            {
                public GZIPOutputStream run()
                                throws IOException
                {
                    File statefulBeanFile = getStatefulBeanFile(fileName, false);
                    FileOutputStream fos = EJSPlatformHelper.isZOS()
                                    ? new WSFileOutputStream(statefulBeanFile, beanTimeoutTime)
                                    : new FileOutputStream(statefulBeanFile);
                    return new GZIPOutputStream(fos); // d651126
                }
            });
        } catch (PrivilegedActionException ex2)
        {
            Exception ex = ex2.getException();
            FFDCFilter.processException(ex, CLASS_NAME + ".getOutputStream", "127", this);
            Tr.warning(tc, "IOEXCEPTION_WRITING_FILE_FOR_STATEFUL_SESSION_BEAN_CNTR0025W",
                       new Object[] { fileName, this, ex }); //p111002.3
            throw new CSIException("Unable to open output stream", ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getOutputStream");
        return result;
    }

    /**
     * Remove any stored representation of conversational state
     * for bean identified by given key. <p>
     */
    public void remove(BeanId beanId)
    {
        final String fileName = getPortableFilename(beanId);
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "remove: key=" + beanId + ", file=" + fileName); //d248470

        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() // 126586
            {
                public Void run()
                                throws Exception
                {
                    File theFile = getStatefulBeanFile(fileName, true);
                    if (isTraceOn && tc.isDebugEnabled()) //d152600
                        Tr.debug(tc, "deleting file: " + theFile.getName() + ", path: " + theFile.getPath());

                    boolean deleted = theFile.delete(); //d152600

                    if (isTraceOn && tc.isDebugEnabled()) //d152600
                        Tr.debug(tc, "File.delete returned: " + deleted);
                    return null;
                }
            });
        } catch (PrivilegedActionException ex)
        {
            FFDCFilter.processException(ex.getException(), CLASS_NAME + ".remove", "150", this);
            if (isTraceOn && tc.isEventEnabled())
                Tr.event(tc, "Failed to remove session bean state", new Object[] { beanId, ex });
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "remove");
    }

    /**
     * Create a "safe" file name given the BeanId. There are a number of
     * characters which can cause problems on different platforms. A safe
     * file name will container only the following characters
     * a-z, A-Z, _, -, .
     * All other characters will get converted to a _
     */
    private String getPortableFilename(BeanId beanId)
    {
        // Historically, this method returned the equivalent of
        // appendPortableFilenameString(beanId.toString()), where BeanId.toString
        // returned "BeanId(" + j2eeName + ", " + pkey + ")", which translates to
        // "BeanId_" + j2eeName + "__" + pkey + "_".                     RTC102568

        StringBuilder result = new StringBuilder();
        result.append(FILENAME_PREFIX);
        appendPortableFilenameString(result, beanId.getJ2EEName().toString());
        result.append("__");
        appendPortableFilenameString(result, beanId.getPrimaryKey().toString());
        result.append('_');
        return result.toString();
    }

    private void appendPortableFilenameString(StringBuilder result, String s)
    {
        int length = s.length(); // PQ48819
        for (int i = 0; i < length; i++)
        {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.')
            {
                result.append(c);
            }
            else
            {
                result.append('_');
            }
        }
    }

    /**
     * LIDB2018-1
     * new method for just dumping in the byte array to a file,
     * byte array in gzip format.
     * 
     * @param key the BeanId for this SFSB
     * 
     * @return an OutputStream
     */
    public OutputStream getOutputStream(BeanId beanId)
                    throws CSIException
    {
        final String fileName = getPortableFilename(beanId);
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getOutputStream: key=" + beanId + ", file=", fileName); //d248740

        final long beanTimeoutTime = getBeanTimeoutTime(beanId);
        FileOutputStream result = null;

        try
        {
            result = (FileOutputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<FileOutputStream>()
            {
                public FileOutputStream run()
                                throws IOException
                {
                    File file = new File(passivationDir, fileName);
                    if (EJSPlatformHelper.isZOS())
                    {
                        return new WSFileOutputStream(file, beanTimeoutTime);
                    }
                    return new FileOutputStream(file); //LIDB2018
                }
            });
        } catch (PrivilegedActionException ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".getUnCompressedOutputStream", "460", this);
            Tr.warning(tc, "IOEXCEPTION_WRITING_FILE_FOR_STATEFUL_SESSION_BEAN_CNTR0025W",
                       new Object[] { fileName, this, ex });
            //p111002.3
            throw new CSIException("Unable to open output stream", ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getOutputStream");
        return result;
    }

    /**
     * LIDB2018-1
     * new method for just reading in the byte array from a file: byte array in gzip format
     * 
     * @param key the BeanId for this SFSB
     * 
     * @return an InputStream
     */
    public InputStream getInputStream(BeanId beanId)
                    throws CSIException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getInputStream", beanId);

        final String fileName = getPortableFilename(beanId);
        FileInputStream result = null;

        try
        {
            result = (FileInputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<FileInputStream>()
            {
                public FileInputStream run() throws IOException
                {
                    return new FileInputStream(new File(passivationDir, fileName));
                }
            });
        } catch (PrivilegedActionException ex)
        {
            //d248470 begin
            Exception ex2 = ex.getException();
            if (ex2 instanceof FileNotFoundException)
            {
                FFDCFilter.processException(ex2, CLASS_NAME + ".getInputStream", "524", this);
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(tc, "No file found while trying to activate passivated stateful session bean", fileName);
                throw new StreamUnavailableException("");
            }

            if (ex2 instanceof IOException)
            {
                FFDCFilter.processException(ex2, CLASS_NAME + ".getInputStream", "531", this);
                Tr.warning(tc, "IOEXCEPTION_READING_FILE_FOR_STATEFUL_SESSION_BEAN_CNTR0024W",
                           new Object[] { fileName, this, ex2 });
                throw new CSIException("IOException reading input stream for stateful session bean", (IOException) ex2);
            }

            throw new CSIException("Unexpected exception reading input stream for stateful session bean", ex2);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getInputStream");
        return result;
    }

    /**
     *  Class used to filter which files should be deleted. Used on the
     *  call to removeAll()
     */
    private class FileBeanFilter implements java.io.FileFilter 
    {

        public boolean accept(java.io.File fileObj) 
        {
            String fileName = fileObj.getName();
            if (fileName.startsWith("BeanId_")) 
            {
                if (fileName.endsWith(serverName) || (clusterName != null && fileName.endsWith(clusterName))) 
                { 
                    return true;
                }
            }
            return false;
        }
    }
}
