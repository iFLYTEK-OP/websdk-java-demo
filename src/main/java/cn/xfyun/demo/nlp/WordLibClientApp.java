
package cn.xfyun.demo.nlp;

import cn.xfyun.api.WordLibClient;
import cn.xfyun.config.CategoryEnum;
import cn.xfyun.config.PropertiesConfig;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * （word-lib）词库操作
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/text_audit">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/nlp/TextModeration/API.html">...</a>
 */
public class WordLibClientApp {

    private static final Logger logger = LoggerFactory.getLogger(WordLibClientApp.class);
    private static final String APP_ID = PropertiesConfig.getAppId();
    private static final String API_KEY = PropertiesConfig.getApiKey();
    private static final String API_SECRET = PropertiesConfig.getApiSecret();

    public static void main(String[] args) {
        try {
            WordLibClient client = new WordLibClient.Builder(APP_ID, API_KEY, API_SECRET).build();

            // 根据appid查询账户下所有词库
            String listLib = client.listLib();
            logger.info("{} 账户下所有词库结果：{}", APP_ID, listLib);

            // 创建白名单
            String whiteLib = client.createWhiteLib("白名单词库1");
            logger.info("创建白名单词库返回结果：{}", whiteLib);
            String whiteLibId = JSON.parseObject(whiteLib).getJSONObject("data").getString("lib_id");
            logger.info("白名单词库id：{}", whiteLibId);

            // 创建黑名单
            String blackLib = client.createBlackLib("黑名单词库1", CategoryEnum.CONTRABAND.getCode(), null);
            logger.info("创建黑名单词库返回结果：{}", blackLib);
            String blackLibId = JSON.parseObject(blackLib).getJSONObject("data").getString("lib_id");
            logger.info("黑名单词库id：{}", blackLibId);

            // 查询词条明细
            String info = client.info(blackLib);
            logger.info("查询词条明细结果：{}", info);

            // 添加词条
            List<String> search = Arrays.asList("傻缺", "蠢才");
            String addWord = client.addWord(blackLib, search);
            logger.info("添加词条结果：{}", addWord);

            // 删除词条
            List<String> delList = Arrays.asList("蠢才");
            String delWord = client.delWord(blackLib, delList);
            logger.info("删除词条结果：{}", delWord);

            // 根据lib_id删除词库
            String deleteLib = client.deleteLib(blackLib);
            logger.info("删除词库结果：{}", deleteLib);
        } catch (Exception e) {
            logger.error("请求失败", e);
        }
    }
}
