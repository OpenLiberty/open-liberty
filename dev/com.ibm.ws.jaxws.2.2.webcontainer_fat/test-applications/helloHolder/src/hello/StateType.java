
package hello;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for StateType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="StateType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="OH"/>
 *     &lt;enumeration value="IN"/>
 *     &lt;enumeration value="TX"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "StateType")
@XmlEnum
public enum StateType {

    OH,
    IN,
    TX;

    public String value() {
        return name();
    }

    public static StateType fromValue(String v) {
        return valueOf(v);
    }

}
