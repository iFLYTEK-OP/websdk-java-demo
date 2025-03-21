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
import java.security.SignatureException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * ( Long Form ASR ) 语音转写
 * 1、APPID、SecretKey信息获取：<a href="https://console.xfyun.cn/services/lfasr">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/asr/ifasr_new/API.html">...</a>
 *
 * @author kaili23
 */
public class LfasrClientApp {

    private static final Logger logger = LoggerFactory.getLogger(LfasrClientApp.class);
    
    private static final Gson GSON = new Gson();

    /**
     * 服务鉴权参数
     */
    private static final String APP_ID = PropertiesConfig.getAppId();
    private static final String SECRET_KEY = PropertiesConfig.getLfasrSecretKey();

    /**
     * 音频文件路径
     * - 本地文件（默认、调用uploadFile方法）
     * - 远程Url（配合参数audioMode = urlLink使用、调用uploadUrl方法）
     */
    private static String audioFilePath;

    private static final String AUDIO_URL = "https://openres.xfyun.cn/xfyundoc/2025-03-19/e7b6a79d-124f-44e0-b8aa-0e799410f453/1742353716311/lfasr.wav";

    /**
     * 任务类型
     * - transfer：转写
     * - translate：翻译（配合参数transLanguage和transMode使用）
     * - predict：质检（配合控制台质检词库使用）
     * - transfer,predict：转写 + 质检
     */
    private static final String TASK_TYPE = "transfer";

    static {
        try {
            audioFilePath = Objects.requireNonNull(LfasrClientApp.class.getResource("/")).toURI().getPath() + "/audio/lfasr_max.wav";
        } catch (Exception e) {
            logger.error("资源路径获取失败", e);
        }
    }

    public static void main(String[] args) throws SignatureException, InterruptedException {
        // 1、创建客户端实例
        LfasrClient lfasrClient = new LfasrClient.Builder(APP_ID, SECRET_KEY)
                 .roleType((short) 1)
                // .transLanguage("en")
//                .audioMode("urlLink")
                .build();

        // 2、上传音频文件（本地/Url）
        logger.info("音频上传中...");
         LfasrResponse uploadResponse = lfasrClient.uploadFile(audioFilePath);
//        LfasrResponse uploadResponse = lfasrClient.uploadUrl(AUDIO_URL);
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
            LfasrResponse resultResponse = lfasrClient.getResult(orderId, TASK_TYPE);
            if (!StringUtils.equals(resultResponse.getCode(), "000000")) {
                logger.error("转写任务失败，错误码：{}，错误信息：{}", resultResponse.getCode(), resultResponse.getDescInfo());
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
        switch (TASK_TYPE) {
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
                logger.warn("未知的任务类型：{}", TASK_TYPE);
                break;
        }
    }

    /**
     * 解析转写结果
     */
    private static void parseOrderResult(String orderResultStr) {
        try {
            LfasrOrderResult orderResult = GSON.fromJson(orderResultStr, LfasrOrderResult.class);
            logger.info("转写结果：\n{}", getLatticeText(orderResult.getLattice()));
        } catch (Exception e) {
            logger.error("转写结果解析失败", e);
        }
    }

    /**
     * 解析翻译结果
     */
    private static void parseTransResult(String transResultStr) {
        try {
            Type transResultListType = new TypeToken<List<LfasrTransResult>>() {
            }.getType();
            List<LfasrTransResult> transResultList = GSON.fromJson(transResultStr, transResultListType);
            logger.info("翻译结果：{}", getTranslationText(transResultList));
        } catch (Exception e) {
            logger.error("翻译结果解析失败", e);
        }
    }

    /**
     * 解析质检结果
     */
    private static void parsePredictResult(String predictResultStr) {
        try {
            LfasrPredictResult predictResult = GSON.fromJson(predictResultStr, LfasrPredictResult.class);
            logger.info("质检结果：{}", predictResult);
        } catch (Exception e) {
            logger.error("质检结果解析失败", e);
        }
    }

    /**
     * 从转写结果的lattice数组中提取文本
     */
    private static String getLatticeText(List<LfasrOrderResult.Lattice> latticeList) {
        StringBuilder resultText = new StringBuilder();
        for (LfasrOrderResult.Lattice lattice : latticeList) {
            LfasrOrderResult.Json1Best json1Best = lattice.getJson1Best();
            if (json1Best == null || json1Best.getSt() == null || json1Best.getSt().getRt() == null) {
                continue;
            }
            String rl = json1Best.getSt().getRl();
            StringBuilder rlText = getRlText(json1Best);
            resultText.append("角色-").append(rl).append("：").append(rlText).append("\n");
        }
        return resultText.toString();
    }

    /**
     * 从Json1Best中提取识别结果文本并拼接
     */
    private static StringBuilder getRlText(LfasrOrderResult.Json1Best json1Best) {
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
        return rlText;
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
