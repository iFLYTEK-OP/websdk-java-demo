package cn.xfyun.demo;

import cn.xfyun.config.PropertiesConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.xfyun.api.RtasrClient;
import cn.xfyun.model.response.rtasr.RtasrResponse;
import cn.xfyun.service.rta.AbstractRtasrWebSocketListener;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 *  ( Real-time ASR ) 实时语音转写
 * 1、APPID、APIKey信息获取：https://console.xfyun.cn/services/rta
 * 2、文档地址：https://www.xfyun.cn/doc/asr/rtasr/API.html
 */
public class RtasrClientApp {

	private static final String APP_ID = PropertiesConfig.getAppId();
	private static final String API_KEY = PropertiesConfig.getRtaAPIKey();

	private static String filePath = "audio/rtasr.pcm";
	private static String resourcePath;

	static {
		try {
			resourcePath = RtasrClientApp.class.getResource("/").toURI().getPath();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	/**
	 *    总共三种发送方式，  方式 1 2 每次调用send将新创建一个webSocket连接，并且进行了分段发送
	 *    和发送结束标识
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// 方式一
		sendByInputStream();

		// 方式二
		 //sendByByte();

		// 方式三
		 //send();
	}


	// 1. 使用文件流发送
	public static void sendByInputStream() throws SignatureException, InterruptedException, FileNotFoundException {
		RtasrClient client = new RtasrClient.Builder()
				.signature(APP_ID, API_KEY).build();
		File file = new File(resourcePath + filePath);
		FileInputStream inputStream = new FileInputStream(file);
		CountDownLatch latch = new CountDownLatch(1);
		client.send(inputStream, new AbstractRtasrWebSocketListener() {
			@Override
			public void onSuccess(WebSocket webSocket, String text) {
				RtasrResponse response = JSONObject.parseObject(text, RtasrResponse.class);
				System.out.println(getContent(response.getData()));
			}

			@Override
			public void onFail(WebSocket webSocket, Throwable t, @Nullable Response response) {
				latch.countDown();
				System.exit(0);
			}

			@Override
			public void onBusinessFail(WebSocket webSocket, String text) {
				System.out.println(text);
				latch.countDown();
				System.exit(0);
			}

			@Override
			public void onClosed() {
				latch.countDown();
				System.exit(0);
			}
		});

		latch.await();

	}

	// 2. 使用字节数组发送
	public static void sendByByte() throws SignatureException, InterruptedException, IOException {
		RtasrClient client = new RtasrClient.Builder()
				.signature(APP_ID, API_KEY).build();

		File file = new File(resourcePath + filePath);
		FileInputStream inputStream = new FileInputStream(file);
		byte[] buffer = new byte[1024000];
		inputStream.read(buffer);
		CountDownLatch latch = new CountDownLatch(1);
		client.send(buffer, null, new AbstractRtasrWebSocketListener() {
			@Override
			public void onSuccess(WebSocket webSocket, String text) {
				RtasrResponse response = JSONObject.parseObject(text, RtasrResponse.class);
				System.out.println(getContent(response.getData()));
			}

			@Override
			public void onFail(WebSocket webSocket, Throwable t, @Nullable Response response) {
				latch.countDown();
				System.exit(0);
			}

			@Override
			public void onBusinessFail(WebSocket webSocket, String text) {
				System.out.println(text);
				latch.countDown();
				System.exit(0);
			}

			@Override
			public void onClosed() {
				latch.countDown();
				System.exit(0);
			}
		});

		latch.await();
	}

	//TODO: 方式3 使用仅创建一个webSocket连接，需要用户自己处理分段 和 发送结束标识。否则服务端返回的结果不完善
	public static void send() throws InterruptedException {
		RtasrClient rtasrClient = new RtasrClient.Builder()
				.signature(APP_ID, API_KEY).build();
		CountDownLatch latch = new CountDownLatch(1);
		WebSocket webSocket = rtasrClient.newWebSocket(new AbstractRtasrWebSocketListener() {
			@Override
			public void onSuccess(WebSocket webSocket, String text) {
				RtasrResponse response = JSONObject.parseObject(text, RtasrResponse.class);
				System.out.println(getContent(response.getData()));
			}

			@Override
			public void onFail(WebSocket webSocket, Throwable t, @Nullable Response response) {
				latch.countDown();
				System.exit(0);
			}

			@Override
			public void onBusinessFail(WebSocket webSocket, String text) {
				System.out.println(text);
				latch.countDown();
				System.exit(0);
			}

			@Override
			public void onClosed() {
				latch.countDown();
				System.exit(0);
			}
		});

		try {
			byte[] bytes = new byte[1280];
			File file = new File(resourcePath + filePath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			int len = -1;
			long lastTs = 0;
			while ((len = raf.read(bytes)) != -1) {
				if (len < 1280) {
					bytes = Arrays.copyOfRange(bytes, 0, len);
					webSocket.send(ByteString.of(bytes));
					break;
				}

				long curTs = System.currentTimeMillis();
				if (lastTs == 0) {
					lastTs = System.currentTimeMillis();
				} else {
					long s = curTs - lastTs;
					if (s < 40) {
						System.out.println("error time interval: " + s + " ms");
					}
				}
				webSocket.send(ByteString.of(bytes));
				// 每隔40毫秒发送一次数据
				Thread.sleep(40);
			}
			// 发送结束标识
			rtasrClient.sendEnd();
		} catch (Exception e) {
			e.printStackTrace();
		}
		latch.await();
	}


	// 把转写结果解析为句子
	public static String getContent(String message) {
		StringBuffer resultBuilder = new StringBuffer();
		try {
			JSONObject messageObj = JSON.parseObject(message);
			JSONObject cn = messageObj.getJSONObject("cn");
			JSONObject st = cn.getJSONObject("st");
			JSONArray rtArr = st.getJSONArray("rt");
			for (int i = 0; i < rtArr.size(); i++) {
				JSONObject rtArrObj = rtArr.getJSONObject(i);
				JSONArray wsArr = rtArrObj.getJSONArray("ws");
				for (int j = 0; j < wsArr.size(); j++) {
					JSONObject wsArrObj = wsArr.getJSONObject(j);
					JSONArray cwArr = wsArrObj.getJSONArray("cw");
					for (int k = 0; k < cwArr.size(); k++) {
						JSONObject cwArrObj = cwArr.getJSONObject(k);
						String wStr = cwArrObj.getString("w");
						resultBuilder.append(wStr);
					}
				}
			}
		} catch (Exception e) {
			return message;
		}

		return resultBuilder.toString();
	}
}
