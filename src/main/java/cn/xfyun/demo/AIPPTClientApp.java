
package cn.xfyun.demo;

import cn.hutool.json.JSONUtil;
import cn.xfyun.api.AIPPTClient;
import cn.xfyun.model.CreateParam;
import cn.xfyun.model.CreateResult;
import cn.xfyun.model.ThemeResponse;

import java.util.UUID;

/**
 * 志文PPT的接口demo
 *
 * @author junzhang27
 */
public class AIPPTClientApp {
    private static final String APP_ID = "替换成你的appid";
    private static final String API_SECRET = "替换成你的apiSecret";

    public static void main(String[] args) {
        try {
            AIPPTClient client = new AIPPTClient.Builder(APP_ID, API_SECRET).readTimeout(10000).build();

            String themeList = client.themeList();
            ThemeResponse themeResponse = JSONUtil.toBean(themeList, ThemeResponse.class);
            System.out.println("ppt主题列表：" + JSONUtil.toJsonStr(themeResponse));

            CreateParam createParam = new CreateParam.Builder()
                    .query("介绍一下中国的春节习俗")
                    .businessId(UUID.randomUUID().toString())
                    .build();
            String response = client.create(createParam);
            CreateResult createResult = JSONUtil.toBean(response, CreateResult.class);
            System.out.println("ppt生成首图结果：" + response);

            String response1 = client.progress(createResult.getData().getSid());
            System.out.println("ppt当前的进度信息：" + response1);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
