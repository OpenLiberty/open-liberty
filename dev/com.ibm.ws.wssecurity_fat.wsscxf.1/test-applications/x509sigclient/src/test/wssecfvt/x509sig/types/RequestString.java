
package test.wssecfvt.x509sig.types;

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
 *         &lt;element name="stringreq" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
    "stringreq"
})
@XmlRootElement(name = "requestString")
public class RequestString {

    @XmlElement(required = true)
    protected String stringreq;

    /**
     * Gets the value of the stringreq property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStringreq() {
        return stringreq;
    }

    /**
     * Sets the value of the stringreq property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStringreq(String value) {
        this.stringreq = value;
    }

}
