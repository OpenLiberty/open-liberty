
package hello;

import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.ws.Holder;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the hello package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups. Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: hello
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Address }
     *
     * @return
     *
     */
    public Holder<Address> createAddress() {
        return new Holder<Address>();
    }

    /**
     * Create an instance of {@link SayHello }
     *
     */
    public UpdateAddress createUpdateAddress() {
        return new UpdateAddress();
    }

    /**
     * Create an instance of {@link Location }
     *
     */
    public Location<Address> createLocation() {
        return new Location<Address>();
    }

}
