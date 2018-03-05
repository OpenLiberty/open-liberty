package com.ibm.ws.app.manager.springboot.container.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * KeyStore element is defined here:<br>
 * /com.ibm.ws.ssl/resources/OSGI-INF/metatype/metatype.xml
 */
public class OutboundConnection extends ConfigElement {

    private String host;
    private String port;

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host to set
     */
    @XmlAttribute(name = "host")
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @param port to set
     */
    @XmlAttribute(name = "port")
    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("OutboundConnection{");
        if (host != null)
            buf.append("host=\"" + host + "\" ");
        if (port != null)
            buf.append("port=\"" + port + "\" ");
        buf.append("}");
        return buf.toString();
    }

}