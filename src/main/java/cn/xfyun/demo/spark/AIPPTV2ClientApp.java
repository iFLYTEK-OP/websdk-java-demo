package cn.xfyun.demo.spark;

import cn.hutool.json.JSONUtil;
import cn.xfyun.api.AIPPTV2Client;
import cn.xfyun.model.aippt.request.Outline;
import cn.xfyun.model.aippt.request.PPTCreate;
import cn.xfyun.model.aippt.request.PPTSearch;
import cn.xfyun.model.aippt.response.PPTCreateResponse;
import cn.xfyun.model.aippt.response.PPTProgressResponse;
import cn.xfyun.model.aippt.response.PPTThemeResponse;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * （ai-ppt-v2）智能PPT（新）
 * 1、APPID、APISecret、APIKey、APIPassword信息获取：<a href="https://console.xfyun.cn/services/zwapi">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/PPTv2.html">...</a>
 */
public class AIPPTV2ClientApp {

    private static final Logger logger = LoggerFactory.getLogger(AIPPTV2ClientApp.class);
    private static final String APP_ID = "替换成你的appid";
    private static final String API_SECRET = "替换成你的apiSecret";
    private static String filePath;
    private static String resourcePath;

    static {
        try {
            filePath = "document/aipptpro.pdf";
            resourcePath = Objects.requireNonNull(AIPPTV2ClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) {
        try {
            AIPPTV2Client client = new AIPPTV2Client.Builder(APP_ID, API_SECRET).build();

            PPTSearch searchParam = PPTSearch.builder()
                    .pageNum(1)
                    .pageSize(2)
                    .build();
            String themeList = client.list(searchParam);
            PPTThemeResponse themeResponse = JSONUtil.toBean(themeList, PPTThemeResponse.class);
            logger.info("ppt主题列表：{}", JSONUtil.toJsonStr(themeResponse));

            PPTCreate createParam = PPTCreate.builder()
                    .query("根据提供的文件生成ppt")
                    .file(new File(resourcePath + filePath), "aipptpro.pdf")
                    .build();
            String create = client.create(createParam);
            PPTCreateResponse createResponse = JSONUtil.toBean(create, PPTCreateResponse.class);
            logger.info("ppt生成返回结果：{}", JSONUtil.toJsonStr(createResponse));

            PPTCreate createOutlineParam = PPTCreate.builder()
                    .query("生成一个介绍科大讯飞的大纲")
                    .build();
            String createOutline = client.createOutline(createOutlineParam);
            PPTCreateResponse createOutlineResp = JSONUtil.toBean(createOutline, PPTCreateResponse.class);
            logger.info("ppt大纲生成返回结果：{}", JSONUtil.toJsonStr(createOutlineResp));

            PPTCreate docParam = PPTCreate.builder()
                    .query("模仿这个文件生成一个随机的大纲")
                    .file(new File(resourcePath + filePath), "aipptpro.pdf")
                    .build();
            String docResult = client.createOutlineByDoc(docParam);
            PPTCreateResponse docResponse = JSONUtil.toBean(docResult, PPTCreateResponse.class);
            logger.info("自定义大纲生成返回结果：{}", JSONUtil.toJsonStr(docResponse));

            PPTCreate outLine = PPTCreate.builder()
                    .query("生成一个介绍科大讯飞的ppt")
                    .outline(getMockOutLine())
                    .build();
            String ppt = client.createPptByOutline(outLine);
            PPTCreateResponse pptResponse = JSONUtil.toBean(ppt, PPTCreateResponse.class);
            logger.info("通过大纲生成PPT生成返回结果：{}", JSONUtil.toJsonStr(pptResponse));

            String progress = client.progress(outLine.getOutlineSid());
            PPTProgressResponse progressResp = JSONUtil.toBean(progress, PPTProgressResponse.class);
            logger.info("查询PPT进度返回结果：{}", JSONUtil.toJsonStr(progressResp));
        } catch (Exception e) {
            logger.error("请求失败", e);
        }
    }

    public static Outline getMockOutLine() {
        String outLineStr = "{\"title\":\"科大讯飞技术与创新概览\",\"subTitle\":\"探索语音识别与人工智能的前沿发展\",\"chapters\":[{\"chapterTitle\":\"科大讯飞简介\",\"chapterContents\":[{\"chapterTitle\":\"公司历史\"},{\"chapterTitle\":\"主要业务\"}]},{\"chapterTitle\":\"技术与创新\",\"chapterContents\":[{\"chapterTitle\":\"语音识别技术\"},{\"chapterTitle\":\"人工智能应用\"}]},{\"chapterTitle\":\"产品与服务\",\"chapterContents\":[{\"chapterTitle\":\"智能语音产品\"},{\"chapterTitle\":\"教育技术服务\"}]},{\"chapterTitle\":\"市场地位\",\"chapterContents\":[{\"chapterTitle\":\"行业领导者\"},{\"chapterTitle\":\"国际影响力\"}]},{\"chapterTitle\":\"未来展望\",\"chapterContents\":[{\"chapterTitle\":\"发展战略\"},{\"chapterTitle\":\"持续创新计划\"}]}]}";
        return JSON.parseObject(outLineStr, Outline.class);
    }
}
