/**
 * 
 */
package mpGraphQL10.rolesAuth;

import java.util.Base64;

import io.smallrye.graphql.client.typesafe.api.GraphQlClientApi;
import io.smallrye.graphql.client.typesafe.api.Header;

@GraphQlClientApi
@Header(name = "Authorization", method = "authHeaderValue")
public interface ClientInterface2 {

    static String authHeaderValue() {
        System.out.println("ANDY 2.authHeaderValue()");
        return "Basic " + Base64.getEncoder().encodeToString(("user2:user2pwd").getBytes());
    }
    
    String permitAll_unannotated();
    String permitAll_permitAll();
    String permitAll_denyAll();
    String permitAll_rolesAllowed1();
    String permitAll_rolesAllowed2();
    
    String denyAll_unannotated();
    String denyAll_permitAll();
    String denyAll_denyAll();
    String denyAll_rolesAllowed1();
    String denyAll_rolesAllowed2();
    
    String rolesAllowed1_unannotated();
    String rolesAllowed1_permitAll();
    String rolesAllowed1_denyAll();
    String rolesAllowed1_rolesAllowed1();
    String rolesAllowed1_rolesAllowed2();
    
    String rolesAllowed2_unannotated();
    String rolesAllowed2_permitAll();
    String rolesAllowed2_denyAll();
    String rolesAllowed2_rolesAllowed1();
    String rolesAllowed2_rolesAllowed2();
    
    String unannotated_unannotated();
    String unannotated_permitAll();
    String unannotated_denyAll();
    String unannotated_rolesAllowed1();
    String unannotated_rolesAllowed2();

    // multiple annotations (generally an error condition...):
    String denyAllAndPermitAll();
    String denyAllAndRolesAllowed1();
    String rolesAllowed1AndPermitAll();
    String allThree();
}
