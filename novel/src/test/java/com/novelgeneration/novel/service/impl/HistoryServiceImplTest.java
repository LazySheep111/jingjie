package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.entity.NovelInfo;
import com.novelgeneration.novel.mapper.HistoryMapper;
import com.novelgeneration.novel.vo.HistoryOutlineRecordVO;
import com.novelgeneration.novel.vo.NovelHistoryDetailVO;
import com.novelgeneration.novel.vo.NovelHistoryItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryServiceImplTest {

    @Mock
    private HistoryMapper historyMapper;

    @InjectMocks
    private HistoryServiceImpl historyService;

    @Test
    void listHistoryReturnsRecordsAndTotal() {
        NovelHistoryItemVO item = new NovelHistoryItemVO();
        item.setNovelId(43L);
        item.setNovelTitle("星海归途");
        item.setTotalChapter(20);
        item.setOverallPlot("寻找归途");
        item.setStatus("FULL_GENERATED");
        item.setCreateTime(LocalDateTime.of(2026, 7, 21, 10, 30));

        when(historyMapper.selectHistoryList(0, 10)).thenReturn(List.of(item));
        when(historyMapper.countHistory()).thenReturn(1L);

        NovelHistoryDetailVO.PageResult<NovelHistoryItemVO> result = historyService.listHistory(1, 10);

        assertEquals(1L, result.getTotal());
        assertEquals("星海归途", result.getRecords().get(0).getNovelTitle());
        assertEquals(20, result.getRecords().get(0).getTotalChapter());
    }

    @Test
    void getOutlineBuildsFormDataAndOutlineForFrontendRestore() {
        NovelInfo novelInfo = new NovelInfo();
        novelInfo.setId(43L);
        novelInfo.setNovelTitle("星海归途");
        novelInfo.setCategory("科幻冒险");
        novelInfo.setNovelLength("长篇");
        novelInfo.setEndingType("开放式结局");
        novelInfo.setWritingStyle("热血");
        novelInfo.setTargetAudience("18-35岁");
        novelInfo.setProtagonist("林澈");
        novelInfo.setRoleList("[\"苏晚\"]");
        novelInfo.setBackground("星际时代");
        novelInfo.setWorldRule("晶核跃迁");
        novelInfo.setTheme("寻找归属");
        novelInfo.setTriggerEvent("发现星舰");
        novelInfo.setForeshadowCount(5);
        novelInfo.setNarrativeView("第三人称");
        novelInfo.setAvoidContent("避免血腥");

        HistoryOutlineRecordVO outlineRecord = new HistoryOutlineRecordVO();
        outlineRecord.setNovelId(43L);
        outlineRecord.setNovelTitle("星海归途");
        outlineRecord.setOverallPlot("寻找归途");
        outlineRecord.setForeshadowListJson("[\"伏笔一\"]");
        outlineRecord.setChapterListJson("[{\"chapterNum\":1,\"chapterTitle\":\"河底古器\",\"chapterSummary\":\"发现古器\",\"wordCount\":2500}]");

        when(historyMapper.selectNovelInfo(43L)).thenReturn(novelInfo);
        when(historyMapper.selectOutlineRecord(43L)).thenReturn(outlineRecord);
        when(historyMapper.countChapters(43L)).thenReturn(0);

        NovelHistoryDetailVO detail = historyService.getOutline(43L);

        assertEquals(43L, detail.getNovelId());
        assertEquals("科幻冒险", detail.getFormData().getCategory());
        assertEquals("苏晚", detail.getFormData().getRoleList().get(0));
        assertEquals("寻找归途", detail.getOutline().getOverallPlot());
        assertEquals(1, detail.getOutline().getChapterList().get(0).getChapterNum());
        assertFalse(detail.getFullGenerated());
    }

    @Test
    void getOutlineMarksFullGeneratedWhenChaptersExist() {
        NovelInfo novelInfo = new NovelInfo();
        novelInfo.setId(44L);
        novelInfo.setNovelTitle("作品44");

        HistoryOutlineRecordVO outlineRecord = new HistoryOutlineRecordVO();
        outlineRecord.setNovelId(44L);
        outlineRecord.setNovelTitle("作品44");

        when(historyMapper.selectNovelInfo(44L)).thenReturn(novelInfo);
        when(historyMapper.selectOutlineRecord(44L)).thenReturn(outlineRecord);
        when(historyMapper.countChapters(44L)).thenReturn(3);

        NovelHistoryDetailVO detail = historyService.getOutline(44L);

        assertEquals("FULL_GENERATED", detail.getStatus());
        assertTrue(detail.getFullGenerated());
    }
}
