/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.utils.encapsulation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.ibm.ws.jpa.diagnostics.utils.encapsulation.xsd10.EncapsulatedDataType;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.xsd10.PropertiesType;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.xsd10.PropertyType;

public class EncapsulatedData {
    public static EncapsulatedData createEncapsulatedData(String name, String id, byte[] data) throws Exception {
        return createEncapsulatedData(name, id, CompressionType.GZIP, "MD5", data);
    }

    public static EncapsulatedData createEncapsulatedData(String name, String id, CompressionType ct,
                                                          String hashAlg, byte[] data) throws Exception {
        EncapsulatedDataType edt = new EncapsulatedDataType();
        EncapsulatedData ed = new EncapsulatedData(edt);

        ed.setId(id);
        ed.setName(name);
        ed.setCompressionType(ct);
        ed.setHashAlgorithm(hashAlg);

        ed.setData(data);

        return ed;
    }

    public static EncapsulatedData createEncapsulatedData(EncapsulatedDataType edt) {
        EncapsulatedData ed = new EncapsulatedData(edt);
        return ed;
    }

    public static EncapsulatedData readEncapsulatedData(InputStream is) throws Exception {
        JAXBContext jc = JAXBContext.newInstance(EncapsulatedDataType.class);
        Unmarshaller um = jc.createUnmarshaller();
        EncapsulatedDataType edt = (EncapsulatedDataType) um.unmarshal(is);
        EncapsulatedData ed = new EncapsulatedData(edt);
        return ed;
    }

    private final EncapsulatedDataType edt;
    private byte[] data = null;

    private EncapsulatedData(EncapsulatedDataType edt) {
        this.edt = edt;
    }

    public String getId() {
        return edt.getId();
    }

    public void setId(String id) {
        edt.setId(id);
    }

    public String getName() {
        return edt.getName();
    }

    public void setName(String name) {
        edt.setName(name);
    }

    public CompressionType getCompressionType() {
        return CompressionType.fromString(edt.getCompression());
    }

    public void setCompressionType(CompressionType ct) {
        edt.setCompression((ct == null) ? "NONE" : ct.toString());
    }

    public String getHashAlgorithm() {
        String alg = edt.getHashAlgorithm();
        if (alg == null) {
            return "MD5";
        } else {
            return alg;
        }
    }

    public void setHashAlgorithm(String alg) {
        if (alg == null) {
            alg = "MD5";
        }
        edt.setHashAlgorithm(alg);
    }

    public String getHashValue() {
        return edt.getHashValue();
    }

    public void setData(byte[] data) throws Exception {
        if (data == null) {
            data = new byte[0];
        }

        // Copy the raw data over
        this.data = Arrays.copyOf(data, data.length);

        // Get the Hash Generator
        final MessageDigest md = MessageDigest.getInstance(getHashAlgorithm());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DigestOutputStream dos = new DigestOutputStream(baos, md);
        // Compress the data, if requested.
        if (CompressionType.ZIP == getCompressionType()) {
            final ZipOutputStream zos = new ZipOutputStream(dos);
            zos.putNextEntry(new ZipEntry(getName()));
            zos.write(data);
            zos.closeEntry();
            zos.close();
        } else if (CompressionType.GZIP == getCompressionType()) {
            final GZIPOutputStream gzos = new GZIPOutputStream(dos);
            gzos.write(data);
            gzos.flush();
            gzos.close();
        } else {
            dos.write(this.data);
            dos.close();
        }

        final BigInteger digestBigInt = new BigInteger(1, md.digest());
        final String hashStr = digestBigInt.toString(16);

        edt.setHashValue(hashStr);
        edt.setData(encodeData(baos.toByteArray()));
    }

