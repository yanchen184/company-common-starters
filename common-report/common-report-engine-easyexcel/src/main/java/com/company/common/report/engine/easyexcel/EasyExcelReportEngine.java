package com.company.common.report.engine.easyexcel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.enums.WriteDirectionEnum;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportEngine;
import com.company.common.report.spi.ReportResult;
import com.company.common.report.spi.SheetData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * EasyExcel 報表引擎實作
 *
 * <p>支援三種模式：
 * <ul>
 *   <li>多 Sheet 模式：context.getSheets() 不為空時，每個 SheetData 對應一個工作表</li>
 *   <li>範本填充模式：context.getTemplatePath() 不為空時，使用範本填充資料</li>
 *   <li>資料寫入模式：context.getData() 不為空時，直接將 List 資料寫入 Excel</li>
 * </ul>
 */
public class EasyExcelReportEngine implements ReportEngine {

    private static final Logger log = LoggerFactory.getLogger(EasyExcelReportEngine.class);

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private static final Set<OutputFormat> SUPPORTED_FORMATS = Set.of(
            OutputFormat.XLSX, OutputFormat.XLS, OutputFormat.CSV
    );

    @Override
    public ReportEngineType getType() {
        return ReportEngineType.EASYEXCEL;
    }

    @Override
    public boolean supports(OutputFormat format) {
        return SUPPORTED_FORMATS.contains(format);
    }

    @Override
    public ReportResult generate(ReportContext context) {
        log.info("--> EasyExcel generate | template={}, format={}, dataSize={}, sheets={}",
                context.getTemplatePath(), context.getOutputFormat(),
                context.getData() != null ? context.getData().size() : 0,
                context.getSheets() != null ? context.getSheets().size() : 0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelTypeEnum excelType = toExcelType(context.getOutputFormat());

        if (context.getSheets() != null && !context.getSheets().isEmpty()) {
            generateMultiSheet(context.getSheets(), out, excelType);
        } else if (context.getTemplatePath() != null && !context.getTemplatePath().isBlank()) {
            generateWithTemplate(context, out, excelType);
        } else if (context.getData() != null && !context.getData().isEmpty()) {
            generateWithData(context, out, excelType);
        } else {
            throw new IllegalArgumentException(
                    "Either sheets, templatePath, or data must be provided for EasyExcel engine");
        }

        validateFileSize(out);

        String fileName = resolveFileName(context);
        String contentType = resolveContentType(context.getOutputFormat());

        log.info("<-- EasyExcel generate | fileName={}, size={} bytes", fileName, out.size());
        return new ReportResult(out.toByteArray(), contentType, fileName);
    }

    @Override
    public ReportResult generateMerged(List<ReportContext> contexts) {
        log.info("--> EasyExcel generateMerged | count={}", contexts.size());

        if (contexts.size() == 1) {
            return generate(contexts.getFirst());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelTypeEnum excelType = toExcelType(contexts.getFirst().getOutputFormat());

        // 每個 context 對應一個 Sheet
        try (ExcelWriter writer = EasyExcel.write(out).excelType(excelType).build()) {
            for (int i = 0; i < contexts.size(); i++) {
                ReportContext ctx = contexts.get(i);
                String sheetName = ctx.getFileName() != null
                        ? ctx.getFileName().replaceAll("\\.[^.]+$", "")
                        : "Sheet" + (i + 1);

                List<?> data = ctx.getData();
                if (data == null || data.isEmpty()) {
                    continue;
                }

                Class<?> headClass = data.getFirst().getClass();
                WriteSheet sheet = EasyExcel.writerSheet(i, sheetName)
                        .head(headClass).build();
                writer.write(data, sheet);
            }
        }

        validateFileSize(out);

        ReportContext first = contexts.getFirst();
        String fileName = resolveFileName(first);
        String contentType = resolveContentType(first.getOutputFormat());

        log.info("<-- EasyExcel generateMerged | fileName={}, size={} bytes", fileName, out.size());
        return new ReportResult(out.toByteArray(), contentType, fileName);
    }

    /**
     * 多 Sheet 模式
     */
    private void generateMultiSheet(List<SheetData> sheets,
                                    ByteArrayOutputStream out,
                                    ExcelTypeEnum excelType) {
        try (ExcelWriter writer = EasyExcel.write(out).excelType(excelType).build()) {
            for (int i = 0; i < sheets.size(); i++) {
                SheetData sd = sheets.get(i);
                Class<?> head = sd.getHeadClass();
                if (head == null && sd.getData() != null && !sd.getData().isEmpty()) {
                    head = sd.getData().getFirst().getClass();
                }
                WriteSheet sheet = EasyExcel.writerSheet(i, sd.getSheetName())
                        .head(head).build();
                writer.write(sd.getData(), sheet);
            }
        }
    }

    /**
     * 使用範本填充模式
     */
    private void generateWithTemplate(ReportContext context,
                                      ByteArrayOutputStream out,
                                      ExcelTypeEnum excelType) {
        InputStream templateStream = getClass().getClassLoader()
                .getResourceAsStream(context.getTemplatePath());
        if (templateStream == null) {
            throw new IllegalArgumentException(
                    "Template not found: " + context.getTemplatePath());
        }

        try (templateStream;
             ExcelWriter writer = EasyExcel.write(out)
                .excelType(excelType)
                .withTemplate(templateStream)
                .build()) {

            WriteSheet sheet = EasyExcel.writerSheet().build();
            FillConfig fillConfig = FillConfig.builder()
                    .direction(WriteDirectionEnum.VERTICAL)
                    .forceNewRow(Boolean.FALSE)
                    .build();

            // 填充參數
            if (context.getParameters() != null && !context.getParameters().isEmpty()) {
                writer.fill(context.getParameters(), sheet);
            }
            // 填充資料列表
            if (context.getData() != null && !context.getData().isEmpty()) {
                writer.fill(context.getData(), fillConfig, sheet);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate report from template", e);
        }
    }

    /**
     * 直接資料寫入模式
     */
    private void generateWithData(ReportContext context,
                                  ByteArrayOutputStream out,
                                  ExcelTypeEnum excelType) {
        if (context.getData() == null || context.getData().isEmpty()) {
            throw new IllegalArgumentException("Data list cannot be empty for data-driven report");
        }
        Class<?> dataClass = context.getData().getFirst().getClass();

        EasyExcel.write(out, dataClass)
                .excelType(excelType)
                .sheet("Sheet1")
                .doWrite(context.getData());
    }

    private void validateFileSize(ByteArrayOutputStream out) {
        if (out.size() > MAX_FILE_SIZE) {
            throw new IllegalStateException("Report file exceeds maximum allowed size: 50MB");
        }
    }

    private ExcelTypeEnum toExcelType(OutputFormat format) {
        return switch (format) {
            case XLS -> ExcelTypeEnum.XLS;
            case CSV -> ExcelTypeEnum.CSV;
            default -> ExcelTypeEnum.XLSX;
        };
    }

    private String resolveFileName(ReportContext context) {
        if (context.getFileName() != null && !context.getFileName().isBlank()) {
            return context.getFileName();
        }
        return "report." + context.getOutputFormat().name().toLowerCase();
    }

    private String resolveContentType(OutputFormat format) {
        return switch (format) {
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case XLS -> "application/vnd.ms-excel";
            case CSV -> "text/csv";
            default -> "application/octet-stream";
        };
    }
}
