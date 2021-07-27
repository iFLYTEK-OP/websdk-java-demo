import cn.xfyun.api.ImageRecClient;
import cn.xfyun.config.ImageRecEnum;
import cn.xfyun.config.PropertiesConfig;
import sun.misc.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Base64;


public class ImageRecClientApp {

    private static final String appId= PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
  
    private static String filePath = "xxxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = ImageRecClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            ImageRecClient client = new ImageRecClient
                 //  ImageRecEnum.SCENE     场景识别
                 //  ImageRecEnum.CURRENCY  物体识别
                .Builder(appId, apiKey, ImageRecEnum.SCENE)
                .build();
            InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
            byte[] imageByteArray = IOUtils.readFully(inputStream, -1, true);
            String imageBase64 = Base64.getEncoder().encodeToString(imageByteArray);
            System.out.println(client.send( "测试", imageByteArray));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("错误码查询链接：https://www.xfyun.cn/document/error-code");
        }
    }
}
