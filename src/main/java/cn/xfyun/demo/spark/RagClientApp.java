package cn.xfyun.demo.spark;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.xfyun.api.RagClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.rag.*;
import cn.xfyun.model.sparkmodel.RoleContent;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Objects;

/**
 * 知识库问答
 * 1、APPID、APISecret、APIKey、APIPassword信息获取：<a href="https://console.xfyun.cn/services/aidoc">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/ChatDoc-API.html">...</a>
 */
public class RagClientApp {

    private static final Logger logger = LoggerFactory.getLogger(RagClientApp.class);
    private static final String APP_ID = PropertiesConfig.getAppId();
    private static final String API_SECRET = PropertiesConfig.getApiSecret();
    private static String filePath;
    private static String resourcePath;

    static {
        try {
            filePath = "document/repo.txt";
            resourcePath = Objects.requireNonNull(RagClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) {
        try {
            RagClient client = new RagClient.Builder(APP_ID, API_SECRET).build();

            String fileId = "";
            String repoId = "";

            // 1. 文件上传
            RepoUpload upload = RepoUpload.builder()
                    .file(new File(resourcePath + filePath))
                    .build();
            String uploadResp = client.fileUpload(upload);
            JSONObject uploadObj = JSONUtil.parseObj(uploadResp);
            fileId = uploadObj.getJSONObject("data").getStr("fileId");
            logger.info("知识库文件上传结果：{}", uploadResp);

            // 2. 文件状态
            // FileStatus status = FileStatus.builder()
            //         .fileIds(fileId)
            //         .build();
            // String statusResp = client.fileStatus(status);
            // logger.info("知识库文件上传状态结果：{}", statusResp);

            // 3. 文档问答
            // RoleContent user = RoleContent.builder()
            //         .role("user")
            //         .content("故事一讲了什么内容")
            //         .build();
            // FileChat chat = FileChat.builder()
            //         .fileIds(Collections.singletonList(fileId))
            //         .messages(Collections.singletonList(user))
            //         .build();
            // client.chat(chat, getListener());

            // 4. 提交萃取任务
            // FileExtract fileExtract = FileExtract.builder()
            //         .fileId(fileId)
            //         .chunkSize(2000)
            //         .numPerChunk(2)
            //         .answerSize(100)
            //         .includeAnswer(true)
            //         .topicPreference(Collections.singletonList("故事"))
            //         // .notifyUrl("萃取任务回调地址")
            //         .build();
            // String extractResp = client.fileExtract(fileExtract);
            // JSONObject extractObj = JSONUtil.parseObj(extractResp);
            // String taskId = extractObj.getStr("data");
            // logger.info("提交萃取任务结果：{}", extractResp);

            // 5. 文件萃取状态查询
            // FileResult fileExtractStatus = FileResult.builder()
            //         .fileId(fileId)
            //         .build();
            // String extractStatus = client.fileExtractStatus(fileExtractStatus);
            // logger.info("文件萃取状态查询结果：{}", extractStatus);

            // 6. 获取萃取结果
            // FileResult fileExtractResult = FileResult.builder()
            //         .taskId(taskId)
            //         .build();
            // String extractResult = client.fileExtractResult(fileExtractResult);
            // logger.info("获取萃取结果查询：{}", extractResult);

            // 7. QA对应用
            // QaApply qaApply = QaApply.builder()
            //         .fileId(fileId)
            //         .question("1+1等于几?")
            //         .answer("2")
            //         .embType("QA")
            //         .build();
            // String qaApplyResult = client.qaApply(qaApply);
            // JSONObject qaApplyResultObj = JSONUtil.parseObj(qaApplyResult);
            // String qaId = qaApplyResultObj.getStr("data");
            // logger.info("QA对应用返回结果：{}", qaApplyResult);

            // 8. QA对查询
            // QaQuery qaQuery = QaQuery.builder()
            //         .fileId(fileId)
            //         // .repoId()
            //         .currentPage(1)
            //         .pageSize(10)
            //         .build();
            // String qaQueryResp = client.qaQuery(qaQuery);
            // logger.info("QA对查询返回结果：{}", qaQueryResp);

            // 9. QA对更新
            // QaApply qaUpdate = QaApply.builder()
            //         .id(qaId)
            //         .fileId(fileId)
            //         .question("2+2等于几?")
            //         .answer("4")
            //         .embType("QA")
            //         .build();
            // String qaUpdateResp = client.qaUpdate(qaUpdate);
            // logger.info("QA对更新返回结果：{}", qaUpdateResp);

            // 10. QA对删除
            // String qaDeleteResp = client.qaDelete(Collections.singletonList(qaId));
            // logger.info("QA对删除返回结果：{}", qaDeleteResp);

            // 11. 发起文档总结
            // String fileSummaryCreate = client.fileSummaryCreate(fileId);
            // logger.info("发起文档总结返回结果：{}", fileSummaryCreate);

            // 12. 获取文档总结信息
            // String fileSummaryQuery = client.fileSummaryQuery(fileId);
            // logger.info("文件萃取状态查询结果：{}", fileSummaryQuery);

            // 13. 文档切分
            // FileSplit fileSplit = FileSplit.builder()
            //         .fileIds(Collections.singletonList(fileId))
            //         .build();
            // String fileSplitResp = client.fileSplit(fileSplit);
            // logger.info("文档切分返回结果：{}", fileSplitResp);

            // 14. 文档向量化
            // String fileEmbeddingResp = client.fileEmbeddingV2(Collections.singletonList(fileId));
            // logger.info("文档向量化返回结果：{}", fileEmbeddingResp);

            // 15. 文档内容相似度检测
            // FileVector fileVector = FileVector.builder()
            //         .fileIds(Collections.singletonList(fileId))
            //         .content("小老虎对着野猪说了什么")
            //         .build();
            // String fileVectorResp = client.fileVector(fileVector);
            // logger.info("文档内容相似度检测返回结果：{}", fileVectorResp);

            // 16. 文档分块内容获取
            // String fileChunksResp = client.fileChunks(fileId);
            // logger.info("文档分块内容获取返回结果：{}", fileChunksResp);

            // 17. 文档详情
            // String fileInfoResp = client.fileInfo(fileId);
            // logger.info("文档详情返回结果：{}", fileInfoResp);

            // 18. 文档列表
            // FileList fileList = FileList.builder()
            //         // .fileName("小老虎")
            //         // .extName("txt")
            //         // .fileStatus("vectored")
            //         // .currentPage(1)
            //         // .pageSize(10)
            //         .build();
            // String fileListResp = client.fileList(fileList);
            // logger.info("文档列表返回结果：{}", fileListResp);

            // 19. 文档删除
            // String fileDeleteResp = client.fileDelete(Collections.singletonList(fileId));
            // logger.info("文档删除返回结果：{}", fileDeleteResp);

            // 20. 知识库创建
            // RepoCreate repoCreate = RepoCreate.builder()
            //         .repoName("测试知识库")
            //         .repoDesc("测试知识库-2025-12-10")
            //         .repoTags("test,example")
            //         .build();
            // String repoCreateResp = client.repoCreate(repoCreate);
            // JSONObject repoCreateObj = JSONUtil.parseObj(repoCreateResp);
            // repoId = repoCreateObj.getStr("data");
            // logger.info("知识库创建返回结果：{}", repoCreateResp);

            // 21. 知识库添加文件
            // RepoFile repoFile = RepoFile.builder()
            //         .fileIds(Collections.singletonList(fileId))
            //         .repoId(repoId)
            //         .build();
            // String repoAddFileResp = client.repoAddFile(repoFile);
            // logger.info("知识库添加文件返回结果：{}", repoAddFileResp);

            // 22. 知识库移除文件
            // String repoRemoveFileResp = client.repoRemoveFile(repoFile);
            // logger.info("知识库移除文件返回结果：{}", repoRemoveFileResp);

            // 23. 知识库列表
            // RepoQuery repoQuery = RepoQuery.builder()
            //         .repoName("测试")
            //         // .currentPage(1)
            //         // .pageSize(10)
            //         .build();
            // String repoListResp = client.repoList(repoQuery);
            // logger.info("知识库列表返回结果：{}", repoListResp);

            // 24. 知识库详情
            // String repoInfoResp = client.repoInfo(repoId);
            // logger.info("知识库详情返回结果：{}", repoInfoResp);

            // 25. 知识库文件列表
            // RepoFileList repoFileList = RepoFileList.builder()
            //         .repoId(repoId)
            //         // .extName("txt")
            //         // .fileName("repo")
            //         // .currentPage(1)
            //         // .pageSize(10)
            //         .build();
            // String repoFileListResp = client.repoFileList(repoFileList);
            // logger.info("知识库文件列表返回结果：{}", repoFileListResp);

            // 26. 知识库删除
            // String repoDeleteResp = client.repoDelete(repoId);
            // logger.info("知识库删除返回结果：{}", repoDeleteResp);

            // 27. 新版本向量化
            // RoleContent user1 = RoleContent.builder()
            //         .role("user")
            //         .content(getContent())
            //         .build();
            // EmbeddingParam param = EmbeddingParam.builder()
            //         .appId("您的向量化APP_ID")
            //         .apiKey("您的向量化API_KEY")
            //         .apiSecret("您的向量化API_SECRET")
            //         .messages(Collections.singletonList(user1))
            //         .domain("query")
            //         .build();
            // String embeddingResp = client.embedding(param);
            // logger.info("新版本向量化返回结果：{}", embeddingResp);
        } catch (Exception e) {
            logger.error("请求失败", e);
        }
    }

    private static WebSocketListener getListener() {
        return new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                JSONObject result = JSONUtil.parseObj(text);
                Integer code = result.getInt("code", -1);
                if (0 != code) {
                    String message = result.getStr("message", "未知错误");
                    String sid = result.getStr("sid");
                    logger.error("错误码：{}，错误信息：{}，sid：{}", code, message, sid);
                }
                Integer status = result.getInt("status");
                if (2 == status) {
                    webSocket.close(1000, "会话结束，正常关闭");
                }
                logger.info("返回结果：{}", text);
            }

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                System.exit(0);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
                System.exit(0);
            }
        };
    }

    private static String getContent() {
        return "故事1：小老虎问路\n" +
                "\n" +
                "一头骄傲的小老虎在大森林里迷了路。\n" +
                "\n" +
                "他走啊走，看到了一头正在蒙头大睡的野猪。小老虎对着野猪的耳朵，大声喊道：“喂，蠢猪，别打呼噜了，告诉我回家的路怎么走吧!”野猪生气地眨了眨眼睛，一言不发，把屁股转向了小老虎，继续睡大觉。小老虎讨了个没趣，无奈地走了。\n" +
                "\n" +
                "小老虎问路\n" +
                "\n" +
                "路上，他看到一只正在忙碌的小松鼠，于是他用自己的大嗓门儿喊道：“喂，如果你告诉我回家的路怎么走，我就让妈妈给你最好的礼物!”小松鼠就像没听见一样，不搭理小老虎，照样干自己的活儿。小老虎勃然大怒，冲向一只戴眼镜的老灰兔：“嘿，花眼的老兔头，快给我指一条回家的路!”老灰兔慢慢地抬起头，和蔼地说：“森林里的路大家都熟悉，可你这样没礼貌，哪怕你问遍所有的动物，你还是找不到回家的路。”听了老灰兔的话，小老虎猛然醒悟过来——对人说话，要有礼貌才行!\n" +
                "\n" +
                "这时，前面过来一只梅花鹿。小老虎走过去，礼貌地说：“梅花鹿你好，请你告诉我回家的路怎么走，好吗?”梅花鹿热情地告诉了小老虎，小老虎高兴的连声说：“谢谢你，梅花鹿，谢谢你!”\n" +
                "\n" +
                "小老虎终于安全地回到了自己的小屋。";
    }
}
