package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

//单元测试要放在和启动类相同的包或者子包
@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticSearchTest {
    private RestHighLevelClient client;

    @Test
    void testConnection(){
        //测试前后都会去执行初始化方法和销毁
        System.out.println("client = " + client);
    }

    @Test
    void testMatchAll() throws IOException {
        //1.创建request对象
        SearchRequest request = new SearchRequest("items");
        //2.配置request参数
        request.source()
                .query(QueryBuilders.matchAllQuery());  //无条件查询
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }

    private static void parseResponseResult(SearchResponse response) {
        //4.解析结果
        SearchHits searchHits = response.getHits();
        //4.1总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("total = " + total);
        //4.2命中的数据
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            //4.2.1获取source结果
            String json = hit.getSourceAsString();
            //4.2.2转为ItemDoc
            ItemDoc doc = JSONUtil.toBean(json, ItemDoc.class);
            //4.3获取高亮结果
            Map<String, HighlightField> hfs = hit.getHighlightFields();
            if(hfs != null && !hfs.isEmpty()){
                //4.3.1根据高亮字段名获取高亮结果
                HighlightField hf = hfs.get("name");
                //4.3.2获取高亮结果，覆盖非高亮结果
                String hfName = hf.getFragments()[0].string();
                doc.setName(hfName);
            }

            System.out.println("doc = " + doc);
        }
    }

    //搜索关键字为脱脂牛奶，品牌必须为德亚，价格必须低于300
    @Test
    void testSearch() throws IOException {
        SearchRequest request = new SearchRequest("items");
        //组织DSL参数
        request.source()
                .query(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("name", "脱脂牛奶"))
                        .filter(QueryBuilders.termQuery("brand", "德亚"))
                        .filter(QueryBuilders.rangeQuery("price").lt(30000))
                );
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }

    @Test
    void testSortAndPage() throws IOException {
        //模拟前端传递的分页参数
        int pageNo = 1, pageSize = 5;

        SearchRequest request = new SearchRequest("items");
        //query条件
        request.source().query(QueryBuilders.matchAllQuery());
        //分页
        request.source().from((pageNo - 1) * pageSize).size(pageSize);
        //排序
        request.source()
                .sort("sold", SortOrder.DESC)
                .sort("price", SortOrder.ASC);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }

    @Test
    void testHighlight() throws IOException {
        SearchRequest request = new SearchRequest("items");
        //query条件
        request.source().query(QueryBuilders.matchQuery("name", "脱脂牛奶"));
        //高亮条件
        request.source().highlighter(SearchSourceBuilder.highlight().field("name"));

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }

    @Test
    void testAggregation() throws IOException {
        SearchRequest request = new SearchRequest("items");
        //分页
        request.source().size(0);
        //聚合条件
        String brandAggName = "brandAgg";
        request.source().aggregation(
                AggregationBuilders.terms(brandAggName).field("brand").size(10)
        );

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        Aggregations aggregations = response.getAggregations();
        //4.1根据聚合名称获取对应的聚合
        Terms brandTerms = aggregations.get(brandAggName);      //用的是什么聚合类型，就要找到对应的接口来接收
        //4.2获取buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        //4.3遍历获取每一个bucket
        for (Terms.Bucket bucket : buckets) {
            System.out.println(bucket.getKeyAsString());
            System.out.println(bucket.getDocCount());
        }
    }

    //初始化方法
    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.139.130:9200")    //集群的话写多个
        ));
    }

    //销毁
    @AfterEach
    void tearDown() throws IOException {
        if(client != null){
            client.close();
        }
    }

}
