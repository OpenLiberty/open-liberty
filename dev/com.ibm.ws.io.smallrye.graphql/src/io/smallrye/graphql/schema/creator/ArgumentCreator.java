package io.smallrye.graphql.schema.creator;

import java.util.Optional;

import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.smallrye.graphql.schema.Annotations;
import io.smallrye.graphql.schema.helper.DefaultValueHelper;
import io.smallrye.graphql.schema.helper.DescriptionHelper;
import io.smallrye.graphql.schema.helper.Direction;
import io.smallrye.graphql.schema.helper.FormatHelper;
import io.smallrye.graphql.schema.helper.IgnoreHelper;
import io.smallrye.graphql.schema.helper.MethodHelper;
import io.smallrye.graphql.schema.helper.NonNullHelper;
import io.smallrye.graphql.schema.model.Argument;
import io.smallrye.graphql.schema.model.Reference;

/**
 * Creates a Argument object
 * 
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class ArgumentCreator {

    private final ReferenceCreator referenceCreator;

    public ArgumentCreator(ReferenceCreator referenceCreator) {
        this.referenceCreator = referenceCreator;
    }

    /**
     * Create an argument model. Arguments exist on Operations as input parameters
     * 
     * @param argumentType The argument type
     * @param methodInfo the operation method
     * @param position the argument position
     * @return an Argument
     */
    public Optional<Argument> createArgument(Type argumentType, MethodInfo methodInfo, short position) {
        Annotations annotationsForThisArgument = Annotations.getAnnotationsForArgument(methodInfo, position);

        if (!IgnoreHelper.shouldIgnore(annotationsForThisArgument)) {
            // Argument Type
            if (methodInfo.parameters() != null && !methodInfo.parameters().isEmpty()) {
                argumentType = methodInfo.parameters().get(position);
            }

            // Name
            String defaultName = methodInfo.parameterName(position);
            String name = annotationsForThisArgument.getOneOfTheseAnnotationsValue(Annotations.NAME)
                    .orElse(defaultName);

            // Description    
            Optional<String> maybeDescription = DescriptionHelper.getDescriptionForField(annotationsForThisArgument,
                    argumentType);

            Reference reference = referenceCreator.createReferenceForOperationArgument(argumentType,
                    annotationsForThisArgument);

            Argument argument = new Argument(defaultName,
                    methodInfo.name(),
                    MethodHelper.getPropertyName(Direction.IN, methodInfo.name()),
                    name,
                    maybeDescription.orElse(null),
                    reference);

            // NotNull
            if (NonNullHelper.markAsNonNull(argumentType, annotationsForThisArgument)) {
                argument.setNotNull(true);
            }

            // Array
            argument.setArray(ArrayCreator.createArray(argumentType).orElse(null));

            // TransformInfo
            argument.setTransformInfo(FormatHelper.getFormat(argumentType, annotationsForThisArgument).orElse(null));

            // Default Value
            argument.setDefaultValue(DefaultValueHelper.getDefaultValue(annotationsForThisArgument).orElse(null));

            return Optional.of(argument);
        }
        return Optional.empty();
    }

}
