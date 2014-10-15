package mobi.srg.js2jsr303;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PropertyIgnoringModule extends SimpleModule {
    private Set<String> knownProperties;

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);

        context.addBeanSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
                for(Iterator<BeanPropertyWriter> iterator = beanProperties.iterator(); iterator.hasNext();){
                    final BeanPropertyWriter bpw = iterator.next();

                    if(processProperty(config, beanDesc, bpw) == null ){
                        iterator.remove();
                    }
                }

                return beanProperties;
            }
        });
    }

    protected BeanPropertyWriter processProperty(SerializationConfig config, BeanDescription beanDesc, BeanPropertyWriter beanProperty){
        if( knownProperties != null &&!knownProperties.contains(beanProperty.getName())){
            return null;
        }

        return beanProperty;
    }

    public void setKnownProperties(Set<String> knownProperties) {
        this.knownProperties = knownProperties;
    }
}