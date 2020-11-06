/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************
 *
 * The purpose of this class is to test if a Java archive is valid without actually reading the entire JAR.
 * This is especially useful in cases where the archive has a script attached at the front of the file.
 * Of course, this is more of a sanity check than a rigorous validation, which would require reading the whole file.
 * 
 *     [ script ]                   <---- start of file  (optional)
 *     [ local file header 1 ]      <---- actual start of archive
 *     [ encryption header 1 ]
 *     [ file data 1 ]
 *     [ data descriptor 1 ]
 *     . 
 *     .
 *     .
 *     [ local file header n ]
 *     [ encryption header n ]
 *     [ file data n ]
 *     [ data descriptor n ]
 *     [ archive decryption header ] 
 *     [ archive extra data record ] 
 *     [ central directory header 1 ]       <---- start of central directory
 *     .
 *     .
 *     .
 *     [ central directory header n ]
 *     [ zip64 end of central directory record ]
 *     [ zip64 end of central directory locator ] 
 *     [ end of central directory record ]
 * 
 *  The validation calculates the size of the last 4 records listed above (as well as doing some
 *  validation checks on the sizes and signatures) and then calculates the actual offset
 *  of the archive within the file (which will normally be 0).  The final step is to check
 *  the signature at the beginning of the archive.
 *  
 *  The actual archive offset is calculated :
 *  
 *    fileLength - eocdRecordSize - zip64RecLengths - CentralDirectorySize - offsetOfCentralDirectory
 *   
 *   Note the offsetOfCentralDirectory is relative to the beginning of the actual archive (not an offset  
 *   from the beginning of the file.)  Subtracting away all of the known pieces gives us the unknown 
 *   piece which is the actual archive offset.  
 */

