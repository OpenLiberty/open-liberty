package web.war.redirectformlogin;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class Security10Servlet
 */
@ApplicationScoped
@WebServlet("/JavaEEsecFormAuthRedirectServlet")
@ServletSecurity(value = @HttpConstraint(EmptyRoleSemantic.DENY),
                 httpMethodConstraints = { @HttpMethodConstraint(value = "POST", rolesAllowed = "javaeesec_form", transportGuarantee = TransportGuarantee.CONFIDENTIAL) })
//FOR FORWARD
//@FormAuthenticationMechanismDefinition(loginToContinue=@LoginToContinue(loginPage="/login.jsp", errorPage="/loginError.jsp"))
//FOR REDIRECT UNCOMMENT THIS
@FormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(loginPage = "/login.jsp", useForwardToLogin = false, errorPage = "/loginError.jsp"))
//@BasicAuthenticationMechanismDefinition(realmName="My Basic Realm")
//@BasicAuthenticationMechanismDefinition
@LdapIdentityStoreDefinition(
                             url = "ldap://localhost:10389/",
                             callerBaseDn = "",
                             callerSearchBase = "o=ibm,c=us",
                             callerSearchScope = LdapIdentityStoreDefinition.LdapSearchScope.SUBTREE,
                             callerSearchFilter = "(&(objectclass=person)(uid=%s))",
                             callerNameAttribute = "uid",
                             groupNameAttribute = "cn",
                             groupSearchBase = "o=ibm,c=us",
                             groupSearchScope = LdapIdentityStoreDefinition.LdapSearchScope.SUBTREE,
                             groupSearchFilter = "(objectclass=groupofnames)",
                             groupMemberAttribute = "member",
                             bindDn = "uid=jaspildapuser1,o=ibm,c=us",
                             bindDnPassword = "s3cur1ty")
public class JavaEEsecFormAuthRedirectServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

//	@Inject
//	private SecurityContext securityContext;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public JavaEEsecFormAuthRedirectServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        response.getWriter().append("Served at: ").append(request.getContextPath());
//		response.getWriter().append("SecurityContext:" + securityContext);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        doGet(request, response);
    }

}
