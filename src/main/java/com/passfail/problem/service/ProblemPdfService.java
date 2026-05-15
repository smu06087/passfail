package com.passfail.problem.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.properties.TextAlignment;
import com.passfail.problem.dto.ProblemResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ProblemPdfService {

    public byte[] generateProblemPdf(ProblemResponse problem) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // 한글 폰트 설정 (Windows 기준 맑은 고딕 사용)
            PdfFont font = null;
            try {
                String fontPath = "C:/Windows/Fonts/malgun.ttf";
                font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                document.setFont(font);
            } catch (Exception e) {
                // 시스템 폰트 로드 실패 시 기본 폰트 사용 (한글 깨질 수 있음)
                System.err.println("Font loading failed: " + e.getMessage());
            }

            // 제목
            String title = problem.getTitle() != null ? problem.getTitle() : "Problem";
            document.add(new Paragraph(title)
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // 문제 정보
            document.add(new Paragraph("난이도: " + (problem.getDifficulty() != null ? problem.getDifficulty() : "-"))
                    .setFontSize(12)
                    .setMarginBottom(5));
            document.add(new Paragraph("시간 제한: " + problem.getTimeLimitMs() + "ms")
                    .setFontSize(12)
                    .setMarginBottom(5));
            document.add(new Paragraph("메모리 제한: " + problem.getMemoryLimitMb() + "MB")
                    .setFontSize(12)
                    .setMarginBottom(20));

            // 문제 설명
            document.add(new Paragraph(" [문제 설명] ")
                    .setFontSize(18)
                    .setBold()
                    .setMarginBottom(10));
            
            String description = problem.getDescription();
            if (description != null && !description.isEmpty()) {
                String[] lines = description.split("\n");
                for (String line : lines) {
                    if (line != null) {
                        document.add(new Paragraph(line.trim()).setFontSize(11));
                    }
                }
            } else {
                document.add(new Paragraph("설명이 없습니다.").setFontSize(11));
            }

            document.close();
        } catch (Exception e) {
            throw new IOException("PDF generation failed: " + e.getMessage(), e);
        }
        return baos.toByteArray();
    }
}
