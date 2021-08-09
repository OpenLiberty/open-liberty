/*******************************************************************************
 * Copyright (c) 1998, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * ExternalizedBeanId is a subclass of BeanId which is externalizable. BeanId
 * objects wherever created in the container code are substituted at the time
 * of serialization with the externalizable subclass. While deserializing a
 * serialized or externalized BeanId object, the appropriate readExternal or
 * readObject is called by Java Serialization.
 * 
 * Note: This class is no longer used, not since WAS 3.5. It remains in
 * existence for the off chance that someone still has a serialized instance
 * stored persistently.
 */
class ExternalizedBeanId extends BeanId implements Externalizable
{
    private static final long serialVersionUID = -7733353009012455282L;

    private static final TraceComponent tc =
                    Tr.register(ExternalizedBeanId.class, "EJBContainer", "com.ibm.ejs.container.container");//d121558

    //
    // Constants added by AMC to support beanId externalization
    //

    /**
     * IF_HOME - constant value used in externalized form of BeanId if it
     * represents a home.
     */
    private static final byte IF_HOME = 0;

    /**
     * IF_REMOTE - constant value used in externalized form of BeanId if it
     * represents an enterprise bean.
     */
    private static final byte IF_REMOTE = 1;

    /**
     * magic string used to mark start of externalized data
     */
    private static final short EXTERNAL_MAGIC = (short) 0xACAC;

    /**
     * magic string used to mark start of externalized data
     */
    private static final short MAJOR_VERSION = (short) 0x01;

    /**
     * magic string used to mark start of externalized data
     */
    private static final short MINOR_VERSION = (short) 0x01;

    /**
     * public no-arg constructor is required by java serialization when
     * impementing externalize.
     * should only be called by Java serialization....
     * 
     */
    public ExternalizedBeanId() {
        pkey = null;
        _isHome = false;
    }

    /**
     * This constructor provides a migration strategy from BeanIds to
     * to ExternalizedBeanIds. The first time the BeanId is deserialized
     * a call to writeReplace will result with the home not being set.
     * This constructor provides a means of avoiding this problem
     * by avoiding calls to the home
     */
    public ExternalizedBeanId(Serializable pkey,
                              J2EEName j2eeName, //89554
                              boolean isHome) {
        this.pkey = pkey;
        this._isHome = isHome;
        this.j2eeName = j2eeName;//89554
        hashValue = computeHashValue(j2eeName, pkey, _isHome);//89554

    }

    ///////////////////////////////////////////////////////////////////
    //
    // Methods from the Externalizable interface
    // Added by AMC.
    //////////////////////////////////////////////////////////////////

