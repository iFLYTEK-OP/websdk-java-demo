package cn.xfyun.demo;

import cn.xfyun.api.SaClient;
import cn.xfyun.config.PropertiesConfig;
/**
 *
 * @author mqgao
 * @version 1.0
 * @date 2021/6/22 11:12
 */
public class SaClientApp {

	private static final String appId = PropertiesConfig.getAppId();
	private static final String apiKey = PropertiesConfig.getApiKey();


	public static void main(String[] args) throws Exception{
		//情感分析
		SaClient client = new SaClient.Builder(appId, apiKey)
				.build();
		System.out.println("请求地址：" + client.getHostUrl());
		System.out.println(client.send("你好啊"));
	}
}
