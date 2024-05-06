package com.collector.batch.domain.certificate.mapper;

import com.collector.batch.domain.certificate.dto.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Mapper
@Repository
public interface CertificateMapper {

    void insertNationalTechnical(@Param("p") Map<String, String> resultMap);

    void insertNationalProfessional(@Param("p") Map<String, String> resultMap);

    void insertPrivate(@Param("p") Map<String, String> resultMap);
}
