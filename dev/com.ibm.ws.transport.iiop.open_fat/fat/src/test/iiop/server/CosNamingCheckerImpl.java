/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.iiop.server;

import static test.iiop.common.util.NamingUtil.CNC_NAME;
import static test.iiop.common.util.NamingUtil.CNC_RESOLVABLE_NAME;
import static test.iiop.common.util.NamingUtil.NESTED_CNC_NAME;
import static test.iiop.common.util.NamingUtil.NESTED_CTX_NAME;

import javax.rmi.PortableRemoteObject;

import org.apache.yoko.orb.spi.naming.Resolver;
import org.omg.CORBA.Object;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.osgi.service.component.annotations.Component;

import test.iiop.common.CosNamingChecker;
import test.iiop.common.util.NamingUtil;

@Component(immediate = true)
public class CosNamingCheckerImpl extends AbstractRemoteService<CosNamingCheckerImpl> implements CosNamingChecker {
    @Override
    public void checkNameServiceIsAvailable() throws Exception {
        debugClass(org.omg.CORBA.Object.class);
        org.omg.CORBA.Object o = requireNonNull(getOrb().resolve_initial_references("NameService"));
        debugClass(NamingContext.class);
        debugClass(NamingContextHelper.class);
        requireNonNull(NamingContextHelper.narrow(o));
    }

    @Override
    public String getNameServiceListingFromServer() throws Exception {
        org.omg.CORBA.Object o = requireNonNull(getOrb().resolve_initial_references("NameService"));
        NamingContext ctx = requireNonNull(NamingContextHelper.narrow(o));
        ctx.rebind(CNC_NAME, getThisRef());
        NamingContext nested = requireNonNull(ctx.new_context());
        nested.bind(CNC_NAME, getThisRef());
        ctx.rebind_context(NESTED_CTX_NAME, nested);
        ctx.rebind(NESTED_CNC_NAME, getThisRef());
        return NamingUtil.getNameServiceListing(ctx);
    }

    @Override
    public void bindResolvable(NameComponent[] name) throws Exception {
        org.omg.CORBA.Object o = requireNonNull(getOrb().resolve_initial_references("NameService"));
        NamingContext ctx = requireNonNull(NamingContextHelper.narrow(o));
        ctx.rebind(name, new Resolver() {
            @Override
            public Object resolve() {
                return getThisRef();
            }
        });
        Object obj = requireNonNull(ctx.resolve(CNC_RESOLVABLE_NAME));
        requireNonNull(PortableRemoteObject.narrow(obj, CosNamingChecker.class));
    }

    @Override
    public void bindResolvableThatThrows(final RuntimeException exceptionToThrow, NameComponent[] name) throws Exception {
        org.omg.CORBA.Object o = requireNonNull(getOrb().resolve_initial_references("NameService"));
        NamingContext ctx = requireNonNull(NamingContextHelper.narrow(o));
        ctx.rebind(name, new Resolver() {
            @Override
            public Object resolve() {
                exceptionToThrow.fillInStackTrace();
                throw exceptionToThrow;
            }
        });
        Object obj = requireNonNull(ctx.resolve(CNC_RESOLVABLE_NAME));
        requireNonNull(PortableRemoteObject.narrow(obj, CosNamingChecker.class));
    }

    private static void debugClass(Class<?> clazz) {
        System.out.println("### " + clazz + " was loaded by " + clazz.getClassLoader());
    }

    private static <T> T requireNonNull(T t) {
        t.toString();
        return t;
    }
}
