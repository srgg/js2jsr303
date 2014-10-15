package mobi.srg.js2jsr303.jsr303;

import org.hibernate.validator.internal.util.ReflectionHelper;

import javax.validation.ConstraintTarget;
import javax.validation.ConstraintValidator;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;


class ConstraintDescriptorImpl implements ConstraintDescriptor {
    public static final String GROUPS = "groups";
    public static final String PAYLOAD = "payload";
    public static final String MESSAGE = "message";
    public static final String VALIDATION_APPLIES_TO = "validationAppliesTo";

    private final Annotation annotation;
    private final boolean isReportAsSingleInvalidConstraint;
    private List<Class<? extends ConstraintValidator>> validators;
    private final Map<String, Object> attributes;

    private transient Set<Class<?>> groups=null;
    private transient Set<Class<? extends Payload>> payload = null;

    private ConstraintDescriptorImpl(final Annotation annotation, List<Class<? extends ConstraintValidator>> validators) {
        this.annotation = annotation;
        Class annotationType = this.annotation.annotationType();
        this.isReportAsSingleInvalidConstraint = annotationType.isAnnotationPresent(
                ReportAsSingleViolation.class
        );
        this.attributes = buildAnnotationParameterMap(annotation);
        this.validators = validators;
    }

    private ConstraintDescriptorImpl(Map<String, Object> attributes) {
        this.annotation = null;
        //Class annotationType = this.annotation.annotationType();
        this.isReportAsSingleInvalidConstraint = true;

        this.attributes = attributes;
        this.validators = Collections.EMPTY_LIST;
    }

    @Override
    public Annotation getAnnotation() {
        return annotation;
    }

    @Override
    public String getMessageTemplate() {
        return (String) attributes.get(MESSAGE);
    }

    @Override
    public Set<Class<?>> getGroups() {
        if(groups == null){
            Class g[] = (Class[]) attributes.get(GROUPS);
            groups = Collections.unmodifiableSet( new HashSet<Class<?>>((Collection)Arrays.asList(g)));
        }
        return groups;
    }

    @Override
    public Set<Class<? extends Payload>> getPayload() {
        if(groups == null){
            Class<? extends Payload> p[] = (Class<? extends Payload>[]) attributes.get(PAYLOAD);
            payload = (Set)Collections.unmodifiableSet( new HashSet<>((Collection)Arrays.asList(p)));
        }
        return payload;
    }

    @Override
    public ConstraintTarget getValidationAppliesTo() {
        return (ConstraintTarget) attributes.get( VALIDATION_APPLIES_TO );
    }

    @Override
    public List<Class<? extends ConstraintValidator>> getConstraintValidatorClasses() {
        return validators;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Set<ConstraintDescriptor<?>> getComposingConstraints() {
        return null;
    }

    @Override
    public boolean isReportAsSingleViolation() {
        return isReportAsSingleInvalidConstraint;
    }


    private Map<String, Object> buildAnnotationParameterMap(Annotation annotation) {
        final Method[] declaredMethods = ReflectionHelper.getDeclaredMethods(annotation.annotationType());
        Map<String, Object> parameters = new HashMap<>( declaredMethods.length );
        for ( Method m : declaredMethods ) {
            Object value = ReflectionHelper.getAnnotationParameter( annotation, m.getName(), Object.class );
            parameters.put( m.getName(), value );
        }
        return Collections.unmodifiableMap(parameters);
    }

    public static ConstraintDescriptor create(Annotation a, List<Class<? extends ConstraintValidator>> validators){
        return new ConstraintDescriptorImpl(a, validators);
    }

    public static ConstraintDescriptor create(Map<String, Object> attributes){
        return new ConstraintDescriptorImpl(attributes);
    }
}
