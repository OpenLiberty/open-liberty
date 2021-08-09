/**
 *
 */
package jaxrs21.fat.classSubRes.sub;

public class UnknownParmCtor extends AbstractSubResource {

    public UnknownParmCtor(PrivateCtor unknownParm) {
        System.out.println(this.getClass().getSimpleName());
    }
}
