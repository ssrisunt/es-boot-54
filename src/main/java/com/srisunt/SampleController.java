package com.srisunt;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.srisunt.entities.SampleEntity;
import com.srisunt.facet.ArticleEntity;
import com.srisunt.facet.ArticleEntityBuilder;
import com.srisunt.facet.EmptyPagable;
import com.srisunt.repostories.custom.SampleCustomMethodRepository;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.ScrolledPage;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.util.CloseableIterator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.lang.Thread.sleep;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toList;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;

@Controller
@EnableAutoConfiguration
@ImportResource("classpath:es-config.xml")
@Configuration
@EnableAsync
@EnableScheduling
public class SampleController implements AsyncConfigurer {

    private static final String INDEX_NAME = "test-index-sample";
    private static final String TYPE_NAME = "test-type";

    private final Logger log = LoggerFactory.getLogger(SampleController.class);

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true);

        return mapper;
    }

    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor getAsyncExecutor() {
        //log.debug("Creating Async Task Executor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("As");
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    /** Configure async support for Spring MVC. */
    @Bean
    public WebMvcConfigurerAdapter webMvcConfigurerAdapter(AsyncTaskExecutor taskExecutor, CallableProcessingInterceptor callableProcessingInterceptor) {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
                configurer.setDefaultTimeout(5000000)
                .setTaskExecutor(taskExecutor);
                configurer.registerCallableInterceptors(callableProcessingInterceptor);
                super.configureAsyncSupport(configurer);
            }
        };
    }

    @Bean
    public CallableProcessingInterceptor callableProcessingInterceptor() {
        return new TimeoutCallableProcessingInterceptor() {
            @Override
            public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
                log.error("timeout!");
                return super.handleTimeout(request, task);
            }
        };
    }


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
                System.out.println("ENd: "+System.currentTimeMillis());

            }
        });

        objectMapper.registerModule(module);
        jsonConverter.setObjectMapper(objectMapper);
        return jsonConverter;
    }

