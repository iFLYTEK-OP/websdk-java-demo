package cn.xfyun.demo.spark;

import cn.xfyun.api.WebSearchClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.websearch.WebSearchParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 聚合搜索
 * 1、APPID、APIPassword信息获取：<a href="https://console.xfyun.cn/services/aggSearch">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/Search_API/search_API.html">...</a>
 */
public class WebSearchClientApp {

    private static final Logger logger = LoggerFactory.getLogger(WebSearchClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiPassword = PropertiesConfig.getApiSecret();

    public static void main(String[] args) throws IOException {
        WebSearchClient client = new WebSearchClient
                .Builder(appId, apiPassword)
                .build();

        WebSearchParam param = WebSearchParam.builder()
                .query("先有鸡还是先有蛋")
                .limit(10)
                .build();
        String resp = client.send(param);
        logger.info("请求结果：{}", resp);
    }
}
