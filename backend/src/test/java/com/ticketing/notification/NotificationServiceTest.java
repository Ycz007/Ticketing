package com.ticketing.notification;

import com.ticketing.common.api.BusinessException;
import com.ticketing.notification.mapper.NotificationMapper;
import com.ticketing.notification.service.NotificationService;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    @Test void noSelfNotificationAndRecipientsAreDeduplicated() {
        var mapper=mock(NotificationMapper.class); var service=new NotificationService(mapper);
        service.publish(10L,100L,"NEW_MESSAGE",1L,Arrays.asList(1L,2L,2L,null,3L));
        verify(mapper).insert(2L,10L,100L,"NEW_MESSAGE"); verify(mapper).insert(3L,10L,100L,"NEW_MESSAGE");
        verifyNoMoreInteractions(mapper);
    }
    @Test void markingReadRequiresMatchingRecipientAndVisibility() {
        var mapper=mock(NotificationMapper.class); var service=new NotificationService(mapper);
        assertEquals(404,assertThrows(BusinessException.class,() -> service.read(1L,99L)).status());
        verify(mapper).markRead(1L,99L);
    }
    @Test void transferInvalidatesOldHandlersNotificationsOnly() {
        var mapper=mock(NotificationMapper.class); var service=new NotificationService(mapper);
        service.invalidate(10L,2L); verify(mapper).invalidate(10L,2L);
    }
}
