package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.engine.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Builds the raw {@code List<MultipartFile>} input for {@code Rule37AuditRule} from the context.
 *
 * <p>Rule 37 still receives raw files because its domain logic ({@code LedgerFileProcessor})
 * parses the Excel bytes directly. The raw file references are stored in
 * {@link AuditDocument#extractedFields()} under key {@code "rawFile"} by {@code DocumentTypeResolver}.
 */
@Component
public class Rule37InputResolver implements InputResolver<List<MultipartFile>> {

    @Override
    public String getRuleId() {
        return "RULE_37_ITC_REVERSAL";
    }

    @Override
    public List<MultipartFile> resolve(AuditContext context) {
        List<MultipartFile> files = context.getDocuments(DocumentType.PURCHASE_LEDGER).stream()
                .map(doc -> (MultipartFile) doc.extractedFields().get("rawFile"))
                .filter(f -> f != null)
                .toList();

        if (files.isEmpty()) {
            throw new IllegalStateException(
                    "Rule37InputResolver: no PURCHASE_LEDGER documents with 'rawFile' found in context");
        }
        return files;
    }
}
