package com.notebooklm.service;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MarkdownChunker {

    private static final int TARGET_SIZE = 1000;
    private static final int OVERLAP = 100;
    
    // Simple regex for headers: # Title
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$");

    public record ChunkResult(String text, String headingPath, int startChar, int endChar, int tokenEstimate) {}

    public List<ChunkResult> chunk(String markdown) {
        List<ChunkResult> chunks = new ArrayList<>();
        String[] lines = markdown.split("\n");
        
        List<String> currentHeadingStack = new ArrayList<>();
        StringBuilder currentSection = new StringBuilder();
        int sectionStartChar = 0;
        int currentCharCount = 0; // Global char index
        
        // This is a naive line-by-line processor.
        // Better: Identify sections, then chunk sections.
        
        // Let's accumulate lines until a header is found.
        
        for (String line : lines) {
            Matcher m = HEADER_PATTERN.matcher(line);
            if (m.matches()) {
                // New header found.
                // 1. Process pending section
                if (!currentSection.isEmpty()) {
                    processSection(chunks, currentSection.toString(), formatHeading(currentHeadingStack), sectionStartChar);
                    currentSection.setLength(0);
                }
                
                // 2. Update stack
                int level = m.group(1).length();
                String title = m.group(2).trim();
                updateHeadingStack(currentHeadingStack, level, title);
                
                // 3. Start new section with the header line? 
                // Usually header is part of the following text.
                // We add the header to the section text so it provides context in the chunk.
                currentSection.append(line).append("\n");
                sectionStartChar = currentCharCount; 
            } else {
                if (currentSection.isEmpty()) {
                    sectionStartChar = currentCharCount;
                }
                currentSection.append(line).append("\n");
            }
            currentCharCount += line.length() + 1; // +1 for newline
        }
        
        // Last section
        if (!currentSection.isEmpty()) {
            processSection(chunks, currentSection.toString(), formatHeading(currentHeadingStack), sectionStartChar);
        }

        return chunks;
    }

    private void processSection(List<ChunkResult> chunks, String sectionText, String headingPath, int startChar) {
        if (sectionText.length() <= TARGET_SIZE) {
            chunks.add(new ChunkResult(sectionText.trim(), headingPath, startChar, startChar + sectionText.length(), sectionText.length() / 4));
        } else {
            // Split larger section
            // Simple sliding window by chars (approx tokens)
            int length = sectionText.length();
            int pos = 0;
            while (pos < length) {
                int end = Math.min(pos + TARGET_SIZE, length);
                // Try to find a space near end to break
                if (end < length) {
                    int lastSpace = sectionText.lastIndexOf(' ', end);
                    if (lastSpace > pos + TARGET_SIZE / 2) {
                        end = lastSpace;
                    }
                }
                
                String subText = sectionText.substring(pos, end);
                chunks.add(new ChunkResult(subText.trim(), headingPath, startChar + pos, startChar + end, subText.length() / 4));
                
                pos += (TARGET_SIZE - OVERLAP);
                if (pos >= length) break; 
                // Adjust pos to not be stuck if overlap > step?
                // Step is TARGET_SIZE - OVERLAP. 
                // If TARGET=1000, OVERLAP=100, step=900.
            }
        }
    }

    private void updateHeadingStack(List<String> stack, int level, String title) {
        // If level is deeper than stack size, append.
        // If level is shallower or equal, pop until level-1.
        // H1 -> level 1. Stack size 0 -> add.
        // H2 -> level 2. Stack size 1 -> add.
        // H1 -> level 1. Stack size 2 -> pop to 0, add.
        
        // This logic assumes well-formed headers.
        if (level > stack.size()) {
            stack.add(title);
        } else {
            // Pop until stack size is level - 1
            while (stack.size() >= level) {
                stack.remove(stack.size() - 1);
            }
            stack.add(title);
        }
    }
    
    private String formatHeading(List<String> stack) {
        return String.join(" > ", stack);
    }
}