package com.ibm.ws.artifact.zip.internal;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.zip.ZipException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class ZipValidator {
    static final TraceComponent tc = Tr.register(ZipFileContainerFactory.class);
    
    // EOCDR is End-of-Central-Directory-Record
    private static final int EOCDR_MIN_SIZE = 22;
    private static final int EOCDR_MAX_COMMENT_LEN = 0xFFFF;
    private static final int EOCDR_MAX_SIZE = EOCDR_MIN_SIZE + EOCDR_MAX_COMMENT_LEN;
    private static final int EOCDR_SIGNATURE = 0x504b0506; 
    private static final int EOCDR_TOTAL_ENTRIES_OFFSET = 10;
    private static final int ZIP64_INDICATOR = 0xFFFF;               // Indicates Zip64 when found at offset EOCDR_TOTAL_ENTRIES_OFFSET 
    private static final int EOCDR_CENTRAL_DIR_SIZE_OFFSET = 12;     // Offset to size of central directory
    private static final int EOCDR_CENTRAL_DIR_OFFSET = 16;          // Offset to central directory relative to beginning of archive
    private static final int EOCDR_COMMENT_LENGTH_OFFSET = 20; 	     // Offset to comment length

    // ZIP64_EOCDR is Zip64-End-of-Central-Directory-Record
    private static final int ZIP64_EOCDR_SIGNATURE = 0x504b0606;
    private static final int ZIP64_EOCDR_MINIMUM_SIZE = 56;
    private static final int ZIP64_EOCDR_HEADER_SIZE = 12;           // 4 bytes for the signature and 8 bytes for the length
    private static final int ZIP64_EOCDR_SIZE_OFFSET = 4;            // The size at this offset is the size of the zip64 EOCDR minus 12. (Meaning it does NOT include the 12-byte header)
    private static final int ZIP64_EOCDR_SIZE_FIELD_LENGTH = 8;      // Number of bytes for size field.

    // ZIP64_LOCATOR is Zip64-End-of-Central-Directory-Record-Locator
    private static final int ZIP64_LOCATOR_SIGNATURE = 0x504b0607;
    private static final int ZIP64_LOCATOR_SIZE = 20;                // Fixed size.
    private static final int ZIP64_LOCATOR_EOCD_OFFSET_OFFSET = 8;   // Position in the locator of the offset of the Zip64-End-of-Central-Direcory-Record

    private static final int LOCAL_FILE_HEADER_SIGNATURE = 0x504b0304;
    private static final int BLOCK_SIZE = 256;
    private static final int FORWARD_BLOCK_SIZE = 8192;

    // Maximum amount of data (usually a script) that is allowed, by this implementation,
    // to be prepended to an archive in Zip64 format. This value may be increased, but it
    // increases the time it takes to fail when the zip is corrupted.  This limitation does 
    // not apply to archives not in Zip64 format.
    private static final int MAX_PREFIX_ALLOWED_FOR_ZIP64 = 200*1024*1024;       

    private byte[] _endOfCentralDirectoryRecord; // Only the minimum size is stored.  Does not include the comment 
    private int    _eocdRecordSize;              // Actual size of End-of-Central-Directory-Record - including comment
    private byte[] _zip64EoCDRLocator;           // 20 bytes;
    private long   _zip64EoCDRLocatorOffset;     // Offset (from end of file) of the Zip64 Locator
    private byte[] _zip64EoCDR;                  // Only the minimum size is stored.  Does not include any extra fields.  
    private long   _zip64EoCDRecSize;            // Size of Zip64-End-of-Central-Directory-Record
    private String _archiveFileName;
    private long   _fileLength;                  // Length of the archive file

    ZipValidator(String archiveFileName) {
        _archiveFileName = archiveFileName;
    }

    /** 
     * @return true if the archive has valid format
     */
    public boolean isValid() {

        try ( RandomAccessFile file = new RandomAccessFile(_archiveFileName, "r") ) {

            _fileLength = file.length();
            _endOfCentralDirectoryRecord = getEndOfCentralDirectoryRecord(file);

            if ( isZip64() ) {
                _zip64EoCDRLocator = getZip64EoCDLocator(file);
                _zip64EoCDRLocatorOffset = _fileLength - _eocdRecordSize - ZIP64_LOCATOR_SIZE;
                _zip64EoCDR = getZip64EndOfCentralDirectoryRecord(file);
                _zip64EoCDRecSize = getZip64EoCDRecordSize( _zip64EoCDR, 0 );
            }

            long archiveOffset = getActualArchiveOffset();

            if (archiveStartsWithSignature(file, archiveOffset)) {
                return true;
            } 

        } catch (Exception e) {
            // FFDC
            Tr.error(tc, "bad.zip.data", _archiveFileName);
        }

        return false;
    }

    /**
     * Find the End-of-Central-Director-Record which is always at the end of the file.
     * It is usually 22 bytes but may contain a comment up to 64K in size.
     * Reads from the end of the file looking backwards for the EoCDR signature.
     * 
     * The implementation reads the file in BLOCK_SIZE blocks from the end of the file
     * and searches each block.  Note that there is some overlap of the successive 
     * blocks; EOCDR_MIN_SIZE bytes of overlap to be precise.  This is because the 
     * record might start at the end of one block and finish in the next.
     * 
     * @param file the zip file 
     */
    private byte[] getEndOfCentralDirectoryRecord(RandomAccessFile file) throws IOException {

        byte[] block = getBlockFromEndOfFile(file, null, BLOCK_SIZE, BLOCK_SIZE);
        if ( block.length < EOCDR_MIN_SIZE ) {
            throw new ZipException("Not a valid zip file.  Less than minimum required length. File [ " + _archiveFileName + " ]");
        }

        int size = EOCDR_MIN_SIZE ;
        int offset = block.length - size;
        int offsetFromEnd = block.length - offset;   // offset from the end of the block (not the file)

        // We will only enter this loop if a comment is attached to the End-of-Central-Directory-Record
        // ... or if the zip is invalid (doesn't have an End-of-Central-Directory-Record.
        // Note that isValidEoCDRec must have EOCDR_MIN_SIZE available at the end of the block
        // to avoid an exception because of a read past the end of the block.
        while ( !isValidEoCDRec( block, offset, size ) ) {
            size++;
            offsetFromEnd++;

            if ( size > EOCDR_MAX_SIZE || size > _fileLength ) {
                throw new ZipException("Cannot find central directory end record in zip. File [ " + _archiveFileName + " ]");
            }

            if ( offsetFromEnd > block.length ) {

                block = getBlockFromEndOfFile(file, block, size + BLOCK_SIZE - EOCDR_MIN_SIZE, BLOCK_SIZE);
                offsetFromEnd = EOCDR_MIN_SIZE;
            }
            offset = block.length - offsetFromEnd;			
        }

        // Copy only the minimum end-of-central-directory-record, but record the actual size.  
        // No need to store the comment, which could be up to 64k.		
        _eocdRecordSize = size;
        return Arrays.copyOfRange(block, offset, offset + EOCDR_MIN_SIZE ); 
    }

    /**
     * Get the 20-byte Zip64 Locator.  It must sit 20 bytes before the End-of-Central-Directory-Record.
     * @param file the zip file
     */
    private byte[] getZip64EoCDLocator(RandomAccessFile file) throws IOException {

        if ( !isZip64() ) {
            return null;
        }

        byte[] block = getBlockFromEndOfFile(file, 
                                             null,
                                             _eocdRecordSize + ZIP64_LOCATOR_SIZE, 
                                             ZIP64_LOCATOR_SIZE);	

        if ( !isValid_Zip64EoCDRLocator(block, 0)) {
            throw new ZipException("Invalid Zip64-End-of-Central-Direcytory-Locator. File [ " + _archiveFileName + " ]");
        }

        return Arrays.copyOfRange(block, 0, ZIP64_LOCATOR_SIZE);
    }

    /**
     * Locates the Zip64-End-of-Central-Directory-Record by using the offset 
     * in the Zip64 Locator.  If not found there, it is assumed that data has
     * been prepended to the archive, and a forward search begins until we find the
     * correct signature.  Of course, a false signature could appear just about  
     * anywhere in the zip.  So the record must pass additional validation.
     * @param file a zip file
     * @return a copy of the Zip64-End-of-Central-Directory-Record
     */
    private byte[] getZip64EndOfCentralDirectoryRecord(RandomAccessFile file ) throws IOException {

        if ( !isZip64() ) {
            return null;
        }

        // The offset of the Zip64-End-of-Central-Directory-Record is found in the Zip64 locator at offset 8.
        // That offset will NOT be correct if data (i.e. a script) has been attached in front of the archive,
        // but in that case, the offset will be our starting point for the search.
        long zip64EoCDOffset = getLittleEndianValue( _zip64EoCDRLocator, ZIP64_LOCATOR_EOCD_OFFSET_OFFSET, 4);

        byte [] block = getBlockFromBeginningOfFile(file, null, zip64EoCDOffset, ZIP64_EOCDR_MINIMUM_SIZE);
        if (block.length < ZIP64_EOCDR_MINIMUM_SIZE) {
            throw new ZipException("Offset to Zip64-End-of-Central-Directory-Record is not correct.  Reached end of file. File [ " + _archiveFileName + " ]");
        }

        long offsetFromBeginningOfFile = zip64EoCDOffset;
        int offsetInBlock = 0;
        int offsetFromStartingPoint = 0;        // The starting point is zip64EoCDOffset;
        boolean blockAllocatedInLoop = false;   // If we need a second block, it will be larger.

        // We will only enter this loop if data has been attached to the front of the archive (or if
        // the archive is invalid.)  The loop searches forward in the file starting at the point where
        // the Zip64 Locator indicates the Zip64-End-of-Central-Directory-Record should be.
        while ( !isValid_Zip64EoCDRecord( block, offsetInBlock, offsetFromBeginningOfFile ) ) {
            offsetInBlock++;
            offsetFromBeginningOfFile++;
            offsetFromStartingPoint++;

            // An artificial limit is placed on the size of data allowed to be prepended to an archive
            // in Zip64 format. This is because this implementation searches through the file.  A fruitless 
            // search through a large file would cause an unacceptable delay if we did not set a limit.
            if (offsetFromStartingPoint > MAX_PREFIX_ALLOWED_FOR_ZIP64) {
                throw new ZipException("Failed to find the Zip64-End-of-Central-Directory-Record after searching " + MAX_PREFIX_ALLOWED_FOR_ZIP64 + " bytes. File [ " + _archiveFileName + " ]");	
            }

            // Must allow 4 bytes at end of block to read signature
            if ( offsetInBlock > block.length - 4 ) {  
            	
                // The normal case is to use only 1 block and not come into this loop.  In the loop, we
                // use a larger block size.  Allocate the bigger block, and then re-use as needed.
                if ( !blockAllocatedInLoop ) {
                    blockAllocatedInLoop = true;
                    block = new byte[FORWARD_BLOCK_SIZE];
                }

                block = getBlockFromBeginningOfFile(file, block, offsetFromBeginningOfFile, FORWARD_BLOCK_SIZE); // re-use block allocation
                if (block.length < 4) {
                    throw new ZipException("Reached end of file while searching for Zip64-End-of-Central-Directory-Record. File [ " + _archiveFileName + " ]");
                }
                offsetInBlock = 0;
            }
        }

        if (block.length < (offsetInBlock + ZIP64_EOCDR_MINIMUM_SIZE ))  {
            block = getBlockFromBeginningOfFile(file, null, offsetFromBeginningOfFile, ZIP64_EOCDR_MINIMUM_SIZE);
            return  Arrays.copyOfRange(block, 0, ZIP64_EOCDR_MINIMUM_SIZE);
        } 

        // Only store the minimum size for this record.  No need to store any extra data.
        return Arrays.copyOfRange(block, offsetInBlock , offsetInBlock + ZIP64_EOCDR_MINIMUM_SIZE);
    }

    /**
     * @param file  a zip file
     * @param bytes  a byte array of length 'size'.  If null, a byte array of 'size' is allocated
     * @param offsetFromEnd  Offset from end of file where reading begins
     * @param sizeRequested  number of bytes of data to read
     * @return  an array of 'size' bytes that starts from 'offsetFromEnd' of file.
     *          if 'offsetFromEnd' >= file length, returns an empty array.
     *          if 'size' + 'offsetFromEnd' > file length, returns an array with length of data read from the file
     * 
     */
    private byte[] getBlockFromEndOfFile(RandomAccessFile file, byte[] bytes, long offsetFromEnd, int sizeRequested) throws IOException {

        int sizeToRead = sizeRequested;
        if (offsetFromEnd > _fileLength) {
            if (offsetFromEnd - sizeRequested >= _fileLength) {
                return new byte[0];
            }
            long adjustment = offsetFromEnd - _fileLength ;
            offsetFromEnd = _fileLength;
            sizeToRead -= adjustment;
            if  (sizeToRead < 0) {
                return new byte[0];
            }
        }

        // Have offset from end.  Get offset from beginning.
        long offset = _fileLength > offsetFromEnd ? (_fileLength - offsetFromEnd) : 0;

        file.seek(offset);

        // if bytes == null, caller is asking to allocate the array.
        // if sizeToRead != sizeRequested, then there are not enough bytes between  
        // offsetFromEnd and the beginning of the file to fill the array.  We need  
        // the array length to match the amount data.
        if ((bytes == null) || (sizeToRead != sizeRequested)) {
            bytes = new byte[sizeToRead];

        } else if (bytes.length != sizeRequested) {
            throw new IllegalArgumentException("The 'bytes' array parameter must be 'size' bytes in length. File [ " + _archiveFileName + " ]");
        }

        int bytesRead = file.read(bytes, 0, sizeToRead );
        if (bytesRead != bytes.length) {
            throw new ZipException("Not enough bytes were read to fill the array. File [ " + _archiveFileName + " ]");       	  
        }
        return bytes;
    }

    /**
     * @param file  a zip file
     * @param bytes  a byte array of length 'size'.  If null, a byte array of 'size' is allocated
     * @param offset  Position from beginning of file, where data is to be read
     * @param size  number of bytes to read
     * @return an array of size 'size' read from file, beginning at 'offset'.
     *         if 'offset' >= file length, returns an empty array.
     *         if 'size' + 'offset' > file length, returns an array with length of data read from the file
     */
    private byte[] getBlockFromBeginningOfFile(RandomAccessFile file, byte[] bytes, long offset, int size) throws IOException {

        if (offset >= _fileLength) {
            return new byte[0];
        }

        // Adjust the amount to allocate for the array based on the offset and file length
        int blockLength;
        if ((size + offset) > _fileLength) {
            blockLength = (int)(_fileLength - offset);
        }
        else {
            blockLength = size;
        }

        file.seek(offset);

        // if bytes == null, caller is asking us to allocate the array.
        // if blockLength != size, then there are not enough bytes from offset to end of file
        // to fill the array.  We want the array length to match the amount data.
        if ((bytes == null) || (blockLength != size)) {
            bytes = new byte[blockLength];
        } else if (bytes.length != size) {
            throw new IllegalArgumentException("The 'bytes' array must be 'size' bytes in length. File [ " + _archiveFileName + " ]");
        }

        int bytesRead = file.read(bytes, 0, size );
        if (bytesRead != bytes.length) {
            throw new ZipException("Not enough bytes were read to fill the array. File [ " + _archiveFileName + " ]");       	  
        }
        return bytes;	
    }

    /** 
     * Test for valid signature and size.
     * @return true if the offset points to a valid End-of-Central-Directory-Record within "bytes"
     */
    private boolean isValidEoCDRec(byte[] bytes, int offset, int centralDirEndRecSize) {
        if ( !isSignature(bytes, offset, EOCDR_SIGNATURE) ) {
            return false;
        }

        long commentLength = getLittleEndianValue(bytes, offset + EOCDR_COMMENT_LENGTH_OFFSET, 2);
        return centralDirEndRecSize == (EOCDR_MIN_SIZE + commentLength);
    }

    /**
     * @return true if the signature parameter matches the bytes at offset
     */
    private boolean isSignature(byte[] bytes, int offset, int signature) {

        int value = getSignature(bytes, offset);

        return (value == signature);
    }

    /** 
     * @param bytes the data
     * @param offsetInByteArray pointer to a possible Zip64-End-Of-Central-Directory-Record
     * @return true if the offset points to a valid Zip64-End-of-Central-Directory-Record within "bytes"
     */
    private boolean isValid_Zip64EoCDRecord(byte[] bytes, int offsetInByteArray, long offsetInFile) {

        if ( !isSignature(bytes, offsetInByteArray, ZIP64_EOCDR_SIGNATURE)) {
            return false;
        } 

        if ( !isValid_Zip64EoCDRecordSize(bytes, offsetInByteArray, offsetInFile) ) {
            return false;
        }

        return true;
    }

    /**
     * @param bytes the data
     * @param offset pointer to a possible Zip64-End-Of-Central-Directory-Record-Locator
     * @return true if the offset points to a valid Zip64-End-of-Central-Directory-Record-Locator within "bytes"
     */
    private boolean isValid_Zip64EoCDRLocator(byte[] bytes, int offset) {

        if ( !isSignature(bytes, offset, ZIP64_LOCATOR_SIGNATURE)) {
            return false;
        }
        return true;
    }

    /**
     * This method assumes you've already found a valid signature for the Zip64-End-Of-Central-Directory-Record.
     * Of course, the signature might just randomly appear anywhere in the data.  So this method
     * tests to see if the length field makes sense.
     * @param bytes the data
     * @param offsetInArray pointer to Zip64-End-Of-Central-Directory-Record in the data
     * @return true if the size makes sense for a 
     */
    private boolean isValid_Zip64EoCDRecordSize(byte[] bytes, int offsetInArray, long offsetInFile) {

        long specifiedZip64EoCDRecSize = getZip64EoCDRecordSize( bytes, offsetInArray );

        if (specifiedZip64EoCDRecSize > (_fileLength - _eocdRecordSize - ZIP64_LOCATOR_SIZE)) {
            return false;
        }

        long calculatedZip64EoCDRecSize = _zip64EoCDRLocatorOffset - offsetInFile;

        return (calculatedZip64EoCDRecSize == specifiedZip64EoCDRecSize);
    }

    /**
     * @param bytes
     * @param offsetInArray pointer to Zip64-End-Of-Central-Directory-Record in the data
     * @return the size of the Zip64-End-Of-Central-Directory-Record 
     */
    private long getZip64EoCDRecordSize(byte[] bytes, int offsetInArray ) {

        long specifiedZip64EoCDRecSize = getLittleEndianValue(bytes, offsetInArray + ZIP64_EOCDR_SIZE_OFFSET, ZIP64_EOCDR_SIZE_FIELD_LENGTH);

        // The recorded size does not include the 12-byte header.   So let's add that
        return specifiedZip64EoCDRecSize + ZIP64_EOCDR_HEADER_SIZE;
    }

    /**
     * @param bytes   array of byte
     * @param offset  beginning of value in array of bytes
     * @param length  Must be <= 4
     * @return the bytes at the offset in reversed order
     */
    private long getLittleEndianValue(byte[] bytes, int offset, int length) {
        long value = 0;
        for (int i = length - 1; i >= 0; i--) {
            value = ((value << 8) | (bytes[offset + i] & 0xFF));
        }
        return value;
    }

    /**
     * @return 4-byte signature using offset into byte array.
     */
    private int getSignature(byte[] bytes, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value = ((value << 8) | (bytes[offset + i] & 0xFF));
        }
        return value;
    }

    /**
     * @return true if the "total # of entries" offset in the End-of-Central-Directory-Record contains 0xFFFF
     */
    private boolean isZip64() {
        return getLittleEndianValue(_endOfCentralDirectoryRecord, EOCDR_TOTAL_ENTRIES_OFFSET, 2) == ZIP64_INDICATOR;
    }

    /**
     * Checks if the beginning of the actual archive, which might not be at the 
     * beginning of the file contains the appropriate local-file-header-signature
     * @param file the zip file
     * @param archiveOffset the offset to the beginning of the archive within the file (Usually 0, but data might be attached in front of the archive).
     * @return true if a local file header signature is found at the 'archiveOffset'
     */
    private boolean archiveStartsWithSignature(RandomAccessFile file, long archiveOffset) throws IOException {

        file.seek(archiveOffset);
        byte[] localFileHeaderSigBytes = new byte[4];
        file.read(localFileHeaderSigBytes, 0, 4);

        if (isSignature(localFileHeaderSigBytes, 0, LOCAL_FILE_HEADER_SIGNATURE)) {
            return true;
        }

        return false;
    }

    /**
     * @return  The location in the file where the archive actually starts. That will normally
     * be 0, but often scripts are prepended to the archive; for example to make a self-extracting zip.
     */
    private long getActualArchiveOffset() throws IOException {

        // At offset 12 into the End-of-Central-Directory-Record is the size of the Central Directory 
        long lengthOfCentralDirectory = getLittleEndianValue( _endOfCentralDirectoryRecord, EOCDR_CENTRAL_DIR_SIZE_OFFSET, 4 );

        // Offset 16 contains the relative offset from the beginning of the actual archive to the Central Directory
        long offsetOfCentralDirectory = getLittleEndianValue( _endOfCentralDirectoryRecord, EOCDR_CENTRAL_DIR_OFFSET, 4 );

        long zip64RecLengths = isZip64() ? ( _zip64EoCDRecSize + ZIP64_LOCATOR_SIZE ) : 0;

        return _fileLength - _eocdRecordSize - zip64RecLengths - lengthOfCentralDirectory - offsetOfCentralDirectory;
    }
}
