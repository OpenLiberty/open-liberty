/**
 *
 */
package builtinAnnoApp.web;

import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.inject.Named;

@Named("VICTORIA")
public class VictoriaSponge implements Cake {

    @Inject
    private SomeRandomClass someRandom;

    private CakeIngredients ingredients;

    private String name;

    public VictoriaSponge() {}

    public VictoriaSponge(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public CakeIngredients getIngredients() {
        return ingredients;
    }

    /*
     * (non-Javadoc)
     *
     * @see builtinAnnoApp.web.BuiltinAnnoType#greeting()
     */
    @Override
    public String greeting() {
        return "Hello I'm a cake named VICTORIA! I have ingredients - " + ingredients;
    }

    /*
     * (non-Javadoc)
     *
     * @see builtinAnnoApp.web.BuiltinAnnoType#getMyBean()
     */
    @Override
    public Bean<?> getCakeBean() {
        return someRandom.getMyBean();
    }
}
