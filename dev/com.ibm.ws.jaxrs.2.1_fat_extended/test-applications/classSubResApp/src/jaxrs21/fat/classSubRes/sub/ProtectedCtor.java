/**
 *
 */
package jaxrs21.fat.classSubRes.sub;

public class ProtectedCtor extends AbstractSubResource {

    protected ProtectedCtor() {
        System.out.println(this.getClass().getSimpleName());
    }
}
