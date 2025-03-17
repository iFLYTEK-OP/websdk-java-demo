package cn.xfyun.demo.nlp;

import cn.xfyun.api.TextCheckClient;
import cn.xfyun.config.PropertiesConfig;

/**
 *（text-check）文本纠错
 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/text_check
 2、文档地址：https://www.xfyun.cn/doc/nlp/textCorrection/API.html
 */
public class TextCheckClientApp {

	private static final String appId = PropertiesConfig.getAppId();
	private static final String apiKey = PropertiesConfig.getApiKey();
	private static final String apiSecret = PropertiesConfig.getApiSecret();


	public static void main(String[] args) throws Exception{
		TextCheckClient client = new TextCheckClient
				.Builder(appId, apiKey, apiSecret)
				.build();
		System.out.println("请求地址：" + client.getHostUrl());
		System.out.println(client.send("画蛇天足"));
	}

}
