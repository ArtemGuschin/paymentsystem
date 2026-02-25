package com.artem.individuals.rest;

import com.artem.individuals.dto.request.WithdrawalConfirmRequestDto;
import com.artem.individuals.dto.request.WithdrawalInitRequestDto;
import com.artem.individuals.dto.response.WithdrawalConfirmResponseDto;
import com.artem.individuals.dto.response.WithdrawalInitResponseDto;
import com.artem.individuals.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/withdrawal")
@RequiredArgsConstructor
public class WithdrawalControllerV1 {

    private final WithdrawalService withdrawalService;


    @PostMapping("/init")
    public Mono<WithdrawalInitResponseDto> init(@RequestBody WithdrawalInitRequestDto withdrawalInitRequestDto) {
        return withdrawalService.init(withdrawalInitRequestDto);
    }

    @PostMapping("/confirm")
    public Mono<WithdrawalConfirmResponseDto> confirm(@RequestBody WithdrawalConfirmRequestDto withdrawalConfirmRequestDto) {
        return withdrawalService.confirm(withdrawalConfirmRequestDto);
    }
}
