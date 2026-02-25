package com.artem.individuals.rest;

import com.artem.individuals.dto.request.TopUpConfirmRequestDto;
import com.artem.individuals.dto.request.TopUpInitRequestDto;
import com.artem.individuals.dto.response.TopUpConfirmResponseDto;
import com.artem.individuals.dto.response.TopUpInitResponseDto;
import com.artem.individuals.service.TopUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/topup")
@RequiredArgsConstructor
public class TopUpControllerV1 {

    private final TopUpService topUpService;

    @PostMapping("/init")
    public Mono<TopUpInitResponseDto> init(@RequestBody TopUpInitRequestDto dto) {
        return topUpService.init(dto);
    }

    @PostMapping("/confirm")
    public Mono <TopUpConfirmResponseDto> confirm(
            @RequestBody TopUpConfirmRequestDto dto
    ) {
        return topUpService.confirm(dto);
    }
}
