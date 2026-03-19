package com.company.common.report.spi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 圖片資料來源
 *
 * <p>搭配 {@link ReportContext#getImages()} 使用，
 * 支援 byte[] 和檔案路徑兩種來源。
 */
public class ImageSource {

    /** 圖片 byte[]（與 filePath 二選一） */
    private final byte[] content;

    /** 圖片檔案路徑（與 content 二選一） */
    private final String filePath;

    /** 圖片寬度 px（選填） */
    private final Integer width;

    /** 圖片高度 px（選填） */
    private final Integer height;

    private ImageSource(byte[] content, String filePath, Integer width, Integer height) {
        this.content = content;
        this.filePath = filePath;
        this.width = width;
        this.height = height;
    }

    /**
     * 從 byte[] 建立
     */
    public static ImageSource fromBytes(byte[] content) {
        return new ImageSource(content, null, null, null);
    }

    /**
     * 從 byte[] 建立，指定寬高
     */
    public static ImageSource fromBytes(byte[] content, int width, int height) {
        return new ImageSource(content, null, width, height);
    }

    /**
     * 從檔案路徑建立
     */
    public static ImageSource fromFile(String filePath) {
        return new ImageSource(null, filePath, null, null);
    }

    /**
     * 從檔案路徑建立，指定寬高
     */
    public static ImageSource fromFile(String filePath, int width, int height) {
        return new ImageSource(null, filePath, width, height);
    }

    /**
     * 解析並回傳圖片 byte[]
     *
     * <p>如果是 filePath 來源，會讀取檔案內容。
     * 優先嘗試 classpath，再嘗試 filesystem。
     */
    public byte[] resolveContent() {
        if (content != null) {
            return content;
        }
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalStateException("ImageSource has no content or filePath");
        }
        // 先嘗試 classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filePath)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            // fall through to filesystem
        }
        // 嘗試 filesystem
        try {
            return Files.readAllBytes(Path.of(filePath));
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read image file: " + filePath, e);
        }
    }

    public byte[] getContent() {
        return content;
    }

    public String getFilePath() {
        return filePath;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }
}
