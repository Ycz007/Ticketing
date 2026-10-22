package com.ticketing.attachment;

import com.ticketing.attachment.mapper.AttachmentMapper;
import com.ticketing.attachment.model.AttachmentRow;
import com.ticketing.attachment.service.*;
import com.ticketing.common.api.BusinessException;
import com.ticketing.config.MinioProperties;
import com.ticketing.ticket.mapper.TicketMapper;
import com.ticketing.ticket.service.TicketPolicy;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.ticketing.support.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AttachmentServiceTest {
    AttachmentMapper mapper; TicketMapper tickets; ObjectStorage storage; AttachmentService service; AttachmentRow image;
    @BeforeEach void setup() {
        mapper=mock(AttachmentMapper.class); tickets=mock(TicketMapper.class); storage=mock(ObjectStorage.class);
        service=new AttachmentService(mapper,tickets,new TicketPolicy(),storage,new ImageValidator(),
                new MinioProperties("http://localhost:9000","test","test-secret","images"),transactions());
        image=new AttachmentRow(); image.id=1L; image.uploaderId=1L; image.storageState="READY";
        image.expiresAt=LocalDateTime.now(ZoneOffset.UTC).plusHours(1); image.bucketName="images"; image.objectKey="key";
        image.mediaType="image/png";
    }
    @Test void cannotBindSomeoneElsesUpload() {
        when(mapper.lockToken("token")).thenReturn(image);
        assertEquals(404,assertThrows(BusinessException.class,() -> service.bind(user(3,"attachment:bind"),10L,100L,List.of("token"))).status());
        verifyNoInteractions(storage);
    }
    @Test void expiredImageCannotBeBound() {
        image.expiresAt=LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        when(mapper.lockToken("token")).thenReturn(image);
        assertThrows(BusinessException.class,() -> service.bind(user(1,"attachment:bind"),10L,100L,List.of("token")));
    }
    @Test void maximumAppliesToExistingAndNewImages() {
        when(mapper.countBound(10L)).thenReturn(5);
        assertThrows(BusinessException.class,() -> service.bind(user(1,"attachment:bind"),10L,100L,List.of("token")));
    }
    @Test void transferredSupportCannotReadOldImage() {
        image.ticketId=10L; image.storageState="BOUND"; when(mapper.find(1L)).thenReturn(image);
        var ticket=ticket(); ticket.assigneeId=3L; when(tickets.find(10L)).thenReturn(ticket);
        assertEquals(404,assertThrows(BusinessException.class,() -> service.read(user(2,"attachment:read","ticket:read:assigned"),1L)).status());
        verifyNoInteractions(storage);
    }
    @Test void boundImageUsesCurrentTicketAccess() {
        image.ticketId=10L; image.storageState="BOUND"; when(mapper.find(1L)).thenReturn(image);
        when(tickets.find(10L)).thenReturn(ticket()); when(storage.read("images","key")).thenReturn(new byte[]{1});
        assertEquals("image/png",service.read(user(1,"attachment:read","ticket:read:own"),1L).mediaType());
    }
    @Test void cleanupOnlyDeletesClaimedExpiredRows() {
        when(mapper.expired()).thenReturn(List.of(image)); when(mapper.claimExpired(1L)).thenReturn(1);
        service.cleanup(); verify(storage).delete("images","key"); verify(mapper).deletePending(1L);
    }
    @Test void failedStorageCleanupPreservesMetadataForRetry() {
        when(mapper.expired()).thenReturn(List.of(image)); when(mapper.claimExpired(1L)).thenReturn(1);
        doThrow(new RuntimeException("unavailable")).when(storage).delete("images","key");
        service.cleanup(); verify(mapper,never()).deletePending(1L);
    }
}
