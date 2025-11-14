package cn.xfyun.demo.spark;

import cn.xfyun.api.AiUiKnowledgeClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.aiui.knowledge.SearchParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Objects;

/**
 * 个性化知识库 Client
 * 1、APPID、APISecret、APIKey、APIPassword信息获取：<a href="https://console.xfyun.cn/services/VCN">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/Interact_KM.html#%E4%B8%80%E3%80%81%E6%8E%A5%E5%8F%A3%E8%AF%B4%E6%98%8E">...</a>
 */
public class AiUiKnowledgeClientApp {

    private static final Logger logger = LoggerFactory.getLogger(AiUiKnowledgeClientApp.class);
    private static final String API_PASSWORD = PropertiesConfig.getApiSecret();
    private static final String APP_ID = PropertiesConfig.getAppId();
    /**
     * 场景名称
     */
    private static final String SCENE_NAME = "sos_app";
    /**
     * 用户唯一标识ID
     */
    private static final long UID = 123123213;
    private static String filePath;
    private static String resourcePath;

    static {
        try {
            filePath = "document/aiuiknowledge.txt";
            resourcePath = Objects.requireNonNull(AiUiKnowledgeClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) {
        try {
            AiUiKnowledgeClient client = new AiUiKnowledgeClient.Builder(API_PASSWORD).build();

            // 创建个性化知识库
            // CreateParam createParam = CreateParam.builder()
            //         .uid(UID)
            //         .name("测试库-001")
            //         .build();
            // String createResp = client.create(createParam);
            // logger.info("创建结果：{}", createResp);

            // 查询个性化知识库，如果知识库没有文件，则查询结果为空
            SearchParam searchParam = SearchParam.builder()
                    .uid(UID)
                    .appId(APP_ID)
                    .sceneName(SCENE_NAME)
                    .build();
            String searchResp = client.list(searchParam);
            logger.info("查询结果：{}", searchResp);

            // 关联知识库，管理知识库传参为全量保存方式
            // LinkParam.Repo repo = new LinkParam.Repo();
            // repo.setGroupId("您创建知识库返回的groupId");
            // LinkParam linkParam = LinkParam.builder()
            //         .appId(APP_ID)
            //         .sceneName(SCENE_NAME)
            //         .uid(UID)
            //         .repos(Collections.singletonList(repo))
            //         .build();
            // String linkResp = client.link(linkParam);
            // logger.info("关联结果：{}", linkResp);

            // // 上传文件  支持本地文件和在线文件两种上传方式  冲突默认取本地上传文件
            // UploadParam.FileInfo fileInfo = new UploadParam.FileInfo("aiuiknowledge.txt",
            //         "https://oss-beijing-m8.openstorage.cn/knowledge-origin-test/knowledge/file/123123213/7741/a838a943/20250910163419/aiuiknowledge.txt",
            //         43L);
            // File file = new File(resourcePath + filePath);
            // UploadParam uploadParam = UploadParam.builder()
            //         .uid(UID)
            //         .groupId("您创建知识库返回的groupId")
            //         // .fileList(Collections.singletonList(fileInfo))
            //         .files(Collections.singletonList(file))
            //         .build();
            // String uploadResp = client.upload(uploadParam);
            // logger.info("上传结果：{}", uploadResp);

            // // 删除知识库或者知识库内文件
            // DeleteParam deleteParam = DeleteParam.builder()
            //         .uid(UID)
            //         .groupId("您创建知识库返回的groupId")
            //         .docId("您上传文件返回的docId")
            //         .build();
            // String deleteResp = client.delete(deleteParam);
            // logger.info("删除结果：{}", deleteResp);
        } catch (Exception e) {
            logger.error("请求失败", e);
        }
    }
}
