package io.ants.modules.utils.service.tencent;

import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

public class CreateVerifyCodeImg {



        //    <dependency>
        //      <groupId>com.github.penggle</groupId>
        //      <artifactId>kaptcha</artifactId>
        //      <version>2.3.2</version>
        //  </dependency>
    public static byte[] createImg(String verifyCode) throws IOException {
        Producer producer = createProducer();
        // 生成随机字符串
        //String verifyCode = producer.createText();
        //System.out.println(verifyCode);
        // 生成图片
        BufferedImage bufferedImage = producer.createImage(verifyCode);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpg", stream);
        //System.out.println(Base64.getEncoder().encodeToString(stream.toByteArray()));
        return stream.toByteArray();
    }

    public static String create_random_text(){
        Producer producer = createProducer();
        return producer.createText();
    }

    private static Producer createProducer()  {
        Properties properties = new Properties();
        properties.setProperty(Constants.KAPTCHA_BORDER, "no");
        properties.setProperty(Constants.KAPTCHA_BORDER_COLOR, "105,179,90");
        properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_FONT_COLOR, "black");
        properties.setProperty(Constants.KAPTCHA_IMAGE_WIDTH, "125");
        properties.setProperty(Constants.KAPTCHA_IMAGE_HEIGHT, "45");
        properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_CHAR_LENGTH, "4");
        properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_FONT_SIZE, "35");
        properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_FONT_NAMES, "宋体,楷体,微软雅黑");
        properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_CHAR_SPACE, "5");
        Config config = new Config(properties);
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
