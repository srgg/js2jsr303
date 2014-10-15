package mobi.srg.js2jsr303;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.validator.internal.engine.ValidatorFactoryImpl;
import org.hibernate.validator.internal.metadata.core.ConstraintHelper;

import javax.validation.*;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;


public class HibernateValidator extends AbstractValidator {

    private ValidatorFactoryImpl validatorFactory;

    @Override
    protected void onInit() throws Exception {
        assert validatorFactory == null;

        validatorFactory = (ValidatorFactoryImpl) Validation.buildDefaultValidatorFactory();
        final ConstraintHelper constraintHelper = (ConstraintHelper) FieldUtils.readDeclaredField(validatorFactory, "constraintHelper", true);

        final Map m = (Map) FieldUtils.readDeclaredField(constraintHelper, "builtinConstraints", true);
        final Map<Class<? extends Annotation>, List<Class<? extends ConstraintValidator>>> builtin = m;


        for( Map.Entry<Class<? extends Annotation>, List<Class<? extends ConstraintValidator>>> e: builtin.entrySet()){
            registerValidator(e.getKey(), (List)e.getValue());
        };
    }
}
