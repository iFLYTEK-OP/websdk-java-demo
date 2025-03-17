package cn.xfyun.demo.speech;

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

import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 *  ( Real-time ASR ) 实时语音转写
 * 1、APPID、APIKey信息获取：https://console.xfyun.cn/services/rta
 * 2、文档地址：https://www.xfyun.cn/doc/asr/rtasr/API.html
 */
public class RtasrClientApp {

	private static final Logger logger = LoggerFactory.getLogger(RtasrClientApp.class);

	/**
	 * 服务鉴权参数
	 */
	private static final String APP_ID = PropertiesConfig.getAppId();
	private static final String API_KEY = PropertiesConfig.getRtaAPIKey();

	/**
	 * 音频文件路径
	 */
	private static String filePath = "audio/rtasr.pcm";
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
     * 记录完整结果
     */
    private static StringBuffer finalResult; 

	/**
	 * 静态变量初始化
	 */
	static {
		rtasrClient = new RtasrClient.Builder()
				.signature(APP_ID, API_KEY).build();

		try {
			resourcePath = RtasrClientApp.class.getResource("/").toURI().getPath();
		} catch (Exception e) {
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
			// finalResult为服务端已转写的完整语句，后面拼接服务端本次返回的当前语句的转写中间结果。
			String tempResult = handleAndReturnContent(response.getData());
			logger.info("实时转写结果：{}", finalResult + tempResult);
			// logger.info("实时转写原生结果：{}", response.getData());
		}

		@Override
		public void onFail(WebSocket webSocket, Throwable t, @Nullable Response response) {
			latch.countDown();
			System.exit(0);
		}

		@Override
		public void onBusinessFail(WebSocket webSocket, String text) {
			logger.error("业务异常，返回信息为：{}", text);
			latch.countDown();
			System.exit(0);
		}

		@Override
		public void onClosing(WebSocket webSocket, int code, String reason) {
			latch.countDown();
			System.exit(0);
		}

		@Override
		public void onClosed() {
			latch.countDown();
			System.exit(0);
		}

	};

	public static void main(String[] args) throws Exception {
		// 方式一：处理输入流形式的音频数据
		// processAudioFromFileInputStream();

		// 方式二：处理字节数组形式的音频数据
		// processAudioFromFileByteArray();

		// 方式三：原生实现，用户自定义处理
		// processAudioRaw();

		// 方式四：处理麦克风输入的音频数据
		processAudioFromMicrophone();
	}

	/**
	 * 处理输入流形式的音频数据
	 */
	public static void processAudioFromFileInputStream() {
		String completeFilePath = resourcePath + filePath;
		FileInputStream inputStream = null;
		finalResult = new StringBuffer();

        try {
            File file = new File(completeFilePath);
            inputStream = new FileInputStream(file);
            latch = new CountDownLatch(1);
            rtasrClient.send(inputStream, rtasrWebSocketListener);
            
            latch.await();
        } catch (FileNotFoundException e) {
            logger.error("音频文件未找到，路径：{}", completeFilePath, e);
            throw new RuntimeException("文件加载失败，请检查路径：" + completeFilePath, e);
        } catch (SignatureException e) {
            logger.error("API签名验证失败", e);
            throw new RuntimeException("服务鉴权失败，请检查密钥配置", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("线程等待被中断", e);
            throw new RuntimeException("转写操作被意外终止", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("文件流关闭异常", e);
                }
            }
        }
	}

	/**
	 * 处理字节数组形式的音频数据
	 */
	public static void processAudioFromFileByteArray(){
		String completeFilePath = resourcePath + filePath;
		FileInputStream inputStream = null;
		finalResult = new StringBuffer();
		
        try {
            File file = new File(completeFilePath);
            inputStream = new FileInputStream(file);
            
            byte[] buffer = new byte[1024000];
            inputStream.read(buffer);

            latch = new CountDownLatch(1);
            rtasrClient.send(buffer, inputStream, rtasrWebSocketListener);
            latch.await();
        } catch (FileNotFoundException e) {
            logger.error("音频文件未找到，路径：{}", completeFilePath, e);
            throw new RuntimeException("文件加载失败，请检查路径", e);
        } catch (SignatureException e) {
            logger.error("API签名验证失败", e);
            throw new RuntimeException("服务鉴权失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("线程等待被中断", e);
            throw new RuntimeException("转写操作被终止", e);
        } catch (IOException e) {
            logger.error("IO操作异常", e);
            throw new RuntimeException("数据传输失败", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("文件流关闭异常", e);
                }
            }
        }
	}

