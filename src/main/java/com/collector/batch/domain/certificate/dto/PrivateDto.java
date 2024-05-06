package com.collector.batch.domain.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateDto {

    private String createDate;

    private String status;

    private String agency;

    private String createNum;

    private String name;

    private String grade;

    private String reception;

    private String summary;

    private String description;

    private String authorizedStatus;

    private String authorizedYn;

    private String coOperation;
}
