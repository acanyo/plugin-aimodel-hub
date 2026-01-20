package com.xhhao.aimodelhub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型列表项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelListItem {
    private String label;
    private String value;
    private long created;
}
