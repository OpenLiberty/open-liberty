
package hello;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Address" type="{http://hello}Address"/>
 *         &lt;element name="Address2" type="{http://hello}Address"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
                                  "address",
                                  "header"
})
@XmlRootElement(name = "sayHelloHeader")
public class SayHelloHeader {

    @XmlElement(name = "address", required = true, nillable = true)
    protected Address address;
    @XmlElement(name = "header", required = true)
    protected Header header;

    /**
     * Gets the value of the address property.
     *
     * @return
     *         possible object is
     *         {@link Address }
     *
     */
    public Address getAddress() {
        return address;
    }

    /**
     * Sets the value of the address property.
     *
     * @param value
     *                  allowed object is
     *                  {@link Address }
     *
     */
    public void setAddress(Address value) {
        this.address = value;
    }

    /**
     * Gets the value of the address2 property.
     *
     * @return
     *         possible object is
     *         {@link Address }
     *
     */
    public Header getHeader() {
        return header;
    }

    /**
     * Sets the value of the address2 property.
     *
     * @param value
     *                  allowed object is
     *                  {@link Address }
     *
     */
    public void setHeader(Header value) {
        this.header = value;
    }

}
