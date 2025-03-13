package com.example.user.service;

import com.example.user.repository.AiChangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiService {
    private final AiChangeRepository aiChangeRepository;

    @Transactional
    @Scheduled(fixedDelay = 600000)
    public void resetCnt() {
        aiChangeRepository.resetCnt();
    }

}
