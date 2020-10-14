/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;                                   /* @F003691C*/

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;

import com.ibm.websphere.ola.IndexedRecordImpl;
import com.ibm.websphere.ola.InteractionSpecImpl;
import com.ibm.websphere.ola.MappedRecordImpl;
import com.ibm.ws.zos.channel.wola.WolaJcaBridge;

/**
 * The Interaction object is used for executing a function against the resource adapter's
 * underlying EIS.
 *
 * In this case, the underlying EIS is a WOLA client-hosted service.
 *
 * A JCA client (e.g. an EJB) obtains one of these Interaction objects via Connection.createInteraction.
 * It then invokes the Interaction via the method Interaction.execute().
 */
public class InteractionImpl implements Interaction {

    /**
     * ServiceTracker for WolaJcaBridge.  The Interaction code uses WolaJcaBridge
     * to bridge into the WOLA channel in order to find the appropriate WOLA connection
     * and to send the request.
     * 
     * Note: this is not initialized immediately in part because of unit testing.
     */
    static WolaJcaBridgeServiceTracker wolaJcaBridgeServiceTracker ;
    
    /**
     * Name of the RAD RecordBytes class.
     */
    static private final String RECORD_BYTES_CLASS = "com.ibm.etools.marshall.RecordBytes";
    
    /**
     * Connection
     */
    private ConnectionImpl _con = null;
    
    /**
     * State of the interaction
     */
    private boolean _closed = true;
    
    /**
     * Three argument call flag                    
     */
    private boolean threeArgsCall = false;    /* @F013381A*/
    private Record threeArgsOutput = null;    /* @F013381A*/
    
    /**
     * Max length of a container name
     */
    private static final int MAX_CONTAINERNAME_LEN = 16;    /* @F014448A */
    
    /**
     * Constructor
     */
    protected InteractionImpl(ConnectionImpl con) {
        _con = con;
        _closed = false;
    }
    
    /**
     * NO-OP.
     */
    public void clearWarnings() throws ResourceException { }

    /**
     * Marks the interaction as closed.  No further execute() calls can be made.
     */
    public void close() throws ResourceException {
        _closed = true;
    }
    
    /**
     * @throws ResourceException if the Interaction is closed.
     */
    private void assertNotClosed() throws ResourceException {
        if (_closed) {
            throw new ResourceException("Interaction is closed");
        }
    }

    /**
     * @param is - An InteractionSpec 
     *
     * @return is casted to InteractionSpecImpl
     *
     * @throws ResourceException if the given InteractionSpec is not of type InteractionSpecImpl. 
     */
    private InteractionSpecImpl verifyInteractionSpec(InteractionSpec is) throws ResourceException {
        if ((is != null) && (is instanceof InteractionSpecImpl)) {
            return (InteractionSpecImpl) is;
        } else {
            throw new ResourceException("Invalid InteractionSpec, " + is);
        }
    }

    /**
     * Make sure the input record is either our IndexedRecord or a 
     * RAD / RD/z generated thing or a MappedRecord
     *
     * @param r - The record to convert. 
     *
     * @return byte[] representing the given Record.
     *
     * @throws ResourceException if the given Record impl is not a supported type.
     */
    protected byte[] convertRecordToBytes(Record r) throws ResourceException {
        if (r == null) {
            return new byte[0];                               /* @578934A*/

        } else if (r instanceof IndexedRecordImpl) {
            // TODO: For now just take the first element.  Later, support
            //       array of elements mapped to channel/containers in CICS.
            return (byte[])(((IndexedRecordImpl)r).get(0));

        } else if (r instanceof MappedRecordImpl) {
            return convertMappedRecordToBytes((MappedRecordImpl)r);

        } else if (isRecordBytes(r)) {
        	try {
        		Class<?> c = r.getClass();
        		Method m = c.getMethod("getBytes");
        		return (byte[]) m.invoke(r, (Object[])null);
        	} catch (Throwable t) {
        		throw new ResourceException("Error converting RecordBytes to byte[]", t);
        	}
        } else {
            throw new ResourceException("Invalid input record. Needs to be RecordBytes, IndexedRecordImpl, or MappedRecordImpl (or null).");
        }
    }

