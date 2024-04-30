package com.collector.batch.domain.certificate.dto;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "body")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Body {

    @XmlElementWrapper(name = "items")
    @XmlElement(name = "item")
    private List<Item> items;
}
