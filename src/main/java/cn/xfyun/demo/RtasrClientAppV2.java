package cn.xfyun.demo;

import cn.xfyun.config.PropertiesConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.xfyun.api.RtasrClient;
import cn.xfyun.model.response.rtasr.RtasrResponse;
import cn.xfyun.service.rta.AbstractRtasrWebSocketListener;
import cn.xfyun.util.MicrophoneRecorderUtil;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;

import javax.annotation.Nullable;
import javax.sound.sampled.LineUnavailableException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 *  ( Real-time ASR ) 实时语音转写
 * 1、APPID、APIKey信息获取：https://console.xfyun.cn/services/rta
 * 2、文档地址：https://www.xfyun.cn/doc/asr/rtasr/API.html
 */
public class RtasrClientAppV2 {

	/**
	 * 服务鉴权参数
	 */
	private static final String APP_ID = PropertiesConfig.getAppId();
	private static final String API_KEY = PropertiesConfig.getRtaAPIKey();

	/**
	 * 音频文件路径
	 */
	private static String filePath = "audio/rtasr.pcm";
	// private static String filePath = "audio/silence_20s.pcm";
	private static String resourcePath;

	/**
	 * 语音转写客户端
	 */
	private static RtasrClient rtasrClient;

	/**
	 * 线程同步
	 */
	private static CountDownLatch latch;

	/**
	 * 静态变量初始化
	 */
	static {

		rtasrClient = new RtasrClient.Builder()
				.signature(APP_ID, API_KEY).build();

		try {
			resourcePath = RtasrClientAppV2.class.getResource("/").toURI().getPath();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	/**
	 * WebSocket监听器实现，负责处理实时语音转写结果
	 * 功能说明：
	 * 1、成功回调：解析转写结果并输出；
	 * 2、连接异常、业务异常和服务端关闭的回调：退出主线程，中止JVM。
	 */
	private static final AbstractRtasrWebSocketListener rtasrWebSocketListener = new AbstractRtasrWebSocketListener() {
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
		public void onClosing(WebSocket webSocket, int code, String reason) {
			System.out.println("服务端正在关闭连接");
			latch.countDown();
			// System.exit(0);
		}

		@Override
		public void onClosed() {
			latch.countDown();
			System.exit(0);
		}
	};

	public static void main(String[] args) throws Exception {

		// 方式一：处理输入流形式的音频数据
		processAudioFromFileInputStream();

		// 方式二：处理字节数组形式的音频数据
		// processAudioFromFileByteArray();

		// 方式三：原生实现，用户自定义处理
		// processAudioRaw();

		// 方式四：处理麦克风输入的音频数据
		// processAudioFromMicrophone();
	}

	/**
	 * 处理输入流形式的音频数据
	 */
	public static void processAudioFromFileInputStream() throws SignatureException, InterruptedException, FileNotFoundException {
		
		File file = new File(resourcePath + filePath);
		FileInputStream inputStream = new FileInputStream(file);
		latch = new CountDownLatch(1);
		rtasrClient.send(inputStream, rtasrWebSocketListener);

		latch.await();
	}

	/**
	 * 处理字节数组形式的音频数据
	 */
	public static void processAudioFromFileByteArray() throws SignatureException, InterruptedException, IOException {

		File file = new File(resourcePath + filePath);
		FileInputStream inputStream = new FileInputStream(file);
		byte[] buffer = new byte[1024000];
		inputStream.read(buffer);
		latch = new CountDownLatch(1);
		rtasrClient.send(buffer, inputStream, rtasrWebSocketListener);

		latch.await();
	}

	/**
	 * 原生实现，用户自定义处理
	 * 仅创建了一个webSocket连接，需要用户自己处理分段 和 发送结束标识。否则服务端返回的结果不完善。
	 */
	public static void processAudioRaw() throws InterruptedException {
		RtasrClient rtasrClient = new RtasrClient.Builder()
				.signature(APP_ID, API_KEY).build();
		latch = new CountDownLatch(1);
		WebSocket webSocket = rtasrClient.newWebSocket(rtasrWebSocketListener);

		try (RandomAccessFile raf = new RandomAccessFile(new File(resourcePath + filePath), "r")) {

			byte[] bytes = new byte[1280];
			int len;
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

	 /**
	 * 处理麦克风输入的音频数据
	 * @throws IOException 
	 */
	public static void processAudioFromMicrophone() throws LineUnavailableException, SignatureException, InterruptedException, IOException {
        
		Scanner scanner = new Scanner(System.in);
        System.out.println("按回车开始实时转写...");
        scanner.nextLine();
		// 创建带缓冲的音频管道流（管道缓存过大/过小会导致数据发送过快/过慢进而导致服务器引擎出错提前结束WebSocket连接）
		PipedInputStream audioInputStream = new PipedInputStream(1280); 
		PipedOutputStream audioOutputStream = new PipedOutputStream(audioInputStream);
		
		// 配置录音工具
		MicrophoneRecorderUtil recorder = new MicrophoneRecorderUtil();
		
		// 开始录音并初始化状态
		recorder.startRecording(audioOutputStream);

		// 初始化倒计时锁
		latch = new CountDownLatch(1);

		// 启动流式转写
		rtasrClient.send(audioInputStream, rtasrWebSocketListener);

		System.out.println("正在聆听，按回车结束转写...");
		scanner.nextLine();
		
		// 停止录音并关闭资源
		recorder.stopRecording();
		rtasrClient.sendEnd();
		latch.await();
		audioOutputStream.close();
		audioInputStream.close();
		scanner.close();
    }

	/**
	 * 把转写结果解析为句子
	 */
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
