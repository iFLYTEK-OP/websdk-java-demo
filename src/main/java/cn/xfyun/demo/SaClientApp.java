package cn.xfyun.demo;

import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.api.SaClinet;
/**
 * @author mqgao
 * @version 1.0
 * @date 2021/6/22 11:12
 */
public class SaClientApp {

	private static final String appId = PropertiesConfig.getAppId();
	private static final String apiKey = PropertiesConfig.getApiKey();


	public static void main(String[] args) throws Exception{
		SaClinet saClinet = new SaClinet.Builder(appId, apiKey)
				.build();
		System.out.println(saClinet.send("你好啊"));
	}
}
