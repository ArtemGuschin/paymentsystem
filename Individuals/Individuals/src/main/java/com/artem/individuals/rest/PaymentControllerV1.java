package com.artem.individuals.rest;



import com.artem.individuals.dto.request.PaymentRequestDto;
import com.artem.individuals.dto.response.PaymentMethodResponseDto;
import com.artem.individuals.dto.response.PaymentResponseDto;
import com.artem.individuals.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(
        value = "/api/v1/payments",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Validated
@Tag(name = "Payment", description = "Payment orchestration API")
@SecurityRequirement(name = "bearerAuth")
public class PaymentControllerV1 {

    private final PaymentService paymentService;

    @Operation(
            summary = "Get available payment methods",
            description = "Returns available payment methods for selected currency and country.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Payment methods successfully returned",
                            content = @Content(
                                    array = @ArraySchema(
                                            schema = @Schema(implementation = PaymentMethodResponseDto.class)
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized"
                    )
            }
    )
    @GetMapping("/methods")
    public Flux<PaymentMethodResponseDto> getAvailablePaymentMethods(

            @Parameter(description = "ISO 4217 currency code", example = "EUR")
            @RequestParam
            @Size(min = 3, max = 3)
            String currencyCode,

            @Parameter(description = "ISO country code", example = "POL")
            @RequestParam
            @Size(min = 3, max = 3)
            String countryCode
    ) {

        return paymentService.getAvailablePaymentMethods(
                currencyCode,
                countryCode
        );
    }

    @Operation(
            summary = "Process payment",
            description = "Sends payment request to Payment Service.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Payment successfully processed",
                            content = @Content(
                                    schema = @Schema(
                                            implementation = PaymentResponseDto.class
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid payment request"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<PaymentResponseDto> processPayment(

            @Valid
            @RequestBody
            PaymentRequestDto request
    ) {

        return paymentService.processPayment(request);
    }
}