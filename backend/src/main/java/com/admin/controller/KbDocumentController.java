package com.admin.controller;

import com.admin.annotation.OperLog;
import com.admin.dto.KbDocumentQueryRequest;
import com.admin.dto.PageResult;
import com.admin.dto.Result;
import com.admin.entity.KbChunk;
import com.admin.entity.KbDocument;
import com.admin.entity.KbDocumentVersion;
import com.admin.service.KbDocumentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kb/document")
@RequiredArgsConstructor
public class KbDocumentController {

    private final KbDocumentService documentService;

    @GetMapping("/list")
    public Result<List<KbDocument>> list() {
        return Result.success(documentService.getAll());
    }

    @GetMapping("/page")
    public Result<PageResult<KbDocument>> page(KbDocumentQueryRequest query) {
        return Result.success(documentService.getPage(query));
    }

    @GetMapping("/{id}")
    public Result<KbDocument> getById(@PathVariable Long id) {
        return Result.success(documentService.getById(id));
    }

    @OperLog(module = "知识库管理", description = "新增文档")
    @PostMapping
    public Result<KbDocument> create(@RequestBody KbDocument document) {
        return Result.success(documentService.create(document));
    }

    @OperLog(module = "知识库管理", description = "修改文档")
    @PutMapping
    public Result<Void> update(@RequestBody KbDocument document) {
        documentService.update(document);
        return Result.success();
    }

    @OperLog(module = "知识库管理", description = "删除文档")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.success();
    }

    @OperLog(module = "知识库管理", description = "上传并解析文档")
    @PostMapping("/upload")
    public Result<KbDocument> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "category", required = false) String category,
                                     @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {
        return Result.success(documentService.uploadAndParse(file, category, uploadedBy));
    }

    @OperLog(module = "知识库管理", description = "重新上传文档")
    @PostMapping("/{id}/reupload")
    public Result<KbDocument> reupload(@PathVariable Long id,
                                        @RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "category", required = false) String category) {
        return Result.success(documentService.reuploadAndParse(id, file, category));
    }

    @GetMapping("/{id}/chunks")
    public Result<List<KbChunk>> getChunks(@PathVariable Long id) {
        return Result.success(documentService.getChunks(id));
    }

    @OperLog(module = "知识库管理", description = "切换文档启用状态")
    @PutMapping("/{id}/enabled")
    public Result<Void> toggleEnabled(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> body) {
        documentService.toggleEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
        return Result.success();
    }

    @GetMapping("/{id}/versions")
    public Result<List<KbDocumentVersion>> versions(@PathVariable Long id) {
        return Result.success(documentService.getVersions(id));
    }

    @GetMapping("/{id}/versions/{version}/content")
    public Result<Map<String, String>> versionContent(@PathVariable Long id, @PathVariable int version) {
        KbDocumentVersion ver = documentService.getVersion(id, version);
        return Result.success(Map.of(
                "documentName", ver.getDocumentName() != null ? ver.getDocumentName() : "",
                "content", ver.getContentSnapshot() != null ? ver.getContentSnapshot() : ""
        ));
    }

    @GetMapping("/{id}/versions/{version}/download")
    public void versionDownload(@PathVariable Long id, @PathVariable int version, HttpServletResponse response) throws IOException {
        KbDocumentVersion ver = documentService.getVersion(id, version);
        String fileName = ver.getDocumentName() != null ? ver.getDocumentName() : "document_v" + version + ".txt";
        String content = ver.getContentSnapshot() != null ? ver.getContentSnapshot() : "";
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }

    @GetMapping("/{id}/content")
    public Result<Map<String, String>> currentContent(@PathVariable Long id) {
        KbDocument doc = documentService.getById(id);
        StringBuilder sb = new StringBuilder();
        for (KbChunk chunk : documentService.getChunks(id)) {
            sb.append(chunk.getChunkText()).append("\n");
        }
        return Result.success(Map.of(
                "documentName", doc.getDocumentName() != null ? doc.getDocumentName() : "",
                "content", sb.toString()
        ));
    }

    @OperLog(module = "知识库管理", description = "回滚文档版本")
    @PostMapping("/{id}/rollback/{version}")
    public Result<Void> rollback(@PathVariable Long id, @PathVariable int version) {
        documentService.rollbackVersion(id, version);
        return Result.success();
    }
}
