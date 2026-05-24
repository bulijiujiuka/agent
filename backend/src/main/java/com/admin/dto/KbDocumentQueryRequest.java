package com.admin.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KbDocumentQueryRequest extends PageRequest {

    private String documentName;

    private String category;

    private String parseStatus;
}
