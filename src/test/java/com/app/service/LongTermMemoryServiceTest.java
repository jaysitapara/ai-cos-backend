package com.app.service;

import com.app.enums.MemoryCategory;
import com.app.model.memory.MemoryItemModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LongTermMemoryServiceTest {

    private LongTermMemoryService service;

    @BeforeEach
    void setUp() {
        service = new LongTermMemoryService();
    }

    @Test
    void testSaveSearchAndDeleteMemory() {
        MemoryItemModel mem = service.saveMemory("user-100", MemoryCategory.PROJECT, "Project Baseline", "Core project context", List.of("project"));

        assertNotNull(mem);
        assertEquals("user-100", mem.getUserId());
        assertEquals(MemoryCategory.PROJECT, mem.getCategory());

        List<MemoryItemModel> found = service.searchMemories("user-100", MemoryCategory.PROJECT);
        assertEquals(1, found.size());

        Map<String, Object> context = service.getUserContext("user-100");
        assertNotNull(context);

        boolean deleted = service.deleteMemory(mem.getMemoryId());
        assertTrue(deleted);
    }
}
