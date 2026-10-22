package com.ticketing.attachment.service;

import com.ticketing.attachment.mapper.AttachmentMapper;
import com.ticketing.attachment.model.AttachmentRow;
import com.ticketing.auth.security.SessionUser;
import com.ticketing.common.api.BusinessException;
import com.ticketing.config.MinioProperties;
import com.ticketing.ticket.mapper.TicketMapper;
import com.ticketing.ticket.service.TicketPolicy;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {
    private final AttachmentMapper mapper;
    private final TicketMapper tickets;
    private final TicketPolicy policy;
    private final ObjectStorage storage;
    private final ImageValidator validator;
    private final MinioProperties properties;
    private final TransactionTemplate tx;
    public AttachmentService(AttachmentMapper mapper, TicketMapper tickets, TicketPolicy policy,
            ObjectStorage storage, ImageValidator validator, MinioProperties properties, PlatformTransactionManager transactions) {
        this.mapper=mapper; this.tickets=tickets; this.policy=policy; this.storage=storage;
        this.validator=validator; this.properties=properties; this.tx=new TransactionTemplate(transactions);
    }

    public AttachmentRow upload(SessionUser user, MultipartFile file) {
        policy.require(user, "attachment:upload");
        if (file.getSize() > 5 * 1024 * 1024) throw BusinessException.invalid("图片最大5 MiB");
        byte[] bytes;
        try { bytes=file.getBytes(); } catch (Exception ex) { throw BusinessException.invalid("无法读取上传文件"); }
        var image=validator.validate(bytes);
        var row=new AttachmentRow();
        row.uploadToken=UUID.randomUUID().toString(); row.uploaderId=user.getId(); row.bucketName=properties.bucket();
        row.objectKey="uploads/"+row.uploadToken+"."+image.extension();
        String name=Objects.requireNonNullElse(file.getOriginalFilename(), "image");
        row.originalFilename=name.length() > 255 ? name.substring(0,255) : name;
        row.mediaType=image.mime(); row.sizeBytes=(long)bytes.length;
        row.imageWidth=image.width(); row.imageHeight=image.height(); row.sha256=hash(bytes);
        tx.executeWithoutResult(s -> {
            if (mapper.lockUploader(user.getId()) == null) throw BusinessException.notFound();
            if (mapper.countTemporary(user.getId()) >= 20) throw BusinessException.conflict("未提交图片过多，请稍后再试");
            mapper.insert(row);
        });
        try {
            storage.put(row.bucketName,row.objectKey,bytes,row.mediaType);
            if (mapper.ready(row.id) != 1) {
                storage.delete(row.bucketName,row.objectKey);
                throw BusinessException.conflict("上传已过期，请重新上传");
            }
            return mapper.find(row.id);
        } catch (RuntimeException ex) { mapper.pending(row.id); throw ex; }
    }

    // Caller holds the ticket row lock in the same database transaction.
    public void bind(SessionUser user, Long ticketId, Long eventId, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return;
        policy.require(user,"attachment:bind");
        var unique=new TreeSet<>(tokens);
        if (unique.size() != tokens.size() || unique.size()+mapper.countBound(ticketId)>5)
            throw BusinessException.invalid("每工单最多5张图片，不能重复绑定");
        for (String token:unique) {
            var row=mapper.lockToken(token);
            if (row == null || !user.getId().equals(row.uploaderId)) throw BusinessException.notFound();
            if (!"READY".equals(row.storageState) || row.expiresAt == null || !row.expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC)))
                throw BusinessException.conflict("图片已绑定或过期，请重新上传");
            storage.verify(row.bucketName,row.objectKey);
            if (mapper.bind(row.id,ticketId,eventId)!=1) throw BusinessException.conflict("图片状态已变更");
        }
    }

    public List<AttachmentRow> list(Long ticketId) { return mapper.list(ticketId); }
    public record ImageContent(String mediaType, byte[] bytes) { }
    public ImageContent read(SessionUser user, Long id) {
        policy.require(user,"attachment:read");
        var row=mapper.find(id);
        if (row == null) throw BusinessException.notFound();
        if (row.ticketId == null) {
            if (!user.getId().equals(row.uploaderId) || !"READY".equals(row.storageState)
                    || row.expiresAt == null || !row.expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC)))
                throw BusinessException.notFound();
        } else policy.readable(user,tickets.find(row.ticketId));
        return new ImageContent(row.mediaType,storage.read(row.bucketName,row.objectKey));
    }

    @Scheduled(fixedDelayString="${app.attachments.cleanup-delay-ms:3600000}", initialDelay=60000)
    public void cleanup() {
        for (var row:mapper.expired()) {
            try {
                boolean claimed=Boolean.TRUE.equals(tx.execute(s -> mapper.claimExpired(row.id)>0));
                if (claimed) { storage.delete(row.bucketName,row.objectKey); mapper.deletePending(row.id); }
            } catch (RuntimeException ex) {
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Temporary attachment cleanup will retry, id={}",row.id);
            }
        }
    }
    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