    /**
     * readExternal is called by Java serialization when readObject is
     * called on an ObjectInputStream
     * for an object of class BeanId. It must read the values in the same
     * sequence and with the
     * same types as written by writeExternal.
     */
    public void readExternal(ObjectInput in)
                    throws IOException, ClassNotFoundException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "readExternal");

        try {
            // read magic
            short magic = in.readShort();
            short majorVersion = in.readShort();
            short minorVersion = in.readShort();

            if (!(magic == EXTERNAL_MAGIC)) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc,
                             "Magic Number absent, trying default Java deserialization");
                throw new IOException("Bean ID externalized stream format error");

            } else if (!(majorVersion == MAJOR_VERSION) ||
                       !(minorVersion == MINOR_VERSION)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc,
                             "BeanID Version mismatch", new Object[] { new Short(majorVersion),
                                                                      new Short(minorVersion) });
                throw new IOException("BeanID version mismatch");
            }

            // read the interface type (HOME or REMOTE)
            byte ifType = in.readByte();
            if (ifType == IF_HOME) {
                _isHome = true;
            } else {
                _isHome = false;
            }

            //89554
            // read the bean name
            byte[] j2eeNameBytes = readExternalJ2EEName(in);

            // read the serialized primary key
            pkey = readExternalPKey(in, j2eeNameBytes);

            // TAKE CARE! We don't have a container reference so can't
            // restore transient fields.
            // This is ok as long
            // as rest of container restores BeanIds using the deserialise
            // method. Since readExternal
            // is a new addition
            // this can be no different to the behaviour that would have been
            // observed when using
            // the default serialization.

        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "readExternal");
        }
    }

    //89554
    private byte[] readExternalJ2EEName(ObjectInput in)
                    throws java.io.IOException {

        int nameLength = in.readInt();
        byte[] name = new byte[nameLength];
        //d164415 start
        int bytesRead = 0;
        for (int offset = 0; offset < nameLength; offset += bytesRead)
        {
            bytesRead = in.read(name, offset, nameLength - offset);
            if (bytesRead == -1)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "readExternalJ2EEName encountered end of stream");
                throw new IOException("end of input stream while reading J2EEName");
            }
        }
        //d164415 end

        return name;
    }

    /**
     * Private helper method for readExternal - reads the Serialized
     * primary key.
     * 
     * @author Adrian Colyer
     * @return java.io.Serializable
     * @param in java.io.ObjectInput
     * @exception java.io.IOException The exception description.
     */
    private Serializable readExternalPKey(ObjectInput in, byte[] j2eeNameBytes)
                    throws java.io.IOException, ClassNotFoundException {

        int pkeyLength = in.readInt();
        byte[] pkeyBytes = new byte[pkeyLength];

        //d164415 start
        int bytesRead = 0;
        for (int offset = 0; offset < pkeyLength; offset += bytesRead)
        {
            bytesRead = in.read(pkeyBytes, offset, pkeyLength - offset);
            if (bytesRead == -1)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "readExternalKey encountered end of stream");
                throw new IOException("end of input stream while reading primary key");
            }
        }
        //d164415 end

        //
        // What we are trying to do here is to get the j2eename of the
        // bean in question. The reason we want to do this, we have get
        // the ClassLoader of the bean before we attempt to deserialize
        // the primary key. The container ClassLoader is no longer aware
        // of the bean classes.
        // This path may be traversed on a call to keyToObject. In that
        // situation, the thread's context ClassLoader is of no use. So we
        // have to use some static methods to access the container and get
        // the specific ClassLoader for the bean
        //
        final EJSContainer container = EJSContainer.getDefaultContainer();
        J2EEName j2eename = container.getJ2EENameFactory().create(j2eeNameBytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(pkeyBytes);

        //
        // Use a ObjectInputStream, it can then use the bean's ClassLoader
        // to resolve the primary key class.
        //
        ClassLoader loader = EJSContainer.getClassLoader(j2eename);
        ObjectInputStream pkeyStream =
                        container.getEJBRuntime().createObjectInputStream(bais, loader); // RTC71814
        Serializable key = (Serializable) pkeyStream.readObject();
        return key;
    }

    /**
     * Return string representation of this BeanId. <p>
     */
    @Override
    public String toString() {

        // ACK! Probably want to add a toString to EJSHome
        return "BeanId(" + j2eeName + ", " + pkey + ")";//89554

    } // toString

    /**
     * writeExternal is called by Java serialization when writeObject
     * is called on an ObjectOutputStream
     * for an object of class BeanId.
     */
    public void writeExternal(ObjectOutput out) throws IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "writeExternal");

        try {

            // write magic
            out.writeShort(EXTERNAL_MAGIC);
            out.writeShort(MAJOR_VERSION);
            out.writeShort(MINOR_VERSION);

            // write the interface type (HOME or REMOTE)
            if (_isHome) {
                out.writeByte(IF_HOME);
            } else {
                out.writeByte(IF_REMOTE);
            }

            //89554
            // write the Java EE name
            writeExternalJ2EEName(out);

            // write the external primary key
            writeExternalPKey(out);
            out.flush();

        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "writeExternal");
        }
    }

    //89554
    private void writeExternalJ2EEName(ObjectOutput out)
                    throws java.io.IOException {
        byte[] j2eeNameBytes = j2eeName.getBytes();
        out.writeInt(j2eeNameBytes.length);
        out.write(j2eeNameBytes);
    }

    /**
     * Private helper method for writeExternal - writes the primary key
     * 
     * @author Adrian Colyer
     * @param out java.io.ObjectOutput
     * @exception java.io.IOException The exception description.
     */
    private void writeExternalPKey(ObjectOutput out)
                    throws java.io.IOException {

        // write the serialized primary key - can't mix eternaized stream
        // and writeObject so convert to bytes...
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream pkeyStream = new ObjectOutputStream(baos);
        pkeyStream.writeObject(pkey);
        pkeyStream.flush();
        pkeyStream.close();
        byte[] pkeyBytes = baos.toByteArray();
        out.writeInt(pkeyBytes.length);
        out.write(pkeyBytes);
    }

} // ExternalizedBeanId
