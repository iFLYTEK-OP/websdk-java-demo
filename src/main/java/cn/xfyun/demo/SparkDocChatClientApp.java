package cn.xfyun.demo;

import cn.xfyun.chat.SparkDoc;
import cn.xfyun.chat.SparkDocChat;
import cn.xfyun.model.UserMessage;

/**
 * @author: rblu2
 * @desc: 选择模型 + 对应得apiPassword
 * From <a href="https://console.xfyun.cn/services/cbm">...</a>
 * @create: 2025-02-18 19:12
 **/
public class SparkDocChatClientApp {

    public static void main(String[] args) throws InterruptedException {
        test4();
    }

    //上传文档，投喂知识-文件
    public static void test1() {
        String result = SparkDoc.prepare("6057995a", "YjRkOTBlODAxM2U2NzIyZmMzMDhmMTdk")
                .uploadFile("D:\\draft\\250224\\背影.txt");
        print(result);
    }

    //上传文档，投喂知识-远程文件
    public static void test2() {
        String result = SparkDoc.prepare("6057995a", "YjRkOTBlODAxM2U2NzIyZmMzMDhmMTdk")
                .uploadUrl("https://openres.xfyun.cn/xfyundoc/2025-02-25/51087fc6-ecb2-4fa4-820b-6a3326f06cab/1740447798025/背影.txt", "背影.txt");
        print(result);
    }

    //查询文档训练状态,fieldId值，在test1上传文档后的返回里结果里获取
    public static void test3() {
        String result = SparkDoc.prepare("6057995a", "YjRkOTBlODAxM2U2NzIyZmMzMDhmMTdk")
                .addFileId("633e74f5f1d44d638f14a2c0a7f8beca")
                .status();
        print(result);

    }

    //对话问答，（训练状态fileStatus为vectored时）
    public static void test4() throws InterruptedException {
        SparkDocChat.prepare("6057995a", "YjRkOTBlODAxM2U2NzIyZmMzMDhmMTdk")
                .addFileId("633e74f5f1d44d638f14a2c0a7f8beca")
                .temperature(0.99f)
                .append(UserMessage.crate("本文的中心思想是什么？"))
                .onMessage(SparkDocChatClientApp::print)
                .execute();
        // 保持主线程运行，防止程序退出
        Thread.sleep(Long.MAX_VALUE);
    }


    //对大模型的返回，开发者自定义处理逻辑
    static void print(String message) {
        System.out.println("receive " + message);
    }
}
