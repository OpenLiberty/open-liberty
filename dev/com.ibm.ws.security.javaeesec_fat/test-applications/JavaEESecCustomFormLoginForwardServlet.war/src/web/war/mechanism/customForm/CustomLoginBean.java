package web.war.mechanism.customForm;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.context.RequestScoped;
//import javax.faces.bean.ManagedBean;
//import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.SecurityContext;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.Password;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.RememberMeCredential;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

//@ManagedBean(name = "customLogin")
@Named("customLogin")
@RequestScoped
public class CustomLoginBean {
    @NotNull
    private String username;
    @NotNull
    private String password;
    @Inject
    private SecurityContext securityContext;
//    @Inject
//    private FacesContext facesContext;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void login() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Credential credential = new UsernamePasswordCredential(username, new Password(password));
//        Credential credential = new BasicAuthenticationCredential("amFzcGl1c2VyMTpzM2N1cjF0eQ==");
//        Credential invalidCred = new RememberMeCredential("jaspiuserCalleronly");

        AuthenticationStatus status = null;
        System.out.println("username : " + username + ", password : " + password);
//        try {
//            status = securityContext.authenticate(getRequest(facesContext), getResponse(facesContext), AuthenticationParameters.withParams().credential(invalidCred));
//        } catch (Exception e) {
//            System.out.println("Toshi: " + e);
//            e.printStackTrace();
//        }
//System.out.println("Toshi: basic auth cred.");
        status = securityContext.authenticate(getRequest(facesContext), getResponse(facesContext), AuthenticationParameters.withParams().credential(credential));
        System.out.println("AuthenticationStatus : " + status);

  //      HttpServletResponse res = getResponse(facesContext);
//        res.setContentType("text/html");
//        PrintWriter out = res.getWriter();
//        out.println("<h1>username : " + username + ", password : " + password + "</h1>");
//        facesContext.responseComplete();
//        }
    }

    private HttpServletRequest getRequest(FacesContext facesContext) {
        return (HttpServletRequest) facesContext.getExternalContext().getRequest();
    }

    private HttpServletResponse getResponse(FacesContext facesContext) {
        return (HttpServletResponse) facesContext.getExternalContext().getResponse();
    }

}
