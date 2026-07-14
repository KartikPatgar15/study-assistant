package com.studyassistant.dto.python;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PythonChatRequest {

    private String uploadId;
    private String question;
}