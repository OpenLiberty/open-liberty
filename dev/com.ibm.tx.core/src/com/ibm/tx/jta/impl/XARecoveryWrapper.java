package com.ibm.tx.jta.impl;
/*******************************************************************************
 * Copyright (c) 2002, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.TMHelper;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.JTA.JTAResource;

public class XARecoveryWrapper implements RecoveryWrapper
{
    private static final TraceComponent tc = Tr.register(XARecoveryWrapper.class,
                                                         TranConstants.TRACE_GROUP,
                                                         TranConstants.NLS_FILE);

    protected static final long serialVersionUID = -4128788408195351556L;

    protected String _xaResFactoryClassName;
    protected Serializable _xaResInfo;
    
    // The XAResourceWrapper Classpath is declared transient
    // as we do not want it to be serialized.
    protected transient String[] _xaResFactoryClasspath;

    // The XAResourceWrapper priority is declared transient
    // as we do not want it to be serialized.  We log it separately
    // in a different recoverable section.
    protected transient int _xaPriority;

    public XARecoveryWrapper(String xaResFactoryClassName, Serializable xaResInfo)
    {
        this(xaResFactoryClassName, xaResInfo, null, JTAResource.DEFAULT_COMMIT_PRIORITY);
    }

    public XARecoveryWrapper(String xaResFactoryClassName, Serializable xaResInfo, String[] xaResFactoryClasspath)
    {
        this(xaResFactoryClassName, xaResInfo, xaResFactoryClasspath, JTAResource.DEFAULT_COMMIT_PRIORITY);
    }

    public XARecoveryWrapper(String xaResFactoryClassName, Serializable xaResInfo, String[] xaResFactoryClasspath, int priority)
    {
        _xaResFactoryClassName = xaResFactoryClassName;
        _xaResInfo             = xaResInfo;
        _xaResFactoryClasspath = canonicalise(xaResFactoryClasspath);
        _xaPriority            = priority;
    }

    public boolean isSameAs(RecoveryWrapper rw)
    {
        if (this == rw) return true;

        if ((rw != null) && (rw instanceof XARecoveryWrapper))
        {
            final XARecoveryWrapper other = (XARecoveryWrapper)rw;

            // Check xaResInfo first as xaResFactoryClassName is same for all CM managed databases
            if (_xaResInfo.equals(other.getXAResourceInfo()))
            {
                if (_xaPriority == other.getPriority())
                {
                    if (_xaResFactoryClassName.equals(other.getXAResourceFactoryClassName()))
                    {
                        String[] thisClasspath = this.getXAResourceFactoryClasspath();
                        String[] otherClasspath = other.getXAResourceFactoryClasspath();

                        // Normalize the classpaths
                        if ((thisClasspath != null) && (thisClasspath.length == 0))
                            thisClasspath = null;
                        if ((otherClasspath != null) && (otherClasspath.length == 0))
                            otherClasspath = null;
                        if (Arrays.equals(thisClasspath, otherClasspath))
                        {
                            return true;
                        }

                        if (tc.isDebugEnabled())
                        {
                            Tr.debug(tc, "XAResFactoryClasspaths differ");
                            // Log the original classpath arrays
                            if (_xaResFactoryClasspath != null)
                            {
                                for (int i = 0; i < _xaResFactoryClasspath.length; i++)
                                {
                                    Tr.debug(tc, "this classpath[" + i + "]=" + _xaResFactoryClasspath[i]);
                                }
                            }

                            if (other._xaResFactoryClasspath != null)
                            {
                                for (int i = 0; i < other._xaResFactoryClasspath.length; i++)
                                {
                                    Tr.debug(tc, "other classpath[" + i + "]=" + other._xaResFactoryClasspath[i]);
                                }
                            }
                        }
                    }
                    else
                    {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "XAResFactoryClassNames differ", new Object[] {_xaResFactoryClassName, other.getXAResourceFactoryClassName()});
                    }
                }
            }
            else
            {
                if (tc.isDebugEnabled()) Tr.debug(tc, "XAResInfos differ", new Object[] {_xaResInfo, other.getXAResourceInfo()});
            }
        }

        return false;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.Transaction.JTA.RecoveryWrapper#serialize()
     */
    public byte[] serialize()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "serialize", this);

        byte[] b = null;
        try
        {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            out.close();
            b = bos.toByteArray();
        }
        catch (IOException e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.Transaction.JTA.XARecoveryWrapper.serialize", "162");
            if (tc.isEventEnabled()) Tr.event(tc, "Unable to serialize an object to the byte stream", e);
            b = null;
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "serialize", b);
        return b;
    }
    
    /*
     * container wraps up an XARecoveryWrapper into a XARecoveryData object suitable
     * for adding to the PartnerLogTable.  This is called at registerResourceInfo time,
     * ie during normally running and not during recovery.
     *
     * The XARecoveryWrapper object will be logged at enlist or prepare.  We perform
     * the serialization step here so we can output a warning if it fails at application
     * startup time rather than later when transactions are running.  We also deserialize
     * the serialized result and see if it compares with the original object.  This is
     * done so that for z/OS where there are multiple SRs accessing the same log, we can
     * check that the same XA RM from each SR will only generate one log record and not
     * multiple ones because of inappropriate local data being logged.
     */
    public PartnerLogData container(FailureScopeController failureScopeController)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "container", failureScopeController);

        final PartnerLogData pld = new XARecoveryData(failureScopeController, this);
        
        //
        // Now try to serialize the wrapper
        //
        byte[] serializedLogData = serialize();

        final String[] classpathArray = _xaResFactoryClasspath;

        if (serializedLogData == null)
        {
            // Output a message now, we will reject future enlists later
            Tr.warning(tc, "WTRN0039_SERIALIZE_FAILED");
            // Create an exceptin to log but dont throw it
            final Exception e = new NotSerializableException("XAResource recovery information failed serialization");
            Tr.audit(tc, "WTRN0045_CANNOT_RECOVER_RESOURCE", new Object[]{_xaResInfo, e});
        }
        else
        {
            // Deserialize the wrapper and check it equals the original wrapper
            // if it does not, then raise an error as we have a problem with the implementation
            // of the resource manager XAResourceInfo - this can cause us problems and
            // multiple partner log entries both on distributed and 390.
            try
            {
                final XARecoveryWrapper wrapper2 = deserialize(serializedLogData);
                if (wrapper2 != null)
                {
                    wrapper2.setXAResourceFactoryClasspath(classpathArray);
                    wrapper2.setPriority(_xaPriority);
                }

                if (!isSameAs(wrapper2))
                {
                    Tr.error(tc, "WTRN0040_OBJECT_DESERIALIZE_FAILED", null);
                    Tr.audit(tc, "WTRN0045_CANNOT_RECOVER_RESOURCE", new Object[]{_xaResInfo, null});
                    serializedLogData = null;

                    if (tc.isDebugEnabled()) Tr.debug(tc, "XAResourceInfo fails equality test");
                }
            }
            catch (Exception e)
            {
                FFDCFilter.processException(e, "com.ibm.ws.Transaction.JTA.XARecoveryWrapper.container", "193");
                Tr.error(tc, "WTRN0040_OBJECT_DESERIALIZE_FAILED", e);
                Tr.audit(tc, "WTRN0045_CANNOT_RECOVER_RESOURCE", new Object[]{_xaResInfo, e});
                if (tc.isDebugEnabled()) Tr.debug(tc, "XAResourceInfo fails deserialization test", e);
                serializedLogData = null;
            }
        }

        if (serializedLogData != null)
        {
            // With the wrapper successfully serialized we must now prepend the
            // xaResInfoClasspath data (if any) delimiting it with byte 0.

            byte[] combinedData = null;

            if (classpathArray != null)
            {
                if (tc.isEventEnabled()) Tr.event(tc, "XAResourceInfo classpath data found. Adding to log data");

                final StringBuffer classpathString = new StringBuffer();

                if (tc.isDebugEnabled()) Tr.event(tc, "Creating String from array elements.");

                for (int i = 0; i < classpathArray.length; i++)
                {
                    if (tc.isDebugEnabled()) Tr.debug(tc, "Element [" + i + "] = " + classpathArray[i]);

                    classpathString.append(classpathArray[i]);
                    classpathString.append(File.pathSeparator);
                }

                if (tc.isDebugEnabled()) Tr.debug(tc, "ResourceInfo classpath", classpathString.toString());

                final byte[] classpathData = classpathString.toString().getBytes();
                combinedData = new byte[classpathData.length + 1 + serializedLogData.length];

                System.arraycopy(classpathData, 0, combinedData, 0, classpathData.length);
                    
                // Delimit the classpath data
                combinedData[classpathData.length] = 0;

                System.arraycopy(serializedLogData, 0, combinedData, classpathData.length + 1, serializedLogData.length);
            }
            else
            {
                combinedData = new byte[1 + serializedLogData.length];
                combinedData[0] = 0;
                    
                System.arraycopy(serializedLogData, 0, combinedData, 1, serializedLogData.length);
            }

            pld.setSerializedLogData(combinedData);
        }
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "container", pld);
        return pld;
    }

    // Deserialize the XARecoveryWrapper from its serialized form 
    // The serializedWrapper byte string does not include a prepended classpath
    // so the deserialized wrapper does not include classpath data

    // Note: this method may be called from "container" in normal running, or
    // directly during recovery by XARecoveryData.deserialize().  This may affect
    // which classloader is passed in and which is on the thread.
    static XARecoveryWrapper deserialize(byte[] serializedWrapper)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "deserialize");

        XARecoveryWrapper wrapper = null;
            
        try
        {
            final ByteArrayInputStream bis = new ByteArrayInputStream(serializedWrapper);
            final ObjectInputStream oin = new ObjectInputStream(bis);
            wrapper = (XARecoveryWrapper) oin.readObject();
            oin.close();
        }
        catch (ClassNotFoundException e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.Transaction.JTA.XARecoveryWrapper.deserialize", "298");
            if (tc.isDebugEnabled()) Tr.debug(tc, "Unable to deserialize an object from byte stream", e);
            String notfound = e.getMessage ();
            final int index = notfound.indexOf (":");
            notfound = notfound.substring (index + 1);
            Tr.error(tc, "WTRN0002_UNABLE_TO_FIND_RESOURCE_CLASS", notfound);
        }
        catch (Throwable t)
        {
            FFDCFilter.processException(t, "com.ibm.ws.Transaction.JTA.XARecoveryWrapper.deserialize", "306");
            if (tc.isDebugEnabled()) Tr.debug(tc, "Unable to deserialize an object from byte stream", t);
            Tr.error(tc, "WTRN0040_OBJECT_DESERIALIZE_FAILED", t);
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "deserialize", wrapper);
        return wrapper;
    }

    public String getXAResourceFactoryClassName()
    {
        return _xaResFactoryClassName;
    }
    
    public Serializable getXAResourceInfo()
    {
        return _xaResInfo;
    }

    // Setter for when we create a wrapper by deserialize as classpath is transient
    public void setXAResourceFactoryClasspath(String[] classpath)
    {
        _xaResFactoryClasspath = classpath;
    }

    public String[] getXAResourceFactoryClasspath()
    {
        return _xaResFactoryClasspath;
    }

    // Setter for when we create a wrapper by deserialize as priority is transient
    public void setPriority(int priority)
    {
        _xaPriority = priority;
    }

    public int getPriority()
    {
        return _xaPriority;
    }

    public String toString()
    {
        String classpath = "";
        if ((_xaResFactoryClasspath != null) && (_xaResFactoryClasspath.length > 0))
        {
            final StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < _xaResFactoryClasspath.length; i++)
            {
                buffer.append(_xaResFactoryClasspath[i]);
                buffer.append(java.io.File.pathSeparator);
            }
            classpath = buffer.toString();
        }
        return _xaResFactoryClassName + ", " + _xaResInfo.toString() + ", " + classpath + ", " + _xaPriority;
    }
    
    /**
     * Canonicalise inbound paths so they can be compared with paths held in the classloaders
     * 
     * @param xaResInfoClasspath
     * @return
     */
    private String[] canonicalise(final String[] xaResInfoClasspath)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "canonicalise", xaResInfoClasspath);

        final String[] result;
        
        if(xaResInfoClasspath != null)
        {
            final ArrayList<String> al = new ArrayList<String>();

            for (final String pathElement : xaResInfoClasspath)
            {
                if(pathElement != null) 
                {
                    String cp;
                    
                    try
                    {
                        cp = (String)TMHelper.runAsSystem(new PrivilegedExceptionAction<String>()
                                {
                                    public String run() throws Exception
                                    {
                                        String path = (new File(pathElement)).getCanonicalPath(); //@D656080A
                                        if (!(new File(path)).exists()) path = null;              //@D656080A
                                        return path;                                              //@D656080C
                                    }
                                }
                        );
                    }
                    catch(Throwable t)
                    {
                        FFDCFilter.processException(t, "com.ibm.ws.Transaction.JTA.XARecoveryWrapper.canonicalise", "512", this);
                        
                        // can't do much else than ....
                        cp = pathElement;
                    }
                    
                    if (cp != null) al.add(cp);  // @D656080C
                }
            }
            
            if (al.size() > 0)                               // @D656080A
            {                                                // @D656080A
                result = al.toArray(new String[al.size()]);
            }                                                // @D656080A
            else                                             // @D656080A
            {                                                // @D656080A
                result = null;                               // @D656080A
            }                                                // @D656080A
        }
        else
        {
            result = null;
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "canonicalise", result);

        return result;
    }
}
