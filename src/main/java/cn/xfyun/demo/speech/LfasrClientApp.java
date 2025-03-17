package cn.xfyun.demo.speech;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cn.xfyun.api.LfasrClient;
import cn.xfyun.config.LfasrFailTypeEnum;
import cn.xfyun.config.LfasrOrderStatusEnum;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.response.lfasr.LfasrOrderResult;
import cn.xfyun.model.response.lfasr.LfasrPredictResult;
import cn.xfyun.model.response.lfasr.LfasrResponse;
import cn.xfyun.model.response.lfasr.LfasrTransResult;

import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *  ( Long Form ASR ) 语音转写
 1、APPID、SecretKey信息获取：https://console.xfyun.cn/services/lfasr
 2、文档地址：https://www.xfyun.cn/doc/asr/ifasr_new/API.html
 */
public class LfasrClientApp {

    private static final Logger logger = LoggerFactory.getLogger(LfasrClientApp.class);

    /**
     * 服务鉴权参数
     */
    private static final String APP_ID = PropertiesConfig.getAppId();
    private static final String SECRET_KEY = PropertiesConfig.getLfasrSecretKey();

    /**
     * 音频文件路径
     */
    private static String audioFilePath;
    private static String audioUrl = "https://openres.xfyun.cn/xfyundoc/2025-03-19/b10ece3a-c883-4215-9c44-e2c213299716/1742355893204/lfasr-%E5%93%88%E5%93%88.wav";
    private static String taskType = "transfer";

    /**
     * 静态变量初始化
     */
    static {
        try {
            audioFilePath = LfasrClientApp.class.getResource("/").toURI().getPath() + "/audio/lfasr_max.wav";
        } catch (Exception e) {
            logger.error("资源路径获取失败", e);
        }
    }

    public static void main(String[] args) throws SignatureException, InterruptedException {
        // 1、创建客户端实例
        LfasrClient lfasrClient = new LfasrClient
                .Builder(APP_ID, SECRET_KEY)
                // .roleType((short) 1)
                // .transLanguage("en")
                // .audioMode("urlLink")
                .build();

        // 2、上传音频文件（本地/Url）
        logger.info("音频上传中...");
        LfasrResponse uploadResponse = lfasrClient.uploadFile(audioFilePath);
        // LfasrResponse uploadResponse = lfasrClient.uploadUrl(audioUrl);
        if (uploadResponse == null) {
            logger.error("上传失败，响应为空");
            return;
        }
        if (!StringUtils.equals(uploadResponse.getCode(), "000000")) {
            logger.error("上传失败，错误码：{}，错误信息：{}", uploadResponse.getCode(), uploadResponse.getDescInfo());
            return;
        }
        String orderId = uploadResponse.getContent().getOrderId();
        logger.info("转写任务orderId：{}", orderId);

        // 3、查询转写结果
        int status = LfasrOrderStatusEnum.CREATED.getKey();
         // 循环直到订单完成或失败
        while (status != LfasrOrderStatusEnum.COMPLETED.getKey() && status != LfasrOrderStatusEnum.FAILED.getKey()) { 
            LfasrResponse resultResponse = lfasrClient.getResult(orderId, taskType);
            if (!StringUtils.equals(resultResponse.getCode(), "000000")) {
                logger.error("查询失败，错误码：{}，错误信息：{}", resultResponse.getCode(), resultResponse.getDescInfo());
                return;
            }
            
            // 获取订单状态信息
            if (resultResponse.getContent() != null && resultResponse.getContent().getOrderInfo() != null) {
                status = resultResponse.getContent().getOrderInfo().getStatus();
                int failType = resultResponse.getContent().getOrderInfo().getFailType();
                
                // 根据状态输出日志
                LfasrOrderStatusEnum statusEnum = LfasrOrderStatusEnum.getEnum(status);
                if (statusEnum != null) {
                    logger.info("订单状态：{}", statusEnum.getValue());
                    
                    // 如果订单失败，输出失败原因
                    if (statusEnum == LfasrOrderStatusEnum.FAILED) {
                        LfasrFailTypeEnum failTypeEnum = LfasrFailTypeEnum.getEnum(failType);
                        logger.error("订单处理失败，失败原因：{}", failTypeEnum.getValue());
                        return;
                    }
                    // 如果订单已完成，输出结果
                    if (statusEnum == LfasrOrderStatusEnum.COMPLETED) {
                        printResult(resultResponse);
                        return;
                    }
                } else {
                    logger.warn("未知的订单状态：{}", status);
                }
            } else {
                logger.warn("返回结果中缺少订单信息");
            }
            
            TimeUnit.SECONDS.sleep(20);
        }
    }

