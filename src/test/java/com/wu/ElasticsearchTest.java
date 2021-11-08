package com.wu;


import com.alibaba.fastjson.JSON;
import com.wu.domain.Goods;
import com.wu.domain.Person;
import com.wu.mapper.GoodsMapper;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticsearchTest {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private GoodsMapper goodsMapper;

    @Test
    public void contextLoads() {
        System.out.println(client);
    }


    /**
     * 添加索引
     */
    @Test
    public void addIndex() throws Exception {
        //1.使用client获取操作索引的对象
        IndicesClient indicesClient = client.indices();
        //创建请求对象
        CreateIndexRequest createRequest = new CreateIndexRequest("abc");
        //执行同步请求创建索引
        //RequestOptions.DEFAULT 默认请求头消息
        CreateIndexResponse response = indicesClient.create(createRequest, RequestOptions.DEFAULT);
        //3.根据返回值判断结果
        System.out.println(response.isAcknowledged());
    }


    /**
     * 添加索引和映射
     */
    @Test
    public void addIndexAndMapping() throws IOException {
        //1.使用client获取操作索引的对象
        IndicesClient indicesClient = client.indices();
        //2.具体操作，获取返回值
        CreateIndexRequest createRequest = new CreateIndexRequest("aaa");
        //2.1 设置mappings
        String mapping = "{\n" +
                "      \"properties\" : {\n" +
                "        \"address\" : {\n" +
                "          \"type\" : \"text\",\n" +
                "          \"analyzer\" : \"ik_max_word\"\n" +
                "        },\n" +
                "        \"age\" : {\n" +
                "          \"type\" : \"long\"\n" +
                "        },\n" +
                "        \"name\" : {\n" +
                "          \"type\" : \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    }";
        createRequest.mapping(mapping, XContentType.JSON);
        CreateIndexResponse response = indicesClient.create(createRequest, RequestOptions.DEFAULT);
        //3.根据返回值判断结果
        System.out.println(response.isAcknowledged());
    }


    /**
     * 查询索引
     */
    @Test
    public void queryIndex() throws IOException {
        IndicesClient indices = client.indices();
        GetIndexRequest getReqeust = new GetIndexRequest("aaa");
        GetIndexResponse response = indices.get(getReqeust, RequestOptions.DEFAULT);
        //获取结果
        Map<String, MappingMetaData> mappings = response.getMappings();
        for (String key : mappings.keySet()) {
            System.out.println(key+":" + mappings.get(key).getSourceAsMap());
        }
    }


    /**
     * 删除索引
     */
    @Test
    public void deleteIndex() throws IOException {
        IndicesClient indices = client.indices();
        DeleteIndexRequest deleteRequest = new DeleteIndexRequest("abc");
        AcknowledgedResponse response = indices.delete(deleteRequest, RequestOptions.DEFAULT);
        System.out.println(response.isAcknowledged());
    }


    /**
     * 判断索引是否存在
     */
    @Test
    public void existIndex() throws IOException {
        IndicesClient indices = client.indices();
        GetIndexRequest getRequest = new GetIndexRequest("abc");
        boolean exists = indices.exists(getRequest, RequestOptions.DEFAULT);
        System.out.println(exists);
    }


    /**
     * 添加文档,使用map作为数据
     */
    @Test
    public void addDoc() throws IOException {
        //数据对象，map
        Map data = new HashMap();
        data.put("address","深圳宝安");
        data.put("name","尚硅谷");
        data.put("age",20);

        //1.获取操作文档的对象
        IndexRequest request = new IndexRequest("aaa").id("1").source(data);
        //添加数据，获取结果
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);

        //打印响应结果
        System.out.println(response.getId());
    }


    /**
     * 添加文档,使用对象作为数据
     */
    @Test
    public void addDoc2() throws IOException {
        //数据对象，javaObject
        Person p = new Person();
        p.setId("2");
        p.setName("硅谷2222");
        p.setAge(30);
        p.setAddress("北京昌平区");

        //将对象转为json
        String data = JSON.toJSONString(p);

        //1.获取操作文档的对象
        IndexRequest request = new IndexRequest("aaa").id(p.getId()).source(data,XContentType.JSON);
        //添加数据，获取结果
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);

        //打印响应结果
        System.out.println(response.getId());
    }

    /**
     * 修改文档：添加文档时，如果id存在则修改，id不存在则添加
     */
    @Test
    public void updateDoc() throws IOException {
        //数据对象，javaObject
        Person p = new Person();
        p.setId("2");
        p.setName("硅谷");
        p.setAge(30);
        p.setAddress("北京昌平区");

        //将对象转为json
        String data = JSON.toJSONString(p);

        //1.获取操作文档的对象
        IndexRequest request = new IndexRequest("aaa").id(p.getId()).source(data,XContentType.JSON);
        //添加数据，获取结果
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);

        //打印响应结果
        System.out.println(response.getId());
    }


    /**
     * 根据id查询文档
     */
    @Test
    public void findDocById() throws IOException {
        GetRequest getReqeust = new GetRequest("aaa","1");
        //getReqeust.id("1");
        GetResponse response = client.get(getReqeust, RequestOptions.DEFAULT);
        //获取数据对应的json
        System.out.println(response.getSourceAsString());
    }


    /**
     * 根据id删除文档
     */
    @Test
    public void delDoc() throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest("aaa","1");
        DeleteResponse response = client.delete(deleteRequest, RequestOptions.DEFAULT);
        System.out.println(response.getId());
        System.out.println(response.getResult());
    }

    /**
     * 1. 批量操作 bulk
     */
    @Test
    public void testBulk() throws IOException {
        //创建bulkrequest对象，整合所有操作
        BulkRequest bulkRequest = new BulkRequest();

        /*
        1. 删除1号记录
        2. 添加6号记录
        3. 修改3号记录 名称为 “三号”
         */
        //添加对应操作
        //1. 删除1号记录
        DeleteRequest deleteRequest = new DeleteRequest("person","1");
        bulkRequest.add(deleteRequest);

        //2. 添加6号记录
        Map map = new HashMap();
        map.put("name","六号");
        IndexRequest indexRequest = new IndexRequest("person").id("6").source(map);
        bulkRequest.add(indexRequest);

        Map map2 = new HashMap();
        map2.put("name","三号");
        //3. 修改3号记录 名称为 “三号”
        UpdateRequest updateReqeust = new UpdateRequest("person","3").doc(map2);
        bulkRequest.add(updateReqeust);

        //执行批量操作
        BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        RestStatus status = response.status();
        System.out.println(status);
    }


    /**
     * 批量导入
     */
    @Test
    public void importData() throws IOException {
        //1.查询所有数据，mysql
        List<Goods> goodsList = goodsMapper.findAll();

        //System.out.println(goodsList.size());
        //2.bulk导入
        BulkRequest bulkRequest = new BulkRequest();

        //2.1 循环goodsList，创建IndexRequest添加数据
        for (Goods goods : goodsList) {
            //2.2 设置spec规格信息 Map的数据   specStr:{}
            //goods.setSpec(JSON.parseObject(goods.getSpecStr(),Map.class));

            String specStr = goods.getSpecStr();
            //将json格式字符串转为Map集合
            Map map = JSON.parseObject(specStr, Map.class);
            //设置spec map
            goods.setSpec(map);
            //将goods对象转换为json字符串
            String data = JSON.toJSONString(goods);//bean --> {}
            IndexRequest indexRequest = new IndexRequest("goods");
            indexRequest.id(goods.getId()+"").source(data, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }

        BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        System.out.println(response.status());
    }

    /**
     * 查询所有
     *  1. matchAll
     *  2. 将查询结果封装为Goods对象，装载到List中
     *  3. 分页。默认显示10条
     */
    @Test
    public void testMatchAll() throws IOException {
        //2. 构建查询请求对象，指定查询的索引名称
        SearchRequest searchRequest = new SearchRequest("goods");
        //4. 创建查询条件构建器SearchSourceBuilder
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        //6. 查询条件
        QueryBuilder query = QueryBuilders.matchAllQuery();//查询所有文档
        //5. 指定查询条件
        sourceBuilder.query(query);

        //3. 添加查询条件构建器 SearchSourceBuilder
        searchRequest.source(sourceBuilder);

        // 8 . 添加分页信息
        sourceBuilder.from(0);
        sourceBuilder.size(100);

        //1. 查询,获取查询结果
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        //7. 获取命中对象 SearchHits
        SearchHits searchHits = searchResponse.getHits();
        //7.1 获取总记录数
        long value = searchHits.getTotalHits().value;
        System.out.println("总记录数："+value);

        List<Goods> goodsList = new ArrayList<>();
        //7.2 获取Hits数据  数组
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            //获取json字符串格式的数据
            String sourceAsString = hit.getSourceAsString();
            //转为java对象
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            goodsList.add(goods);
        }

        for (Goods goods : goodsList) {
            System.out.println(goods);
        }
    }



    /**
     * termQuery:词条查询
     */
    @Test
    public void testTermQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");

        SearchSourceBuilder sourceBulider = new SearchSourceBuilder();

        QueryBuilder query = QueryBuilders.termQuery("title","华为");//term词条查询
        sourceBulider.query(query);

        searchRequest.source(sourceBulider);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits searchHits = searchResponse.getHits();
        //获取记录数
        long value = searchHits.getTotalHits().value;
        System.out.println("总记录数："+value);

        List<Goods> goodsList = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            //转为java
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            goodsList.add(goods);
        }

        for (Goods goods : goodsList) {
            System.out.println(goods);
        }
    }


    /**
     * matchQuery:词条分词查询，分词之后的等值匹配
     */
    @Test
    public void testMatchQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBulider = new SearchSourceBuilder();
        MatchQueryBuilder query = QueryBuilders.matchQuery("title", "华为手机");
        query.operator(Operator.OR);//求并集

        sourceBulider.from(0);
        sourceBulider.size(100);

        sourceBulider.query(query);



        searchRequest.source(sourceBulider);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits searchHits = searchResponse.getHits();
        //获取记录数
        long value = searchHits.getTotalHits().value;
        System.out.println("总记录数："+value);

        List<Goods> goodsList = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            //转为java
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            goodsList.add(goods);
        }

        for (Goods goods : goodsList) {
            System.out.println(goods);
        }
    }


    /**
     * 模糊查询:WildcardQuery
     */
    @Test
    public void testWildcardQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBulider = new SearchSourceBuilder();
        WildcardQueryBuilder query = QueryBuilders.wildcardQuery("title", "华*");
        sourceBulider.query(query);
        searchRequest.source(sourceBulider);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = searchResponse.getHits();
        //获取记录数
        long value = searchHits.getTotalHits().value;
        System.out.println("总记录数："+value);
        List<Goods> goodsList = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            //转为java
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            goodsList.add(goods);
        }
        for (Goods goods : goodsList) {
            System.out.println(goods);
        }
    }


    /**
     * 模糊查询:perfixQuery
     */
    @Test
    public void testPrefixQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBulider = new SearchSourceBuilder();
        PrefixQueryBuilder query = QueryBuilders.prefixQuery("brandName", "三");
        sourceBulider.query(query);
        searchRequest.source(sourceBulider);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits searchHits = searchResponse.getHits();
        //获取记录数
        long value = searchHits.getTotalHits().value;
        System.out.println("总记录数："+value);

        List<Goods> goodsList = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            //转为java
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            goodsList.add(goods);
        }

        for (Goods goods : goodsList) {
            System.out.println(goods);
        }
    }



    /**
     * 1. 范围查询：rangeQuery
     * 2. 排序
     */
    @Test
    public void testRangeQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBulider = new SearchSourceBuilder();
        //范围查询
        RangeQueryBuilder query = QueryBuilders.rangeQuery("price");

        //指定下限 gte大于等于
        query.gte(2000);
        //指定上限 小于等于
        query.lte(3000);

        sourceBulider.query(query);

        //排序
        sourceBulider.sort("price", SortOrder.DESC);
        searchRequest.source(sourceBulider);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits searchHits = searchResponse.getHits();
        //获取记录数
        long value = searchHits.getTotalHits().value;
        System.out.println("总记录数："+value);

        List<Goods> goodsList = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            //转为java
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            goodsList.add(goods);
        }
        for (Goods goods : goodsList) {
            System.out.println(goods);
        }
    }


    /**
     * queryString
     */
    @Test
    public void testQueryStringQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBulider = new SearchSourceBuilder();

        //queryString
        QueryStringQueryBuilder query = QueryBuilders.queryStringQuery("华为手机")
                .field("title")
                .field("categoryName")
                .field("brandName")
                .defaultOperator(Operator.AND);

        sourceBulider.query(query);
        searchRequest.source(sourceBulider);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits searchHits = searchResponse.getHits();
        //获取记录数
        long value = searchHits.getTotalHits().value;
        System.out.println("总记录数："+value);

        List<Goods> goodsList = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            //转为java
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            goodsList.add(goods);
        }

        for (Goods goods : goodsList) {
            System.out.println(goods);
        }
    }


    /**
     * 布尔查询：boolQuery
     * 1. 查询品牌名称为:华为
     * 2. 查询标题包含：手机
     * 3. 查询价格在：2000-3000
     */
    @Test
    public void testBoolQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBulider = new SearchSourceBuilder();

        //1.构建boolQuery
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        //2.构建各个查询条件
        //2.1 查询品牌名称为:华为
        QueryBuilder termQuery = QueryBuilders.termQuery("brandName","华为");
        query.must(termQuery);

        //2.2. 查询标题包含：手机
        QueryBuilder matchQuery = QueryBuilders.matchQuery("title","手机");
        query.filter(matchQuery);

        //2.3 查询价格在：2000-3000
        QueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
        ((RangeQueryBuilder) rangeQuery).gte(2000);
        ((RangeQueryBuilder) rangeQuery).lte(3000);
        query.filter(rangeQuery);

        //3.使用boolQuery连接
        sourceBulider.query(query);
        searchRequest.source(sourceBulider);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = searchResponse.getHits();
        //获取记录数
        long value = searchHits.getTotalHits().value;
        System.out.println("总记录数："+value);

        List<Goods> goodsList = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            //转为java
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            goodsList.add(goods);
        }

        for (Goods goods : goodsList) {
            System.out.println(goods);
        }
    }

    /**
     * 聚合查询：桶聚合，分组查询
     * 1. 查询title包含手机的数据
     * 2. 查询品牌列表
     */
    @Test
    public void testAggQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBulider = new SearchSourceBuilder();
        // 1. 查询title包含手机的数据
        MatchQueryBuilder query = QueryBuilders.matchQuery("title", "手机");

        sourceBulider.query(query);
        // 2. 查询品牌列表
        /* 参数：
            1. 自定义的名称，将来用于获取数据
            2. 分组的字段
         */
        AggregationBuilder agg = AggregationBuilders.terms("goods_brands").field("brandName").size(100);
        sourceBulider.aggregation(agg);

        searchRequest.source(sourceBulider);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = searchResponse.getHits();
        //获取记录数
        long value = searchHits.getTotalHits().value;
        System.out.println("总记录数："+value);

        List<Goods> goodsList = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            //转为java
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            goodsList.add(goods);
        }

        for (Goods goods : goodsList) {
            System.out.println(goods);
        }

        // 获取聚合结果
        Aggregations aggregations = searchResponse.getAggregations();
        Map<String, Aggregation> aggregationMap = aggregations.asMap();

        //System.out.println(aggregationMap);
        Terms goods_brands = (Terms) aggregationMap.get("goods_brands");
        List<? extends Terms.Bucket> buckets = goods_brands.getBuckets();

        List<Map<String,Object>> brands = new ArrayList();
        for (Terms.Bucket bucket : buckets) {
            Map<String,Object> map = new HashMap<>();
            Object key = bucket.getKey();
            long docCount = bucket.getDocCount();
            map.put("key", key);
            map.put("docCount", docCount);
            brands.add(map);
        }



        for (Map brand : brands) {
            System.out.println(brand.get("key") + ":" + brand.get("docCount"));
        }
    }


    /**
     *
     * 高亮查询：
     *  1. 设置高亮
     *      * 高亮字段
     *      * 前缀
     *      * 后缀
     *  2. 将高亮了的字段数据，替换原有数据
     */
    @Test
    public void testHighLightQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBulider = new SearchSourceBuilder();

        // 1. 查询title包含手机的数据
        MatchQueryBuilder query = QueryBuilders.matchQuery("title", "手机");
        sourceBulider.query(query);

        //设置高亮
        HighlightBuilder highlighter = new HighlightBuilder();
        //设置三要素
        highlighter.field("title");
        highlighter.preTags("<font color='red'>");
        highlighter.postTags("</font>");
        sourceBulider.highlighter(highlighter);

        // 2. 查询品牌列表
        /*
        参数：
            1. 自定义的名称，将来用于获取数据
            2. 分组的字段
         */
        AggregationBuilder agg = AggregationBuilders.terms("goods_brands").field("brandName").size(100);
        sourceBulider.aggregation(agg);

        searchRequest.source(sourceBulider);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits searchHits = searchResponse.getHits();
        //获取记录数
        long value = searchHits.getTotalHits().value;
        System.out.println("总记录数："+value);

        List<Goods> goodsList = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            //转为java
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            // 获取高亮结果，替换goods中的title
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField HighlightField = highlightFields.get("title");
            Text[] fragments = HighlightField.fragments();
            //替换
            goods.setTitle(fragments[0].toString());
            goodsList.add(goods);
        }

        for (Goods goods : goodsList) {
            System.out.println(goods);
        }
    }
}
