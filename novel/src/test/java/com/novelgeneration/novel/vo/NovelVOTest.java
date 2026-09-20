package com.novelgeneration.novel.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NovelVOTest {

    @Test
    void ignoresAiGeneratedStringNovelIdAndKeepsChapterText() throws Exception {
        String json = """
                {
                  "novelId": "novel_001",
                  "novelTitle": "星海归途",
                  "novelText": [
                    {
                      "chapterNum": 1,
                      "chapterTitle": "河底古器",
                      "chapterSummary": "章节概要",
                      "chapterText": "章节正文"
                    }
                  ]
                }
                """;

        NovelVO novelVO = new ObjectMapper().readValue(json, NovelVO.class);

        assertNull(novelVO.getNovelId());
        assertEquals("星海归途", novelVO.getNovelTitle());
        assertEquals(1, novelVO.getNovelText().size());
        assertEquals("河底古器", novelVO.getNovelText().get(0).getChapterTitle());
    }
}
