package com.srisunt;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.srisunt.entities.SampleEntity;
import com.srisunt.facet.ArticleEntity;
import com.srisunt.facet.ArticleEntityBuilder;
import com.srisunt.facet.EmptyPagable;
import com.srisunt.repostories.custom.SampleCustomMethodRepository;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.util.CloseableIterator;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;

@Controller
@EnableAutoConfiguration
@ImportResource("classpath:es-config.xml")
@Configuration
public class SampleController {

    private static final String INDEX_NAME = "test-index-sample";
    private static final String TYPE_NAME = "test-type";




    @PostConstruct
    public void init() throws Exception {
        System.out.println("start..");
        //elasticsearchTemplate.deleteIndex(SampleEntity.class);
        //elasticsearchTemplate.createIndex(SampleEntity.class);
        //elasticsearchTemplate.putMapping(SampleEntity.class);
        //elasticsearchTemplate.refresh(SampleEntity.class);
        System.out.println("test..");
//        before();
//        shouldReturnAggregatedResponseForGivenSearchQuery();


        //createSampleEntities("abc", 300000);
        //repository.saveAll(entities);
        ///List<SampleEntity> entities = createSampleEntities("abc", 1000);
        //repository.saveAll(entities);

    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper =jsonConverter.getObjectMapper();
        SimpleModule module = new SimpleModule("Stream");
        module.addSerializer(CloseableIterator.class, new JsonSerializer<CloseableIterator>() {
            @Override
            public void serialize(CloseableIterator value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException, JsonProcessingException {
                serializers.findValueSerializer(Iterator.class, null)
                        .serialize(value, gen, serializers);

            }
        });

        objectMapper.registerModule(module);
        jsonConverter.setObjectMapper(objectMapper);
        return jsonConverter;
    }


    private void createSampleEntities(String type, int numberOfEntities) {

            List<SampleEntity> entities = new ArrayList<>();
            for (int i = 0; i < numberOfEntities; i++) {
                SampleEntity entity = new SampleEntity();
                entity.setId(i + "");
                entity.setAvailable(true);
                entity.setMessage("Message :" + i);
                entity.setType(type);
                entity.setRate(i);
                entities.add(entity);
                if ( i%10000 == 0 ) {
                    repository.saveAll(entities);
                    entities = new ArrayList<>();
                    System.out.println("SAve :" + i);
                }

            }
            repository.saveAll(entities);


        //return entities;
    }




    @Autowired
    private SampleCustomMethodRepository repository;

    @Autowired private ElasticsearchTemplate elasticsearchTemplate;



    @RequestMapping(path = "/create", method=RequestMethod.GET)
    public @ResponseBody String sayHello(@RequestParam(value="no") int no) {
        createSampleEntities("abc", no);
        return "Done";
    }


    @RequestMapping(value = "/stream", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public CloseableIterator<SampleEntity> stream(@RequestParam(value="no") int no) throws IOException {

        CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
        criteriaQuery.addIndices(INDEX_NAME);
        criteriaQuery.addTypes(TYPE_NAME);
        //criteriaQuery.addCriteria(new Criteria("id").lessThan(100));
        criteriaQuery.addCriteria(new Criteria("rate").lessThanEqual(no));
        //criteriaQuery.addFields();
        criteriaQuery.setPageable(PageRequest.of(0, 900));

        CloseableIterator<SampleEntity> stream = elasticsearchTemplate.stream(criteriaQuery, SampleEntity.class);


        return stream;
    }

    @RequestMapping(value = "/stream2", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<SampleEntity> stream2(HttpServletResponse response) throws IOException {

        CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
        criteriaQuery.addIndices(INDEX_NAME);
        criteriaQuery.addTypes(TYPE_NAME);
        //criteriaQuery.addCriteria(new Criteria("rate").lessThanEqual(40000));
        criteriaQuery.setPageable(PageRequest.of(0, 5000));

        CloseableIterator<SampleEntity> stream = elasticsearchTemplate.stream(criteriaQuery, SampleEntity.class);

        List<SampleEntity> a = new ArrayList<>();

        stream.forEachRemaining(item->a.add(item));

        return a;
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(SampleController.class, args);
    }


    public static final String RIZWAN_IDREES = "Rizwan Idrees";
    public static final String MOHSIN_HUSEN = "Mohsin Husen";
    public static final String JONATHAN_YAN = "Jonathan Yan";
    public static final String ARTUR_KONCZAK = "Artur Konczak";
    public static final int YEAR_2002 = 2002;
    public static final int YEAR_2001 = 2001;
    public static final int YEAR_2000 = 2000;

    public void before() {
        elasticsearchTemplate.deleteIndex(ArticleEntity.class);
        elasticsearchTemplate.createIndex(ArticleEntity.class);
        elasticsearchTemplate.putMapping(ArticleEntity.class);
        elasticsearchTemplate.refresh(ArticleEntity.class);

        IndexQuery article1 = new ArticleEntityBuilder("1").title("article four").subject("computing").addAuthor(RIZWAN_IDREES).addAuthor(ARTUR_KONCZAK).addAuthor(MOHSIN_HUSEN).addAuthor(JONATHAN_YAN).score(10).buildIndex();
        IndexQuery article2 = new ArticleEntityBuilder("2").title("article three").subject("computing").addAuthor(RIZWAN_IDREES).addAuthor(ARTUR_KONCZAK).addAuthor(MOHSIN_HUSEN).addPublishedYear(YEAR_2000).score(20).buildIndex();
        IndexQuery article3 = new ArticleEntityBuilder("3").title("article two").subject("computing").addAuthor(RIZWAN_IDREES).addAuthor(ARTUR_KONCZAK).addPublishedYear(YEAR_2001).addPublishedYear(YEAR_2000).score(30).buildIndex();
        IndexQuery article4 = new ArticleEntityBuilder("4").title("article one").subject("accounting").addAuthor(RIZWAN_IDREES).addPublishedYear(YEAR_2002).addPublishedYear(YEAR_2001).addPublishedYear(YEAR_2000).score(40).buildIndex();

        elasticsearchTemplate.index(article1);
        elasticsearchTemplate.index(article2);
        elasticsearchTemplate.index(article3);
        elasticsearchTemplate.index(article4);
        elasticsearchTemplate.refresh(ArticleEntity.class);
    }

    public void shouldReturnAggregatedResponseForGivenSearchQuery() {
        // given
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withPageable(EmptyPagable.INSTANCE)
                .withQuery(matchAllQuery())
                .withSearchType(SearchType.fromId((byte) 1))
                .withIndices("articles").withTypes("article")
                .addAggregation(terms("subjects").field("subject"))
                .build();
        // when
        Aggregations aggregations = elasticsearchTemplate.query(searchQuery, new ResultsExtractor<Aggregations>() {
            @Override
            public Aggregations extract(SearchResponse response) {
                return response.getAggregations();
            }
        });
        // then


        List<FieldObject> fieldObjectList = new ArrayList<>();
        SearchQuery aSearchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withIndices("articles").withTypes("article")
                .addAggregation(
                        terms("ByField1").field("title").subAggregation(AggregationBuilders.terms("ByField2").field("subject")))
                .build();
        Aggregations aField1Aggregations = elasticsearchTemplate.query(aSearchQuery, new ResultsExtractor<Aggregations>() {
            @Override
            public Aggregations extract(SearchResponse aResponse) {
                return aResponse.getAggregations();
            }
        });
        Terms aField1Terms = aField1Aggregations.get("ByField1");
        aField1Terms.getBuckets().stream().forEach(aField1Bucket -> {
            String field1Value = (String) aField1Bucket.getKey();
            Terms aField2Terms = aField1Bucket.getAggregations().get("ByField2");

            aField2Terms.getBuckets().stream().forEach(aField2Bucket -> {
                String field2Value = (String) aField2Bucket.getKey();

                FieldObject fieldObject = new FieldObject();
                fieldObject.setField1(field1Value);
                fieldObject.setField2(field2Value);
                fieldObject.setCount(aField2Bucket.getDocCount());
                fieldObjectList.add(fieldObject);
            });
        });

        System.out.println(fieldObjectList);


        CardinalityAggregationBuilder aggregation =
                AggregationBuilders
                        .cardinality("agg")
                        .field("subject");

        SearchQuery searchQuery2 = new NativeSearchQueryBuilder()
                .withPageable(EmptyPagable.INSTANCE)
                .withQuery(matchAllQuery())
                .withSearchType(SearchType.fromId((byte) 1))
                .withIndices("articles").withTypes("article")
                .addAggregation(aggregation)
                .build();

        Aggregations aggregations2 = elasticsearchTemplate.query(searchQuery2, new ResultsExtractor<Aggregations>() {
            @Override
            public Aggregations extract(SearchResponse response) {
                return response.getAggregations();
            }
        });
    }


    private class FieldObject {
        private String field1;
        private String field2;
        private long count;

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public String getField2() {
            return field2;
        }

        public void setField2(String field2) {
            this.field2 = field2;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}