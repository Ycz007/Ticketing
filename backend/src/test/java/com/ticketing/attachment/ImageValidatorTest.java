package com.ticketing.attachment;

import com.ticketing.attachment.service.ImageValidator;
import com.ticketing.common.api.BusinessException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImageValidatorTest {
    final ImageValidator validator=new ImageValidator();
    @Test void validPngUsesRealImageFormatAndDimensions() throws Exception {
        var bytes=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(12,8,BufferedImage.TYPE_INT_RGB),"png",bytes);
        var info=validator.validate(bytes.toByteArray());
        assertEquals("image/png",info.mime()); assertEquals(12,info.width()); assertEquals(8,info.height());
    }
    @Test void fakeImageIsRejected() {
        assertThrows(BusinessException.class,() -> validator.validate("<svg onload='alert(1)'/>".getBytes()));
    }
    @Test void emptyAndOversizeAreRejected() {
        assertThrows(BusinessException.class,() -> validator.validate(new byte[0]));
        assertThrows(BusinessException.class,() -> validator.validate(new byte[5*1024*1024+1]));
    }
    @Test void gifIsNotInAllowedFormats() throws Exception {
        var out=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB),"gif",out);
        assertThrows(BusinessException.class,() -> validator.validate(out.toByteArray()));
    }
}
