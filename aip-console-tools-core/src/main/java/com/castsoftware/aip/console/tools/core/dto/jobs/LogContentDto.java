package com.castsoftware.aip.console.tools.core.dto.jobs;

import java.util.Set;

public class LogContentDto {
    private boolean endOfFile;
    private int nbLines;
    private int startOffset;
    private Set<LogLine> lines;

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

    public Set<LogLine> getLines() {
        return lines;
    }

    public void setLines(Set<LogLine> lines) {
        this.lines = lines;
    }
}
