package com.studyassistant.dto.python;

import lombok.Data;

import java.util.List;

@Data
public class PythonChatResponse {

    private String answer;

    private List<PythonSource> sources;

    private List<String> images;
}