/**
 *
 */
package builtinAnnoApp.web;

import javax.enterprise.inject.spi.Bean;

/**
 *
 */
public interface Cake {
    public String greeting();

    public Bean<?> getCakeBean();

    public CakeIngredients getIngredients();
}
