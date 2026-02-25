package com.artem.individuals.rest;

import com.artem.individuals.dto.request.TransferConfirmRequestDto;
import com.artem.individuals.dto.request.TransferInitRequestDto;
import com.artem.individuals.dto.response.TransferConfirmResponseDto;
import com.artem.individuals.dto.response.TransferInitResponseDto;
import com.artem.individuals.dto.response.WithdrawalConfirmResponseDto;
import com.artem.individuals.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/transfer")
@RequiredArgsConstructor
public class TransferControllerV1 {
    private final TransferService transferService;

    @PostMapping("/init")
    public Mono<TransferInitResponseDto> init(@RequestBody TransferInitRequestDto transferInitRequestDto) {
        return transferService.init(transferInitRequestDto);
    }

    @PostMapping("/confirm")
    public Mono<TransferConfirmResponseDto> confirm(@RequestBody TransferConfirmRequestDto transferConfirmRequestDto) {
        return transferService.confirm(transferConfirmRequestDto);
    }
}
