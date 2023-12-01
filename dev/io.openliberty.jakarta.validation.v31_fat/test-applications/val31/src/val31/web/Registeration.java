/**
 *
 */
package val31.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
*
*/
public record Registeration(
                @NotNull String companyid,
                @AssertTrue(
                            message = "Company should be registered",
                            groups = RegistrationChecks.class) boolean isRegistered) {

}
