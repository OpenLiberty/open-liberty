
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
 *         &lt;element name="stringres" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
    "stringres"
})
@XmlRootElement(name = "responseString")
public class ResponseString {

    @XmlElement(required = true)
    protected String stringres;

    /**
     * Gets the value of the stringres property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStringres() {
        return stringres;
    }

    /**
     * Sets the value of the stringres property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStringres(String value) {
        this.stringres = value;
    }

}
