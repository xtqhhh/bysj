package com.gov.security.service;

import com.gov.security.common.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务
 * 生成随机字符验证码图片，存入 Redis，TTL 5 分钟
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final long TTL_MINUTES = 5L;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private final StringRedisTemplate redisTemplate;

    /**
     * 生成验证码，返回 uuid 和 Base64 图片
     */
    public CaptchaResult generate() {
        String code = randomCode();
        String uuid = UUID.randomUUID().toString().replace("-", "");

        // 存入 Redis，TTL 5 分钟
        redisTemplate.opsForValue().set(
                RedisKeyConstants.captchaKey(uuid), code, TTL_MINUTES, TimeUnit.MINUTES);

        String base64Image = renderBase64(code);
        log.debug("验证码已生成，uuid={}", uuid);
        return new CaptchaResult(uuid, base64Image);
    }

    /**
     * 校验验证码（校验后立即删除，防止重放）
     */
    public boolean verify(String uuid, String inputCode) {
        if (uuid == null || inputCode == null) return false;
        String key = RedisKeyConstants.captchaKey(uuid);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) return false;
        redisTemplate.delete(key);
        return stored.equalsIgnoreCase(inputCode.trim());
    }

    // ---- private helpers ----

    private String randomCode() {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(rnd.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String renderBase64(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            // 背景
            g.setColor(new Color(240, 240, 240));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            Random rnd = new Random();
            g.setStroke(new BasicStroke(1.0f));
            for (int i = 0; i < 5; i++) {
                g.setColor(new Color(rnd.nextInt(180), rnd.nextInt(180), rnd.nextInt(180)));
                g.drawLine(rnd.nextInt(WIDTH), rnd.nextInt(HEIGHT),
                        rnd.nextInt(WIDTH), rnd.nextInt(HEIGHT));
            }

            // 字符
            g.setFont(new Font("Arial", Font.BOLD, 26));
            int x = 8;
            for (char c : code.toCharArray()) {
                g.setColor(new Color(rnd.nextInt(100), rnd.nextInt(100), rnd.nextInt(150)));
                g.drawString(String.valueOf(c), x, 30 + rnd.nextInt(6) - 3);
                x += 26;
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }

    public record CaptchaResult(String uuid, String image) {}
}
