package com.company.common.report.engine.xdocreport;

import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportEngine;
import com.company.common.report.spi.ReportResult;
import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * xDocReport 報表引擎實作
 *
 * <p>使用 DOCX 範本搭配 Velocity 模板引擎產製文件。
 * <ul>
 *   <li>DOCX 輸出：直接 process（不轉換）</li>
 *   <li>PDF 輸出：透過 xDocReport converter 轉換（需引入 converter 依賴）</li>
 * </ul>
 */
public class XDocReportEngine implements ReportEngine {

    private static final Logger log = LoggerFactory.getLogger(XDocReportEngine.class);

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
        log.info("--> xDocReport generate | format={}, template={}",
                context.getOutputFormat(), context.getTemplatePath());

        if (context.getTemplatePath() == null || context.getTemplatePath().isBlank()) {
            throw new IllegalArgumentException(
                    "templatePath is required for xDocReport engine");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (InputStream templateStream = loadTemplate(context.getTemplatePath())) {
            // 1. 載入範本
            IXDocReport report = XDocReportRegistry.getRegistry()
                    .loadReport(templateStream, TemplateEngineKind.Velocity);

            // 2. 建立 context 並填入參數
            IContext velocityContext = report.createContext();
            if (context.getParameters() != null) {
                context.getParameters().forEach(velocityContext::put);
            }

            // 3. 如果有 data list，放入 context
            if (context.getData() != null && !context.getData().isEmpty()) {
                velocityContext.put("items", context.getData());
            }

            // 4. 根據輸出格式產製
            if (context.getOutputFormat() == OutputFormat.PDF) {
                Options options = Options.getTo(ConverterTypeTo.PDF);
                report.convert(velocityContext, options, out);
            } else {
                report.process(velocityContext, out);
            }

        } catch (IOException | XDocReportException e) {
            throw new IllegalStateException("Failed to generate xDocReport", e);
        }

        // 檔案大小保護（50MB）
        if (out.size() > 50 * 1024 * 1024) {
            throw new IllegalStateException(
                    "Report file exceeds maximum allowed size: 50MB");
        }

        String fileName = resolveFileName(context);
        String contentType = resolveContentType(context.getOutputFormat());

        log.info("<-- xDocReport generate | fileName={}, size={} bytes",
                fileName, out.size());
        return new ReportResult(out.toByteArray(), contentType, fileName);
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
