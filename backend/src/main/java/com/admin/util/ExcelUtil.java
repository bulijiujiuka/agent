package com.admin.util;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 通用导出工具类
 */
public class ExcelUtil {

    /**
     * 导出 Excel
     * @param response   HTTP响应
     * @param fileName   文件名（不含后缀）
     * @param sheetName  Sheet名
     * @param clazz      数据类型
     * @param data       数据列表
     */
    public static <T> void export(HttpServletResponse response, String fileName,
                                   String sheetName, Class<T> clazz, List<T> data) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), clazz)
                .sheet(sheetName)
                .doWrite(data);
    }
}
