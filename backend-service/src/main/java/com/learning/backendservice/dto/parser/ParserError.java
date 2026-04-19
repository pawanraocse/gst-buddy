package com.learning.backendservice.dto.parser;

import java.util.List;

public record ParserError(
    String status,
    String error_code,
    String error_message,
    List<String> suggestions
) {}