	/**
	 * 原生实现，用户自定义处理
	 * 仅创建了一个webSocket连接，需要用户自己处理分段和发送结束标识。否则服务端返回的结果不完善。
	 */
	public static void processAudioRaw(){
		String completeFilePath = resourcePath + filePath;
		finalResult = new StringBuffer();
		latch = new CountDownLatch(1);
		
		try {
			WebSocket webSocket = rtasrClient.newWebSocket(rtasrWebSocketListener);

			try (RandomAccessFile raf = new RandomAccessFile(new File(completeFilePath), "r")) {
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
							logger.warn("error time interval: " + s + " ms");
						}
					}
					webSocket.send(ByteString.of(bytes));
					// 每隔40毫秒发送一次数据
					Thread.sleep(40);
				}
				// 发送结束标识
				rtasrClient.sendEnd();
			}
			latch.await();
		} catch (FileNotFoundException e) {
            logger.error("音频文件未找到：{}", completeFilePath, e);
            throw new RuntimeException("文件加载失败", e);
        } catch (IOException e) {
            logger.error("数据读写异常", e);
            throw new RuntimeException("网络通信失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("线程执行被中断", e);
            throw new RuntimeException("转写操作被终止", e);
        }
	}

	 /**
	 * 处理麦克风输入的音频数据
	 */
	public static void processAudioFromMicrophone() {
		Scanner scanner = null;
        MicrophoneRecorderUtil recorder = null;
        PipedInputStream audioInputStream = null;
        PipedOutputStream audioOutputStream = null;
		finalResult = new StringBuffer();

		// 处理录音初始化与交互
		try {
			scanner = new Scanner(System.in);
			logger.info("按回车开始实时转写...");
			scanner.nextLine();

			// 创建带缓冲的音频管道流（管道缓存过大/过小会导致数据发送过快/过慢进而导致服务器引擎出错提前结束WebSocket连接）
			audioInputStream = new PipedInputStream(1280); 
			audioOutputStream = new PipedOutputStream(audioInputStream);

			// 配置录音工具并启动录音
			recorder = new MicrophoneRecorderUtil();
			recorder.startRecording(audioOutputStream);

			// 初始化倒计时锁并启动流式读写
			latch = new CountDownLatch(1);
			rtasrClient.send(audioInputStream, rtasrWebSocketListener);

			logger.info("正在聆听，按回车结束转写...");
			scanner.nextLine();
		} catch (LineUnavailableException e) {
            logger.error("录音设备不可用，请检查麦克风权限", e);
            throw new RuntimeException("音频输入设备初始化失败", e);
        } catch (SignatureException e) {
            logger.error("API签名验证失败", e);
            throw new RuntimeException("服务鉴权失败", e);
        } catch (IOException e) {
            logger.error("网络通信异常", e);
            throw new RuntimeException("连接转写服务失败", e);
        } finally {
            // 停止录音并释放资源
			if (recorder != null) {
				recorder.stopRecording();
			}
			if (scanner != null) {
				scanner.close();
			}
			if (latch != null) {
				latch.countDown();
			}
        }

		// 处理转写结果等待
		try {
            // 发送结束标识并等待结果
            rtasrClient.sendEnd();
            if (latch != null) {
                latch.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("转写等待被中断", e);
            throw new RuntimeException("转写操作被终止", e);
        }
    }

	/**
	 * 解析转写流式响应，更新实时结果，返回此次的中间结果
	 */
	public static String handleAndReturnContent(String message) {
		StringBuffer tempResult = new StringBuffer();

		try {
			// 解析本次服务端返回的文本内容
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
						tempResult.append(wStr);
					}
				}
			}

			String type = st.getString("type");
			if (StringUtils.equals("1", type)) {
				// 此时服务端返回的是当前语句的实时转写内容，不保存到最终结果中，返回到调用处进行拼接展示。
				return tempResult.toString();
			} else if (StringUtils.equals("0", type)) {
				// 此时服务端返回的是当前语句的完整转写内容，保存到最终结果中，并返回空字符串。
				finalResult.append(tempResult);
				return "";
			} else {
				logger.warn("未知的转写响应类型：{}", type);
				return tempResult.toString();
			}
			
		} catch (Exception e) {
			return message;
		}
	}
}
