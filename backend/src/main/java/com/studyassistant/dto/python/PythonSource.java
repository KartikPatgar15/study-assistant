package com.studyassistant.dto.python;

import lombok.Data;

@Data
public class PythonSource {

    private int chunkId;
    private String title;
    private int startPage;
    private int endPage;
}