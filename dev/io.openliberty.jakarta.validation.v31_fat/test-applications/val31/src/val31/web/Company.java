/**
 *
 */
package val31.web;

import jakarta.validation.GroupSequence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;

/**
*
*/
@GroupSequence({ CompanyChecks.class, Company.class })
public record Company(
                @NotNull String companyName,
                @Valid @ConvertGroup(from = Default.class, to = RegistrationChecks.class) Registeration registeration)

{

}
