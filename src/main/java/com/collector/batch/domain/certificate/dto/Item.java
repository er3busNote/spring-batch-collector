package com.collector.batch.domain.certificate.dto;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "body")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Item {

    @XmlElement(name = "career")
    private String career;

    @XmlElement(name = "engJmNm")
    private String engJmNm;

    @XmlElement(name = "hist")
    private String hist;

    @XmlElement(name = "implNm")
    private String implNm;

    @XmlElement(name = "instiNm")
    private String instiNm;

    @XmlElement(name = "jmCd")
    private String jmCd;

    @XmlElement(name = "jmNm")
    private String jmNm;

    @XmlElement(name = "job")
    private String job;

    @XmlElement(name = "mdobligFldNm")
    private String mdobligFldNm;

    @XmlElement(name = "seriesCd")
    private String seriesCd;

    @XmlElement(name = "seriesNm")
    private String seriesNm;

    @XmlElement(name = "summary")
    private String summary;

    @XmlElement(name = "trend")
    private String trend;
}
