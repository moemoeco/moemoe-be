package com.moemoe.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.springframework.test.web.servlet.ResultActions;

import java.io.UnsupportedEncodingException;

@UtilityClass
public class ResponseConvertUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T convertResponseToClass(ResultActions resultActions, Class<T> type) {
        String content = getContent(resultActions);
        try {
            return objectMapper.readValue(content, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getContent(ResultActions resultActions) {
        String content = null;
        try {
            content = resultActions.andReturn()
                    .getResponse()
                    .getContentAsString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return content;
    }
}
