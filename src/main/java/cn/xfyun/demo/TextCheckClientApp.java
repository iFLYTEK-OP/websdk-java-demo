package cn.xfyun.demo;

import cn.xfyun.api.TextCheckClient;
import cn.xfyun.config.PropertiesConfig;

/**
 *    文本纠错
 *
 * @author mqgao
 * @version 1.0
 * @date 2021/6/22 11:07
 */
public class TextCheckClientApp {

	private static final String appId = PropertiesConfig.getAppId();
	private static final String apiKey = PropertiesConfig.getApiKey();
	private static final String apiSecret = PropertiesConfig.getApiSecret();


	public static void main(String[] args) throws Exception{
		TextCheckClient checkClient = new TextCheckClient
				.Builder(appId, apiSecret, apiKey)
				.build();
		String result = checkClient.send("画蛇天足");
		System.out.println("返回结果: " + result);
	}

}
