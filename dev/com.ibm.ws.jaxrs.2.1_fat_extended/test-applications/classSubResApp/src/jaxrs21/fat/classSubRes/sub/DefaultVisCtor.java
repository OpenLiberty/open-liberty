/**
 *
 */
package jaxrs21.fat.classSubRes.sub;

public class DefaultVisCtor extends AbstractSubResource {

    DefaultVisCtor() {
        System.out.println(this.getClass().getSimpleName());
    }
}
