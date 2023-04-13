
package hello;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.ws.Holder;

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
                                  "address2"
})
@XmlRootElement(name = "updateAddress")
public class UpdateAddress {

    @XmlElement(name = "Address", required = true, nillable = true)
    protected Holder<Address> address = new Holder<Address>();
    @XmlElement(name = "Address2", required = true, nillable = true)
    protected Holder<Address> address2 = new Holder<Address>();

    /**
     * Gets the value of the address property.
     *
     * @return
     *         possible object is
     *         {@link Address }
     *
     */
    public Address getAddress() {
        return address.value;
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
        this.address.value = value;
    }

    /**
     * Gets the value of the address2 property.
     *
     * @return
     *         possible object is
     *         {@link Address }
     *
     */
    public Address getAddress2() {
        return address2.value;
    }

    /**
     * Sets the value of the address2 property.
     *
     * @param value
     *                  allowed object is
     *                  {@link Address }
     *
     */
    public void setAddress2(Address value) {
        this.address2.value = value;
    }

}
