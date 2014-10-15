package mobi.srg.js2jsr303;

import mobi.srg.js2jsr303.meta.EntityInfo;
import mobi.srg.js2jsr303.meta.FieldInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.validation.*;
import javax.validation.ConstraintValidator;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public abstract class AbstractValidator {

    // Script Context Literals
    public static final String CURRENTLY_VALIDATED_FIELD_INFO = "currentField";
    public static final String CURRENTLY_VALIDATED_FIELD_VALUE = "currentFieldValue";

    private static class ConstraintInfo {
        public Class<? extends Annotation> annotation;
        public Map<String, Object> params;
        public List<Class<? extends ConstraintValidator<?,?>>> validators;

        public ConstraintInfo(Class<? extends Annotation> annotation, Map<String, Object> params, List<Class<? extends ConstraintValidator<?,?>>> validators) {
            this.annotation = annotation;
            this.params = params;
            this.validators = validators;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(AbstractValidator.class);
    private Map<String, ConstraintInfo> jsr303Constraints = new HashMap<>();
    private Set<ConstraintViolation> violations;
    private List<String> scripts = new LinkedList<>();
    private ScriptEngineManager scriptEngineManager;
    private ScriptEngine se;

    protected AbstractValidator() {
    }

    abstract protected void onInit() throws Exception;

    protected final void registerScript(String script) throws Exception {
        if(se != null){
            throw new IllegalStateException("Script registration  must be done before initialization");
        }

        scripts.add(script);
    }

    protected final <A extends Annotation> void registerValidator(Class<A> annotation){
        final Constraint  c= annotation.getAnnotation(Constraint.class);
        final Class<? extends ConstraintValidator<?, ?>>[] validatedBy = c.validatedBy();
        if(validatedBy.length == 0){
            throw new IllegalStateException("Validator classes should be set via 'validatedBy' annotation property");
        }
        registerValidator(annotation, (List)Arrays.asList(validatedBy));
    }

    protected final <A extends Annotation> void registerValidator(Class<A> annotation, List<Class<? extends ConstraintValidator<A, ?>>> validators){
        final String name = StringUtils.uncapitalize(annotation.getSimpleName());
        logger.trace("Registering annotation '{}'({})...", name, annotation.getName());

        final Method methods[] = annotation.getDeclaredMethods();

        List<Map.Entry<String, Object>> list = new LinkedList<>();
        for(Method m :methods){
            list.add(new AbstractMap.SimpleEntry<>(m.getName(), m.getDefaultValue()));
        }
        Collections.sort(list, new AnnotationParameterComparator());
        LinkedHashMap<String,Object> params = new LinkedHashMap<>(list.size());
        for(Map.Entry<String, Object> e:list){
            params.put(e.getKey(), e.getValue());
        }

        logger.trace("The following annotation params was found (name=default value):\n\t{}", params);
        jsr303Constraints.put(name, new ConstraintInfo(annotation, params, (List)validators));
    }

    public final void init() throws Exception {
        scriptEngineManager = new ScriptEngineManager();
        scriptEngineManager.put("__validator", this);

        onInit();

        final StringWriter sw = new StringWriter();
        final String validationBridge = String.format(
                "function __doValidation(vname, json){\n" +
                 "  return __validator.validateConstraint(%s, vname, typeof json == 'undefined' ? null : json, %s);\n}",
                CURRENTLY_VALIDATED_FIELD_INFO, CURRENTLY_VALIDATED_FIELD_VALUE);

        sw.write(validationBridge);

        final StringBuilder builder = new StringBuilder();

        for(Map.Entry<String, ConstraintInfo> e: jsr303Constraints.entrySet() ){
            if(builder.length() > 0){
                builder.append("\n\t");
            }

            builder.append(e.getKey());

            ConstraintInfo ci = e.getValue();
            for(Class<? extends ConstraintValidator> c: ci.validators){
                builder.append("\n\t\t")
                        .append(c.getName());
            }
            sw.write(String.format("\nfunction %s(json){ return __doValidation('%s', json); }", "null".equals(e.getKey()) ? "isNull" : e.getKey(), e.getKey()));
        }
        logger.debug("The following JSR 303 Validators available:\n{}", builder);

        final String validationScript = sw.toString();
        logger.trace("Validation script has been generated:\n {}", validationScript);
        registerScript(validationScript);

        se = createScriptEngine();
    }

    private ScriptEngine createScriptEngine() throws ScriptException {
        final ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");

        for(String s: scripts){
            scriptEngine.eval(s);
        }
        return scriptEngine;
    }

    public final Set<ConstraintViolation> validate(EntityInfo entityInfo, Map<String,Object> entityProperties, Class<?>... groups){
        assert violations == null;
        try {
            for(Map.Entry<String, Object> e: entityProperties.entrySet()){
                final FieldInfo fi = entityInfo.get(e.getKey());

                if(fi == null){
                    continue;
                }

                final String expression = fi.getValidator();
                if(StringUtils.isBlank(expression)){
                    continue;
                }

                se.put(AbstractValidator.CURRENTLY_VALIDATED_FIELD_INFO, fi);
                se.put(AbstractValidator.CURRENTLY_VALIDATED_FIELD_VALUE, e.getValue());

                try {
                    se.eval(expression);
                } catch (ScriptException e1) {
                    throw new RuntimeException(
                            String.format(
                                "Unexpected exception occurred during the '%s.%s' field validation",
                                entityInfo.getName(),
                                fi.getName()
                            ),
                            e1
                    );
                }
            }

            final Set v = violations;
            return v;
        }finally {
            se.put(AbstractValidator.CURRENTLY_VALIDATED_FIELD_INFO, null);
            se.put(AbstractValidator.CURRENTLY_VALIDATED_FIELD_VALUE, null);
            violations = null;
        }
    }

    /**
     * Do not call this method directly from Java, it was designed as a callback for validation corresponding constraint,
     * hence it will be called from the JavaScript Engine.
     *
     * @param fieldInfo
     * @param constraint
     * @param params
     * @param value
     * @return
     */
    public boolean validateConstraint(FieldInfo fieldInfo, String constraint, Object params, String value){
        if(fieldInfo == null){
            throw new IllegalArgumentException("FieldInfo can't be NULL");
        }

        logger.trace("Field '{}': Validating constrain '{}'", fieldInfo.getName(), constraint);
        try {
            final ConstraintInfo ci = jsr303Constraints.get(constraint);
            if (ci == null) {
                throw new IllegalArgumentException(String.format("Constraint '%s' is undefined.", constraint));
            }

            if (params instanceof Map) {
                logger.info("map");
            } else if (params != null) {
                throw new IllegalArgumentException("Argument 'params' has wrong type, expected type is Map");
            }

            final Object rawValue = fieldInfo.parseValue(value);
            final ConstraintViolation cv =  mobi.srg.js2jsr303.jsr303.ConstraintValidator.validate(fieldInfo.getName(), ci.annotation, rawValue, ci.validators, ci.params, (Map) params);

            if(cv != null){
                if(violations == null ){
                    violations = new HashSet<>();
                }
                violations.add(cv);
            }
            logger.debug("Field '{}': Validated {} with constraint '{}'", fieldInfo.getName(), cv== null ? "PASSED" : "FAILED", constraint );
            return cv == null;
        }catch (Exception e){
            logger.error(String.format("[ERR] Field '{}': Validation with constraint '{}' was failed due to exception.", fieldInfo.getName(), constraint),
                    e
                );

            if(e instanceof RuntimeException){
                throw (RuntimeException)e;
            }

            throw new RuntimeException(e);
        }
    }

    /**
     * Do not call this method from directly from Java, it was designed as a helper for custom javascript validation logic.
    */
    public void addViolation(String str, String message, Map attributes){
        if (!(attributes instanceof Map) && attributes != null ) {
            throw new IllegalArgumentException("Argument 'attributes' has wrong type, expected type is Map");
        }

        final FieldInfo fi = (FieldInfo) se.getContext().getAttribute(CURRENTLY_VALIDATED_FIELD_INFO);
        final Object rawValue = se.getContext().getAttribute(CURRENTLY_VALIDATED_FIELD_VALUE);

        final ConstraintViolation v =  mobi.srg.js2jsr303.jsr303.ConstraintValidator.createViolation(fi.getName(), message, rawValue, attributes);

        if(violations == null ){
            violations = new HashSet<>();
        }
        violations.add(v);
    }

    private static class AnnotationParameterComparator implements Comparator<Map.Entry<String, Object>> {
        private static Map<String, Integer> order = new HashMap<>();

        static {
            order.put("value", -6);
            order.put("message", 1);
            order.put("groups", 2);
            order.put("payload", 3);
        }

        @Override
        public int compare(Map.Entry<String, Object> o1, Map.Entry<String, Object> o2) {
            Integer i1 = order.get(o1.getKey());

            if(i1 == null){
                i1 = 0;
            }

            Integer i2 = order.get(o2.getKey());
            if(i2 == null){
                i2 = 0;
            }

            if(i1  != 0 || i2 != 0 ){
                return Integer.compare(i1,i2);
            }

            if(o1.getValue() == null){
                if(o2.getValue() == null){
                    return 0;
                }
                return -11;
            }

            if(o2.getValue() == null){
                return 1;
            }
            return 0;
        }
    }
}