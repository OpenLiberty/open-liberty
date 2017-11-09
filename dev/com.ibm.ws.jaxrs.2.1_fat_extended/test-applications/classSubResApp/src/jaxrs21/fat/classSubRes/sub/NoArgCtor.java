/**
 *
 */
package jaxrs21.fat.classSubRes.sub;

public class NoArgCtor extends AbstractSubResource {

    public NoArgCtor() {
        System.out.println(this.getClass().getSimpleName());
    }
}
