package com.company.common.report.engine.xdocreport;

import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.spi.ImageSource;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportEngine;
import com.company.common.report.spi.ReportResult;
import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.images.ByteArrayImageProvider;
import fr.opensagres.xdocreport.document.images.IImageProvider;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * xDocReport 報表引擎實作
 *
 * <p>使用 DOCX 範本搭配 Velocity 模板引擎產製文件。
 * <ul>
 *   <li>DOCX 輸出：直接 process（不轉換）</li>
 *   <li>PDF 輸出：透過 xDocReport converter 轉換（需引入 converter 依賴）</li>
 *   <li>圖片插入：透過 {@link ReportContext#getImages()} 註冊圖片欄位</li>
 * </ul>
 */
public class XDocReportEngine implements ReportEngine {

    private static final Logger log = LoggerFactory.getLogger(XDocReportEngine.class);

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private static final Set<OutputFormat> SUPPORTED_FORMATS = Set.of(
            OutputFormat.DOCX, OutputFormat.PDF, OutputFormat.ODT
    );

    @Override
    public ReportEngineType getType() {
        return ReportEngineType.XDOCREPORT;
    }

    @Override
    public boolean supports(OutputFormat format) {
        return SUPPORTED_FORMATS.contains(format);
    }

    @Override
    public ReportResult generate(ReportContext context) {
        log.info("--> xDocReport generate | format={}, template={}, images={}",
                context.getOutputFormat(), context.getTemplatePath(),
                context.getImages() != null ? context.getImages().size() : 0);

        if (context.getTemplatePath() == null || context.getTemplatePath().isBlank()) {
            throw new IllegalArgumentException(
                    "templatePath is required for xDocReport engine");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (InputStream templateStream = loadTemplate(context.getTemplatePath())) {
            // 1. 載入範本
            IXDocReport report = XDocReportRegistry.getRegistry()
                    .loadReport(templateStream, TemplateEngineKind.Velocity);

            // 2. 註冊圖片欄位 metadata
            registerImageMetadata(report, context.getImages());

            // 3. 建立 context 並填入參數
            IContext velocityContext = report.createContext();
            if (context.getParameters() != null) {
                context.getParameters().forEach(velocityContext::put);
            }

            // 4. 如果有 data list，放入 context
            if (context.getData() != null && !context.getData().isEmpty()) {
                velocityContext.put("items", context.getData());
            }

            // 5. 註冊圖片到 Velocity context
            registerImageProviders(velocityContext, context.getImages());

            // 6. 根據輸出格式產製
            if (context.getOutputFormat() == OutputFormat.PDF) {
                Options options = Options.getTo(ConverterTypeTo.PDF);
                report.convert(velocityContext, options, out);
            } else {
                report.process(velocityContext, out);
            }

        } catch (IOException | XDocReportException e) {
            throw new IllegalStateException("Failed to generate xDocReport", e);
        }

        validateFileSize(out);

        String fileName = resolveFileName(context);
        String contentType = resolveContentType(context.getOutputFormat());

        log.info("<-- xDocReport generate | fileName={}, size={} bytes",
                fileName, out.size());
        return new ReportResult(out.toByteArray(), contentType, fileName);
    }

    @Override
    public ReportResult generateMerged(List<ReportContext> contexts) {
        if (contexts.size() == 1) {
            return generate(contexts.getFirst());
        }

        log.info("--> xDocReport generateMerged | count={}", contexts.size());

        // 合併所有 context 的 parameters 和 images，data 以 items_0, items_1 分別放入
        ReportContext first = contexts.getFirst();

        Map<String, Object> mergedParams = new HashMap<>();
        Map<String, ImageSource> mergedImages = new java.util.LinkedHashMap<>();

        for (int i = 0; i < contexts.size(); i++) {
            ReportContext ctx = contexts.get(i);
            if (ctx.getParameters() != null) {
                mergedParams.putAll(ctx.getParameters());
            }
            if (ctx.getImages() != null) {
                mergedImages.putAll(ctx.getImages());
            }
            if (ctx.getData() != null && !ctx.getData().isEmpty()) {
                mergedParams.put("items_" + i, ctx.getData());
            }
        }

        // 用第一個 context 的 template 和 format 產製
        ReportContext merged = ReportContext.builder()
                .templatePath(first.getTemplatePath())
                .engineType(first.getEngineType())
                .outputFormat(first.getOutputFormat())
                .fileName(first.getFileName())
                .parameters(mergedParams)
                .images(mergedImages)
                .build();

        return generate(merged);
    }

    /**
     * 註冊圖片欄位到 FieldsMetadata
     */
    private void registerImageMetadata(IXDocReport report,
                                       Map<String, ImageSource> images) throws XDocReportException {
        if (images == null || images.isEmpty()) {
            return;
        }
        FieldsMetadata metadata = report.createFieldsMetadata();
        for (String fieldName : images.keySet()) {
            metadata.addFieldAsImage(fieldName);
        }
    }

    /**
     * 將圖片 IImageProvider 放入 Velocity context
     */
    private void registerImageProviders(IContext velocityContext,
                                        Map<String, ImageSource> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (Map.Entry<String, ImageSource> entry : images.entrySet()) {
            byte[] imgBytes = entry.getValue().resolveContent();
            IImageProvider provider = new ByteArrayImageProvider(imgBytes);

            if (entry.getValue().getWidth() != null) {
                provider.setWidth(entry.getValue().getWidth().floatValue());
            }
            if (entry.getValue().getHeight() != null) {
                provider.setHeight(entry.getValue().getHeight().floatValue());
            }

            velocityContext.put(entry.getKey(), provider);
        }
    }

    private void validateFileSize(ByteArrayOutputStream out) {
        if (out.size() > MAX_FILE_SIZE) {
            throw new IllegalStateException(
                    "Report file exceeds maximum allowed size: 50MB");
        }
    }

    private InputStream loadTemplate(String templatePath) {
        // 先嘗試從 classpath 載入
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(templatePath);
        if (stream != null) {
            return stream;
        }
        // 嘗試從 file system 載入
        try {
            return new FileInputStream(templatePath);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(
                    "Template not found: " + templatePath, e);
        }
    }

    private String resolveFileName(ReportContext context) {
        if (context.getFileName() != null && !context.getFileName().isBlank()) {
            return context.getFileName();
        }
        return "report" + getExtension(context.getOutputFormat());
    }

    private String getExtension(OutputFormat format) {
        return switch (format) {
            case DOCX -> ".docx";
            case PDF -> ".pdf";
            case ODT -> ".odt";
            default -> ".docx";
        };
    }

    private String resolveContentType(OutputFormat format) {
        return switch (format) {
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case PDF -> "application/pdf";
            case ODT -> "application/vnd.oasis.opendocument.text";
            default -> "application/octet-stream";
        };
    }
}
