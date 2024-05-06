package com.collector.batch.domain.certificate.service;

import com.collector.batch.domain.certificate.dto.Item;
import com.collector.batch.domain.certificate.dto.NationalProDto;
import com.collector.batch.domain.certificate.dto.NationalTechDto;
import com.collector.batch.domain.certificate.dto.Response;
import com.collector.batch.domain.certificate.mapper.CertificateMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CertificateService {

    private static final String API_URL = "http://openapi.q-net.or.kr/api/service/rest/InquiryQualInfo/getList";
    private static final String API_KEY = "";
    private static final String SERVICE_CODE = "04"; // (계열코드) 01:기술사, 02:기능장, 03:기사, 04:기능사

    @Autowired
    CertificateMapper certificateMapper;

    public List<Item> getNationalTechnical() {
        MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
        param.add("serviceKey", API_KEY);
        param.add("seriesCd", SERVICE_CODE);

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10 * 1000) //miliseconds
                .responseTimeout(Duration.ofMillis(60 * 1000L))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(3 * 60 * 1000L, TimeUnit.MILLISECONDS))  //sec
                            .addHandlerLast(new WriteTimeoutHandler(60 * 1000L, TimeUnit.MILLISECONDS)) //sec
                );
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1)) // to unlimited memory size
                .build();
        WebClient webClient = WebClient.builder()
                .uriBuilderFactory(factory)	// 추가
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .build();
        // URI를 생성하여 WebClient로 GET 요청 보내기
        String apiUrl = UriComponentsBuilder.fromUriString(API_URL)
                .queryParams(param)
                .build()
                .toUriString();
        log.info(apiUrl);
        Mono<String> response = webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(String.class);
        String xmlData = response.block(); // 동기식 호출

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Response.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(Objects.requireNonNull(xmlData));
            Response buildingInfoResponse = (Response) unmarshaller.unmarshal(reader);
            List<Item> itemList = buildingInfoResponse.getBody().getItems();
            log.info("item = {}", itemList.get(0));
            return itemList;
        } catch (NullPointerException | JAXBException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void saveNationalTechnical(Item item) {
        log.info("item = {}", item.toString());
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("name", item.getJmNm());
        resultMap.put("type", item.getSeriesNm());
        resultMap.put("agency", item.getInstiNm());
        certificateMapper.insertNationalTechnical(resultMap);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void saveNationalTechnical(NationalTechDto nationalTechDto) {
        log.info("item = {}", nationalTechDto.toString());
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("name", nationalTechDto.getName());
        resultMap.put("type", nationalTechDto.getType());
        resultMap.put("agency", nationalTechDto.getAgency());
        certificateMapper.insertNationalTechnical(resultMap);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void saveNationalProfessional(NationalProDto nationalProDto) {
        String name = nationalProDto.getName();
        String agency = nationalProDto.getAgency();
        String gradeInfo = nationalProDto.getGrade();
        ArrayList<String> grades = parseString(gradeInfo);
        for (String grade : grades) {
            log.info("name = {}, agency = {}, grade = {}", name, agency, grade);
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("name", name);
            resultMap.put("grade", grade);
            resultMap.put("type", "국가전문자격");
            resultMap.put("agency", agency);
            certificateMapper.insertNationalProfessional(resultMap);
        }
    }

    public static ArrayList<String> parseString(String gradeInfo) {
        if(gradeInfo.contains(" : ")) {
            return combineValues(gradeInfo);
        }
        return splitString(gradeInfo);
    }

    public static ArrayList<String> splitString(String input) {
        ArrayList<String> items = new ArrayList<>();
        Pattern pattern = Pattern.compile("[\\w|가-힣|\\s]+\\(.*?\\)|[\\w|가-힣|\\s]+");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String match = matcher.group().trim();
            if (match.contains("(")) {
                items.addAll(splitParentheses(match));
            } else {
                items.add(match);
            }
        }
        return items;
    }

    public static ArrayList<String> splitParentheses(String input) {
        ArrayList<String> items = new ArrayList<>();
        String[] parts = input.split("\\(");
        String prefix = parts[0];
        String[] arguments = parts[1].replace(")", "").split(", ");
        for (String arg : arguments) {
            items.add(prefix + "(" + arg + ")");
        }
        return items;
    }

    public static ArrayList<String> combineValues(String input) {
        ArrayList<String> combinedValues = new ArrayList<>();
        Pattern pattern = Pattern.compile("(분야|등급|구분)\\s*:\\s*([^:]+)\\s*(?=(?:분야|등급|구분)|$)");
        Matcher matcher = pattern.matcher(input);
        ArrayList<ArrayList<String>> allValues = new ArrayList<>();

        // 필드별 값들을 모두 저장합니다.
        while (matcher.find()) {
            String fieldValues = matcher.group(2).trim();
            String[] values = fieldValues.split(", ");
            ArrayList<String> fieldList = new ArrayList<>();
            for (String value : values) {
                fieldList.add(value);
            }
            allValues.add(fieldList);
        }

        // 재귀적으로 모든 경우의 수를 조합하여 출력합니다.
        combineValuesRecursive(allValues, 0, new ArrayList<>(), combinedValues);
        return combinedValues;
    }

    public static void combineValuesRecursive(ArrayList<ArrayList<String>> allValues, int currentIndex, ArrayList<String> currentCombination, ArrayList<String> combinedValues) {
        if (currentIndex == allValues.size()) {
            combinedValues.add(String.join(" ", currentCombination));
            return;
        }

        ArrayList<String> valuesForCurrentField = allValues.get(currentIndex);
        for (String value : valuesForCurrentField) {
            ArrayList<String> newCombination = new ArrayList<>(currentCombination);
            newCombination.add(value);
            combineValuesRecursive(allValues, currentIndex + 1, newCombination, combinedValues);
        }
    }
}
