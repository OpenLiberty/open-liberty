/**
 *
 */
package builtinAnnoApp.web;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScoped
@Named("BATTENBERG")
public class Battenberg implements Cake, Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SomeRandomClass someRandom;

    /*
     * (non-Javadoc)
     *
     * @see builtinAnnoApp.web.BuiltinAnnoType#greeting()
     */
    @Override
    public String greeting() {
        return "Hello Builtin ApplicationScoped Bean named BATTENBERG!";
    }

    @Override
    public Bean<?> getCakeBean() {
        return someRandom.getMyBean();
    }

    /*
     * (non-Javadoc)
     *
     * @see builtinAnnoApp.web.Cake#getIngredients()
     */
    @Override
    public CakeIngredients getIngredients() {
        // TODO Auto-generated method stub
        return null;
    }
}
