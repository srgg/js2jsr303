package mobi.srg.js2jsr303;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import mobi.srg.js2jsr303.meta.EntityInfo;
import mobi.srg.js2jsr303.meta.FieldInfo;
import net.javacrumbs.jsonunit.JsonAssert;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public abstract class AbstractValidationTest {
    private static ObjectMapper mapper;
    private static PropertyIgnoringModule propertyIgnoringModule;
    private static AbstractValidator validator;
    private static PathSerializer pathSerializer = new PathSerializer();

    protected static class SimpleValidator extends HibernateValidator{
    }

    private static class PathSerializer extends JsonSerializer<Path>{
        @Override
        public void serialize(Path value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.toString());
        }
    }


    protected AbstractValidator createValidator(){
        return new SimpleValidator();
    }

    @Before
    public void initValidator() throws Exception {
        validator = createValidator();
        validator.init();

        propertyIgnoringModule = new PropertyIgnoringModule(){
            @Override
            protected BeanPropertyWriter processProperty(SerializationConfig config, BeanDescription beanDesc, BeanPropertyWriter beanProperty) {
                final BeanPropertyWriter bpw = super.processProperty(config, beanDesc, beanProperty);

                if( bpw != null && beanDesc.getBeanClass().getName().equals("mobi.srg.js2jsr303.jsr303.ConstraintViolationImpl")){
                    if("propertyPath".equals(bpw.getName())){
                        bpw.assignSerializer((JsonSerializer)pathSerializer);
                    }
                }
                return bpw;
            }
        };

        // -- mapper initialization
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mapper.registerModule(propertyIgnoringModule);
    }

    final protected static <T> T fromJSON(Class<T> clazz, String json){
        if (!json.startsWith("{")) {
            json = "{" + json;
        }

        if (!json.endsWith("}")) {
            json = json + "}";
        }

        try {
            return mapper.readValue(json, clazz);
        }catch(IOException e){
            throw new RuntimeException("Incorrect json", e);
        }
    }

    protected static EntityInfo parseAsSingleFieldEntity(String entityName, String fieldInfoJSON){
        final FieldInfo fi = fromJSON(FieldInfo.class, fieldInfoJSON);
        final EntityInfo ei = new EntityInfo();
        ei.setName(entityName);
        ei.setFields(Collections.singletonMap(fi.getName(), fi));
        return ei;
    }

    protected Set<ConstraintViolation> validate(EntityInfo ei, String fieldValidationOverrides, String data){
        final Map<String, String> overrides = StringUtils.isBlank(fieldValidationOverrides) ? Collections.EMPTY_MAP : fromJSON(TreeMap.class, fieldValidationOverrides);
        final Map<String, String> values = fromJSON(TreeMap.class, data);

        // preserver original values
        final Map<String,String> old = new HashMap<>();
        for(FieldInfo fi: ei){
            old.put(fi.getName(),fi.getValidator());

            if(overrides.containsKey(fi.getName())){
                fi.setValidator(overrides.get(fi.getName()));
            }
        }

        try {
            return validator.validate(ei, (Map) values);
        }finally{
            // restore original values
            for(FieldInfo fi: ei){
                fi.setValidator(
                        old.get(fi.getName())
                );
            }
        }
    }

    protected Set<ConstraintViolation> validate(EntityInfo ei, String data){
        return validate(ei, null, data);
    }


    protected void assertEqualsToSingleViolation(String jsonExpected, Set<ConstraintViolation> violations){
        assertEquals("Set with the only one violation was expected", 1, violations == null ? 0 : violations.size());
        assertEqualsToExpectedOnly(jsonExpected, violations.iterator().next());
    }

    protected static void assertEqualsToExpectedOnly(String jsonExpected, Object actual) {
        try {
            final Map<String, Object> expectedProps = fromJSON(TreeMap.class, jsonExpected);
            propertyIgnoringModule.setKnownProperties(expectedProps.keySet());

            final String strActual;
            try {
                strActual = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual);
            }finally {
                propertyIgnoringModule.setKnownProperties(null);
            }

            final String strExpected = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(expectedProps);
            JsonAssert.assertJsonEquals(strExpected, strActual);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }}
