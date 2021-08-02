//A test service for the RespectBinding feature

//Package does not match the eclipse directory. Needs to be that way
// so webservice.xml and web.xml can be easily recycled across
// multiple services.

package jaxws22.respectbinding.server.annofalse; // don't change this package

import javax.jws.WebService;
import javax.xml.ws.RespectBinding;

import jaxws22.respectbinding.server.Exception_Exception;

// The initial test run is from the validRequiredNoFeature test taken from tWAS
@RespectBinding(enabled = false)
@WebService(targetNamespace = "http://server.respectbinding.jaxws22/", wsdlLocation = "WEB-INF/wsdl/EchoService.wsdl")
public class Echo {
    public String echo(String in) throws Exception_Exception {
        System.out.println("EchoPort's RespectBinding is disabled and called with arg:" + in);
        return in;
    }
}
