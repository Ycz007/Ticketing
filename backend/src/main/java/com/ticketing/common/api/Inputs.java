package com.ticketing.common.api;

public final class Inputs {
    private Inputs() { }
    public static String text(String text) {
        if (text == null || text.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)))
            throw BusinessException.invalid("文字不能为空或仅包含空白");
        if (text.codePointCount(0, text.length()) > 10000)
            throw BusinessException.invalid("文字不能超过10000个字符");
        return text;
    }
}
