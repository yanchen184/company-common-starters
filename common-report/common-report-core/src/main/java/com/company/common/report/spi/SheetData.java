package com.company.common.report.spi;

import java.util.List;
import java.util.Map;

/**
 * 多 Sheet Excel 的單頁資料描述
 *
 * <p>搭配 {@link ReportContext#getSheets()} 使用，
 * 每個 SheetData 對應 Excel 中的一個工作表。
 */
public class SheetData {

    /** Sheet 名稱 */
    private final String sheetName;

    /** 該 Sheet 的資料集 */
    private final List<?> data;

    /** 用於 EasyExcel head 推導的 Class（選填，為 null 時自動從 data 推導） */
    private final Class<?> headClass;

    /** 該 Sheet 的範本參數（選填，用於範本填充模式） */
    private final Map<String, Object> parameters;

    private SheetData(String sheetName, List<?> data, Class<?> headClass,
                      Map<String, Object> parameters) {
        this.sheetName = sheetName;
        this.data = data;
        this.headClass = headClass;
        this.parameters = parameters;
    }

    public static SheetData of(String sheetName, List<?> data) {
        return new SheetData(sheetName, data, null, null);
    }

    public static SheetData of(String sheetName, List<?> data, Class<?> headClass) {
        return new SheetData(sheetName, data, headClass, null);
    }

    public static SheetData of(String sheetName, List<?> data, Class<?> headClass,
                               Map<String, Object> parameters) {
        return new SheetData(sheetName, data, headClass, parameters);
    }

    public String getSheetName() {
        return sheetName;
    }

    public List<?> getData() {
        return data;
    }

    public Class<?> getHeadClass() {
        return headClass;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }
}
