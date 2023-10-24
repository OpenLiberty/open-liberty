/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.security.csiv2.tools;

import java.util.Arrays;

import org.omg.CORBA.Any;
import org.omg.CORBA.Context;
import org.omg.CORBA.ContextList;
import org.omg.CORBA.DomainManager;
import org.omg.CORBA.ExceptionList;
import org.omg.CORBA.InterfaceDef;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.NVList;
import org.omg.CORBA.NamedValue;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.Policy;
import org.omg.CORBA.Request;
import org.omg.CORBA.SetOverrideType;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;

import com.ibm.ws.transport.iiop.security.util.Util;

/**
 * Test codec used while encoding/decoding a token.
 * Only the methods decode_value and encode_value are needed by the current tests.
 */
public class TestCodec extends LocalObject implements Codec {
    @Override
    public Any decode(byte[] data) throws FormatMismatch { return null; }

    @Override
    public Any decode_value(byte[] data, TypeCode tc) throws FormatMismatch, TypeMismatch {
        System.out.println("The data to decode: " + Util.byteToString(data));
        // create an Any using the singleton ORB
        Any any = ORB.init().create_any();
        // use the Any to create a CORBA output stream (not connected to the Any particularly)
        OutputStream os = any.create_output_stream();
        // write the byte array to the CORBA output stream
        os.write_octet_array(data, 0, data.length);
        // read the data from the CORBA output stream into the Any
        any.read_value(os.create_input_stream(), tc);
        return any;
    }

    @Override
    public byte[] encode(Any data) throws InvalidTypeForEncoding { return null; }

    @Override
    public byte[] encode_value(Any data) throws InvalidTypeForEncoding {
        // create an empty CORBA output stream
        OutputStream os = data.create_output_stream();
        // write the value from the Any into the output stream
        data.write_value(os);
        return asBytes(os);
    }

    private static byte[] asBytes(OutputStream os) {
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        // get the input stream for the data in the output stream
        InputStream is = os.create_input_stream();
        try {
            // read one byte at a time until we get a MARSHAL exception
            for (; ; ) {
                // if we've run out of space then double the buffer
                if (bytesRead == buffer.length) buffer = Arrays.copyOf(buffer, buffer.length * 2);
                buffer[bytesRead] = is.read_octet();
                bytesRead++;
            }
        } catch (MARSHAL endOfData) {}
        // trim the buffer to the exact length read
        buffer = Arrays.copyOf(buffer, bytesRead);
        return buffer;
    }
}
