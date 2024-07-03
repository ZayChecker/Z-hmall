package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

//单元测试要放在和启动类相同的包或者子包
@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticDocumentTest {
    private RestHighLevelClient client;
    @Autowired
    private IItemService itemService;

    @Test
    void testConnection(){
        //测试前后都会去执行初始化方法和销毁
        System.out.println("client = " + client);
    }

    @Test
    void testIndexDoc() throws IOException {
        //0.准备文档数据
        //0.1根据id查询数据库数据
        Item item = itemService.getById(317578L);
        //0.2把数据库数据转为文档数据
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        //1.准备Request对象
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        //2.准备请求参数
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        //3.发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGetDoc() throws IOException {
        GetRequest request = new GetRequest("items","317578");
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        //解析响应结果
        String json = response.getSourceAsString();
        ItemDoc doc = JSONUtil.toBean(json, ItemDoc.class);
        System.out.println("doc = " + doc);
    }

    @Test
    void testDeleteDoc() throws IOException {
        DeleteRequest request = new DeleteRequest("items","317578");
        client.delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void testUpdateDoc() throws IOException {
        UpdateRequest request = new UpdateRequest("items","317578");
        request.doc(
                "price", 100
        );
        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    void testBulkDoc() throws IOException {
        int pageNo = 1, pageSize = 500;
        while (true){
            Page<Item> page = itemService.lambdaQuery().
                    eq(Item::getStatus, 1).                   //正常商品
                            page(new Page<Item>(pageNo, pageSize));       //取出一页，500条
            List<Item> records = page.getRecords();               //取出一页的数据
            if(records == null || records.isEmpty()){
                return;
            }

            BulkRequest request = new BulkRequest();
            for (Item item : records) {
                request.add(new IndexRequest("items")
                        .id(item.getId().toString())
                        .source(JSONUtil.toJsonStr(BeanUtil.copyProperties(item, ItemDoc.class)), XContentType.JSON));
            }
            client.bulk(request, RequestOptions.DEFAULT);
            //翻页
            pageNo++;
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
