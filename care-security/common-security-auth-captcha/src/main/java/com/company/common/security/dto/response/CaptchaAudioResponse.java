package com.company.common.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "CAPTCHA audio response for accessibility")
public record CaptchaAudioResponse(
        @Schema(description = "Base64-encoded WAV audio of the verification code")
        String audioBase64
) {}
