package com.company.common.security.service;

import com.company.common.security.autoconfigure.CareSecurityProperties;
import com.company.common.security.spi.CaptchaVerifier;
import org.springframework.data.redis.core.RedisTemplate;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Generates and verifies image CAPTCHAs.
 * Codes are stored in Redis with a configurable TTL.
 * Each CAPTCHA is single-use: consumed on verification.
 */
public class CaptchaService implements CaptchaVerifier {

    private static final String REDIS_KEY_PREFIX = "captcha:";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 每個字元對應的音訊頻率（Hz） */
    private static final Map<Character, Double> CHAR_FREQ_MAP = buildCharFreqMap();

    private final RedisTemplate<String, Object> redisTemplate;
    private final CareSecurityProperties.Captcha config;

    public CaptchaService(RedisTemplate<String, Object> redisTemplate,
                          CareSecurityProperties.Captcha config) {
        this.redisTemplate = redisTemplate;
        this.config = config;
    }

    public record CaptchaResult(String captchaId, String imageBase64) {}

    /**
     * Generate a new CAPTCHA: random code from configured charset + distorted image.
     */
    public CaptchaResult generateCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        String code = generateCode(config.getLength());

        // Store in Redis
        String key = REDIS_KEY_PREFIX + captchaId;
        redisTemplate.opsForValue().set(key, code, Duration.ofSeconds(config.getExpireSeconds()));

        // Generate image
        String imageBase64 = renderImage(code);

