
package cn.xfyun.demo;

import cn.xfyun.api.WordLibClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * 词库黑白名单Demo
 *
 * @author zyding6
 */
public class WordLibClientApp {

    private static final Logger logger = LoggerFactory.getLogger(WordLibClientApp.class);

    private static final String APP_ID = "图片/文本合规的appId";
    private static final String API_KEY = "图片/文本合规的apiKey";
    private static final String API_SECRET = "图片/文本合规的apiSecret";

    public static void main(String[] args) {
        try {
            WordLibClient client = new WordLibClient.Builder(APP_ID, API_KEY, API_SECRET).build();

            // 根据appid查询账户下所有词库
            String listLib = client.listLib();
            logger.info("{} 账户下所有词库结果：{}", APP_ID, listLib);

            // 创建白名单
            String whiteLib = client.createWhiteLib("白名单词库1");
            logger.info("创建白名单词库返回结果：{}", whiteLib);

            // 创建黑名单
            String blackLib = client.createBlackLib("黑名单词库1", "contraband");
            logger.info("创建黑名单词库返回结果：{}", blackLib);

            // 查询词条明细
            String info = client.info("04eb999dfc024b7fa61b45d057cbca37");
            logger.info("查询词条明细结果：{}", info);

            // 添加词条
            List<String> search = Arrays.asList("傻缺", "蠢才");
            String addWord = client.addWord("04eb999dfc024b7fa61b45d057cbca37", search);
            logger.info("添加词条结果：{}", addWord);

            // 删除词条
            List<String> delList = Arrays.asList("蠢才");
            String delWord = client.delWord("04eb999dfc024b7fa61b45d057cbca37", delList);
            logger.info("删除词条结果：{}", delWord);

            // 根据lib_id删除词库
            String deleteLib = client.deleteLib("04eb999dfc024b7fa61b45d057cbca37");
            logger.info("删除词库结果：{}", deleteLib);
        } catch (Exception e) {
            logger.error("请求失败", e);
        }
    }
}