    /**
     * The byte[] format is, for each container map entry:
     * 
     * <containerName> (blank-padded to 16 bytes)
     * <byte[]-length>
     * <byte[]>
     * ...
     * 
     * TODO: should this method be in MappedRecordImpl?
     *
     * @return byte[] representing the given MappedRecordImpl.
     */
    private byte[] convertMappedRecordToBytes(MappedRecordImpl mr) {
        Set<String> containerNames = mr.keySet();                                              /* @F014448A */

        // Determine the size of the input data.  This is done by totaling up
        // the following 3 data sizes for each container:
        //    - container name (16 characters)
        //  - int bytes of data
        //  - actual data size
        // 
        int dataSize = 0;                                                                     /* @F014448A */
        // Add size for actual data
        dataSize += mr.getDataSize();                                                          /* @F014448A */
        // Add size for the lengths of the data
        dataSize += containerNames.size() * 4;                                                /* @F014448A */
        // Add size for the container names
        dataSize += containerNames.size() * 16;                                               /* @F014448A */

        // Build the input byte[]
        ByteBuffer bytebuf = ByteBuffer.allocate(dataSize);                 /* @F014448A */

        for(String currentKey : containerNames) {                                             /* @F014448A */
            try {
                putEbcdicStringField(bytebuf, currentKey, MAX_CONTAINERNAME_LEN);

                byte[] data = mr.get(currentKey);                                                  /* @F014448A */

                // Add the length of the data
                bytebuf.putInt(data.length);                                                      /* @F014448A */

                // Add the data
                bytebuf.put(data);                                                                /* @F014448A */
            } catch( Exception e ) {
                // TODO: Throw an exception
                e.printStackTrace();
            }
        }

        bytebuf.rewind();                                                                     /* @F014448A */
        byte[] tmpInputBytes = new byte[bytebuf.limit()];                                     /* @F014448A */
        bytebuf.get(tmpInputBytes);                                                           /* @F014448A */
        return tmpInputBytes;
    }

    /**
     * Execute a function against the underlying EIS.  In this case, the
     * EIS is a WOLA client-hosted service.
     * 
     * The InteractionSpec contains the serviceName of the client-hosted service.
     * The connection associated with this Interaction contains the client's 
     * registration name.  
     */
    public Record execute(InteractionSpec is, Record r)
            throws ResourceException 
    {
        // Make sure the state of the interaction is valid
        assertNotClosed();

        // Make sure the InteractionSpec is valid
        InteractionSpecImpl isi = verifyInteractionSpec(is);

        return executeHere(isi, r);
    }

    /**
     *
     * @return The bridge into the WOLA CFW code, for sending the request.
     * 
     * @throws ResourceException if the bridge could not be loaded
     */
    private WolaJcaBridge getWolaJcaBridge() throws ResourceException {
        
        if (InteractionImpl.wolaJcaBridgeServiceTracker == null) {
            InteractionImpl.wolaJcaBridgeServiceTracker = new WolaJcaBridgeServiceTracker().openMe();
        }
        
        WolaJcaBridge retMe = InteractionImpl.wolaJcaBridgeServiceTracker.getService();
        if (retMe == null) {
            throw new ResourceException("The WolaJcaBridge service could not be loaded from the ServiceTracker");
        }
        
        return retMe;
    }

    /**
     *
     * @param outputBytes - the result of the execution
     * @param r - the input record (used to determine the type of the output record)
     *
     * @return an output Record
     */
    protected Record convertBytesToRecord(byte[] outputBytes, Record r) throws UnsupportedEncodingException {

        if (r instanceof MappedRecordImpl) {
            return convertBytesToMappedRecord(outputBytes);
        } else {
            IndexedRecordImpl output = new IndexedRecordImpl();
            output.add(outputBytes);        
            return output;
        }
    }

    /**
     * TODO: should this method be in MappedRecordImpl?
     * 
     * @return A MappedRecordImpl built from the given outputBytes
     */
    private Record convertBytesToMappedRecord(byte[] outputBytes) throws UnsupportedEncodingException {
        if( outputBytes == null ) {
            return new MappedRecordImpl();
        } else {
            MappedRecordImpl output = new MappedRecordImpl();
            ByteBuffer outputBuffer = ByteBuffer.allocate(outputBytes.length);    /* @F014448A */
            outputBuffer.put(outputBytes);                                                          /* @F014448A */
            outputBuffer.rewind();                                                                  /* @F014448A */

            while( outputBuffer.hasRemaining() ) {                                                  /* @F014448A */
                // Read the container name
                byte[] containerNameBytes = new byte[16];                                           /* @F014448A */
                outputBuffer.get(containerNameBytes, 0, containerNameBytes.length);                 /* @F014448A */
                String containerName = (new String(containerNameBytes, "Cp1047")).trim();                     /* @F014448A */

                // Read the length of the data
                int dataLength = outputBuffer.getInt();                                             /* @F014448A */

                // Read the data
                byte[] data = new byte[dataLength];                                                 /* @F014448A */
                outputBuffer.get(data, 0, data.length);                                             /* @F014448A */

                // Add this container name / data pair to the MappedRecordImpl
                output.put(containerName, data);                                                    /* @F014448A */
            }

            return output;
        }
    }

    /**
     * Execute the WOLA client-hosted service.
     *
     * @param isi - contains the serviceName
     * @param r - The input data
     *
     * @return The result
     *
     * @throws ResourceException
     */
    private Record executeHere(InteractionSpecImpl isi, Record r) throws ResourceException {

        try {
        	
        	byte[] outputBytes;
        	
        	if (_con.getUseOTMA() == 0) {
	            _con.aboutToDriveInteraction();                        /* @F003691A*/
	
	            outputBytes = getWolaJcaBridge().jcaInvoke( _con.getRegisterName(), 
	                                                        isi.getServiceName(), 
	                                                        convertRecordToBytes(r),
	                                                        _con.getWolaJcaRequestInfo() );
        	} else {
        		outputBytes = getWolaJcaBridge().otmaInvoke(convertRecordToBytes(r), _con.getWolaOtmaRequestInfo());
        	}
        	
            return convertBytesToRecord(outputBytes, r);

        } catch (Throwable t) {
        	throw ((t instanceof ResourceException) ? (ResourceException)t : new ResourceException(t));
        }
    }

