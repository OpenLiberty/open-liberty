/**
 *
 */
package jaxrs21.fat.classSubRes.sub;

public class PrivateCtor extends AbstractSubResource {

    private PrivateCtor() {
        System.out.println(this.getClass().getSimpleName());
    }
}