//    @Bean
//    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter2() {
//        MappingJackson2HttpMessageConverter jackson = new MappingJackson2HttpMessageConverter();
//        ObjectMapper om = jackson.getObjectMapper();
//
//        JsonSerializer<?> streamSer = new StdSerializer<CloseableIterator<?>>(CloseableIterator.class, true) {
//            @Override public void serialize(
//                    CloseableIterator<?> stream, JsonGenerator jgen, SerializerProvider provider
//            ) throws IOException, JsonGenerationException
//            {
//                provider.findValueSerializer(Iterator.class, null)
//                        .serialize(stream, jgen, provider);
//                System.out.println("ENd: "+System.currentTimeMillis());
//            }
//        };
//        om.registerModule(new SimpleModule("Streams API", unknownVersion(), asList(streamSer)));
//        return jackson;
//    }


    @Bean
    public HttpMessageConverters customConverters() {
        return new HttpMessageConverters(mappingJackson2HttpMessageConverter());
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
    public CloseableIterator<SampleEntity> stream(@RequestParam(value="no") int no,@RequestParam(value="page", defaultValue = "10000") int page) throws IOException {

        CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
        criteriaQuery.addIndices(INDEX_NAME);
        criteriaQuery.addTypes(TYPE_NAME);
        criteriaQuery.addCriteria(new Criteria("id").lessThan(no));
        //criteriaQuery.addFields();
        criteriaQuery.setPageable(PageRequest.of(0, page));

        System.out.println("Start: "+System.currentTimeMillis());

        CloseableIterator<SampleEntity> stream = elasticsearchTemplate.stream(criteriaQuery, SampleEntity.class);

        return stream;
    }

    @RequestMapping(value = "/stream2", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<SampleEntity> stream2(@RequestParam(value="no") int no,@RequestParam(value="page", defaultValue = "10000") int page) throws IOException {

        CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
        criteriaQuery.addIndices(INDEX_NAME);
        criteriaQuery.addTypes(TYPE_NAME);
        criteriaQuery.addCriteria(new Criteria("rate").lessThanEqual(no));
        criteriaQuery.setPageable(PageRequest.of(0, page));

        CloseableIterator<SampleEntity> stream = elasticsearchTemplate.stream(criteriaQuery, SampleEntity.class);

        List<SampleEntity> a = new ArrayList<>();

        stream.forEachRemaining(item->a.add(item));

        return a;
    }

    @RequestMapping(value = "/stream5", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> stream5(@RequestParam(value="no") int no,@RequestParam(value="page", defaultValue = "10000") int page) throws IOException {

        CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
        criteriaQuery.addIndices(INDEX_NAME);
        criteriaQuery.addTypes(TYPE_NAME);
        criteriaQuery.addCriteria(new Criteria("rate").lessThanEqual(no));
        criteriaQuery.setPageable(PageRequest.of(0, page));

        CloseableIterator<SampleEntity> stream = elasticsearchTemplate.stream(criteriaQuery, SampleEntity.class);


        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(
                out -> {
                    //StreamingGZIPOutputStream gzip = new StreamingGZIPOutputStream(out);
                    //ByteArrayOutputStream gzipped = new ByteArrayOutputStream();

                    //GZIPOutputStream gzip = new GZIPOutputStream(out);

                    OutputStream gzip = out;

                    stream.forEachRemaining(item -> {

                                try {
                                    gzip.write(objectMapper().writeValueAsString(item).getBytes());
                                    gzip.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                    );
                    //gzip.close();
                }
        );

    }




    @RequestMapping(value = "/stream3", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StreamingResponseBody> stream3(@RequestParam(value="no") int no,@RequestParam(value="page", defaultValue = "1000") int page) throws IOException {

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(
                out -> {
                    //StreamingGZIPOutputStream gzip = new StreamingGZIPOutputStream(out);

                    final ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue<List<SampleEntity>>();

                    CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria());
                    criteriaQuery.addIndices(INDEX_NAME);
                    criteriaQuery.addTypes(TYPE_NAME);
                    criteriaQuery.addCriteria(new Criteria("rate").lessThanEqual(no));
                    criteriaQuery.setPageable(PageRequest.of(0, page));

                    System.out.println("========");


                    List<CompletableFuture<String>> futures = Arrays.asList(
                            CompletableFuture.supplyAsync(
                                    () -> {
                                        boolean done = false;
                                        while (!done) {
                                            try {
                                                Object u = queue.poll();
                                                if (u!=null ) {
                                                    WorkingUnit workUnit = (WorkingUnit) u;
                                                    if (workUnit.getItem() != null) {
                                                        System.out.println("write :" + workUnit.getSeq());
                                                        out.write(objectMapper().writeValueAsString(workUnit.getItem()).getBytes());
                                                    }
                                                    if (workUnit.isLast()) done = true;
                                                }
                                            } catch (Exception e) {
                                                //e.printStackTrace();
                                            }
                                        }
                                        return "Consumer Done";
                                    }
                            ),
                            CompletableFuture.supplyAsync(
                                    () -> {
                                        long scrollTimeInMillis = TimeValue.timeValueMinutes(1L).millis();
                                        final ScrolledPage<SampleEntity> scroll1 = (ScrolledPage) elasticsearchTemplate.startScroll(scrollTimeInMillis, criteriaQuery, SampleEntity.class);
                                        int i= 0;

                                        Iterator<SampleEntity> currentHits = scroll1.iterator();
                                        String scrollId = scroll1.getScrollId();
                                        boolean finished = false;

                                        if (scroll1.hasContent()) {
                                            queue.offer(new WorkingUnit(i++, finished, scroll1.getContent()));
                                            System.out.println("get :"+ i);
                                        } else {
                                            finished = true;
                                        }


                                        while (!finished ) {
                                            ScrolledPage scroll = (ScrolledPage) elasticsearchTemplate.continueScroll(scrollId, scrollTimeInMillis, SampleEntity.class);
                                            currentHits = scroll.iterator();
                                            finished = !currentHits.hasNext();
                                            scrollId = scroll.getScrollId();
                                            if (scroll.hasContent()) {
                                                i++;
                                                queue.offer(new WorkingUnit(i, finished, scroll.getContent()));
                                                System.out.println("get :"+ i);
                                            }
                                        }

                                        queue.offer(new WorkingUnit(true, null));

                                        return "Producer Done";
                                    })
                    );
                    CompletableFuture<String> c = anyOf(futures);
                    log.info(c.join());



                    //CompletableFuture<Object> many = CompletableFuture.anyOf(eventProducer(queue, criteriaQuery, out), eventConsumer(queue, out));

                    //many.whenComplete((result, ex) -> System.out.println( "done."));
                });

    }

    public static <T> CompletableFuture<T> anyOf(List<? extends CompletionStage<? extends T>> l) {

        CompletableFuture<T> f=new CompletableFuture<>();
        Consumer<T> complete=f::complete;
        CompletableFuture.allOf(
                l.stream().map(s -> s.thenAccept(complete)).toArray(CompletableFuture<?>[]::new)
        ).exceptionally(ex -> { f.completeExceptionally(ex); return null; });
        return f;
    }

    public class ConcurrentStack<E> {
        AtomicReference<Node<E>> head = new AtomicReference<Node<E>>();

        public void push(E item) {
            Node<E> newHead = new Node<E>(item);
            Node<E> oldHead;
            do {
                oldHead = head.get();
                newHead.next = oldHead;
            } while (!head.compareAndSet(oldHead, newHead));
        }

        public E pop() {
            Node<E> oldHead;
            Node<E> newHead;
            do {
                oldHead = head.get();
                if (oldHead == null)
                    return null;
                newHead = oldHead.next;
            } while (!head.compareAndSet(oldHead,newHead));
            return oldHead.item;
        }

        class Node<E> {
            final E item;
            Node<E> next;

            public Node(E item) { this.item = item; }
        }
    }

    public class WorkingUnit {
        int seq;
        boolean last = false;
        List<SampleEntity> item;

        public boolean isLast() {
            return last;
        }

        public void setLast(boolean last) {
            this.last = last;
        }

        public List<SampleEntity> getItem() {
            return item;
        }

        public void setItem(List<SampleEntity> item) {
            this.item = item;
        }

        public int getSeq() {
            return seq;
        }

        public void setSeq(int seq) {
            this.seq = seq;
        }

        public WorkingUnit(boolean last, List<SampleEntity> item) {
            this.last = last;
            this.item = item;
        }

        public WorkingUnit(int seq, boolean last, List<SampleEntity> item) {
            this.seq = seq;
            this.last = last;
            this.item = item;
        }
    }

    @Async
    public CompletableFuture<String> eventConsumer(ConcurrentLinkedQueue<WorkingUnit> queue, OutputStream out) {
        boolean done = false;
        while(!done) {
            try {
                WorkingUnit workUnit = queue.poll();
                if (workUnit.getItem()!=null) {
                    System.out.println("write :" + workUnit.getSeq());
                    out.write(objectMapper().writeValueAsString(workUnit.getItem()).getBytes());
                }
                if (workUnit.isLast()) done = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return CompletableFuture.completedFuture("result");
    }

    @Async
    public CompletableFuture<String> eventProducer(ConcurrentLinkedQueue queue,CriteriaQuery criteriaQuery, OutputStream out) {
        long scrollTimeInMillis = TimeValue.timeValueMinutes(1L).millis();
        final ScrolledPage<SampleEntity> page = (ScrolledPage) elasticsearchTemplate.startScroll(scrollTimeInMillis, criteriaQuery, SampleEntity.class);
        int i= 0;

        Iterator<SampleEntity> currentHits = page.iterator();
        String scrollId = page.getScrollId();
        boolean finished = false;

        if (page.hasContent()) {
            queue.offer(new WorkingUnit(i++, finished, page.getContent()));
            System.out.println("get :"+ i);
        } else {
            finished = true;
        }


        while (!finished ) {
            ScrolledPage scroll = (ScrolledPage) elasticsearchTemplate.continueScroll(scrollId, scrollTimeInMillis, SampleEntity.class);
            currentHits = scroll.iterator();
            finished = !currentHits.hasNext();
            scrollId = scroll.getScrollId();
            if (scroll.hasContent()) {
                i++;
                queue.offer(new WorkingUnit(i, finished, scroll.getContent()));
                System.out.println("get :"+ i);
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        queue.add(new WorkingUnit(true,null));

        return CompletableFuture.completedFuture("result");
    }


    // Exercise using curl http://localhost:8080/async?input=lorem,ipsum,dolor,sit,amet
    @RequestMapping(path = "/async2", method = RequestMethod.GET)
    public void get(@RequestParam List<String> input) throws Exception {
        concatenateAsync(input);
    }

    private Future<String> concatenateAsync(List<String> input) {
        // Create the collection of futures.
        List<Future<String>> futures =
                input.stream()
                        .map(str -> supplyAsync(() -> callApi(str)))
                        .collect(toList());

        // Restructure as varargs because that's what CompletableFuture.allOf requires.
        CompletableFuture<?>[] futuresAsVarArgs = futures
                .toArray(new CompletableFuture[futures.size()]);

        // Create a new future to gather results once all of the previous futures complete.
        CompletableFuture<Void> ignored = CompletableFuture.allOf(futuresAsVarArgs);

        CompletableFuture<String> output = new CompletableFuture<>();

        // Once all of the futures have completed, build out the result string from results.
        ignored.thenAccept(i -> {
            StringBuilder stringBuilder = new StringBuilder();
            futures.forEach(f -> {
                try {
                    stringBuilder.append(f.get());
                } catch (Exception e) {
                    output.completeExceptionally(e);
                }
            });
            output.complete(stringBuilder.toString());
        });

        return output;
    }

    String callApi(String str) {
        try {
            // restTemplate.invoke(...)
            sleep(1000);
        } catch (Exception e) {
        }
        return str.toUpperCase();
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
