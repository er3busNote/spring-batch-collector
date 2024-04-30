package com.collector.batch.domain.certificate.dto;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "response")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Response {

    @XmlElement(name = "header")
    private Header header;

    @XmlElement(name = "body")
    private Body body;
}
