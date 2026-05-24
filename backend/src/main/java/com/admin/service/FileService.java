package com.admin.service;

import com.admin.config.FileStorageProperties;
import com.admin.entity.FileInfo;
import com.admin.exception.BusinessException;
import com.admin.mapper.FileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMapper fileMapper;
    private final FileStorageProperties properties;

    /**
     * 上传文件
     * @param file      上传的文件
     * @param bizType   业务类型（avatar/post/resource等），可选
     * @param uploadUser 上传用户，可选
     * @return 文件信息
     */
    public FileInfo upload(MultipartFile file, String bizType, String uploadUser) {
        // 1. 校验文件
        validateFile(file);

        // 2. 生成存储信息
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = getFileExtension(originalName);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        // 按日期分目录：2026/03/25
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = datePath + "/" + storedName;

        // 3. 存储文件
        String fullPath = storeLocal(file, relativePath);

        // 4. 构建文件URL
        String fileUrl = properties.getUrlPrefix() + "/" + relativePath;

        // 5. 保存数据库记录
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalName(originalName);
        fileInfo.setStoredName(storedName);
        fileInfo.setFilePath(fullPath);
        fileInfo.setFileUrl(fileUrl);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileType(file.getContentType());
        fileInfo.setFileExt(ext);
        fileInfo.setStorageType(properties.getStorageType());
        fileInfo.setUploadUser(uploadUser);
        fileInfo.setBizType(bizType);

        fileMapper.insert(fileInfo);
        log.info("文件上传成功: {} -> {}", originalName, fileUrl);

        return fileInfo;
    }

    /**
     * 批量上传
     */
    public List<FileInfo> uploadBatch(MultipartFile[] files, String bizType, String uploadUser) {
        return java.util.Arrays.stream(files)
                .map(f -> upload(f, bizType, uploadUser))
                .toList();
    }

    /**
     * 根据ID查询文件
     */
    public FileInfo getById(Long id) {
        return fileMapper.findById(id);
    }

    /**
     * 根据业务类型查询
     */
    public List<FileInfo> getByBizType(String bizType) {
        return fileMapper.findByBizType(bizType);
    }

    /**
     * 查询全部文件
     */
    public List<FileInfo> getAll() {
        return fileMapper.findAll();
    }

    /**
     * 删除文件（同时删除物理文件和数据库记录）
     */
    public void delete(Long id) {
        FileInfo fileInfo = fileMapper.findById(id);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }

        // 删除物理文件
        deleteLocalFile(fileInfo.getFilePath());

        // 删除数据库记录
        fileMapper.deleteById(id);
        log.info("文件删除成功: {}", fileInfo.getOriginalName());
    }

    /**
     * 获取文件的本地磁盘路径（用于下载）
     */
    public Path getFilePath(Long id) {
        FileInfo fileInfo = fileMapper.findById(id);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        return Paths.get(fileInfo.getFilePath());
    }

    // ==================== 私有方法 ====================

    /**
     * 校验文件合法性
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 校验文件大小
        long maxBytes = properties.getMaxSize() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("文件大小超过限制（最大 " + properties.getMaxSize() + "MB）");
        }

        // 校验扩展名
        String ext = getFileExtension(file.getOriginalFilename());
        if (!properties.getAllowedExtensions().contains(ext.toLowerCase())) {
            throw new BusinessException("不支持的文件类型: " + ext);
        }
    }

    /**
     * 本地存储
     */
    private String storeLocal(MultipartFile file, String relativePath) {
        try {
            Path targetPath = Paths.get(properties.getLocalPath()).resolve(relativePath);
            Files.createDirectories(targetPath.getParent());

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return targetPath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new BusinessException("文件存储失败: " + e.getMessage());
        }
    }

    /**
     * 删除本地文件
     */
    private void deleteLocalFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("物理文件删除失败: {}", filePath, e);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
