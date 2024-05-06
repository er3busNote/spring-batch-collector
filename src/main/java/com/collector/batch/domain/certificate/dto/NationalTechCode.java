package com.collector.batch.domain.certificate.dto;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum NationalTechCode {

    PROFESSIONAL_ENGINEER("01", "기술사"),
    MASTER_CRAFTSMAN("02", "기능장"),
    ENGINEER("03", "기사"),
    CRAFTSMAN("04", "기능사");

    private static final Map<String, String> CODE_MAP = Collections.unmodifiableMap(Stream.of(values()).collect(Collectors.toMap(NationalTechCode::getCode, NationalTechCode::name)));

    private final String code;
    private final String name;

    NationalTechCode(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static NationalTechCode of(final String code) {
        return NationalTechCode.valueOf(CODE_MAP.get(code));
    }
}
