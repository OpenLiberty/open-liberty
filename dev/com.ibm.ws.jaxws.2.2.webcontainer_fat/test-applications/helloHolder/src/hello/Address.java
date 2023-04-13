
package hello;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for Address complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Address">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="streetNum" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="streetName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="city" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="state" type="{http://hello}StateType"/>
 *         &lt;element name="zip" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *       &lt;attribute name="lang" type="{http://www.w3.org/2001/XMLSchema}string" default="EN" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Address", propOrder = {
                                         "streetNum",
                                         "streetName",
                                         "city",
                                         "state",
                                         "zip"
})
public class Address {

    protected int streetNum;
    @XmlElement(required = true, nillable = true)
    protected String streetName;
    @XmlElement(required = true, nillable = true)
    protected String city;
    @XmlElement(required = true, nillable = true)
    @XmlSchemaType(name = "string")
    protected StateType state;
    protected int zip;
    @XmlAttribute(name = "lang")
    protected String lang;

    /**
     * Gets the value of the streetNum property.
     *
     */
    public int getStreetNum() {
        return streetNum;
    }

    /**
     * Sets the value of the streetNum property.
     *
     */
    public void setStreetNum(int value) {
        this.streetNum = value;
    }

    /**
     * Gets the value of the streetName property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    public String getStreetName() {
        return streetName;
    }

    /**
     * Sets the value of the streetName property.
     *
     * @param value
     *                  allowed object is
     *                  {@link String }
     *
     */
    public void setStreetName(String value) {
        this.streetName = value;
    }

    /**
     * Gets the value of the city property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    public String getCity() {
        return city;
    }

    /**
     * Sets the value of the city property.
     *
     * @param value
     *                  allowed object is
     *                  {@link String }
     *
     */
    public void setCity(String value) {
        this.city = value;
    }

    /**
     * Gets the value of the state property.
     *
     * @return
     *         possible object is
     *         {@link StateType }
     *
     */
    public StateType getState() {
        return state;
    }

    /**
     * Sets the value of the state property.
     *
     * @param value
     *                  allowed object is
     *                  {@link StateType }
     *
     */
    public void setState(StateType value) {
        this.state = value;
    }

    /**
     * Gets the value of the zip property.
     *
     */
    public int getZip() {
        return zip;
    }

    /**
     * Sets the value of the zip property.
     *
     */
    public void setZip(int value) {
        this.zip = value;
    }

    /**
     * Gets the value of the lang property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    public String getLang() {
        if (lang == null) {
            return "EN";
        } else {
            return lang;
        }
    }

    /**
     * Sets the value of the lang property.
     *
     * @param value
     *                  allowed object is
     *                  {@link String }
     *
     */
    public void setLang(String value) {
        this.lang = value;
    }

}
