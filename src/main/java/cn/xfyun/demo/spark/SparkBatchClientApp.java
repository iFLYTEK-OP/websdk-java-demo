
package cn.xfyun.demo.spark;

import cn.xfyun.api.SparkBatchClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.sparkmodel.batch.BatchInfo;
import cn.xfyun.model.sparkmodel.batch.BatchListResponse;
import cn.xfyun.model.sparkmodel.batch.DeleteResponse;
import cn.xfyun.model.sparkmodel.batch.FileInfo;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * （spark-batch）星火大模型批处理
 * 1、APPID、APISecret、APIKey、APIPassword信息获取：<a href="https://console.xfyun.cn/services/bm3.5batch">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/BatchAPI.html">...</a>
 */
public class SparkBatchClientApp {

    private static final Logger logger = LoggerFactory.getLogger(SparkBatchClientApp.class);
    private static final String APP_ID = PropertiesConfig.getAppId();
    private static final String API_PASSWORD = PropertiesConfig.getSparkBatchKey();
    private static String filePath;
    private static String resourcePath;

    static {
        try {
            filePath = "document/batch.jsonl";
            resourcePath = Objects.requireNonNull(SparkBatchClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) {
        try {
            SparkBatchClient client = new SparkBatchClient.Builder(APP_ID, API_PASSWORD).build();

            String upload = client.upload(new File(resourcePath + filePath));
            FileInfo uploadResp = JSON.parseObject(upload, FileInfo.class);
            logger.info("文件上传返回结果 ==> {}", JSON.toJSONString(uploadResp));

            String fileList = client.listFile(1, 20);
            logger.info("文件查询返回结果 ==> {}", fileList);

            String getFile = client.getFile(uploadResp.getId());
            FileInfo getFileResp = JSON.parseObject(getFile, FileInfo.class);
            logger.info("获取文件信息返回结果 ==> {}", JSON.toJSONString(getFileResp));

            String download = client.download(uploadResp.getId());
            logger.info("下载文件返回结果 ==> {}", download);

            String deleteFile = client.deleteFile(uploadResp.getId());
            DeleteResponse deleteResp = JSON.parseObject(deleteFile, DeleteResponse.class);
            logger.info("删除文件返回结果 ==> {}", JSON.toJSONString(deleteResp));

            String upload1 = client.upload(new File(resourcePath + filePath));
            FileInfo uploadResp1 = JSON.parseObject(upload1, FileInfo.class);
            logger.info("文件上传返回结果 ==> {}", JSON.toJSONString(uploadResp1));

            String create = client.create(uploadResp1.getId(), null);
            BatchInfo createResp = JSON.parseObject(create, BatchInfo.class);
            logger.info("创建任务返回结果 ==> {}", JSON.toJSONString(createResp));

            String getBatch = client.getBatch(createResp.getId());
            BatchInfo getBatchResp = JSON.parseObject(getBatch, BatchInfo.class);
            logger.info("获取任务信息返回结果 ==> {}", JSON.toJSONString(getBatchResp));

            String cancel = client.cancel(createResp.getId());
            BatchInfo cancelResp = JSON.parseObject(cancel, BatchInfo.class);
            logger.info("取消任务返回结果 ==> {}", JSON.toJSONString(cancelResp));

            String listBatch = client.listBatch(10, null);
            BatchListResponse listBatchResp = JSON.parseObject(listBatch, BatchListResponse.class);
            logger.info("查询Batch列表返回结果 ==> {}", JSON.toJSONString(listBatchResp));
        } catch (Exception e) {
            logger.error("请求失败", e);
        }
    }
}
