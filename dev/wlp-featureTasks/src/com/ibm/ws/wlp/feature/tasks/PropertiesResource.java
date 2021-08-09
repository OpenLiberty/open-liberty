package com.ibm.ws.wlp.feature.tasks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Properties;

import aQute.bnd.osgi.Resource;

public class PropertiesResource implements Resource {
    private String extra;
    private byte[] bytes;

    public PropertiesResource(Properties props) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        props.store(bytesOut, null);
        bytes = bytesOut.toByteArray();
        bytesOut.close();
    }

    @Override
    public InputStream openInputStream() throws Exception {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void write(OutputStream out) throws Exception {
        out.write(bytes);
    }

    @Override
    public long lastModified() {
        return 0;
    }

    @Override
    public void setExtra(String extra) {
        this.extra = extra;
    }

    @Override
    public String getExtra() {
        return extra;
    }

    @Override
    public long size() throws Exception {
        return bytes.length;
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ByteBuffer buffer() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
