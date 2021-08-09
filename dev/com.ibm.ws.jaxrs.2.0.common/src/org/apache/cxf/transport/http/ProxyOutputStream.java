package org.apache.cxf.transport.http;

import java.io.OutputStream;

import org.apache.cxf.io.AbstractWrappedOutputStream;

public class ProxyOutputStream extends AbstractWrappedOutputStream {
    public void setWrappedOutputStream(OutputStream os) {
        this.wrappedStream = os;
    }
}
