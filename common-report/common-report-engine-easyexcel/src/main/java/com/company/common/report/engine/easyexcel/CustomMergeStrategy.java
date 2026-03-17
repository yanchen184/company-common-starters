package com.company.common.report.engine.easyexcel;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.merge.AbstractMergeStrategy;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.HashMap;
import java.util.Map;

/**
 * 自訂合併策略：相同值自動垂直合併
 *
 * <p>指定要合併的欄位索引，當相鄰行的該欄位值相同時自動合併儲存格。
 * 適用於分組報表中需要合併相同分類的場景。
 *
 * <p>使用方式：
 * <pre>
 * EasyExcel.write(out, DataClass.class)
 *     .registerWriteHandler(new CustomMergeStrategy(0, 1))  // 合併第 0、1 欄
 *     .sheet("Sheet1")
 *     .doWrite(dataList);
 * </pre>
 */
public class CustomMergeStrategy extends AbstractMergeStrategy {

    /** 要合併的欄位索引 */
    private final int[] mergeColumnIndexes;

    /** 每欄最後一個合併區域的快取，避免每次遍歷所有合併區域 */
    private final Map<Integer, CellRangeAddress> lastMergeMap = new HashMap<>();

    /**
     * @param mergeColumnIndexes 要進行垂直合併的欄位索引（0-based）
     */
    public CustomMergeStrategy(int... mergeColumnIndexes) {
        this.mergeColumnIndexes = mergeColumnIndexes;
    }

    @Override
    protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
        if (relativeRowIndex == null || relativeRowIndex == 0) {
            return;
        }

        int currentColumnIndex = cell.getColumnIndex();
        if (!shouldMerge(currentColumnIndex)) {
            return;
        }

        int currentRowIndex = cell.getRowIndex();
        String currentValue = getCellStringValue(cell);
        String previousValue = getCellStringValue(
                sheet.getRow(currentRowIndex - 1).getCell(currentColumnIndex));

        if (currentValue != null && currentValue.equals(previousValue)) {
            CellRangeAddress lastRange = lastMergeMap.get(currentColumnIndex);
            if (lastRange != null && lastRange.getLastRow() == currentRowIndex - 1) {
                // 擴展現有合併區域：移除舊的，建立新的
                sheet.removeMergedRegion(findMergedRegionIndex(sheet, lastRange));
                CellRangeAddress newRange = new CellRangeAddress(
                        lastRange.getFirstRow(), currentRowIndex,
                        currentColumnIndex, currentColumnIndex);
                sheet.addMergedRegion(newRange);
                lastMergeMap.put(currentColumnIndex, newRange);
            } else {
                // 建立新的合併區域
                CellRangeAddress newRange = new CellRangeAddress(
                        currentRowIndex - 1, currentRowIndex,
                        currentColumnIndex, currentColumnIndex);
                sheet.addMergedRegion(newRange);
                lastMergeMap.put(currentColumnIndex, newRange);
            }
        }
    }

    private int findMergedRegionIndex(Sheet sheet, CellRangeAddress target) {
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            if (range.getFirstRow() == target.getFirstRow()
                    && range.getLastRow() == target.getLastRow()
                    && range.getFirstColumn() == target.getFirstColumn()
                    && range.getLastColumn() == target.getLastColumn()) {
                return i;
            }
        }
        return -1;
    }

    private boolean shouldMerge(int columnIndex) {
        for (int index : mergeColumnIndexes) {
            if (index == columnIndex) {
                return true;
            }
        }
        return false;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        }
        return null;
    }
}
