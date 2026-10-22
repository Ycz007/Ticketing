package com.ticketing;

import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MyBatisMappingTest {
    @Test void mapperXmlLoadsWithoutDatabase() throws Exception {
        var configuration = new Configuration();
        for (String path : new String[]{"mapper/auth/AccountMapper.xml", "mapper/notification/NotificationMapper.xml",
                "mapper/ticket/TicketMapper.xml", "mapper/attachment/AttachmentMapper.xml"}) {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(input);
                new XMLMapperBuilder(input, configuration, path, configuration.getSqlFragments()).parse();
            }
        }
        assertTrue(configuration.hasStatement("com.ticketing.notification.mapper.NotificationMapper.countUnread"));
        assertTrue(configuration.hasStatement("com.ticketing.auth.mapper.AccountMapper.findByUsername"));
    }
}
