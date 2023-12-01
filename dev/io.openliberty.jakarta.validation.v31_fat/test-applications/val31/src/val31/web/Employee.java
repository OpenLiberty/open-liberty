/**
 *
 */
package val31.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 *
 */
public record Employee(@NotNull String empid, @Valid EmailAddress email) {
}
