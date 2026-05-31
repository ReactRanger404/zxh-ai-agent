package org.example.aiagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "你好.pdf";
        String content = "加油！@";
        String result = tool.generatePDF(fileName, content);
        assertNotNull(result);
    }
}