    /**
     * Tells us whether this Record is an instance of the RecordBytes interface (meaning it was
     * generated from RAD).
     * 
     * Since we require applications in Liberty to package the RecordBytes interface and supporting
     * code (marshall.jar) inside their application, we can't just do an instanceof check here to
     * see if we have an instance of RecordBytes.  The RecordBytes instance that we are checking
     * against will have been loaded by a different classloader, and the check will fail.
     * 
     * Instead, we must use the classloader which loaded/created the Record class, or just query
     * the class for the interfaces it implements and see if the string name of the RecordBytes
     * interface is one of those.
     */
    private boolean isRecordBytes(Record r) {
    	try {
    		Class<?> recordClass = r.getClass();
    		ClassLoader cl = recordClass.getClassLoader();
    		Class<?> recordBytesClass = cl.loadClass(RECORD_BYTES_CLASS);
    		return recordBytesClass.isAssignableFrom(recordClass);
    	} catch (Throwable t) {
    		/* FFDC */
    	}

    	return false;
    }
    
    /**
     * Verify the types of inputRecord and outputRecord are consistent and supported.
     *
     * First, we check the Record type of the inputRecord.  If it's a MappedRecordImpl, we know
     * to use the multi-container path (and verify the outputRecord is also a MappedRecordImpl.
     * If not, we go through the regular path as normal.
     *
     * @throws ResourceException if the types are invalid.
     */
    protected void verifyInputAndOutputRecord(Record inputRecord, Record outputRecord) throws ResourceException {
        if (inputRecord instanceof MappedRecordImpl) {
            if (!(outputRecord instanceof MappedRecordImpl)) {
                throw new ResourceException("Invalid output record type for input record type MappedRecordImpl. Needs to be MappedRecordImpl."
                                            + " InputRecord: " + inputRecord 
                                            + ". OutputRecord: " + outputRecord);
            }
        } else {

            if ((outputRecord instanceof IndexedRecordImpl) || (isRecordBytes(outputRecord))) {
                // OK.
            } else {
                throw new ResourceException("Invalid output record type. Needs to be RecordBytes or IndexedRecordImpl." 
                                            + " InputRecord: " + inputRecord 
                                            + ". OutputRecord: " + outputRecord);
            }
        }
    }

    /**
     * Copy the record contents from one to the other.
     *
     */
    protected void copyRecord(Record toRecord, Record fromRecord) throws ResourceException {

        if (toRecord instanceof MappedRecordImpl) {
            // If toRecord is a MappedRecordImpl, then we assume fromRecord is also a MappedRecordImpl.
            ((MappedRecordImpl)toRecord).putAll(((MappedRecordImpl)fromRecord));

        } else {

            // If toRecord is NOT a MappedRecordImpl, then we assume fromRecord is an IndexedRecordImpl.
            byte[] output = (byte[])(((IndexedRecordImpl)fromRecord).get(0));

            if (toRecord instanceof IndexedRecordImpl) {
                ((IndexedRecordImpl)toRecord).add(output);      
            } else if (isRecordBytes(toRecord)) {
            	try {
            		Class<?> c = toRecord.getClass();
            		Method m = c.getMethod("setBytes", new Class[] {byte[].class});
            		m.invoke(toRecord, new Object[] {output});
            	} catch (Throwable t) {
            		throw new ResourceException("Error setting response into RecordBytes instance", t);
            	}
            } else {
                // This should never happen - verifyInputAndOutputRecord should have already checked this.
                throw new ResourceException("Invalid output record type. Needs to be RecordBytes or IndexedRecordImpl. Record: " + toRecord);
            }
        }
    }

    /**
     *  Added support for a three argument call with the WID BPM exploitation.
     */
    public boolean execute(InteractionSpec is, Record inputRecord, Record outputRecord) throws ResourceException {
         
        verifyInputAndOutputRecord(inputRecord, outputRecord);

        Record dummyOutput = execute(is, inputRecord);

        copyRecord(outputRecord, dummyOutput);

        return true;
    }

    /**
     * @return the connection associated with this Interation.
     */
    public Connection getConnection() {
        return _con;
    }

    /**
     * @return null (NO-OP)
     */
    public ResourceWarning getWarnings() throws ResourceException {
        return null;
    }

    /**
     * Put the given string (encoded in EBCIDC) into the given ByteBuffer at its 
     * current position. The ByteBuffer's position is moved forward by fieldLen.
     * 
     * If the String is longer than fieldLen it is truncated.
     * 
     * @param bb - the target ByteBuffer
     * @param s - the string to write
     * @param fieldLen - the size of the field to write into
     * 
     */
    protected void putEbcdicStringField(ByteBuffer bb, String s, int fieldLen) {
        bb.put( CodepageUtils.getEbcdicBytesPadded(s, fieldLen) );
    }
    
}
