/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.hpack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.genericbnf.internal.GenericConstants;
import com.ibm.ws.http.channel.h2internal.H2ConnectionSettings;
import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants.ByteFormatType;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants.LiteralIndexType;
import com.ibm.ws.http.channel.h2internal.huffman.HuffmanDecoder;
import com.ibm.ws.http.channel.h2internal.huffman.HuffmanEncoder;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public class H2Headers {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(H2Headers.class,
                                                         GenericConstants.GENERIC_TRACE_NAME,
                                                         null);

    /**
     * Decode header bytes without validating against connection settings
     *
     * @param WsByteBuffer
     * @param H2HeaderTable
     * @return H2HeaderField
     * @throws CompressionException
     */
    public static H2HeaderField decodeHeader(WsByteBuffer buffer, H2HeaderTable table) throws CompressionException {
        return decodeHeader(buffer, table, true, false, null);
    }

    /**
     * Decode header bytes, validating against a given H2ConnectionSettings
     *
     * @param WsByteBuffer
     * @param H2HeaderTable
     * @param isFirstHeader
     * @param H2ConnectionSettings
     * @return H2HeaderField
     * @throws CompressionException if invalid header names or values are found
     */
    public static H2HeaderField decodeHeader(WsByteBuffer buffer, H2HeaderTable table, boolean isFirstHeader,
                                             boolean isTrailerBlock, H2ConnectionSettings settings) throws CompressionException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "decodeHeader");
        }

        if (!table.isDynamicTableValid()) {
            throw new CompressionException("The context for this dynamic table is not valid.");
        }

        if (buffer == null || !buffer.hasRemaining()) {
            throw new CompressionException("Invalid attempt to decode empty or null buffer.");
        }

        //Get first byte for header being decoded
        byte currentByte = buffer.get();
        buffer.position(buffer.position() - 1);
        //Format this instruction byte so that 4 LSB are set to '1',
        //allowing for quick assessment of what instruction this byte
        //contains.
        byte maskedByte = HpackUtils.format(currentByte, HpackConstants.MASK_0F);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Operation Byte: " + HpackUtils.byteToHexString(currentByte));
        }

        int decodedInteger = 0;
        H2HeaderField header = null;

        //If the MSB is 1, the masked byte will be negative since this is
        //a signed byte.
        if (maskedByte < HpackConstants.MASK_00) {

            //Check against the byte '1000000' as the index value of 0
            //for indexed headers is not used and MUST be treated as a
            //decoding error.
            if (currentByte == HpackConstants.MASK_80) {
                throw new CompressionException("An indexed header cannot have an index of 0");
            }
            //Look for the corresponding integer representation - each byte for
            //the integer representation decoder, for this case, has N = 7.

            decodedInteger = IntegerRepresentation.decode(buffer, ByteFormatType.INDEXED);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Operation byte indicated header is already indexed at index location: " + decodedInteger +
                             ". Searching table...");
            }

            //Get both header name and value using the given index location.

            header = table.getHeaderEntry(decodedInteger);

            if (header == null) {
                throw new CompressionException("Received an invalid header index");
            }
            checkIsValidH2Header(header, isTrailerBlock);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found header: [" + header.getName() + ", " + header.getValue() + "]");
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.exit(tc, "decodeHeader");
            }
            return header;
        }

        //Not currently index - verify literal index type and handle
        //accordingly. There are three types of possible encodings for a <HeaderField>.
        //Bits 7, 6, and 5 will tell us what type of encoding this is. There is
        //also the possibility of a table update command.
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Header not fully indexed in table, determining indexing type.");
            }

            /*
             * If bit 7 is '1', then this LiteralIndexType.INCREMENTAL
             *
             * Literal Header Field with Incremental Indexing
             * +---+---+---+---+---+---+---+---+
             * | 0 | 1 | Index (6+) ---------- |
             * +---+---+-----------------------+
             */
            if (maskedByte >= HpackConstants.MASK_4F) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Indexing decode type: INCREMENTAL");
                }

                header = decodeHeader(buffer, table, ByteFormatType.INCREMENTAL);

            } else if (maskedByte >= HpackConstants.MASK_2F) {
                if (!isFirstHeader) {
                    throw new CompressionException("dynamic table size update must occur at the beginning of the first header block");
                }

                int fragmentLength = IntegerRepresentation.decode(buffer, ByteFormatType.TABLE_UPDATE);
                if (settings != null && fragmentLength > settings.getHeaderTableSize()) {
                    throw new CompressionException("dynamic table size update size was larger than SETTINGS_HEADER_TABLE_SIZE");
                }
                table.updateTableSize(fragmentLength);

                return null;
            }

            /*
             * Bit 7 is '0' & bits 6-5 match '01', then this is
             * LiteralIndexType.NEVERINDEX
             */
            else if (maskedByte == HpackConstants.MASK_1F) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Indexing decode type: NEVERINDEX");
                }

                header = decodeHeader(buffer, table, ByteFormatType.NEVERINDEX);

            }

            /*
             * Bit 7 is '0' & bits 6-5 match '00', then this is
             * LiteralIndexType.NOINDEXING
             */
            else if (maskedByte == HpackConstants.MASK_0F) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Indexing decode type: NOINDEXING");
                }

                header = decodeHeader(buffer, table, ByteFormatType.NOINDEXING);

            }

        }

        checkIsValidH2Header(header, isTrailerBlock);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Decoded the header: [" + header.getName() + ", " + header.getValue() + "]");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "decodeHeader");
        }

        return header;

    }

    private static H2HeaderField decodeHeader(WsByteBuffer buffer, H2HeaderTable table, ByteFormatType type) throws CompressionException {
        //Four possible table operations: Indexed, Incremental, Not Indexed, Never Indexed
        //Indexed case is considered in the <HeaderBlock> decode.

        int integerLength = 0;
        String decodedName;
        String decodedValue;
        H2HeaderField header;

        //Decode index location.
        integerLength = IntegerRepresentation.decode(buffer, type);

        //if non-zero, then this matches a name entry stored
        if (integerLength > 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Operation byte indicates header name is indexed at location "
                             + integerLength + ". Searching table for header name...");
            }
            decodedName = table.getHeaderEntry(integerLength).getName();

        }

        else {
            //Name not in table, therefore decode the next fragment
            //to get the name for this header.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Decoding header name.");
            }
            decodedName = decodeFragment(buffer);

            if (decodedName.trim().isEmpty()) {
                throw new CompressionException("Header field names must not be empty.");
            }

            if (!HpackUtils.isAllLower(decodedName)) {
                throw new CompressionException("Header field names must not contain uppercase "
                                               + "characters. Decoded header name: " + decodedName);
            }

        }

        //decode value - same logic as decoding the name.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Decoding header value.");
        }
        decodedValue = decodeFragment(buffer);

        //Create header and Index it if applicable
        header = new H2HeaderField(decodedName, decodedValue);

        if (type == ByteFormatType.INCREMENTAL) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Incremental indexing: adding decoded header to table.");
            }
            table.addHeaderEntry(header);
        }
        return header;
    }

    /**
     * Decode a Fragment of the Header. Fragment in this case refers to the bytes that
     * represent the integer length and octets length of either the Name or Value of
     * a Header. For instance, in the case of the Name, the fragment is
     *
     * +---+---+-----------------------+
     * | H | Name Length (7+) |
     * +---+---------------------------+
     * | Name String (Length octets) |
     * +---+---------------------------+
     *
     * @param buffer Contains all bytes that will be decoded into this <HeaderField>
     * @return String representation of the fragment. Stored as either the key or value of this <HeaderField>
     * @throws CompressionException
     */
    private static String decodeFragment(WsByteBuffer buffer) throws CompressionException {
        String decodedResult = null;

        try {
            byte currentByte = buffer.get();

            //Reset back position to decode the integer length of this segment.
            buffer.position(buffer.position() - 1);
            boolean huffman = (HpackUtils.getBit(currentByte, 7) == 1);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Decoding using huffman encoding: " + huffman);
            }

            //HUFFMAN and NOHUFFMAN ByteFormatTypes have the same
            //integer decoding bits (N=7). Therefore, either enum value
            //is valid for the integer representation decoder.
            int fragmentLength = IntegerRepresentation.decode(buffer, ByteFormatType.HUFFMAN);

            byte[] bytes = new byte[fragmentLength];
            //Transfer bytes from the buffer into byte array.
            buffer.get(bytes);

            if (huffman && bytes.length > 0) {
                HuffmanDecoder decoder = new HuffmanDecoder();
                bytes = decoder.convertHuffmanToAscii(bytes);
            }
            decodedResult = new String(bytes, Charset.forName(HpackConstants.HPACK_CHAR_SET));

        } catch (Exception e) {
            throw new CompressionException("Received an invalid header block fragment");
        }

        return decodedResult;

    }

    public static byte[] encodeHeader(H2HeaderTable table, String name, String value, LiteralIndexType type) throws CompressionException, IOException {
        return encodeHeader(table, name, value, type, true);
    }

    public static byte[] encodeHeader(H2HeaderTable table, String name, String value,
                                      LiteralIndexType type, boolean huffman) throws CompressionException, IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "encodeHeader", "Encoding [" + name + ", " + value);
        }

        if (table == null || !table.isDynamicTableValid()) {
            throw new CompressionException("The context for this dynamic table is not valid.");
        }

        //Build a stream of bytes until we are done encoding
        ByteArrayOutputStream encodedHeader = new ByteArrayOutputStream();
        //H2 header names must be all lower case to be compliant. Ensure this before encoding
        //or modifying table.
        String compliantName = name.toLowerCase();

        //First byte will specify the indexing type and index location, if any, for the header.

        int indexLocation = 0; // default index not in table.

        H2HeaderField indexedHeader = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Searching in table for header...");
        }

        //Look in table for all instances of this header name. API returns when it finds one that
        //matches both header name or value or the last one that matches name. Otherwise, it returns
        //null.
        indexedHeader = table.getHeaderEntry(compliantName, value);

        if (indexedHeader != null) { //found indexed header
            //Location of found indexed header in table
            indexLocation = indexedHeader.getCurrentIndex();

            if (indexedHeader.getValueHash() == value.hashCode()) {
                //If the header name and value both match, then this header is already indexed.
                //Encode using a reference to the table entry. No changes are to be made to the
                //dynamic table. With nothing table operations, return at this point.

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Header name and value found in table at index location: " + indexLocation +
                                 ". Encoding header as INDEXED.");
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.exit(tc, "encodeHeader");
                }
                return IntegerRepresentation.encode(indexLocation, ByteFormatType.INDEXED);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found header name in table at location: " + indexLocation + ".");
            }
        }

        //Encode first byte
        encodedHeader.write(IntegerRepresentation.encode(indexLocation, type));

        //Encode Header Name if necessary
        if (indexLocation == 0) {
            //Will require the header name to be encoded, as there was no matching entry in the table.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Header name not found in table, encoding header name...");
            }
            encodeFragment(encodedHeader, compliantName, huffman);
        }

        //Encode Header Value
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Encoding header value...");
        }
        encodeFragment(encodedHeader, value, huffman);

        //If this header is to be indexed, do so now.

        if (type == LiteralIndexType.INDEX) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Incremental indexing: adding encoded header to table.");
            }
            table.addHeaderEntry(new H2HeaderField(compliantName, value));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "encodeHeader");
        }

        return encodedHeader.toByteArray();

    }

    private static void encodeFragment(ByteArrayOutputStream encodedHeader, String headerFragment, boolean huffman) throws CompressionException, IOException {
        //TODO: consider return type as boolean (false - exception caught, true it encoded fragment)

        byte[] fragmentBytes = headerFragment.getBytes(HpackConstants.HPACK_CHAR_SET);
        ByteFormatType encodingType = ByteFormatType.NOHUFFMAN;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Encoding using huffman encoding: " + huffman);
        }

        if (huffman) {
            fragmentBytes = HuffmanEncoder.convertAsciiToHuffman(fragmentBytes);
            //set encoding type to huffman
            encodingType = ByteFormatType.HUFFMAN;
        }

        //Encode length
        encodedHeader.write(IntegerRepresentation.encode(fragmentBytes.length, encodingType));

        //Put encode value in buffer
        encodedHeader.write(fragmentBytes);

    }

    /**
     * Validate a H2HeaderField object
     *
     * @param H2HeaderField
     * @throws CompressionException if the H2HeaderField is not valid
     */
    private static void checkIsValidH2Header(H2HeaderField header, boolean isTrailerField) throws CompressionException {
        if (!header.getName().startsWith(":")) {
            String headerName = header.getName();
            String headerValue = header.getValue();

            for (String name : HpackConstants.connectionSpecificHeaderList) {
                if (name.equalsIgnoreCase(headerName)) {
                    throw new CompressionException("Invalid Connection header received: " + header.toString());
                }
            }
            if ("Connection".equalsIgnoreCase(headerName) && !"TE".equalsIgnoreCase(headerValue)) {
                throw new CompressionException("Invalid Connection header received: " + header.toString());
            }
            if ("TE".equalsIgnoreCase(headerName) && !"trailers".equalsIgnoreCase(headerValue)) {
                throw new CompressionException("Invalid header: TE header must have value \"trailers\": " + header.toString());
            }
        } else {
            if (isTrailerField) {
                throw new CompressionException("Psuedo-headers are not allowed in trailers: " + header.toString());
            }
        }
    }

    /**
     * Check to see if a given header name is valid to write on an HTTP/2 connection
     *
     * @param H2HeaderField
     * @throws CompressionException if the H2HeaderField is not valid
     */
    public static boolean checkIsValidH2WriteHeader(String headerName) {
        if (!headerName.startsWith(":")) {
            if ("Connection".equalsIgnoreCase(headerName)) {
                return false;
            }
            if ("TE".equalsIgnoreCase(headerName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the content length value from a header, if one exists
     *
     * @param H2HeaderField
     * @return the content-length value, or -1 if no content-length is found
     */
    public static int getContentLengthValue(H2HeaderField header) {
        if ("content-length".equalsIgnoreCase(header.getName())) {
            return Integer.parseInt(header.getValue());
        }
        return -1;
    }

}
