package web.staticannotationpure;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.annotation.WebServlet;

import web.common.BaseServlet;

//redundant annotation. both HttpConstraint and HttpMetodConstraint map Manager role.
@WebServlet("/StaticAnnotationPure8")
@ServletSecurity(value = @HttpConstraint(/* value = EmptyRoleSemantic.DENY, */ rolesAllowed = "Manager"),
                 httpMethodConstraints = { @HttpMethodConstraint(value = "GET", rolesAllowed = "Employee", transportGuarantee = TransportGuarantee.NONE),
                                           @HttpMethodConstraint(value = "POST", rolesAllowed = "Manager", transportGuarantee = TransportGuarantee.CONFIDENTIAL) })
public class StaticAnnotationPure8 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public StaticAnnotationPure8() {
        super("StaticAnnotationPure8");
    }

}
