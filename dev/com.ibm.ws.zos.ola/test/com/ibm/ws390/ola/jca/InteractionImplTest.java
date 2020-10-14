/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.resource.ResourceException;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.etools.marshall.RecordBytes;
import com.ibm.websphere.ola.IndexedRecordImpl;
import com.ibm.websphere.ola.InteractionSpecImpl;
import com.ibm.websphere.ola.MappedRecordImpl;
import com.ibm.ws.zos.channel.wola.WolaJcaBridge;
import com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo;

/**
 * 
 */
public class InteractionImplTest {
    
    /**
     * Mock environment.
     */
    private Mockery mockery = null;

    /**
     * Create the mockery environment for each test. Setting up a new mockery
     * environment for each test helps isolate Expectation sets, making it easier to
     * debug when some Expectation fails and all the Expectations are dumped
     * to the error log.
     */
    @Before
    public void before() {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE); // for mocking classes
            }
        };
    }

    /**
     * There are alternative ways to do this.
     * 1) Use @RunWith(JMock.class) (deprecated)
     * 2) Declare the field: @Rule public final JUnitRuleMockery context = new JUnitRuleMockery();
     * (this version of Junit is not in our codebase).
     * 
     * Doing it the manual way for now.
     */
    @After
    public void after() {
        mockery.assertIsSatisfied();
    }
    
    /**
     *
     */
    @Test
    public void testConvertRecordToBytesRecordBytes() throws Exception {
        
        byte[] ba = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 };
        assertArrayEquals(ba, new InteractionImpl(null).convertRecordToBytes( new TestRecordBytes(ba) ) );
    }
    
    /**
     *
     */
    @Test
    public void testConvertRecordToBytesIndexedRecord() throws Exception {

        byte[] ba = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 };
        IndexedRecord ir = new IndexedRecordImpl();
        ir.add( ba );
        
        assertArrayEquals(ba, new InteractionImpl(null).convertRecordToBytes( ir ) );
    }
    
    /**
     *
     */
    @Test
    public void testConvertRecordToBytesMappedRecord() throws Exception {
        
        byte[] ba1 = new byte[] { 0x00, 0x01, 0x02, 0x03 };
        byte[] ba2 = new byte[] { 0x10, 0x11, 0x12, 0x13, 0x14 };
        
        MappedRecordImpl mri = new MappedRecordImpl();
        
        mri.put("ba1", ba1);
        mri.put("ba2", ba2);
        
        // Note: The order in which the keys will be marshalled is non-deterministic.
        // So there's two possible results for each possible ordering.  Verify that
        // the actual result is one of the two possibilities.
        ByteBuffer bb1 = ByteBuffer.allocate( 16 * 2 + 4 * 2 + ba1.length + ba2.length );
        bb1.put( CodepageUtils.getEbcdicBytesPadded("ba1",16) );
        bb1.putInt( ba1.length );
        bb1.put( ba1 );
        bb1.put( CodepageUtils.getEbcdicBytesPadded("ba2",16) );
        bb1.putInt( ba2.length );
        bb1.put( ba2 );
        
        ByteBuffer bb2 = ByteBuffer.allocate( 16 * 2 + 4 * 2 + ba1.length + ba2.length );
        bb2.put( CodepageUtils.getEbcdicBytesPadded("ba2",16) );
        bb2.putInt( ba2.length );
        bb2.put( ba2 );
        bb2.put( CodepageUtils.getEbcdicBytesPadded("ba1",16) );
        bb2.putInt( ba1.length );
        bb2.put( ba1 );
        
        byte[] result = new InteractionImpl(null).convertRecordToBytes( mri );

        assertTrue( Arrays.equals( bb1.array(), result ) || Arrays.equals(bb2.array(), result) );
    }
    
    /**
     *
     */
    @Test
    public void testConvertRecordToBytesMappedRecordLongName() throws Exception {

        byte[] ba1 = new byte[] { 0x00, 0x01, 0x02, 0x03 };
        byte[] ba2 = new byte[] { 0x10, 0x11, 0x12, 0x13, 0x14 };

        MappedRecordImpl mri = new MappedRecordImpl();

        mri.put("ba1", ba1);
        mri.put("ba2-this-name-is-longer-than-16-but-will-be-capped", ba2);
        
        // Note: The order in which the keys will be marshalled is non-deterministic.
        // So there's two possible results for each possible ordering.  Verify that
        // the actual result is one of the two possibilities.
        ByteBuffer bb1 = ByteBuffer.allocate( 16 * 2 + 4 * 2 + ba1.length + ba2.length );
        bb1.put( CodepageUtils.getEbcdicBytesPadded("ba1",16) );
        bb1.putInt( ba1.length );
        bb1.put( ba1 );
        bb1.put( CodepageUtils.getEbcdicBytesPadded("ba2-this-name-is-longer-than-16-but-will-be-capped",16) );
        bb1.putInt( ba2.length );
        bb1.put( ba2 );
        
        ByteBuffer bb2 = ByteBuffer.allocate( 16 * 2 + 4 * 2 + ba1.length + ba2.length );
        bb2.put( CodepageUtils.getEbcdicBytesPadded("ba2-this-name-is-longer-than-16-but-will-be-capped",16) );
        bb2.putInt( ba2.length );
        bb2.put( ba2 );
        bb2.put( CodepageUtils.getEbcdicBytesPadded("ba1",16) );
        bb2.putInt( ba1.length );
        bb2.put( ba1 );
        
        byte[] result = new InteractionImpl(null).convertRecordToBytes( mri );

        assertTrue( Arrays.equals( bb1.array(), result ) || Arrays.equals(bb2.array(), result) );
    }
    
    /**
     *
     */
    @Test
    public void testConvertBytesToRecordMappedRecord() throws Exception {

        byte[] ba1 = new byte[] { 0x00, 0x01, 0x02, 0x03 };
        byte[] ba2 = new byte[] { 0x10, 0x11, 0x12, 0x13, 0x14 };

        ByteBuffer bb1 = ByteBuffer.allocate( 16 * 2 + 4 * 2 + ba1.length + ba2.length );
        bb1.put( CodepageUtils.getEbcdicBytesPadded("ba1",16) );
        bb1.putInt( ba1.length );
        bb1.put( ba1 );
        bb1.put( CodepageUtils.getEbcdicBytesPadded("ba2",16) );
        bb1.putInt( ba2.length );
        bb1.put( ba2 );

        MappedRecordImpl mri = (MappedRecordImpl) new InteractionImpl(null).convertBytesToRecord( bb1.array(), new MappedRecordImpl() );

        assertTrue( mri.containsKey("ba1") );
        assertTrue( mri.containsKey("ba2") );
        assertArrayEquals( ba1, mri.get("ba1"));
        assertArrayEquals( ba2, mri.get("ba2"));
    }
    
    /**
     *
     */
    @Test
    public void testConvertBytesToRecordIndexedRecord() throws Exception {

        byte[] ba = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 };
        IndexedRecord ir = (IndexedRecord) new InteractionImpl(null).convertBytesToRecord( ba, new IndexedRecordImpl() ) ;
               
        assertArrayEquals(ba, (byte[])ir.get(0));
    }
    
    /**
     * Test successful verifications of input/output records.
     * 
     * verifyInputAndOutputRecord throws a ResourceException if the verification fails.
     */
    @Test
    public void testVerifyInputAndOutputRecord() throws Exception {
        
        new InteractionImpl(null).verifyInputAndOutputRecord( new MappedRecordImpl(), new MappedRecordImpl());
        new InteractionImpl(null).verifyInputAndOutputRecord( new IndexedRecordImpl(), new IndexedRecordImpl());
        new InteractionImpl(null).verifyInputAndOutputRecord( new IndexedRecordImpl(), new TestRecordBytes(null));
    }
    
    /**
     * Test failed verifications of input/output records.
     * 
     * verifyInputAndOutputRecord throws a ResourceException if the verification fails.
     */
    @Test(expected=ResourceException.class)
    public void testVerifyInputAndOutputRecordBad1() throws Exception {
        
        // MappedRecordImpl goes only with MappedRecordImpl
        new InteractionImpl(null).verifyInputAndOutputRecord( new MappedRecordImpl(), new IndexedRecordImpl());
    }
    
    /**
     * Test failed verifications of input/output records.
     * 
     * verifyInputAndOutputRecord throws a ResourceException if the verification fails.
     */
    @Test(expected=ResourceException.class)
    public void testVerifyInputAndOutputRecordBad2() throws Exception {
        
        // NON-MappedRecordImpl goes only with IndexedRecord or RecordBytes
        new InteractionImpl(null).verifyInputAndOutputRecord( new IndexedRecordImpl(), new MappedRecordImpl());
    }
    
    /**
     * Test failed verifications of input/output records.
     * 
     * verifyInputAndOutputRecord throws a ResourceException if the verification fails.
     */
    @Test(expected=ResourceException.class)
    public void testVerifyInputAndOutputRecordBad3() throws Exception {
        
        // NON-MappedRecordImpl goes only with IndexedRecord or RecordBytes
        new InteractionImpl(null).verifyInputAndOutputRecord( new TestRecordBytes(null), new MappedRecordImpl());
    }
    
    /**
     * Test failed verifications of input/output records.
     * 
     * verifyInputAndOutputRecord throws a ResourceException if the verification fails.
     */
    @Test(expected=ResourceException.class)
    public void testVerifyInputAndOutputRecordBad4() throws Exception {
        
        // MappedRecordImpl goes only with MappedRecordImpl
        new InteractionImpl(null).verifyInputAndOutputRecord( new MappedRecordImpl(), new TestRecordBytes(null));
    }
    
    /**
     * 
     */
    @Test
    public void testCopyRecordMappedRecord() throws Exception {
        
        // Create source record
        byte[] ba1 = new byte[] { 0x00, 0x01, 0x02, 0x03 };
        byte[] ba2 = new byte[] { 0x10, 0x11, 0x12, 0x13, 0x14 };
        
        MappedRecordImpl src = new MappedRecordImpl();
        src.put("ba1", ba1);
        src.put("ba2", ba2);
        
        MappedRecordImpl target = new MappedRecordImpl();
        new InteractionImpl(null).copyRecord(target, src);
        
        assertTrue( target.containsKey("ba1"));
        assertTrue( target.containsKey("ba2"));
        
        assertArrayEquals( ba1, src.get("ba1") );
        assertArrayEquals( ba1, target.get("ba1") );
        assertArrayEquals( ba2, src.get("ba2") );
        assertArrayEquals( ba2, target.get("ba2") );
    }
    
    /**
     * 
     */
    @Test
    public void testCopyRecordIndexedRecord() throws Exception {
        
        // Create source record
        byte[] ba1 = new byte[] { 0x00, 0x01, 0x02, 0x03 };
        
        IndexedRecordImpl src = new IndexedRecordImpl();
        src.add(ba1);
       
        IndexedRecordImpl target = new IndexedRecordImpl();
        new InteractionImpl(null).copyRecord(target, src);
        
        assertArrayEquals( ba1, (byte[])src.get(0) );
        assertArrayEquals( ba1, (byte[])target.get(0) );
    }
    
    /**
     * 
     */
    @Test
    public void testCopyRecordRecordBytes() throws Exception {
        
        // Create source record.  In the case where the output record is RecordBytes,
        // the source record will always be an IndexedRecord (see verifyInputAndOutputRecord).
        byte[] ba1 = new byte[] { 0x00, 0x01, 0x02, 0x03 };
        
        IndexedRecordImpl src = new IndexedRecordImpl();
        src.add(ba1);
       
        TestRecordBytes target = new TestRecordBytes(null);
        new InteractionImpl(null).copyRecord(target, src);
        
        assertArrayEquals( ba1, (byte[])src.get(0) );
        assertArrayEquals( ba1, target.getBytes() );
    }
    
    /**
     * 
     */
    @Test(expected=ClassCastException.class)
    public void testCopyRecordInvalidRecordType() throws Exception {
        
        // Create source record.  In the case where the output record is RecordBytes,
        // the source record will always be an IndexedRecord (see verifyInputAndOutputRecord).
        byte[] ba1 = new byte[] { 0x00, 0x01, 0x02, 0x03 };
        
        IndexedRecordImpl src = new IndexedRecordImpl();
        src.add(ba1);
       
        // Cannot copy from Indexed to Mapped.
        MappedRecordImpl target = new MappedRecordImpl();
        new InteractionImpl(null).copyRecord(target, src);
    }
    
    /**
     * 
     */
    @Test(expected=ClassCastException.class)
    public void testCopyRecordInvalidRecordType2() throws Exception {
        
        MappedRecordImpl src = new MappedRecordImpl();
        IndexedRecordImpl target = new IndexedRecordImpl();
        
        new InteractionImpl(null).copyRecord(target, src);
    }
    
    /**
     * Test execute method that returns output record.
     */
    @Test
    public void testExecute() throws Exception {
        
        final ConnectionImpl mockConnectionImpl = mockery.mock(ConnectionImpl.class);
        final InteractionSpecImpl mockInteractionSpecImpl = mockery.mock(InteractionSpecImpl.class);
        final WolaJcaRequestInfo mockWolaJcaRequestInfo = mockery.mock(WolaJcaRequestInfo.class);
        InteractionImpl.wolaJcaBridgeServiceTracker = mockery.mock(WolaJcaBridgeServiceTracker.class);
        final WolaJcaBridge mockWolaJcaBridge = mockery.mock(WolaJcaBridge.class);
        
        final String registerName = "registerName";
        final String serviceName = "serviceName";
        final byte[] inputRecordBytes = new byte[] { 0x00, 0x01, 0x02, 0x03 };
        final byte[] outputRecordBytes = new byte[] { 0x10, 0x11, 0x12, 0x13 };
        
        mockery.checking( new Expectations() {
            {
                one(mockConnectionImpl).getUseOTMA();
                
            	one(mockConnectionImpl).aboutToDriveInteraction();
               
                one(InteractionImpl.wolaJcaBridgeServiceTracker).getService();
                will(returnValue(mockWolaJcaBridge));
                
                one(mockConnectionImpl).getRegisterName();
                will(returnValue(registerName));
                
                one(mockInteractionSpecImpl).getServiceName();
                will(returnValue(serviceName));
                
                one(mockConnectionImpl).getWolaJcaRequestInfo();
                will(returnValue(mockWolaJcaRequestInfo));
                
                one(mockWolaJcaBridge).jcaInvoke(registerName, serviceName, inputRecordBytes, mockWolaJcaRequestInfo);
                will(returnValue(outputRecordBytes));
            }
        });
        
        IndexedRecordImpl input = new IndexedRecordImpl();
        input.add(inputRecordBytes);
        
        IndexedRecordImpl output = (IndexedRecordImpl) new InteractionImpl(mockConnectionImpl).execute(mockInteractionSpecImpl, input);
        
        assertArrayEquals(outputRecordBytes, (byte[]) output.get(0));
    }
        
    
    /**
     * Test execute method that takes both input and output record.
     */
    @Test
    public void testExecute2() throws Exception {
        
        final ConnectionImpl mockConnectionImpl = mockery.mock(ConnectionImpl.class);
        final InteractionSpecImpl mockInteractionSpecImpl = mockery.mock(InteractionSpecImpl.class);
        final WolaJcaRequestInfo mockWolaJcaRequestInfo = mockery.mock(WolaJcaRequestInfo.class);
        InteractionImpl.wolaJcaBridgeServiceTracker = mockery.mock(WolaJcaBridgeServiceTracker.class);
        final WolaJcaBridge mockWolaJcaBridge = mockery.mock(WolaJcaBridge.class);
        
        final String registerName = "registerName";
        final String serviceName = "serviceName";
        final byte[] inputRecordBytes = new byte[] { 0x00, 0x01, 0x02, 0x03 };
        final byte[] outputRecordBytes = new byte[] { 0x10, 0x11, 0x12, 0x13 };
        
        mockery.checking( new Expectations() {
            {
                
            	one(mockConnectionImpl).getUseOTMA();
            	
            	one(mockConnectionImpl).aboutToDriveInteraction();
               
                one(InteractionImpl.wolaJcaBridgeServiceTracker).getService();
                will(returnValue(mockWolaJcaBridge));
                
                one(mockConnectionImpl).getRegisterName();
                will(returnValue(registerName));
                
                one(mockInteractionSpecImpl).getServiceName();
                will(returnValue(serviceName));
                
                one(mockConnectionImpl).getWolaJcaRequestInfo();
                will(returnValue(mockWolaJcaRequestInfo));
                
                one(mockWolaJcaBridge).jcaInvoke(registerName, serviceName, inputRecordBytes, mockWolaJcaRequestInfo);
                will(returnValue(outputRecordBytes));
            }
        });
        
        IndexedRecordImpl input = new IndexedRecordImpl();
        input.add(inputRecordBytes);
        
        IndexedRecordImpl output = new IndexedRecordImpl();
        new InteractionImpl(mockConnectionImpl).execute(mockInteractionSpecImpl, input, output);
        
        assertArrayEquals(outputRecordBytes, (byte[]) output.get(0));
    }
    
    /**
     * Test execute when the interaction is closed.  Should throw a ResourceException
     */
    @Test(expected=ResourceException.class)
    public void testExecuteInteractionClosed() throws Exception {
        
        final ConnectionImpl mockConnectionImpl = mockery.mock(ConnectionImpl.class);
        final InteractionSpecImpl mockInteractionSpecImpl = mockery.mock(InteractionSpecImpl.class);
 
        
        InteractionImpl ii = new InteractionImpl(mockConnectionImpl);
        ii.close();
        
        ii.execute(mockInteractionSpecImpl, new IndexedRecordImpl());
    }
    
    /**
     * Test execute when the interaction is closed.  Should throw a ResourceException
     */
    @Test(expected=ResourceException.class)
    public void testExecuteInvalidInteractionSpecType() throws Exception {
        
        final ConnectionImpl mockConnectionImpl = mockery.mock(ConnectionImpl.class);
        final InteractionSpec mockInteractionSpec = mockery.mock(InteractionSpec.class);
 
        
        InteractionImpl ii = new InteractionImpl(mockConnectionImpl);
        ii.execute(mockInteractionSpec, new IndexedRecordImpl());
    }
    
    /**
     * Test execute method when jcaInvoke throws an exception.
     * execute() shall throw a ResourceException with the jcaInvoke exception wrapped inside.
     */
    @Test(expected=ResourceException.class)
    public void testExecuteJcaInvokeThrowsException() throws Exception {
        
        final ConnectionImpl mockConnectionImpl = mockery.mock(ConnectionImpl.class);
        final InteractionSpecImpl mockInteractionSpecImpl = mockery.mock(InteractionSpecImpl.class);
        final WolaJcaRequestInfo mockWolaJcaRequestInfo = mockery.mock(WolaJcaRequestInfo.class);
        InteractionImpl.wolaJcaBridgeServiceTracker = mockery.mock(WolaJcaBridgeServiceTracker.class);
        final WolaJcaBridge mockWolaJcaBridge = mockery.mock(WolaJcaBridge.class);
        
        final String registerName = "registerName";
        final String serviceName = "serviceName";
        final byte[] inputRecordBytes = new byte[] { 0x00, 0x01, 0x02, 0x03 };
        
        mockery.checking( new Expectations() {
            {
            	one(mockConnectionImpl).getUseOTMA();
            	
            	one(mockConnectionImpl).aboutToDriveInteraction();
               
                one(InteractionImpl.wolaJcaBridgeServiceTracker).getService();
                will(returnValue(mockWolaJcaBridge));
                
                one(mockConnectionImpl).getRegisterName();
                will(returnValue(registerName));
                
                one(mockInteractionSpecImpl).getServiceName();
                will(returnValue(serviceName));
                
                one(mockConnectionImpl).getWolaJcaRequestInfo();
                will(returnValue(mockWolaJcaRequestInfo));
                
                one(mockWolaJcaBridge).jcaInvoke(registerName, serviceName, inputRecordBytes, mockWolaJcaRequestInfo);
                will(throwException(new Exception()));
            }
        });
        
        IndexedRecordImpl input = new IndexedRecordImpl();
        input.add(inputRecordBytes);
        
        new InteractionImpl(mockConnectionImpl).execute(mockInteractionSpecImpl, input );
    }
        
}


/**
 * Simple test class impl of RecordBytes.
 */
class TestRecordBytes implements RecordBytes, Record {

    private byte[] bytes;
    
    public TestRecordBytes(byte[] bytes) {
        setBytes(bytes);
    }
    
    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public int getSize() {
        return bytes.length;
    }

    @Override
    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public String getRecordName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRecordShortDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setRecordName(String arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setRecordShortDescription(String arg0) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public Object clone() { 
        return null; 
    }
    
}