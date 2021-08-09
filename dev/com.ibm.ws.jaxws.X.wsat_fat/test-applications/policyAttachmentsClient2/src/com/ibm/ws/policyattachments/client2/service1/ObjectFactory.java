//
// Generated By:JAX-WS RI IBM 2.2.1-07/09/2014 01:52 PM(foreman)- (JAXB RI IBM 2.2.3-07/07/2014 12:54 PM(foreman)-)
//


package com.ibm.ws.policyattachments.client2.service1;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ibm.ws.policyattachments.client2.service1 package. 
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

    private final static QName _HelloWithYouWant_QNAME = new QName("http://service1.policyattachments.ws.ibm.com/", "helloWithYouWant");
    private final static QName _HelloWithYouWantResponse_QNAME = new QName("http://service1.policyattachments.ws.ibm.com/", "helloWithYouWantResponse");
    private final static QName _HelloWithoutPolicy_QNAME = new QName("http://service1.policyattachments.ws.ibm.com/", "helloWithoutPolicy");
    private final static QName _HelloWithPolicy_QNAME = new QName("http://service1.policyattachments.ws.ibm.com/", "helloWithPolicy");
    private final static QName _HelloWithOptionalPolicy_QNAME = new QName("http://service1.policyattachments.ws.ibm.com/", "helloWithOptionalPolicy");
    private final static QName _HelloWithPolicyResponse_QNAME = new QName("http://service1.policyattachments.ws.ibm.com/", "helloWithPolicyResponse");
    private final static QName _HelloWithOptionalPolicyResponse_QNAME = new QName("http://service1.policyattachments.ws.ibm.com/", "helloWithOptionalPolicyResponse");
    private final static QName _HelloWithoutPolicyResponse_QNAME = new QName("http://service1.policyattachments.ws.ibm.com/", "helloWithoutPolicyResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.policyattachments.client2.service1
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link HelloWithoutPolicyResponse }
     * 
     */
    public HelloWithoutPolicyResponse createHelloWithoutPolicyResponse() {
        return new HelloWithoutPolicyResponse();
    }

    /**
     * Create an instance of {@link HelloWithYouWant }
     * 
     */
    public HelloWithYouWant createHelloWithYouWant() {
        return new HelloWithYouWant();
    }

    /**
     * Create an instance of {@link HelloWithoutPolicy }
     * 
     */
    public HelloWithoutPolicy createHelloWithoutPolicy() {
        return new HelloWithoutPolicy();
    }

    /**
     * Create an instance of {@link HelloWithOptionalPolicy }
     * 
     */
    public HelloWithOptionalPolicy createHelloWithOptionalPolicy() {
        return new HelloWithOptionalPolicy();
    }

    /**
     * Create an instance of {@link HelloWithYouWantResponse }
     * 
     */
    public HelloWithYouWantResponse createHelloWithYouWantResponse() {
        return new HelloWithYouWantResponse();
    }

    /**
     * Create an instance of {@link HelloWithPolicyResponse }
     * 
     */
    public HelloWithPolicyResponse createHelloWithPolicyResponse() {
        return new HelloWithPolicyResponse();
    }

    /**
     * Create an instance of {@link HelloWithPolicy }
     * 
     */
    public HelloWithPolicy createHelloWithPolicy() {
        return new HelloWithPolicy();
    }

    /**
     * Create an instance of {@link HelloWithOptionalPolicyResponse }
     * 
     */
    public HelloWithOptionalPolicyResponse createHelloWithOptionalPolicyResponse() {
        return new HelloWithOptionalPolicyResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HelloWithYouWant }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service1.policyattachments.ws.ibm.com/", name = "helloWithYouWant")
    public JAXBElement<HelloWithYouWant> createHelloWithYouWant(HelloWithYouWant value) {
        return new JAXBElement<HelloWithYouWant>(_HelloWithYouWant_QNAME, HelloWithYouWant.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HelloWithYouWantResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service1.policyattachments.ws.ibm.com/", name = "helloWithYouWantResponse")
    public JAXBElement<HelloWithYouWantResponse> createHelloWithYouWantResponse(HelloWithYouWantResponse value) {
        return new JAXBElement<HelloWithYouWantResponse>(_HelloWithYouWantResponse_QNAME, HelloWithYouWantResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HelloWithoutPolicy }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service1.policyattachments.ws.ibm.com/", name = "helloWithoutPolicy")
    public JAXBElement<HelloWithoutPolicy> createHelloWithoutPolicy(HelloWithoutPolicy value) {
        return new JAXBElement<HelloWithoutPolicy>(_HelloWithoutPolicy_QNAME, HelloWithoutPolicy.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HelloWithPolicy }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service1.policyattachments.ws.ibm.com/", name = "helloWithPolicy")
    public JAXBElement<HelloWithPolicy> createHelloWithPolicy(HelloWithPolicy value) {
        return new JAXBElement<HelloWithPolicy>(_HelloWithPolicy_QNAME, HelloWithPolicy.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HelloWithOptionalPolicy }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service1.policyattachments.ws.ibm.com/", name = "helloWithOptionalPolicy")
    public JAXBElement<HelloWithOptionalPolicy> createHelloWithOptionalPolicy(HelloWithOptionalPolicy value) {
        return new JAXBElement<HelloWithOptionalPolicy>(_HelloWithOptionalPolicy_QNAME, HelloWithOptionalPolicy.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HelloWithPolicyResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service1.policyattachments.ws.ibm.com/", name = "helloWithPolicyResponse")
    public JAXBElement<HelloWithPolicyResponse> createHelloWithPolicyResponse(HelloWithPolicyResponse value) {
        return new JAXBElement<HelloWithPolicyResponse>(_HelloWithPolicyResponse_QNAME, HelloWithPolicyResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HelloWithOptionalPolicyResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service1.policyattachments.ws.ibm.com/", name = "helloWithOptionalPolicyResponse")
    public JAXBElement<HelloWithOptionalPolicyResponse> createHelloWithOptionalPolicyResponse(HelloWithOptionalPolicyResponse value) {
        return new JAXBElement<HelloWithOptionalPolicyResponse>(_HelloWithOptionalPolicyResponse_QNAME, HelloWithOptionalPolicyResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HelloWithoutPolicyResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service1.policyattachments.ws.ibm.com/", name = "helloWithoutPolicyResponse")
    public JAXBElement<HelloWithoutPolicyResponse> createHelloWithoutPolicyResponse(HelloWithoutPolicyResponse value) {
        return new JAXBElement<HelloWithoutPolicyResponse>(_HelloWithoutPolicyResponse_QNAME, HelloWithoutPolicyResponse.class, null, value);
    }

}