    private static void printResult(LfasrResponse resultResponse) {
        switch (taskType) {
            case "transfer":
                parseOrderResult(resultResponse.getContent().getOrderResult());
                break;
            case "translate":
                parseTransResult(resultResponse.getContent().getTransResult());
                break;
            case "predict":
                parsePredictResult(resultResponse.getContent().getPredictResult());
                break;
            case "transfer,predict":
                parseOrderResult(resultResponse.getContent().getOrderResult());
                parsePredictResult(resultResponse.getContent().getPredictResult());
                break;
            default:
                logger.warn("未知的任务类型：{}", taskType);
                break;
        }
    }

    /**
     * 解析转写结果
     */
    private static void parseOrderResult(String orderResultStr) {
        // 使用Gson将JSON字符串转换为LfasrOrderResult对象
        Gson gson = new Gson();
        try {
            LfasrOrderResult orderResult = gson.fromJson(orderResultStr, LfasrOrderResult.class);
            logger.info("转写结果：\n{}", getLatticeText(orderResult.getLattice()));
        } catch (Exception e) {
            logger.error("转写结果解析失败", e);
        }
    }

    /**
     * 解析翻译结果
     */
    private static void parseTransResult(String transResultStr) {
        Gson gson = new Gson();
        try {
            Type transResultListType = new TypeToken<List<LfasrTransResult>>(){}.getType();
            List<LfasrTransResult> transResultList = gson.fromJson(transResultStr, transResultListType);
            logger.info("翻译结果：{}", getTranslationText(transResultList));
           
        } catch (Exception e) {
            logger.error("翻译结果解析失败", e);
        }
        
    }

    /**
     * 解析质检结果
     */
    private static void parsePredictResult(String predictResultStr) {
        // 使用Gson将JSON字符串转换为LfasrOrderResult对象
        Gson gson = new Gson();
        try {
            LfasrPredictResult predictResult = gson.fromJson(predictResultStr, LfasrPredictResult.class);
            logger.info("质检结果：{}", predictResult);
            
            
        } catch (Exception e) {
            logger.error("质检结果解析失败", e);
        }
    }

    /**
     * 从转写结果的lattice数组中提取文本并追加到结果中
     */
    private static String getLatticeText(List<LfasrOrderResult.Lattice> latticeList) {
        StringBuilder resultText = new StringBuilder();
        for (LfasrOrderResult.Lattice lattice : latticeList) {
            LfasrOrderResult.Json1Best json1Best = lattice.getJson1Best();
            if (json1Best == null || json1Best.getSt() == null || json1Best.getSt().getRt() == null) {
                continue;
            }
            String rl = json1Best.getSt().getRl();
            StringBuilder rlText = new StringBuilder();
            for (LfasrOrderResult.RecognitionResult rt : json1Best.getSt().getRt()) {
                if (rt.getWs() == null) {
                    continue;
                }
                for (LfasrOrderResult.WordResult ws : rt.getWs()) {
                    if (ws.getCw() != null && !ws.getCw().isEmpty()) {
                        // 获取每个词的识别结果
                        String word = ws.getCw().get(0).getW();
                        if (word != null && !word.isEmpty()) {
                            rlText.append(word);
                        }
                    }
                }
            }
            resultText.append("角色-" + rl + "：" + rlText).append("\n");
        }
        return resultText.toString();
    }

     /**
     * 从翻译结果列表中提取并拼接翻译文本
     */
    private static String getTranslationText(List<LfasrTransResult> transResults) {
        StringBuilder translationText = new StringBuilder();
        for (LfasrTransResult result : transResults) {
            if (!cn.xfyun.util.StringUtils.isNullOrEmpty(result.getDst())) {
                translationText.append(result.getDst());
            }
        }
        return translationText.toString();
    }

}
