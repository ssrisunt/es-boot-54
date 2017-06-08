package com.srisunt;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Created by ssrisunt on 6/7/17.
 */

@Configuration
public class EsConfig  {

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper =jsonConverter.getObjectMapper();
        SimpleModule module = new SimpleModule("Stream");
        module.addSerializer(Stream.class, new JsonSerializer<Stream>() {
            @Override
            public void serialize(Stream value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException, JsonProcessingException {
                serializers.findValueSerializer(Iterator.class, null)
                        .serialize(value.iterator(), gen, serializers);

            }
        });

        objectMapper.registerModule(module);
        jsonConverter.setObjectMapper(objectMapper);
        return jsonConverter;
    }

    @Bean
    public HttpMessageConverters customConverters() {
        return new HttpMessageConverters(mappingJackson2HttpMessageConverter());
    }

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    void configureObjectMapper( final ObjectMapper objectMapper ) {
        SimpleModule module = new SimpleModule("Stream");
        module.addSerializer(Stream.class, new JsonSerializer<Stream>() {

            @Override
            public void serialize(Stream value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException, JsonProcessingException {
                serializers.findValueSerializer(Iterator.class, null)
                        .serialize(value.iterator(), gen, serializers);

            }
        });
        objectMapper.registerModule(module);

    }

    @JsonComponent
    public class Example {

        public class Serializer extends JsonSerializer<Stream<?>> {

            @Override
            public void serialize(Stream stream, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

            }
        }
    }

}
