package com.example.post.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AwsS3Service {
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    @SneakyThrows
    public String upload(MultipartFile file) {
        if(file.isEmpty() || Objects.isNull(file.getOriginalFilename()))
            throw new Exception("파일이 비어있음");
        return checkAndUpload(file);
    }

    private String checkAndUpload(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        String ext = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
        String fileName = UUID.randomUUID().toString().substring(0, 8) + originalFileName;

        String url = "";
        try{
            InputStream inputStream = file.getInputStream();
            byte[] bytes = IOUtils.toByteArray(inputStream);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType("image/" + ext);


            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName, fileName, byteArrayInputStream, metadata);
            amazonS3.putObject(putObjectRequest);

            byteArrayInputStream.close();

            url = amazonS3.getUrl(bucketName, fileName).toString();

        }catch (Exception e){
            e.printStackTrace();
        }
        return url;

    }
}