        return new CaptchaResult(captchaId, imageBase64);
    }

    /**
     * Verify the user's answer (case-insensitive). Single-use: deletes from Redis on any attempt.
     */
    public boolean verifyCaptcha(String captchaId, String answer) {
        if (captchaId == null || answer == null) {
            return false;
        }

        String key = REDIS_KEY_PREFIX + captchaId;
        Object stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return false;
        }

        // Delete immediately (single-use)
        redisTemplate.delete(key);

        return stored.toString().equalsIgnoreCase(answer.trim());
    }

    /**
     * Generate audio representation of the CAPTCHA code as WAV base64.
     * Each character is rendered as a beep tone with a unique frequency.
     *
     * @param captchaId the CAPTCHA ID (code is read from Redis without deleting)
     * @return WAV base64 string, or null if captchaId not found
     */
    public String generateAudioBase64(String captchaId) {
        if (captchaId == null) {
            return null;
        }

        String key = REDIS_KEY_PREFIX + captchaId;
        Object stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return null;
        }

        String code = stored.toString();
        return renderAudio(code);
    }

    /**
     * Test-only: retrieve the stored answer for a captcha id.
     * Do NOT use in production code.
     */
    public String getAnswerForTest(String captchaId) {
        String key = REDIS_KEY_PREFIX + captchaId;
        Object stored = redisTemplate.opsForValue().get(key);
        return stored != null ? stored.toString() : null;
    }

    public boolean isAudioEnabled() {
        return config.isAudioEnabled();
    }

    // ---- private helpers ----

    private String generateCode(int length) {
        String chars = config.getChars();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String renderImage(String code) {
        int width = config.getWidth();
        int height = config.getHeight();
        int fontSize = config.getFontSize();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);

        // Noise lines
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
            g.setStroke(new BasicStroke(1 + RANDOM.nextFloat()));
            g.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height),
                       RANDOM.nextInt(width), RANDOM.nextInt(height));
        }

        // Noise dots
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
            g.fillOval(RANDOM.nextInt(width), RANDOM.nextInt(height), 2, 2);
        }

        // Draw each character with slight rotation and color variation
        Font font = new Font("Arial", Font.BOLD, fontSize);
        g.setFont(font);
        int charWidth = width / (code.length() + 1);

        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(RANDOM.nextInt(100), RANDOM.nextInt(100), RANDOM.nextInt(100)));

            // Slight rotation
            double angle = (RANDOM.nextDouble() - 0.5) * 0.4;
            int x = charWidth * (i + 1) - 8;
            int y = (height / 2) + (fontSize / 3) + RANDOM.nextInt(6) - 3;

            g.rotate(angle, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-angle, x, y);
        }

        g.dispose();

        // Encode to base64 PNG
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render CAPTCHA image", e);
        }
    }

    /**
     * 用 javax.sound.sampled 產生 WAV 音訊：
     * 每個字元用不同頻率的 beep 音，每字元 0.5 秒，字元間間隔 0.2 秒。
     */
    private String renderAudio(String code) {
        float sampleRate = 16000f;
        int bitsPerSample = 16;
        double charDuration = 0.5;   // 每個字元 0.5 秒
        double gapDuration = 0.2;    // 字元間 0.2 秒間隔
        double totalDuration = code.length() * charDuration + (code.length() - 1) * gapDuration;
        int totalSamples = (int) (totalDuration * sampleRate);

        byte[] audioData = new byte[totalSamples * 2]; // 16-bit = 2 bytes per sample

        for (int ci = 0; ci < code.length(); ci++) {
            char ch = Character.toUpperCase(code.charAt(ci));
            double freq = CHAR_FREQ_MAP.getOrDefault(ch, 440.0);

            int startSample = (int) ((ci * (charDuration + gapDuration)) * sampleRate);
            int charSamples = (int) (charDuration * sampleRate);

            for (int s = 0; s < charSamples; s++) {
                int sampleIndex = startSample + s;
                if (sampleIndex >= totalSamples) {
                    break;
                }

                // 正弦波，振幅 0.8（避免破音）
                double t = s / sampleRate;
                double value = 0.8 * Math.sin(2.0 * Math.PI * freq * t);

                // 套用淡入淡出避免爆音（前後 5% 的 samples）
                int fadeLen = charSamples / 20;
                if (s < fadeLen) {
                    value *= (double) s / fadeLen;
                } else if (s > charSamples - fadeLen) {
                    value *= (double) (charSamples - s) / fadeLen;
                }

                short sample = (short) (value * Short.MAX_VALUE);
                // Little-endian 16-bit
                audioData[sampleIndex * 2] = (byte) (sample & 0xFF);
                audioData[sampleIndex * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }

        // 寫成 WAV 格式
        AudioFormat format = new AudioFormat(sampleRate, bitsPerSample, 1, true, false);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
             AudioInputStream ais = new AudioInputStream(bais, format, totalSamples);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render CAPTCHA audio", e);
        }
    }

    /** 建立字元 -> 頻率的對應表 */
    private static Map<Character, Double> buildCharFreqMap() {
        return Map.ofEntries(
                // 數字 0-9（C4 到 E5 的自然音階）
                Map.entry('0', 261.63),  // C4
                Map.entry('1', 293.66),  // D4
                Map.entry('2', 329.63),  // E4
                Map.entry('3', 349.23),  // F4
                Map.entry('4', 392.00),  // G4
                Map.entry('5', 440.00),  // A4
                Map.entry('6', 493.88),  // B4
                Map.entry('7', 523.25),  // C5
                Map.entry('8', 587.33),  // D5
                Map.entry('9', 659.25),  // E5
                // 英文字母 A-Z（不同頻率，避免重疊）
                Map.entry('A', 440.00),
                Map.entry('B', 493.88),
                Map.entry('C', 523.25),
                Map.entry('D', 587.33),
                Map.entry('E', 659.25),
                Map.entry('F', 698.46),
                Map.entry('G', 783.99),
                Map.entry('H', 830.61),
                Map.entry('I', 880.00),
                Map.entry('J', 932.33),
                Map.entry('K', 987.77),
                Map.entry('L', 1046.50),
                Map.entry('M', 1108.73),
                Map.entry('N', 1174.66),
                Map.entry('O', 1244.51),
                Map.entry('P', 1318.51),
                Map.entry('Q', 1396.91),
                Map.entry('R', 1479.98),
                Map.entry('S', 1567.98),
                Map.entry('T', 1661.22),
                Map.entry('U', 1760.00),
                Map.entry('V', 1864.66),
                Map.entry('W', 1975.53),
                Map.entry('X', 2093.00),
                Map.entry('Y', 2217.46),
                Map.entry('Z', 2349.32)
        );
    }
}
