package mobi.srg.js2jsr303.jsr303;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import javax.validation.*;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstraintValidator {

    private static Class<?> DEFAULT_GROUPS[] = {};
    private static Class<? extends Payload> DEFAULT_PAYLOAD[] = new Class[0];

    public static ConstraintViolation createViolation(String propertyPath, String message,Object actualValue, Map<String, Object> props){
        final HashMap<String, Object> attrs = new HashMap<>();

        if(props != null) {
            attrs.putAll(props);
        }

        if(!attrs.containsKey(ConstraintDescriptorImpl.MESSAGE)){
            attrs.put(ConstraintDescriptorImpl.MESSAGE, message);
        }

        if(!attrs.containsKey(ConstraintDescriptorImpl.GROUPS)){
            attrs.put(ConstraintDescriptorImpl.GROUPS, DEFAULT_GROUPS);
        }

        if(!attrs.containsKey(ConstraintDescriptorImpl.PAYLOAD)){
            attrs.put(ConstraintDescriptorImpl.GROUPS, DEFAULT_PAYLOAD);
        }

        final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        final ConstraintDescriptor cd = ConstraintDescriptorImpl.create(attrs);
        MessageInterpolator.Context context = MessageInterpolatorContextImpl.create(cd, actualValue);
        final MessageInterpolator mi = validatorFactory.getMessageInterpolator();
        final String msg =  mi.interpolate(cd.getMessageTemplate(), context);
        assert msg != null;
        return ConstraintViolationImpl.create(msg, cd, propertyPath, actualValue);
    }

    public static <A extends Annotation> ConstraintViolation validate(String propertyPath, Class<A> annotationClass, Object value, List<Class<? extends javax.validation.ConstraintValidator<?,?>>> validators, Map<String, Object> defaults, Map<String, Object> overrides){
        Class<? extends javax.validation.ConstraintValidator<?,?>> validatorClass = null;

        if(validators.size() == 1){
            validatorClass = validators.get(0);
        }else {
            for (Class<? extends javax.validation.ConstraintValidator<?, ?>> c : validators) {
                Type interfaces[] = c.getGenericInterfaces();

                Class expectedType = null;

                for(Type t: interfaces){
                    final ParameterizedTypeImpl pt = (ParameterizedTypeImpl) t;
                    assert pt != null;

                    if(javax.validation.ConstraintValidator.class.equals(pt.getRawType())){
                        assert pt.getActualTypeArguments().length == 2;
                        annotationClass = (Class)pt.getActualTypeArguments()[0];
                        expectedType = (Class)pt.getActualTypeArguments()[1];
                        break;
                    }
                }

                if(expectedType == null){
                    throw new IllegalArgumentException();
                }

                if(expectedType.isAssignableFrom(value.getClass())){
                    validatorClass = c;
                }
                assert expectedType != null;
            }

            if(validatorClass == null){
                throw new IllegalStateException(String.format("Can't find proper validator for constraint '%s'", annotationClass.getSimpleName()));
            }
        }

        final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        final ConstraintValidatorFactory vf = validatorFactory.getConstraintValidatorFactory();

        final javax.validation.ConstraintValidator cv = vf.getInstance(validatorClass);

        final Annotation a = initializeConstraint(cv, annotationClass, defaults, overrides);
        final boolean retVal;
        try{
            retVal = cv.isValid(value, null);
        }finally {
            vf.releaseInstance(cv);
        }

        if(!retVal){
            final ConstraintDescriptor cd = ConstraintDescriptorImpl.create(a, (List)validators);
            MessageInterpolator.Context context = MessageInterpolatorContextImpl.create(cd, value);
            final MessageInterpolator mi = validatorFactory.getMessageInterpolator();
            final String msg =  mi.interpolate(cd.getMessageTemplate(), context);
            assert msg != null;
            return ConstraintViolationImpl.create(msg, cd, propertyPath, value);
        }
        return null;
    }

    private static <A extends Annotation> A initializeConstraint(javax.validation.ConstraintValidator constraintValidator, Class<A> annotationClass, Map<String, Object> defaults, Map<String, Object> overrides){
        final Map<String, Object> effective = new HashMap<>(defaults);
        effective.putAll(overrides);

        for(Map.Entry<String, Object> e: effective.entrySet()){
            if(e.getValue() == null){
                throw new IllegalArgumentException(String.format("Value '%s' can't be NULL", e.getKey()));
            }
        }

        final A annotation = AnnotationInvocationHandler.create(annotationClass, effective);
        constraintValidator.initialize(annotation);
        return annotation;
    }

}
