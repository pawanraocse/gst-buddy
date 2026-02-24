package com.learning.backendservice.domain.ledger;

import com.learning.backendservice.domain.rule37.CalculationSummary;
import com.learning.backendservice.domain.rule37.LedgerResult;
import com.learning.backendservice.domain.rule37.Rule37InterestCalculator;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Processes one ledger file: parse → Rule 37 calculate → LedgerResult.
 */
@Component
public class Rule37LedgerFileProcessor implements LedgerFileProcessor {

    private final LedgerParser ledgerParser;
    private final Rule37InterestCalculator calculator;

    public Rule37LedgerFileProcessor(LedgerParser ledgerParser, Rule37InterestCalculator calculator) {
        this.ledgerParser = ledgerParser;
        this.calculator = calculator;
    }

    @Override
    public LedgerResult process(InputStream inputStream, String filename, LocalDate asOnDate) {
        return processWithLedgerCount(inputStream, filename, asOnDate).result();
    }

    @Override
    public ProcessingOutcome processWithLedgerCount(InputStream inputStream, String filename, LocalDate asOnDate) {
        String ledgerName = getFileNameWithoutExtension(filename);
        List<LedgerEntry> entries = ledgerParser.parse(inputStream, filename);
        int ledgerCount = ledgerParser.countLedgers(entries);
        CalculationSummary summary = calculator.calculate(entries, asOnDate);
        LedgerResult result = LedgerResult.builder()
                .ledgerName(ledgerName)
                .summary(summary)
                .build();
        return new ProcessingOutcome(result, ledgerCount);
    }

    private static String getFileNameWithoutExtension(String filename) {
        if (filename == null) return "Unknown";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
