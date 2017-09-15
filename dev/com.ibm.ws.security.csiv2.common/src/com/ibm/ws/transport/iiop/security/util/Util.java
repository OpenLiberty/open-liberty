/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.UnexpectedException;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.portable.IDLEntity;
import org.omg.CORBA.portable.ResponseHandler;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;
import org.omg.CSI.EstablishContext;
import org.omg.CSI.X501DistinguishedNameHelper;
import org.omg.CSI.X509CertificateChainHelper;
import org.omg.GSSUP.GSSUPMechOID;
import org.omg.GSSUP.InitialContextToken;
import org.omg.GSSUP.InitialContextTokenHelper;
import org.omg.IOP.Codec;
import org.omg.Security.OpaqueHelper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.csiv2.config.LTPAMech;
import com.ibm.ws.transport.iiop.asn1.ASN1InputStream;
import com.ibm.ws.transport.iiop.asn1.ASN1TaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERInputStream;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERObjectIdentifier;
import com.ibm.ws.transport.iiop.asn1.DEROutputStream;
import com.ibm.ws.transport.iiop.asn1.x509.GeneralName;
import com.ibm.ws.transport.iiop.asn1.x509.X509Name;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.SASInvalidMechanismException;
import com.ibm.ws.transport.iiop.security.config.tss.TSSConfig;

/**
 * Various utility functions.
 * <p/>
 * Note: #getORB() and #getCodec() rely on UtilInitializer to initialze the ORB and codec.
 * 
 * @version $Rev: 503493 $ $Date: 2007-02-04 13:47:55 -0800 (Sun, 04 Feb 2007) $
 * @see UtilInitializer
 */
public final class Util {
    private static final TraceComponent tc = Tr.register(Util.class);
    private static final byte ASN_TAG_NT_EXPORTED_NAME1 = 0x04;
    private static final byte ASN_TAG_NT_EXPORTED_NAME2 = 0x01;
    private static final byte ASN_TAG_OID = 0x06;
    private static final byte ASN_TAG_GSS = 0x60;
    private static final byte[] ASN_TAG_GSS_BYTE_ARRAY = { ASN_TAG_GSS };

    private static final Map<String, TSSConfig> tssConfigs = new ConcurrentHashMap<String, TSSConfig>();

    //    private static HandleDelegate handleDelegate;
//    private static CorbaApplicationServer corbaApplicationServer = new CorbaApplicationServer();
//    private static HashMap<String, ORBConfiguration> configuredOrbs = new HashMap<String, ORBConfiguration>();

//
//    public static void registerORB(String id, ORBConfiguration orb) {
//        configuredOrbs.put(id, orb);
//    }
//
//    public static ORBConfiguration getRegisteredORB(String id) {
//        return configuredOrbs.get(id);
//    }
//
//    public static void unregisterORB(String id) {
//        configuredOrbs.remove(id);
//    }
//

//    public static HandleDelegate getHandleDelegate() throws NamingException {
//        if (handleDelegate == null) {
//            InitialContext ic = new InitialContext();
//            handleDelegate = (HandleDelegate) ic.lookup("java:comp/HandleDelegate");
//        }
//        return handleDelegate;
//    }
//
//    public static Object getEJBProxy(ProxyInfo info) {
//        if (info.getInterfaceType().isHome()) {
//            return corbaApplicationServer.getEJBHome(info);
//        }
//        else {
//            return corbaApplicationServer.getEJBObject(info);
//        }
//    }

    /**
     * @param orb_id
     * @return
     */
    public static TSSConfig getRegisteredTSSConfig(String orb_id) {
        return tssConfigs.get(orb_id);
    }

    //TODO liberty what should call this??
    public static void registerTSSConfig(String orb_id, TSSConfig tssConfig) {
        tssConfigs.put(orb_id, tssConfig);
    }

    public static byte[] encodeOID(String oid) throws IOException {
        oid = (oid.startsWith("oid:") ? oid.substring(4) : oid);
        return encodeOID(new DERObjectIdentifier(oid));
    }

