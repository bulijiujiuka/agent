package com.admin.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SemanticChunkService {
    public record ChunkBlock(String text,String sectionTitle, String titlePath){}

    private static final Pattern TITLE = Pattern.compile(
            "^(#{1,6}\\s+.+|[一二三四五六七八九十]+[、.．]\\s*.+|\\d+(\\.\\d+)+\\s+.+)$"
    );

    public static List<ChunkBlock> split(String text, int maxChars, int overlap){
        List<ChunkBlock> result = new ArrayList<>();
        List<String> path = new ArrayList<>();
        StringBuilder section = new StringBuilder();
        String sectionTitle = "";

        for (String line : text.replace("\r\n", "\n").split("\n")) {
            String trimmed = line.trim();
            if (istitle(trimmed)){
                flushSection(result,section.toString(),
                        sectionTitle,String.join(" > ", path),maxChars, overlap);
                section.setLength(0);
                sectionTitle = cleanTitle(trimmed);
                path = update(path, sectionTitle);
            }
            section.append(line).append("\n");
        }
        flushSection(result,section.toString(),sectionTitle,
                String.join(" > ", path),maxChars, overlap);
        return result;
    }

    private static boolean istitle(String line) {
        return !line.isBlank() && line.length() <=80 && TITLE.matcher(line).matches();
    }

    private static String cleanTitle(String line) {
        return line.replaceFirst("^#{1,6}\\s*", "").trim();
    }

    private static List<String> update(List<String> oldpath, String title) {
        List<String> next = new ArrayList<>(oldpath);
        if (next.size() >= 3) next.remove(next.size()-1);
        next.add(title);
        return next;
    }

    private static void flushSection(List<ChunkBlock> out, String section, String title,
                                     String path, int maxChars, int overlap){
        if (section == null || section.isBlank()) return;
        String[] paragraghs = section.trim().split("\\n\\s*\\n");
        StringBuilder buf = new StringBuilder();

        for (String p : paragraghs) {
            if (p.length() > maxChars){
                flushBuffer(out, buf, title, path);
                spiltLongParagraph(out, p, title, path, maxChars, overlap);
            }else if (buf.length() + p.length() +2 > maxChars){
                flushBuffer(out, buf, title, path);
                buf.append(p);
            }else {
                if (!buf.isEmpty()) buf.append("\n\n");
                buf.append(p);
            }
        }
        flushBuffer(out, buf, title, path);
    }

    private static void flushBuffer(List<ChunkBlock> out, StringBuilder buf, String title,
                                     String path){
        if (!buf.isEmpty()){
           out.add(new ChunkBlock(buf.toString().trim(), title, path));
           buf.setLength(0);
        }
    }

    private static void spiltLongParagraph(List<ChunkBlock> out, String paragraph, String title,
                                           String path, int maxChars, int overlap){
        int start = 0;

        while (start < paragraph.length()) {
            int end = Math.min(start+maxChars, paragraph.length());
            out.add(new ChunkBlock(paragraph.substring(start, end), title, path));
            if (end == paragraph.length()) break;
            start = Math.max(end-overlap, start+1);
        }
    }
}
