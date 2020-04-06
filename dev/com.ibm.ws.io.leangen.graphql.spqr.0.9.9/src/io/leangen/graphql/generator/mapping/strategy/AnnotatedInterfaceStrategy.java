package io.leangen.graphql.generator.mapping.strategy;

import org.eclipse.microprofile.graphql.Interface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class AnnotatedInterfaceStrategy extends AbstractInterfaceMappingStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(AnnotatedInterfaceStrategy.class);

    public AnnotatedInterfaceStrategy(boolean mapClasses) {
        super(mapClasses);
    }

    @Override
    public boolean supportsInterface(AnnotatedType interfase) {
        boolean b = interfase.isAnnotationPresent(Interface.class);
        if (log.isDebugEnabled()) {
            log.debug("isInterface ( " + interfase.getType() + " )? " + b);
        }
        return b;
    }
    
    @Override
    public String interfaceName(String typeName, AnnotatedType interfase) {
        String interfaceName = typeName;
        Interface interfaceAnno = interfase.getAnnotation(Interface.class);
        if (interfaceAnno != null) {
            String annoName = interfaceAnno.value();
            if (Utils.isNotEmpty(annoName)) {
                interfaceName = annoName;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("name, " + interfaceName + ", derived from ( " + typeName + ", " + 
                            (interfaceAnno != null ? interfaceAnno.value() : "no annotation") + " )");
        }
        return interfaceName;
    }
}
