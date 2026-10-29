package com.sinema.common.guvenlik;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring MVC'ye ulaşmadan (güvenlik filtresinde) üretilen hataları, MVC'deki ile aynı
 * problem+json biçiminde doğrudan cevaba yazar.
 */
public class ProblemJsonYazici {

    // Yalnızca düz bir Map yazılır; özel Jackson ayarı gerekmez
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public void yaz(HttpServletRequest istek, HttpServletResponse cevap, ProblemDetail problem) throws IOException {
        problem.setInstance(URI.create(istek.getRequestURI()));
        cevap.setStatus(problem.getStatus());
        cevap.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        cevap.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(cevap.getOutputStream(), duzAlanlar(problem));
    }

    // MVC'nin yazdığıyla aynı alanlar: ek alanlar üst seviyede ("properties" altında değil), boş alan yazılmaz
    private static Map<String, Object> duzAlanlar(ProblemDetail problem) {
        Map<String, Object> alanlar = new LinkedHashMap<>();
        if (problem.getType() != null) {
            alanlar.put("type", problem.getType().toString());
        }
        if (problem.getInstance() != null) {
            alanlar.put("instance", problem.getInstance().toString());
        }
        alanlar.put("title", problem.getTitle());
        alanlar.put("status", problem.getStatus());
        alanlar.put("detail", problem.getDetail());
        if (problem.getProperties() != null) {
            alanlar.putAll(problem.getProperties());
        }
        return alanlar;
    }
}
