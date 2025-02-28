package cn.xfyun.demo;

import cn.xfyun.config.SparkModelEum;
import cn.xfyun.domain.HttpSparkChat;
import cn.xfyun.domain.WsSparkChat;
import cn.xfyun.model.SystemMessage;
import cn.xfyun.model.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author: rblu2
 * @desc: 选择模型 + 对应得apiPassword
 * From <a href="https://console.xfyun.cn/services/cbm">...</a>
 * @create: 2025-02-18 19:12
 **/
public class SparkChatClientApp {

    private static final Logger logger = LoggerFactory.getLogger(SparkChatClientApp.class);
    
    public static void main(String[] args) throws InterruptedException {
        test3();
    }

    //http非流式调用，适用于简单的回答
    public static void test1() {
        String result = HttpSparkChat.prepare(SparkModelEum.LITE, "xx")
                .append(UserMessage.create("来一个妹子喜欢听的笑话"))
                .execute();
        print("result " + result);
    }

    //http流式调用
    public static void test2() {
        HttpSparkChat.prepare(SparkModelEum.V4_ULTRA, "xx")
                .webSearch()
                .append(SystemMessage.create("你是一个新闻工作者")).append(UserMessage.create("今日3条热点娱乐新闻"))
                .execute(SparkChatClientApp::print);
    }

    //WEBSOCKET调用
    public static void test3() throws InterruptedException {
        logger.debug("test3");
        WsSparkChat.prepare(SparkModelEum.GENERAL_V35, "6057995a", "28bb4b72196174bb290e8d60876a1013", "YjRkOTBlODAxM2U2NzIyZmMzMDhmMTdk")
                .onMessage(SparkChatClientApp::print)
                .append(SystemMessage.create("你现在扮演李白")).append(UserMessage.create("你喝醉过吗"))
                .execute();
        // 保持主线程运行，防止程序退出
        Thread.sleep(Long.MAX_VALUE);
    }


    //对大模型的返回，开发者自定义处理逻辑
    static void print(String message) {
        System.out.println("receive " + message);
    }
}
