//
// Generated By:JAX-WS RI IBM 2.2.1-07/09/2014 01:52 PM(foreman)- (JAXB RI IBM 2.2.3-07/07/2014 12:54 PM(foreman)-)
//

package com.ibm.ws.wsat.webservice.client;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "WSAT0606FaultService", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsat/2006/06")
public class WSAT0606FaultService
                extends Service
{

    private final static URL WSAT0606FAULTSERVICE_WSDL_LOCATION;
    private final static WebServiceException WSAT0606FAULTSERVICE_EXCEPTION;
    private final static QName WSAT0606FAULTSERVICE_QNAME = new QName("http://docs.oasis-open.org/ws-tx/wsat/2006/06", "WSAT0606FaultService");

    static {
        WSAT0606FAULTSERVICE_WSDL_LOCATION = WSAT0606FaultService.class.getResource("/META-INF/wsdl/WSAT11Fault.wsdl");
        WebServiceException e = null;
        if (WSAT0606FAULTSERVICE_WSDL_LOCATION == null) {
            e = new WebServiceException("Cannot find 'META-INF/wsdl/WSAT11Fault.wsdl' wsdl. Place the resource correctly in the classpath.");
        }
        WSAT0606FAULTSERVICE_EXCEPTION = e;
    }

    public WSAT0606FaultService() {
        super(__getWsdlLocation(), WSAT0606FAULTSERVICE_QNAME);
    }

    public WSAT0606FaultService(WebServiceFeature... features) {
        super(__getWsdlLocation(), WSAT0606FAULTSERVICE_QNAME, features);
    }

    public WSAT0606FaultService(URL wsdlLocation) {
        super(wsdlLocation, WSAT0606FAULTSERVICE_QNAME);
    }

    public WSAT0606FaultService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, WSAT0606FAULTSERVICE_QNAME, features);
    }

    public WSAT0606FaultService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public WSAT0606FaultService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *         returns WSAT0606FaultPort
     */
    @WebEndpoint(name = "WSAT0606FaultPort")
    public WSAT0606FaultPort getWSAT0606FaultPort() {
        return super.getPort(new QName("http://docs.oasis-open.org/ws-tx/wsat/2006/06", "WSAT0606FaultPort"), WSAT0606FaultPort.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their default
     *            values.
     * @return
     *         returns WSAT0606FaultPort
     */
    @WebEndpoint(name = "WSAT0606FaultPort")
    public WSAT0606FaultPort getWSAT0606FaultPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://docs.oasis-open.org/ws-tx/wsat/2006/06", "WSAT0606FaultPort"), WSAT0606FaultPort.class, features);
    }

    private static URL __getWsdlLocation() {
        if (WSAT0606FAULTSERVICE_EXCEPTION != null) {
            throw WSAT0606FAULTSERVICE_EXCEPTION;
        }
        return WSAT0606FAULTSERVICE_WSDL_LOCATION;
    }

}