    public static byte[] encodeOID(DERObjectIdentifier oid) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);

        dOut.writeObject(oid);

        return bOut.toByteArray();
    }

    public static String decodeOID(byte[] oid) throws IOException {
        return decodeOIDDERObjectIdentifier(oid).getId();
    }

    public static DERObjectIdentifier decodeOIDDERObjectIdentifier(byte[] oid) throws IOException {
        ByteArrayInputStream bIn = new ByteArrayInputStream(oid);
        DERInputStream dIn = new DERInputStream(bIn);

        return (DERObjectIdentifier) dIn.readObject();
    }

    public static byte[] encodeGeneralName(String name) throws IOException {
        return encodeGeneralName(new X509Name(name));
    }

    public static byte[] encodeGeneralName(X509Name x509Name) throws IOException {
        return encodeGeneralName(new GeneralName(x509Name));
    }

    public static byte[] encodeGeneralName(GeneralName generalName) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);

        dOut.writeObject(generalName);

        return bOut.toByteArray();
    }

    public static String decodeGeneralName(byte[] name) throws IOException {
        ByteArrayInputStream bIn = new ByteArrayInputStream(name);
        ASN1InputStream aIn = new ASN1InputStream(bIn);
        DERObject dObj = aIn.readObject();
        aIn.close();
        X509Name xName = X509Name.getInstance((ASN1TaggedObject) dObj, true);
        return xName.toString();
    }

    /**
     * This method encodes a name as if it was encoded using the GSS-API
     * gss_export_name() function call (see RFC 2743, page 84).
     * The oid to indicate names of this format is:<br/>
     * {1(iso), 3(org), 6(dod), 1(internet), 5(security), 6(nametypes),
     * 4(gss-api-exported-name)}<br/>
     * The token has the following format:
     * <table>
     * <tr><td><b>Offset</b></td><td><b>Meaning</b></td><td><b>Value</b></td></tr>
     * <tr><td>0</td><td>token id</td><td>0x04</td></tr>
     * <tr><td>1</td><td>token id</td><td>0x01</td></tr>
     * <p/>
     * <tr><td>2</td><td>oid length</td><td>hi-byte (len/0xFF)</td></tr>
     * <tr><td>3</td><td>oid length</td><td>lo-byte (len%0xFF)</td></tr>
     * <p/>
     * <tr><td>4</td><td>oid</td><td>oid:1.3.6.1.5.6.4</td></tr>
     * <p/>
     * <tr><td>n+0</td><td>name length</td><td>len/0xFFFFFF</td></tr>
     * <tr><td>n+1</td><td>name length</td><td>(len%0xFFFFFF)/0xFFFF</td></tr>
     * <tr><td>n+2</td><td>name length</td><td>((len%0xFFFFFF)%0xFFFF)/0xFF</td></tr>
     * <tr><td>n+3</td><td>name length</td><td>((len%0xFFFFFF)%0xFFFF)%0xFF</td></tr>
     * <p/>
     * <tr><td>n+4</td><td>name</td><td>foo</td></tr>
     * </table>
     * 
     * @param oid The oid of the mechanism this name is exported from.
     * @param name The name to be exported.
     * @return The byte array representing the exported name object.
     */
    public static byte[] encodeGSSExportName(String oid, String name) {
        try {
            byte[] oid_arr = encodeOID(oid);
            int oid_len = oid_arr.length;
            byte[] name_arr = name.getBytes("UTF-8");
            int name_len = name_arr.length;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // token id at 0
            baos.write(ASN_TAG_NT_EXPORTED_NAME1);
            baos.write(ASN_TAG_NT_EXPORTED_NAME2);

            // write the two length bytes
            baos.write((byte) (oid_len & 0xFF00) >> 8);
            baos.write((byte) (oid_len & 0x00FF));

            // oid at 2
            baos.write(oid_arr);

            // name length at n
            baos.write((byte) (name_len & 0xFF000000) >> 24);
            baos.write((byte) (name_len & 0x00FF0000) >> 16);
            baos.write((byte) (name_len & 0x0000FF00) >> 8);
            baos.write((byte) (name_len & 0x000000FF));

            // name at n+4
            baos.write(name_arr);
            return baos.toByteArray();
        } catch (Exception ex) {
//            Tr.debug(tc, "Could not encode : " + id);
        }
        return null;
    }

    /**
     * This function reads a name from a byte array which was created
     * by the gssExportName() method.
     * 
     * @param name_tok The GSS name token.
     * @return The name from the GSS name token.
     */

    public static GSSExportedName decodeGSSExportedName(byte[] name_tok) {
        if (name_tok != null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(name_tok);
            try {
                // GSSToken tag 1 0x04
                int t1 = bais.read();
                if (t1 == ASN_TAG_NT_EXPORTED_NAME1) {
                    // GSSToken tag 2 0x01
                    int t2 = bais.read();
                    if (t2 == ASN_TAG_NT_EXPORTED_NAME2) {
                        // read the two length bytes
                        int l = bais.read() << 8;
                        l += bais.read();

                        // read the oid
                        byte[] oid_arr = new byte[l];
                        bais.read(oid_arr, 0, l);
                        String oid = decodeOID(oid_arr);

                        int l1 = bais.read();
                        int l2 = bais.read();
                        int l3 = bais.read();
                        int l4 = bais.read();

                        int name_len = (l1 << 24) + (l2 << 16) + (l3 << 8) + l4;
                        byte[] name_arr = new byte[name_len];
                        bais.read(name_arr, 0, name_len);
                        String name = new String(name_arr);
                        return new GSSExportedName(name, oid);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                // do nothing, return null
            }
        }
        return null;
    }

    private static final Pattern SCOPED_NAME_EXTRACTION_PATTERN = Pattern.compile("(\\\\\\\\)|(\\\\@)|(@)|(\\z)");

    /**
     * See csiv2 spec 16.2.5 par. 63-64. We extract the username if any and un-escape any
     * escaped \ and @ characters.
     * 
     * @param scopedNameBytes
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String extractUserNameFromScopedName(byte[] scopedNameBytes) throws UnsupportedEncodingException {
        String scopedUserName = new String(scopedNameBytes, "UTF8");
        return extractUserNameFromScopedName(scopedUserName);
    }

    public static String extractUserNameFromScopedName(String scopedUserName) {
        Matcher m = SCOPED_NAME_EXTRACTION_PATTERN.matcher(scopedUserName);
        StringBuffer buf = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(buf, "");
            if (m.group(1) != null) {
                buf.append('\\');
            } else if (m.group(2) != null) {
                buf.append("@");
            } else if (m.group(3) != null) {
                break;
            }
        }
        return buf.toString();
    }

    private static final Pattern SCOPED_NAME_ESCAPE_PATTERN = Pattern.compile("(\\\\)|(@)");

    public static String buildScopedUserName(String user, String domain) {
        StringBuffer buf = new StringBuffer();
        if (user != null) {
            escape(user, buf);
        }
        if (domain != null) {
            buf.append('@');
            escape(domain, buf);
        }
        return buf.toString();
    }

    private static void escape(String s, StringBuffer buf) {
        Matcher m = SCOPED_NAME_ESCAPE_PATTERN.matcher(s);
        while (m.find()) {
            m.appendReplacement(buf, "");
            if (m.group(1) != null) {
                buf.append("\\\\");
            } else if (m.group(2) != null) {
                buf.append("\\@");
            }
        }
        m.appendTail(buf);
    }

    /**
     * Encode a mechanism independent initial context token (GSSToken). Defined
     * in [IETF RFC 2743] Section 3.1, "Mechanism-Independent token Format" pp. 81-82.
     * <table>
     * <tr><td><b>Offset</b></td><td><b>Meaning</b></td></tr>
     * <tr><td>0</td><td>ASN1 tag</td></tr>
     * <tr><td>1</td><td>token length (&lt;128)</td></tr>
     * <tr><td>2</td><td>mechanism oid</td></tr>
     * <tr><td>n</td><td>mechanism specific token (e.g. GSSUP::InitialContextToken)</td></tr>
     * </table>
     * Currently only one mechanism specific token is supported: GSS username password
     * (GSSUP::InitialContextToken).
     * 
     * @param codec The codec to do the encoding of the Any.
     * @param user The username.
     * @param pwd The password of the user.
     * @param target The target name.
     * @return The byte array of the ASN1 encoded GSSToken.
     */
    @Sensitive
    public static byte[] encodeGSSUPToken(Codec codec, String user, @Sensitive char[] pwd, String target) {
        byte[] result = null;
        try {
            // create and encode a GSSUP initial context token
            InitialContextToken init_token = new InitialContextToken();
            init_token.username = user.getBytes("UTF-8");
            init_token.password = getUTF8Bytes(pwd);
            init_token.target_name = encodeGSSExportName(GSSUPMechOID.value.substring(4), target);

            Any a = ORB.init().create_any();
            InitialContextTokenHelper.insert(a, init_token);
            byte[] init_ctx_token = codec.encode_value(a);

            result = createGSSToken(codec, GSSUPMechOID.value, init_ctx_token);
        } catch (Exception ex) {
            // do nothing, return null
        }
        return result;
    }

    /*
     * In Liberty a single GSSToken is created.
     */
    @Sensitive
    public static byte[] encodeLTPAToken(Codec codec, @Sensitive byte[] ltpaTokenBytes) {
        byte[] result = null;
        try {
            // create and encode the initial context token
            Any a = ORB.init().create_any();
            OpaqueHelper.insert(a, ltpaTokenBytes);
            byte[] init_ctx_token = codec.encode_value(a);

            result = createGSSToken(codec, LTPAMech.LTPA_OID, init_ctx_token);
        } catch (Exception ex) {
            // do nothing, return null
        }
        return result;
    }

    /**
     * WAS classic needs the GSSToken containing the encoded LTPA token inside another GSSToken
     * 
     * @param codec The codec to do the encoding of the Any.
     * @param ltpaTokenBytes the bytes of the LTPA token to encode.
     * 
     * @return the GSSToken containing the LTPA token wrapped inside another GSSToken.
     */
    @Sensitive
    public static byte[] encodeLTPATokenForWASClassic(Codec codec, @Sensitive byte[] ltpaTokenBytes) {
        byte[] result = null;
        try {
            byte[] innerGSSToken = encodeLTPAToken(codec, ltpaTokenBytes);
            if (innerGSSToken != null) {
                result = createGSSToken(codec, LTPAMech.LTPA_OID, innerGSSToken);
            }
        } catch (Exception ex) {
            // do nothing, return null
        }
        return result;
    }

    @Sensitive
    private static byte[] createGSSToken(Codec codec, String oid, @Sensitive byte[] data) throws Exception {
        // write the GSS ASN tag
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ASN_TAG_GSS);

        // encode the mechanism oid
        byte[] oid_arr = encodeOID(oid);

        // write the length
        writeTokenLength(baos, oid_arr.length + data.length);

        // write the mechanism oid
        baos.write(oid_arr);

        // write the data
        baos.write(data);

        // get the bytes
        return baos.toByteArray();
    }

    /*
     * Write the token length in the same way as in tWAS to ensure compatibility.
     */
    private static void writeTokenLength(ByteArrayOutputStream baos, int tokenLength) {
        if (tokenLength < 128) {
            baos.write((byte) tokenLength);
        } else if (tokenLength < (1 << 8)) {
            baos.write((byte) 0x081);
            baos.write((byte) tokenLength);
        } else if (tokenLength < (1 << 16)) {
            baos.write((byte) 0x082);
            baos.write((byte) (tokenLength >> 8));
            baos.write((byte) tokenLength);
        } else if (tokenLength < (1 << 24)) {
            baos.write((byte) 0x083);
            baos.write((byte) (tokenLength >> 16));
            baos.write((byte) (tokenLength >> 8));
            baos.write((byte) tokenLength);
        } else {
            baos.write((byte) 0x084);
            baos.write((byte) (tokenLength >> 24));
            baos.write((byte) (tokenLength >> 16));
            baos.write((byte) (tokenLength >> 8));
            baos.write((byte) tokenLength);
        }
    }

    /**
     * @param pwd
     * @return
     */
    @Sensitive
    private static byte[] getUTF8Bytes(@Sensitive char[] pwd) {
        Charset utf8 = Charset.forName("UTF-8");
        ByteBuffer bb = utf8.encode(CharBuffer.wrap(pwd));
        try {
            bb.rewind(); // not clear whether this is necessary
            byte[] bytes = new byte[bb.limit()];
            bb.get(bytes);
            return bytes;
        } finally {
            // fill it with zeroes
            bb.rewind();
            bb.put(new byte[bb.limit()]);
        }
    }

    /**
     * Decode an GSSUP InitialContextToken from a GSSToken.
     * 
     * @param codec The codec to do the encoding of the Any.
     * @param gssup_tok The InitialContextToken struct to fill in the decoded values.
     * @return Return true when decoding was successful, false otherwise.
     */
    public static boolean decodeGSSUPToken(Codec codec, @Sensitive byte[] token_arr,
                                           InitialContextToken gssup_tok) {
        boolean result = false;
        if (gssup_tok != null) {
            try {
                byte[] data = readGSSTokenData(GSSUPMechOID.value.substring(4), token_arr);
                if (data != null) {
                    Any a = codec.decode_value(data, InitialContextTokenHelper.type());
                    InitialContextToken token = InitialContextTokenHelper.extract(a);
                    if (token != null) {
                        gssup_tok.username = token.username;
                        gssup_tok.password = token.password;
                        gssup_tok.target_name = decodeGSSExportedName(token.target_name).getName().getBytes("UTF-8");

                        result = true;
                    }
                }
            } catch (Exception ex) {
                // do nothing, return false
            }
        }
        return result;
    }

    /**
     * WAS classic encodes the GSSToken containing the encoded LTPA token inside another GSSToken.
     * This code detects if there is another GSSToken inside the GSSToken,
     * obtains the internal LTPA token bytes, and decodes them.
     * 
     * @param codec The codec to do the encoding of the Any.
     * @param token_arr the bytes of the GSS token to decode.
     * 
     * @return the LTPA token bytes.
     */
    @Sensitive
    public static byte[] decodeLTPAToken(Codec codec, @Sensitive byte[] token_arr) throws SASException {
        byte[] ltpaTokenBytes = null;

        try {
            byte[] data = readGSSTokenData(LTPAMech.LTPA_OID.substring(4), token_arr);
            if (data != null) {
                // Check if it is double-encoded WAS classic token and get the encoded ltpa token bytes
                if (isGSSToken(LTPAMech.LTPA_OID.substring(4), data)) {
                    data = readGSSTokenData(LTPAMech.LTPA_OID.substring(4), data);
                }

                Any any = codec.decode_value(data, org.omg.Security.OpaqueHelper.type());
                ltpaTokenBytes = org.omg.Security.OpaqueHelper.extract(any);
            }
        } catch (Exception ex) {
            // TODO: Modify SASException to take a message?
            throw new SASException(2, ex);
        }
        if (ltpaTokenBytes == null || ltpaTokenBytes.length == 0) {
            throw new SASException(2);
        }
        return ltpaTokenBytes;
    }

    @FFDCIgnore(Exception.class)
    public static boolean isGSSToken(String expectedOID, @Sensitive byte[] data) {
        boolean result = false;

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        int c = bais.read();
        try {
            if (c == ASN_TAG_GSS) {
                // GSSToken length
                int token_len = readTokenLength(bais);
                // OID tag
                int oid_tag = bais.read();
                if (oid_tag == ASN_TAG_OID) {
                    // OID length
                    int oid_len = bais.read();
                    String oid = readOID(bais, oid_len);
                    if (oid.equals(expectedOID)) {
                        result = true;
                    }
                }
            }
        } catch (Exception e) {
            // It is not a GSS Token
        }
        return result;
    }

    @Sensitive
    public static byte[] readGSSTokenData(String expectedOID, @Sensitive byte[] gssToken) throws Exception {
        byte[] data = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(gssToken);
        // GSSToken tag
        int c = bais.read();
        if (c == ASN_TAG_GSS) {
            // GSSToken length
            int token_len = readTokenLength(bais);
            // OID tag
            int oid_tag = bais.read();
            if (oid_tag == ASN_TAG_OID) {
                // OID length
                int oid_len = bais.read();
                String oid = readOID(bais, oid_len);
                if (oid.equals(expectedOID)) { // TODO: Determine if this check is needed.
                    // TODO: Check if this is off if length is greater than 128.
                    // Subtract 2 because the ASN_TAG_OID and the OID length were part of the total.
                    int len = token_len - (oid_len + 2);
                    data = new byte[len];
                    bais.read(data, 0, len);
                }
            }
        }
        return data;
    }

    /*
     * WARNING: The stream must have read up to the token length.
     * Read token length the same way as tWAS to ensure compatibility.
     * tWAS encodes lengths greater than 128 using a marker followed by 1, 2, 3, or 4 bytes.
     */
    private static int readTokenLength(ByteArrayInputStream bais) {
        int tokenLength = 0;
        byte lengthMarker = (byte) bais.read();

        if (lengthMarker == (byte) 0x081) {
            int firstByte = bais.read();
            tokenLength = firstByte;
        } else if (lengthMarker == (byte) 0x082) {
            int secondByte = bais.read();
            int firstByte = bais.read();
            tokenLength = (secondByte << 8) + firstByte;
        } else if (lengthMarker == (byte) 0x083) {
            int thirdByte = bais.read();
            int secondByte = bais.read();
            int firstByte = bais.read();
            tokenLength = (thirdByte << 16) + (secondByte << 8) + firstByte;
        } else if (lengthMarker == (byte) 0x084) {
            int fourthByte = bais.read();
            int thirdByte = bais.read();
            int secondByte = bais.read();
            int firstByte = bais.read();
            tokenLength = (fourthByte << 24) + (thirdByte << 16) + (secondByte << 8) + firstByte;
        } else {
            // If not one of the markers above, this is the actual length.
            tokenLength = lengthMarker;
        }

        return tokenLength;
    }

    /*
     * WARNING: The stream must have read up to the OID length.
     * This method reads and decodes the OID after the length.
     */
    private static String readOID(ByteArrayInputStream bais, int oid_len) throws IOException {
        byte[] oid_tmp_arr = new byte[oid_len];
        bais.read(oid_tmp_arr, 0, oid_len);
        byte[] oid_arr = new byte[oid_len + 2];
        oid_arr[0] = ASN_TAG_OID;
        oid_arr[1] = (byte) oid_len;
        System.arraycopy(oid_tmp_arr, 0, oid_arr, 2, oid_len);
        return decodeOID(oid_arr);
    }

    /**
     * Encode a distinguished name into a codec encoded ASN.1 X501 encoded Distinguished Name.
     * 
     * @param codec The codec to do the encoding of the Any.
     * @param distinguishedName The distinguished name.
     * @return the codec encoded ASN.1 X501 encoded Distinguished Name.
     * @throws Exception
     */
    public static byte[] encodeDN(Codec codec, String distinguishedName) throws Exception {
        X500Principal issuer = new X500Principal(distinguishedName);
        X509CertSelector certSelector = new X509CertSelector();
        certSelector.setIssuer(issuer);
        byte[] asnX501DN = certSelector.getIssuerAsBytes();

        Any a = ORB.init().create_any();
        X501DistinguishedNameHelper.insert(a, asnX501DN);
        return codec.encode_value(a);
    }

    /**
     * Decode a distinguished name from an ASN.1 X501 encoded Distinguished Name
     * 
     * @param codec The codec to do the decoding of the Any.
     * @param encodedDN The codec encoded byte[] containing the ASN.1 X501 encoded Distinguished Name.
     * @return the distinguished name.
     * @throws SASException
     */
    public static String decodeDN(Codec codec, byte[] encodedDN) throws SASException {
        String dn = null;

        try {
            Any any = codec.decode_value(encodedDN, X501DistinguishedNameHelper.type());
            byte[] asnX501DN = X501DistinguishedNameHelper.extract(any);

            X500Principal x500Principal = new X500Principal(asnX501DN);
            // To maintain compatibility with tWAS, the toString() method
            // "is intentionally used because this is the only method which decodes extended attributes"
            dn = x500Principal.toString();
        } catch (Exception e) {
            throw new SASException(1, e);
        }
        return dn;
    }

    /**
     * Encode an X509 certificate chain.
     * This logic was brought from tWAS' SessionEntry.convertCertChainToBytes to maintain compatibility.
     * 
     * @param codec The codec to do the encoding of the Any.
     * @param certificateChain The X509 certificate chain to encode.
     * @return the codec encoded X509 certificate chain.
     */
    @Sensitive
    public static byte[] encodeCertChain(Codec codec, @Sensitive X509Certificate[] certificateChain) {
        byte[] result = null;

        try {
            if (certificateChain != null) {
                List<X509Certificate> certList = getCertChainAsListWithoutDuplicates(certificateChain);

                CertificateFactory cfx = CertificateFactory.getInstance("X.509");
                CertPath certPath = cfx.generateCertPath(certList);

                Any any = ORB.init().create_any();
                org.omg.CSI.X509CertificateChainHelper.insert(any, certPath.getEncoded());
                result = codec.encode_value(any);
            }
        } catch (Exception e) {
            // do nothing, return null
        }
        return result;
    }

    @Sensitive
    private static List<X509Certificate> getCertChainAsListWithoutDuplicates(@Sensitive X509Certificate[] certificateChain) throws CertificateException, CertificateEncodingException {
        List<X509Certificate> certChainAsList = new ArrayList<X509Certificate>(certificateChain.length);
        HashSet<X509Certificate> uniqueCertificates = new HashSet<X509Certificate>(certificateChain.length);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

        for (int i = 0; i < certificateChain.length; i++) {
            ByteArrayInputStream bais = new ByteArrayInputStream(certificateChain[i].getEncoded());
            X509Certificate tmpClientCert = (java.security.cert.X509Certificate) certificateFactory.generateCertificate(bais);

            // Prevent duplicates
            if (uniqueCertificates.add(tmpClientCert)) {
                certChainAsList.add(tmpClientCert);
            }
        }
        return certChainAsList;
    }

    /**
     * Decode an X509 certificate chain.
     * 
     * @param codec The codec to do the decoding of the Any.
     * @param encodedCertChain The codec encoded byte[] containing the X509 certificate chain.
     * @return the X509 certificate chain
     * @throws SASException
     */
    @SuppressWarnings("unchecked")
    @Sensitive
    public static X509Certificate[] decodeCertChain(Codec codec, @Sensitive byte[] encodedCertChain) throws SASException {
        X509Certificate[] certificateChain = null;

        try {
            Any any = codec.decode_value(encodedCertChain, X509CertificateChainHelper.type());
            byte[] decodedCertificateChain = X509CertificateChainHelper.extract(any);

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(decodedCertificateChain);
            CertPath certPath = certificateFactory.generateCertPath(bais);
            List<X509Certificate> certificates = (List<X509Certificate>) certPath.getCertificates();

            certificateChain = new X509Certificate[certificates.size()];
            for (int i = 0; i < certificates.size(); i++) {
                certificateChain[i] = certificates.get(i);
            }
        } catch (Exception e) {
            throw new SASException(1, e);
        }
        return certificateChain;
    }

    public static String byteToString(byte[] data) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            buffer.append(HEXCHAR[(data[i] >>> 4) & 0x0F]);
            buffer.append(HEXCHAR[(data[i]) & 0x0F]);
        }
        return buffer.toString();

    }

    private static final char[] HEXCHAR = {
                                           '0', '1', '2', '3', '4', '5', '6', '7',
                                           '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

//    public static void writeObject(Class<?> type, Object object, OutputStream out) {
//        if (type == Void.TYPE) {
//            // do nothing for a void
//        } else if (type == Boolean.TYPE) {
//            out.write_boolean(((Boolean) object).booleanValue());
//        } else if (type == Byte.TYPE) {
//            out.write_octet(((Byte) object).byteValue());
//        } else if (type == Character.TYPE) {
//            out.write_wchar(((Character) object).charValue());
//        } else if (type == Double.TYPE) {
//            out.write_double(((Double) object).doubleValue());
//        } else if (type == Float.TYPE) {
//            out.write_float(((Float) object).floatValue());
//        } else if (type == Integer.TYPE) {
//            out.write_long(((Integer) object).intValue());
//        } else if (type == Long.TYPE) {
//            out.write_longlong(((Long) object).longValue());
//        } else if (type == Short.TYPE) {
//            out.write_short(((Short) object).shortValue());
//        } else {
//            // object types must bbe written in the context of the corba application server
//            // which properly write replaces our objects for corba
//            //TODO liberty what corresponds to the ServerFederation???
////            ApplicationServer oldApplicationServer = ServerFederation.getApplicationServer();
//            try {
////                ServerFederation.setApplicationServer(corbaApplicationServer);
//
//                // todo check if
//                // copy the result to force replacement
//                // corba does not call writeReplace on remote proxies
//                //
//                // HOWEVER, if this is an array, then we don't want to do the replacement 
//                // because we can end up with a replacement element that's not compatible with the 
//                // original array type, which results in an ArrayStoreException.  Fortunately, 
//                // the Yoko RMI support appears to be able to sort this out for us correctly. 
//                if (object instanceof Serializable && !object.getClass().isArray()) {
//                    try {
//                        object = copyObj(Thread.currentThread().getContextClassLoader(), object);
//                    } catch (Exception e) {
//                        Tr.debug(tc, "Exception in result copy", e);
//                        throw new UnknownException(e);
//                    }
//                }
//
//                if (type == Object.class || type == Serializable.class) {
//                    javax.rmi.CORBA.Util.writeAny(out, object);
//                } else if (org.omg.CORBA.Object.class.isAssignableFrom(type)) {
//                    out.write_Object((org.omg.CORBA.Object) object);
//                } else if (Remote.class.isAssignableFrom(type)) {
//                    javax.rmi.CORBA.Util.writeRemoteObject(out, object);
//                } else if (type.isInterface() && Serializable.class.isAssignableFrom(type)) {
//                    javax.rmi.CORBA.Util.writeAbstractObject(out, object);
//                } else {
//                    out.write_value((Serializable) object, type);
//                }
//            } finally {
////                ServerFederation.setApplicationServer(oldApplicationServer);
//            }
//        }
//    }
//
//    private static Object copyObj(ClassLoader classLoader, Object object) throws IOException, ClassNotFoundException {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        ObjectOutputStream oos = new ObjectOutputStream(baos);
//        oos.writeObject(object);
//        oos.flush();
//        oos.close();
//        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//        ObjectInputStreamExt ois = new ObjectInputStreamExt(bais, classLoader);
//        return ois.readObject();
//    }
//
//    public static Object readObject(Class<?> type, InputStream in) {
//        if (type == Void.TYPE) {
//            return null;
//        } else if (type == Boolean.TYPE) {
//            return Boolean.valueOf(in.read_boolean());
//        } else if (type == Byte.TYPE) {
//            return Byte.valueOf(in.read_octet());
//        } else if (type == Character.TYPE) {
//            return Character.valueOf(in.read_wchar());
//        } else if (type == Double.TYPE) {
//            return Double.valueOf(in.read_double());
//        } else if (type == Float.TYPE) {
//            return Float.valueOf(in.read_float());
//        } else if (type == Integer.TYPE) {
//            return Integer.valueOf(in.read_long());
//        } else if (type == Long.TYPE) {
//            return Long.valueOf(in.read_longlong());
//        } else if (type == Short.TYPE) {
//            return Short.valueOf(in.read_short());
//        } else if (type == Object.class || type == Serializable.class) {
//            return javax.rmi.CORBA.Util.readAny(in);
//        } else if (org.omg.CORBA.Object.class.isAssignableFrom(type)) {
//            return in.read_Object(type);
//        } else if (Remote.class.isAssignableFrom(type)) {
//            return PortableRemoteObject.narrow(in.read_Object(), type);
//        } else if (type.isInterface() && Serializable.class.isAssignableFrom(type)) {
//            return in.read_abstract_interface();
//        } else {
//            return in.read_value(type);
//        }
//    }

    public static void throwException(Method method, InputStream in) throws Throwable {
        // read the exception id
        final String id = in.read_string();

        // get the class name from the id
        if (!id.startsWith("IDL:")) {
            Tr.error(tc, "Malformed exception id: " + id);
            return;
        }

        Class<?>[] exceptionTypes = method.getExceptionTypes();
        for (int i = 0; i < exceptionTypes.length; i++) {
            Class<?> exceptionType = exceptionTypes[i];

            String exceptionId = getExceptionId(exceptionType);
            if (id.equals(exceptionId)) {
                throw (Throwable) in.read_value(exceptionType);
            }
        }
        throw new UnexpectedException(id);
    }

    public static OutputStream writeUserException(Method method, ResponseHandler reply, Exception exception) throws Exception {
        if (exception instanceof RuntimeException || exception instanceof RemoteException) {
            throw exception;
        }

        Class<?>[] exceptionTypes = method.getExceptionTypes();
        for (int i = 0; i < exceptionTypes.length; i++) {
            Class<?> exceptionType = exceptionTypes[i];
            if (!exceptionType.isInstance(exception)) {
                continue;
            }

            OutputStream out = (OutputStream) reply.createExceptionReply();
            String exceptionId = getExceptionId(exceptionType);
            out.write_string(exceptionId);
            out.write_value(exception);
            return out;
        }
        throw exception;
    }

    private static String getExceptionId(Class<?> exceptionType) {
        String exceptionName = exceptionType.getName().replace('.', '/');
        if (exceptionName.endsWith("Exception")) {
            exceptionName = exceptionName.substring(0, exceptionName.length() - "Exception".length());
        }
        exceptionName += "Ex";
        String exceptionId = "IDL:" + exceptionName + ":1.0";
        return exceptionId;
    }

    public static String[] createCorbaIds(Class<?> type) {
        List<String> ids = new LinkedList<String>();
        for (Class<?> superInterface : getAllInterfaces(type)) {
            if (Remote.class.isAssignableFrom(superInterface) && superInterface != Remote.class) {
                ids.add("RMI:" + superInterface.getName() + ":0000000000000000");
            }
        }
        return ids.toArray(new String[ids.size()]);
    }

    private static Set<Class<?>> getAllInterfaces(Class<?> intfClass) {
        Set<Class<?>> allInterfaces = new LinkedHashSet<Class<?>>();

        LinkedList<Class<?>> stack = new LinkedList<Class<?>>();
        stack.addFirst(intfClass);

        while (!stack.isEmpty()) {
            Class<?> intf = stack.removeFirst();
            allInterfaces.add(intf);
            stack.addAll(0, Arrays.asList(intf.getInterfaces()));
        }

        return allInterfaces;
    }

    public static Map<Method, String> mapMethodToOperation(Class<?> intfClass) {
        HashMap<Method, String> methodToOperation = new HashMap<Method, String>();
        iiopMap(intfClass, methodToOperation, null);
        return methodToOperation;
    }

    public static Map<String, Method> mapOperationToMethod(Class<?> intfClass) {
        HashMap<String, Method> operationToMethod = new HashMap<String, Method>();
        iiopMap(intfClass, null, operationToMethod);
        return operationToMethod;
    }

    private static void iiopMap(Class<?> intfClass, HashMap<Method, String> methodToOperation, HashMap<String, Method> operationToMethod) {
        Method[] methods = getAllMethods(intfClass);

        // find every valid getter
        HashMap<Method, String> getterByMethod = new HashMap<Method, String>(methods.length);
        HashMap<String, Method> getterByName = new HashMap<String, Method>(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String methodName = method.getName();

            // no arguments allowed
            if (method.getParameterTypes().length != 0) {
                continue;
            }

            // must start with get or is
            String verb;
            if (methodName.startsWith("get") && methodName.length() > 3 && method.getReturnType() != void.class) {
                verb = "get";
            } else if (methodName.startsWith("is") && methodName.length() > 2 && method.getReturnType() == boolean.class) {
                verb = "is";
            } else {
                continue;
            }

            // must only throw Remote or Runtime Exceptions
            boolean exceptionsValid = true;
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            for (int j = 0; j < exceptionTypes.length; j++) {
                Class<?> exceptionType = exceptionTypes[j];
                if (!RemoteException.class.isAssignableFrom(exceptionType) &&
                    !RuntimeException.class.isAssignableFrom(exceptionType) &&
                    !Error.class.isAssignableFrom(exceptionType)) {
                    exceptionsValid = false;
                    break;
                }
            }
            if (!exceptionsValid) {
                continue;
            }

            String propertyName;
            if (methodName.length() > verb.length() + 1 && Character.isUpperCase(methodName.charAt(verb.length() + 1))) {
                propertyName = methodName.substring(verb.length());
            } else {
                propertyName = Character.toLowerCase(methodName.charAt(verb.length())) + methodName.substring(verb.length() + 1);
            }
            getterByMethod.put(method, propertyName);
            getterByName.put(propertyName, method);
        }

        HashMap<Method, String> setterByMethod = new HashMap<Method, String>(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String methodName = method.getName();

            // must have exactally one arg
            if (method.getParameterTypes().length != 1) {
                continue;
            }

            // must return non void
            if (method.getReturnType() != void.class) {
                continue;
            }

            // must start with set
            if (!methodName.startsWith("set") || methodName.length() <= 3) {
                continue;
            }

            // must only throw Remote or Runtime Exceptions
            boolean exceptionsValid = true;
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            for (int j = 0; j < exceptionTypes.length; j++) {
                Class<?> exceptionType = exceptionTypes[j];
                if (!RemoteException.class.isAssignableFrom(exceptionType) &&
                    !RuntimeException.class.isAssignableFrom(exceptionType) &&
                    !Error.class.isAssignableFrom(exceptionType)) {
                    exceptionsValid = false;
                    break;
                }
            }
            if (!exceptionsValid) {
                continue;
            }

            String propertyName;
            if (methodName.length() > 4 && Character.isUpperCase(methodName.charAt(4))) {
                propertyName = methodName.substring(3);
            } else {
                propertyName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            }

            // must have a matching getter
            Method getter = getterByName.get(propertyName);
            if (getter == null) {
                continue;
            }

            // setter property must match gettter return value
            if (!method.getParameterTypes()[0].equals(getter.getReturnType())) {
                continue;
            }
            setterByMethod.put(method, propertyName);
        }

        // index the methods by name... used to determine which methods are overloaded
        HashMap<String, List<Method>> overloadedMethods = new HashMap<String, List<Method>>(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (getterByMethod.containsKey(method) || setterByMethod.containsKey(method)) {
                continue;
            }
            String methodName = method.getName();
            List<Method> methodList = overloadedMethods.get(methodName);
            if (methodList == null) {
                methodList = new LinkedList<Method>();
                overloadedMethods.put(methodName, methodList);
            }
            methodList.add(method);
        }

        // index the methods by lower case name... used to determine which methods differ only by case
        HashMap<String, Set<String>> caseCollisionMethods = new HashMap<String, Set<String>>(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (getterByMethod.containsKey(method) || setterByMethod.containsKey(method)) {
                continue;
            }
            String lowerCaseMethodName = method.getName().toLowerCase();
            Set<String> methodSet = caseCollisionMethods.get(lowerCaseMethodName);
            if (methodSet == null) {
                methodSet = new HashSet<String>();
                caseCollisionMethods.put(lowerCaseMethodName, methodSet);
            }
            methodSet.add(method.getName());
        }

        String className = getClassName(intfClass);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            String iiopName = getterByMethod.get(method);
            if (iiopName != null) {
                // if we have a leading underscore prepend with J
                if (iiopName.charAt(0) == '_') {
                    iiopName = "J_get_" + iiopName.substring(1);
                } else {
                    iiopName = "_get_" + iiopName;
                }
            } else {
                iiopName = setterByMethod.get(method);
                if (iiopName != null) {
                    // if we have a leading underscore prepend with J
                    if (iiopName.charAt(0) == '_') {
                        iiopName = "J_set_" + iiopName.substring(1);
                    } else {
                        iiopName = "_set_" + iiopName;
                    }
                } else {
                    iiopName = method.getName();

                    // if we have a leading underscore prepend with J
                    if (iiopName.charAt(0) == '_') {
                        iiopName = "J" + iiopName;
                    }
                }
            }

            // if this name only differs by case add the case index to the end
            Set<String> caseCollisions = caseCollisionMethods.get(method.getName().toLowerCase());
            if (caseCollisions != null && caseCollisions.size() > 1) {
                iiopName += upperCaseIndexString(iiopName);
            }

            // if this is an overloaded method append the parameter string
            List<Method> overloads = overloadedMethods.get(method.getName());
            if (overloads != null && overloads.size() > 1) {
                iiopName += buildOverloadParameterString(method.getParameterTypes());
            }

            // if we have a leading underscore prepend with J
            iiopName = replace(iiopName, '$', "U0024");

            // if we have matched a keyword prepend with an underscore
            if (keywords.contains(iiopName.toLowerCase())) {
                iiopName = "_" + iiopName;
            }

            // if the name is the same as the class name, append an underscore
            if (iiopName.equalsIgnoreCase(className)) {
                iiopName += "_";
            }

            if (operationToMethod != null) {
                operationToMethod.put(iiopName, method);
            }
            if (methodToOperation != null) {
                methodToOperation.put(method, iiopName);
            }
        }

    }

    private static Method[] getAllMethods(Class<?> intfClass) {
        LinkedList<Method> methods = new LinkedList<Method>();
        for (Class<?> intf : getAllInterfaces(intfClass)) {
            methods.addAll(Arrays.asList(intf.getDeclaredMethods()));
        }

        return methods.toArray(new Method[methods.size()]);
    }

    /**
     * Return the a string containing an underscore '_' index of each uppercase character in the iiop name.
     * 
     * This is used for distinction of names that only differ by case, since corba does not support case sensitive names.
     */
    private static String upperCaseIndexString(String iiopName) {
        StringBuilder StringBuilder = new StringBuilder();
        for (int i = 0; i < iiopName.length(); i++) {
            char c = iiopName.charAt(i);
            if (Character.isUpperCase(c)) {
                StringBuilder.append('_').append(i);
            }
        }
        return StringBuilder.toString();
    }

    /**
     * Replaces any occurnace of the specified "oldChar" with the nes string.
     * 
     * This is used to replace occurances if '$' in corba names since '$' is a special character
     */
    private static String replace(String source, char oldChar, String newString) {
        StringBuilder StringBuilder = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == oldChar) {
                StringBuilder.append(newString);
            } else {
                StringBuilder.append(c);
            }
        }
        return StringBuilder.toString();
    }

    /**
     * Return the a string containing a double underscore '__' list of parameter types encoded using the Java to IDL rules.
     * 
     * This is used for distinction of methods that only differ by parameter lists.
     */
    private static String buildOverloadParameterString(Class<?>[] parameterTypes) {
        StringBuilder name = new StringBuilder();
        if (parameterTypes.length == 0) {
            name.append("__");
        } else {
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                name.append(buildOverloadParameterString(parameterType));
            }
        }
        int index = -1;
        while ((index = name.indexOf(".", index)) > -1) {
            name.setCharAt(index, '_');
        }
        return name.toString();
    }

    /**
     * Returns a single parameter type encoded using the Java to IDL rules.
     */
    private static String buildOverloadParameterString(Class<?> parameterType) {
        String name = "_";

        int arrayDimensions = 0;
        while (parameterType.isArray()) {
            arrayDimensions++;
            parameterType = parameterType.getComponentType();
        }

        // arrays start with org_omg_boxedRMI_
        if (arrayDimensions > 0) {
            name += "_org_omg_boxedRMI";
        }

        // IDLEntity types must be prefixed with org_omg_boxedIDL_
        if (IDLEntity.class.isAssignableFrom(parameterType)) {
            name += "_org_omg_boxedIDL";
        }

        // add package... some types have special mappings in corba
        String packageName = specialTypePackages.get(parameterType.getName());
        if (packageName == null) {
            packageName = getPackageName(parameterType.getName());
        }
        if (packageName.length() > 0) {
            name += "_" + packageName;
        }

        // arrays now contain a dimension indicator
        if (arrayDimensions > 0) {
            name += "_" + "seq" + arrayDimensions;
        }

        // add the class name
        String className = specialTypeNames.get(parameterType.getName());
        if (className == null) {
            className = buildClassName(parameterType);
        }
        name += "_" + className;

        return name;
    }

    /**
     * Returns a string contianing an encoded class name.
     */
    private static String buildClassName(Class<?> type) {
        if (type.isArray()) {
            throw new IllegalArgumentException("type is an array: " + type);
        }

        // get the classname
        String typeName = type.getName();
        int endIndex = typeName.lastIndexOf('.');
        if (endIndex < 0) {
            return typeName;
        }
        StringBuilder className = new StringBuilder(typeName.substring(endIndex + 1));

        // for innerclasses replace the $ separator with two underscores
        // we can't just blindly replace all $ characters since class names can contain the $ character
        if (type.getDeclaringClass() != null) {
            String declaringClassName = getClassName(type.getDeclaringClass());
            assert className.toString().startsWith(declaringClassName + "$");
            className.replace(declaringClassName.length(), declaringClassName.length() + 1, "__");
        }

        // if we have a leading underscore prepend with J
        if (className.charAt(0) == '_') {
            className.insert(0, "J");
        }
        return className.toString();
    }

    private static String getClassName(Class<?> type) {
        if (type.isArray()) {
            throw new IllegalArgumentException("type is an array: " + type);
        }

        // get the classname
        String typeName = type.getName();
        int endIndex = typeName.lastIndexOf('.');
        if (endIndex < 0) {
            return typeName;
        }
        return typeName.substring(endIndex + 1);
    }

    private static String getPackageName(String interfaceName) {
        int endIndex = interfaceName.lastIndexOf('.');
        if (endIndex < 0) {
            return "";
        }
        return interfaceName.substring(0, endIndex);
    }

    public static String getMechOidFromAuthToken(Codec codec, EstablishContext msg) throws SASException
    {
        if (msg == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "establish context is null");
            return null;
        }
        byte[] authToken = msg.client_authentication_token;

        if ((authToken == null) || (authToken.length == 0))
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "authToken is null or blank");
            return null;
        }

        int i = 0;
        // decode for tag
        if (authToken[i] != ASN_TAG_GSS_BYTE_ARRAY[0])
        //        if (authToken[i] != ASN_TAG_OID)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Invalid token form encountered.");
            throw new SASInvalidMechanismException();
        }

        i++;

        int fullTokenLen = 0;
        // decode for token length
        if (authToken[i] == (byte) 0x081)
        {
            fullTokenLen = (authToken[i + 1] & 0xff);
            i += 2;
        }
        else if (authToken[i] == (byte) 0x082)
        {
            fullTokenLen = ((authToken[i + 1] & 0xff) << 8) + (authToken[i + 2] & 0xff);
            i += 3;
        }
        else if (authToken[i] == (byte) 0x083)
        {
            fullTokenLen = ((authToken[i + 1] & 0xff) << 16) + ((authToken[i + 2] & 0xff) << 8) + (authToken[i + 3] & 0xff);
            i += 4;
        }
        else if (authToken[i] == (byte) 0x084)
        {
            fullTokenLen = ((authToken[i + 1] & 0xff) << 24) + ((authToken[i + 2] & 0xff) << 16) + ((authToken[i + 3] & 0xff) << 8) + (authToken[i + 4] & 0xff);
            i += 5;
        }
        else
        {
            fullTokenLen = authToken[i];
            i++;
        }

        if (fullTokenLen > 0)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Full token length: " + fullTokenLen);
        }
        else
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Invalid full token length: " + fullTokenLen);
            return null;
        }

        // decode for OID
        if (authToken[i] != (byte) 0x06)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Invalid OID encountered.");
            throw new SASInvalidMechanismException();
        }

        int oidstart = i;
        int oidLen = 0;

        i++;

        // decode for OID length
        if (authToken[i] == (byte) 0x081)
        {
            oidLen = (authToken[i + 1] & 0xff);
            i += 2;
        }
        else if (authToken[i] == (byte) 0x082)
        {
            oidLen = ((authToken[i + 1] & 0xff) << 8) + (authToken[i + 2] & 0xff);
            i += 3;
        }
        else if (authToken[i] == (byte) 0x083)
        {
            oidLen = ((authToken[i + 1] & 0xff) << 16) + ((authToken[i + 2] & 0xff) << 8) + (authToken[i + 3] & 0xff);
            i += 4;
        }
        else if (authToken[i] == (byte) 0x084)
        {
            oidLen = ((authToken[i + 1] & 0xff) << 24) + ((authToken[i + 2] & 0xff) << 16) + ((authToken[i + 3] & 0xff) << 8) + (authToken[i + 4] & 0xff);
            i += 5;
        }
        else
        {
            oidLen = authToken[i];
            i++;
        }

        if (oidLen > 0)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "OID length: " + oidLen);
        }
        else
        {
            if (tc.isDebugEnabled() == true)
                Tr.debug(tc, "Invalid OID length: " + oidLen);
            throw new SASInvalidMechanismException();
        }

        //i += oidLen;

        byte[] derOID = new byte[oidLen + 2];

        System.arraycopy(authToken, oidstart, derOID, 0, oidLen + 2);
        String mechOid;
        try {
            mechOid = decodeOID(derOID);
        } catch (IOException e) {
            throw new SASInvalidMechanismException();
        }

        return "oid:" + mechOid;
    }

    private static final Map<String, String> specialTypeNames;
    private static final Map<String, String> specialTypePackages;
    private static final Set<String> keywords;

    static {
        specialTypeNames = new HashMap<String, String>();
        specialTypeNames.put("boolean", "boolean");
        specialTypeNames.put("char", "wchar");
        specialTypeNames.put("byte", "octet");
        specialTypeNames.put("short", "short");
        specialTypeNames.put("int", "long");
        specialTypeNames.put("long", "long_long");
        specialTypeNames.put("float", "float");
        specialTypeNames.put("double", "double");
        specialTypeNames.put("java.lang.Class", "ClassDesc");
        specialTypeNames.put("java.lang.String", "WStringValue");
        specialTypeNames.put("org.omg.CORBA.Object", "Object");

        specialTypePackages = new HashMap<String, String>();
        specialTypePackages.put("boolean", "");
        specialTypePackages.put("char", "");
        specialTypePackages.put("byte", "");
        specialTypePackages.put("short", "");
        specialTypePackages.put("int", "");
        specialTypePackages.put("long", "");
        specialTypePackages.put("float", "");
        specialTypePackages.put("double", "");
        specialTypePackages.put("java.lang.Class", "javax.rmi.CORBA");
        specialTypePackages.put("java.lang.String", "CORBA");
        specialTypePackages.put("org.omg.CORBA.Object", "");

        keywords = new HashSet<String>();
        keywords.add("abstract");
        keywords.add("any");
        keywords.add("attribute");
        keywords.add("boolean");
        keywords.add("case");
        keywords.add("char");
        keywords.add("const");
        keywords.add("context");
        keywords.add("custom");
        keywords.add("default");
        keywords.add("double");
        keywords.add("enum");
        keywords.add("exception");
        keywords.add("factory");
        keywords.add("false");
        keywords.add("fixed");
        keywords.add("float");
        keywords.add("in");
        keywords.add("inout");
        keywords.add("interface");
        keywords.add("long");
        keywords.add("module");
        keywords.add("native");
        keywords.add("object");
        keywords.add("octet");
        keywords.add("oneway");
        keywords.add("out");
        keywords.add("private");
        keywords.add("public");
        keywords.add("raises");
        keywords.add("readonly");
        keywords.add("sequence");
        keywords.add("short");
        keywords.add("string");
        keywords.add("struct");
        keywords.add("supports");
        keywords.add("switch");
        keywords.add("true");
        keywords.add("truncatable");
        keywords.add("typedef");
        keywords.add("union");
        keywords.add("unsigned");
        keywords.add("valuebase");
        keywords.add("valuetype");
        keywords.add("void");
        keywords.add("wchar");
        keywords.add("wstring");
    }
}
