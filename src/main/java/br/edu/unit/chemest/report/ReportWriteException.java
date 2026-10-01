package br.edu.unit.chemest.report;
import java.io.IOException;
public class ReportWriteException extends IOException {
    public ReportWriteException(String message,IOException cause) { super(message,cause); }
}
