package com.admin.controller;

import com.admin.dto.Result;
import com.admin.entity.FileInfo;
import com.admin.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 单文件上传
     * @param file      文件
     * @param bizType   业务类型（可选：avatar/post/resource等）
     * @param uploadUser 上传用户（可选）
     */
    @PostMapping("/upload")
    public Result<FileInfo> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "bizType", required = false) String bizType,
                                   @RequestParam(value = "uploadUser", required = false) String uploadUser) {
        FileInfo fileInfo = fileService.upload(file, bizType, uploadUser);
        return Result.success(fileInfo);
    }

    /**
     * 批量上传
     */
    @PostMapping("/upload/batch")
    public Result<List<FileInfo>> uploadBatch(@RequestParam("files") MultipartFile[] files,
                                              @RequestParam(value = "bizType", required = false) String bizType,
                                              @RequestParam(value = "uploadUser", required = false) String uploadUser) {
        List<FileInfo> fileInfos = fileService.uploadBatch(files, bizType, uploadUser);
        return Result.success(fileInfos);
    }

    /**
     * 文件下载
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {
        FileInfo fileInfo = fileService.getById(id);
        if (fileInfo == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = fileService.getFilePath(id);
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String encodedName = URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(resource);
    }

    /**
     * 查询文件信息
     */
    @GetMapping("/{id}")
    public Result<FileInfo> getById(@PathVariable Long id) {
        return Result.success(fileService.getById(id));
    }

    /**
     * 按业务类型查询
     */
    @GetMapping("/list")
    public Result<List<FileInfo>> list(@RequestParam(value = "bizType", required = false) String bizType) {
        if (bizType != null && !bizType.isEmpty()) {
            return Result.success(fileService.getByBizType(bizType));
        }
        return Result.success(fileService.getAll());
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.success();
    }
}