    private String encodeData(final byte[] data) {
        if (data == null) {
            return "";
        }

        final byte[] encodedDataBytes = com.ibm.ws.common.internal.encoder.Base64Coder.base64Encode(data);
        final String encodedData = (encodedDataBytes == null) ? "" : new String(encodedDataBytes);
        final String[] lines = encodedData.split("(?<=\\G.{120})");

        final StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private byte[] decodeData(String dataBase64) {
        if (dataBase64 == null || "".equals(dataBase64.trim())) {
            return null;
        }

        dataBase64 = dataBase64.replaceAll("\\n", "");

        byte[] encodedDataBytes = dataBase64.getBytes();
        data = com.ibm.ws.common.internal.encoder.Base64Coder.base64Decode(encodedDataBytes);

//        Base64.Decoder decoder = Base64.getDecoder();
//        data = decoder.decode(dataBase64);
        return data;
    }

    public Map<String, String> getProperties() {
        HashMap<String, String> propertiesMap = new HashMap<String, String>();

        PropertiesType pt = edt.getProperties();
        if (pt != null) {
            List<PropertyType> propList = pt.getProperty();
            for (PropertyType ptEntry : propList) {
                propertiesMap.put(ptEntry.getName(), ptEntry.getValue());
            }
        }

        return propertiesMap;
    }

    public void setProperty(String name, String value) {
        if (name == null || value == null) {
            return; // No null keys or values allowed.
        }

        PropertiesType pt = edt.getProperties();
        if (pt == null) {
            pt = new PropertiesType();
            edt.setProperties(pt);
        }

        List<PropertyType> propList = pt.getProperty();

        PropertyType newPt = new PropertyType();
        propList.add(newPt);
        newPt.setName(name);
        newPt.setValue(value);;
        newPt.setType(value.getClass().getCanonicalName());
    }

    public void clearProperties() {
        PropertiesType pt = edt.getProperties();
        if (pt != null) {
            pt.getProperty().clear();
        }
    }

    public void removeProperty(String name) {
        if (name == null) {
            return;
        }

        PropertiesType pt = edt.getProperties();
        if (pt != null) {
            List<PropertyType> propToRemove = new ArrayList<PropertyType>();
            List<PropertyType> propList = pt.getProperty();

            for (PropertyType p : propList) {
                if (p.getName().equals(name)) {
                    propToRemove.add(p);
                }
            }

            for (PropertyType p : propToRemove) {
                propList.remove(p);
            }
        }
    }

    public InputStream getDataAsInputStream() throws Exception {
        byte[] data = getData();
        return new ByteArrayInputStream(data);
    }

    public byte[] getData() throws Exception {
        if (data == null) {
            data = decodeData(edt.getData()); //  decoder.decode(edt.getData());

            if (data == null) {
                return new byte[0];
            }

            // Get the Hash Generator
            final MessageDigest md = MessageDigest.getInstance(getHashAlgorithm());

            // Uncompress the data
            final DigestInputStream dis = new DigestInputStream(new ByteArrayInputStream(data), md);
            byte[] buffer = new byte[4096];
            int read = 0;
            if (CompressionType.ZIP == getCompressionType()) {
                final ZipInputStream zis = new ZipInputStream(dis);
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                zis.getNextEntry();

                while ((read = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                zis.closeEntry();
                zis.close();

                data = baos.toByteArray();
            } else if (CompressionType.GZIP == getCompressionType()) {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final GZIPInputStream gis = new GZIPInputStream(dis);

                while ((read = gis.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                gis.close();

                data = baos.toByteArray();
            } else {
                while ((read = dis.read(buffer)) != -1) {
                }
                dis.close();
            }

            final BigInteger digestBigInt = new BigInteger(1, md.digest());
            final String hashStr = digestBigInt.toString(16);

            // Verify the hash value
            if (!hashStr.equals(getHashValue())) {
                throw new IllegalStateException("The data's hash '" + hashStr +
                                                "' does not match the expected value " +
                                                "'" + getHashValue() + "'.");
            }
        }

        return Arrays.copyOf(data, data.length);
    }

    EncapsulatedDataType getEncapsulatedDataType() {
        return edt;
    }

    public void writeToString(OutputStream os) throws Exception {
        JAXBContext jc = JAXBContext.newInstance(EncapsulatedDataType.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(edt, os);
    }
}
