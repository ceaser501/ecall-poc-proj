package com.ecall.location.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 위치 추출 서비스
 * 담당자: 임송은
 */
@Slf4j
@Service
public class LocationService {

    public String extractLocation(String text) {
        log.info("[임송은] 위치 추출 로직");

        // TODO: 임송은 - 위치 추출 비즈니스 로직 구현
        // 1. 텍스트에서 주소/장소 키워드 추출
        // 2. Azure Maps API 호출
        // 3. Kakao Maps API 연동
        // 4. 좌표 변환

        return "서울특별시 강남구";
    }
}