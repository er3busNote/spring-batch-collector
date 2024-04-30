package com.collector.batch.domain.certificate.dto;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "header")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Header {

    @XmlElement(name = "resultCode")
    private String resultCode;

    @XmlElement(name = "resultMsg")
    private String resultMsg;
}
