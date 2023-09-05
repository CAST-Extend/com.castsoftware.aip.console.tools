package com.castsoftware.aip.console.tools.core.dto.jobs;

import java.util.List;

public class LogContentDto {
    private boolean endOfFile;
    private int nbLines;
    private int startOffset;
    private List<LogLine> lines;

    public boolean isEndOfFile() {
        return endOfFile;
    }

    public void setEndOfFile(boolean endOfFile) {
        this.endOfFile = endOfFile;
    }

    public int getNbLines() {
        return nbLines;
    }

    public void setNbLines(int nbLines) {
        this.nbLines = nbLines;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public List<LogLine> getLines() {
        return lines;
    }

    public void setLines(List<LogLine> lines) {
        this.lines = lines;
    }
}
