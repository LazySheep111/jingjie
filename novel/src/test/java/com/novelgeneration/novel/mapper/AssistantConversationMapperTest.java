package com.novelgeneration.novel.mapper;

import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantConversationMapperTest {

    @Test
    void summaryUpdateKeepsExistingTitleWhenSummaryDoesNotProvideOne() throws Exception {
        Update update = AssistantConversationMapper.class
                .getMethod("updateSummary", com.novelgeneration.novel.vo.AssistantConversationVO.class)
                .getAnnotation(Update.class);

        assertTrue(String.join(" ", update.value()).contains("COALESCE(#{title}, title)"));
    }
}
