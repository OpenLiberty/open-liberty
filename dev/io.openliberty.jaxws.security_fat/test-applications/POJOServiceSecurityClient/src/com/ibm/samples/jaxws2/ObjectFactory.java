
package com.ibm.samples.jaxws2;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ibm.samples.jaxws2 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _SayHello_QNAME = new QName("http://jaxws2.samples.ibm.com", "sayHello");
    private final static QName _SayHelloResponse_QNAME = new QName("http://jaxws2.samples.ibm.com", "sayHelloResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.samples.jaxws2
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SayHelloResponse }
     * 
     */
    public SayHelloResponse createSayHelloResponse() {
        return new SayHelloResponse();
    }

    /**
     * Create an instance of {@link SayHello_Type }
     * 
     */
    public SayHello_Type createSayHello_Type() {
        return new SayHello_Type();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHello_Type }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://jaxws2.samples.ibm.com", name = "sayHello")
    public JAXBElement<SayHello_Type> createSayHello(SayHello_Type value) {
        return new JAXBElement<SayHello_Type>(_SayHello_QNAME, SayHello_Type.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHelloResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://jaxws2.samples.ibm.com", name = "sayHelloResponse")
    public JAXBElement<SayHelloResponse> createSayHelloResponse(SayHelloResponse value) {
        return new JAXBElement<SayHelloResponse>(_SayHelloResponse_QNAME, SayHelloResponse.class, null, value);
    }

}
