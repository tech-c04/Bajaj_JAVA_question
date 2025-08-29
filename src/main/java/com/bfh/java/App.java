package com.bfh.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class App implements ApplicationRunner {

    private static final String GENERATE_WEBHOOK_URL =
            "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
    private static final String TEST_WEBHOOK_URL =
            "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public App(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        String name = "Chinmayee Chaudhary";
        String regNo = "22BDS0254";  
        String email = "chinmayee.chaudhary2022@vitstudent.ac.in";
      
        Map<String,String> generateBody = new HashMap<>();
        generateBody.put("name", name);
        generateBody.put("regNo", regNo);
        generateBody.put("email", email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String,String>> req = new HttpEntity<>(generateBody, headers);

        System.out.println("Calling generateWebhook API...");
        ResponseEntity<String> genResp = restTemplate.postForEntity(GENERATE_WEBHOOK_URL, req, String.class);

        if (!genResp.getStatusCode().is2xxSuccessful()) {
            System.err.println("generateWebhook failed: " + genResp.getStatusCode());
            System.err.println("Body: " + genResp.getBody());
            return;
        }

        JsonNode genJson = objectMapper.readTree(genResp.getBody());
        String webhookUrl = genJson.path("webhook").asText("");
        String accessToken = genJson.path("accessToken").asText("");

        System.out.println("Received webhook: " + webhookUrl);
        System.out.println("Received accessToken: " + (accessToken.isEmpty() ? "(empty)" : "(present)"));

        if (accessToken.isEmpty()) {
            System.err.println("No accessToken received. Cannot continue.");
            return;
        }

        int lastTwo = extractLastTwoDigits(regNo);
        boolean isEven = (lastTwo % 2 == 0);

        String finalQuery;
            finalQuery = """
                    SELECT 
                        e1.EMP_ID,
                        e1.FIRST_NAME,
                        e1.LAST_NAME,
                        d.DEPARTMENT_NAME,
                        COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT
                    FROM EMPLOYEE e1
                    JOIN DEPARTMENT d 
                        ON e1.DEPARTMENT = d.DEPARTMENT_ID
                    LEFT JOIN EMPLOYEE e2
                        ON e1.DEPARTMENT = e2.DEPARTMENT
                       AND e2.DOB > e1.DOB
                    GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME
                    ORDER BY e1.EMP_ID DESC;
                    """;

        Map<String,String> submitBody = new HashMap<>();
        submitBody.put("finalQuery", finalQuery);

        HttpHeaders submitHeaders = new HttpHeaders();
        submitHeaders.setContentType(MediaType.APPLICATION_JSON);

        submitHeaders.set("Authorization", accessToken);

        HttpEntity<Map<String,String>> submitReq = new HttpEntity<>(submitBody, submitHeaders);

        String targetUrl = TEST_WEBHOOK_URL;
        if (!webhookUrl.isEmpty()) {
        }

        System.out.println("Submitting finalQuery to: " + targetUrl);
        ResponseEntity<String> submitResp = restTemplate.postForEntity(targetUrl, submitReq, String.class);

        System.out.println("Submission status: " + submitResp.getStatusCode());
        System.out.println("Submission response: " + submitResp.getBody());
    }

    private int extractLastTwoDigits(String regNo) {
        String digits = regNo.replaceAll("\\D+", "");
        if (digits.length() >= 2) return Integer.parseInt(digits.substring(digits.length()-2));
        if (!digits.isEmpty()) return Integer.parseInt(digits);
        return 0;
    }
}
