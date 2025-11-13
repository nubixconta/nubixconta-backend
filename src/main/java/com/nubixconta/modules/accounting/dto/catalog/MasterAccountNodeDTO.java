package com.nubixconta.modules.accounting.dto.catalog;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class MasterAccountNodeDTO {
    private Integer id; // ID de la tabla 'account'
    private String name;
    private String code;
    private boolean isPostable;
    private List<MasterAccountNodeDTO> children = new ArrayList<>();
}