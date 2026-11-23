# SOMCHAI Local Brain Runtime

## Purpose

Add real on-device language generation to the existing LocalFirstBrain boundary. Ordinary chat must use the local model when installed. It must never silently call the optional remote provider. If the model is absent or fails, the app reports and uses the deterministic internal fallback.

## Native inference engine

- Runtime: llama.cpp, pinned to upstream commit `8d60873dfb8619f2b5ed31adfbab07cccb40db5c`.
- Android integration: CMake FetchContent + JNI, ARM64 Android ABI.
- Model loading and generation occur on the device.
- Initial context window: 2,048 tokens.
- Initial output cap: 384 tokens.
- CPU inference is used; GPU offload is not enabled in this first integration.
- Model file is stored under app-private `files/somchai/models/`.
- A downloaded model is not trusted until its GGUF magic and pinned SHA-256 are verified.

## Initial model

- Publisher: Qwen
- Model: Qwen2.5-0.5B-Instruct-GGUF
- Quantization: Q4_K_M
- Approximate download size: 491 MB
- License: Apache-2.0
- Official model card: https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF
- Direct model asset: https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf
- SHA-256: `74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db`

Qwen's model card lists Thai among the supported languages. The 0.5B model is a starter model for device feasibility and basic Thai conversation, not a claim of advanced or human-level reasoning. Its answer quality must be measured against the fixed Phase 1 test set before acceptance.

## User flow

1. Open Settings.
2. Select "ดาวน์โหลดและติดตั้งสมอง AI".
3. The app checks free space, downloads the public model over HTTPS, validates the file size range, GGUF header and SHA-256, then installs it into app-private storage.
4. The app loads the model through llama.cpp.
5. Ordinary chat is routed through LocalFirstBrain to llama.cpp.
6. If model load or generation fails, the app uses the existing deterministic fallback and records the fallback state.

The model download is explicit and user initiated. It is approximately 491 MB; Wi-Fi and at least 650 MB free storage are recommended. No API key, account, subscription or paid provider is required.

## Safety and limitations

- Model weights are treated as data, not trusted code.
- No downloaded model is executed as a program.
- No automatic external AI fallback is allowed.
- The optional `/ai` provider route remains separate and explicit.
- Initial JNI implementation is synchronous generation on a background dispatcher, with a 384-token output cap.
- This first integration does not yet implement streaming tokens, cancellation, adaptive context sizing, GPU acceleration, model swapping UI, or model benchmark reports.
- A 0.5B model can make mistakes and may not satisfy all reasoning prompts. A larger model may be evaluated only after target-device RAM, speed, heat and battery benchmarks.
- Do not claim this runtime passes Phase 1 until CI and real-device tests pass.

## Verification gates

- Build native llama.cpp for ARM64 using the pinned upstream commit.
- Run JVM tests, including GGUF file validation.
- Install the debug APK on the target Android device.
- Download the official model and verify its SHA-256.
- Test Thai open-ended prompts, multi-turn context, contradictory premises and insufficient-information questions.
- Record first-token latency, total generation time, peak memory, heat, battery impact and crash behavior.
- Verify ordinary chat produces no remote provider network request.
- Re-run the complete Phase 1 acceptance suite. Keep all previous thresholds unchanged.


## Download and research network reliability — 2026-11-23
- Interrupted model downloads retain a partial file in app-private storage and retry using HTTP Range.
- A resumed response must be HTTP 206 with a valid Content-Range beginning at the exact local partial-file length. If the server rejects a stale range with HTTP 416, the installer restarts from byte zero.
- A partial file is never promoted to the installed model until the complete file passes size, GGUF and pinned SHA-256 verification. A complete already-verified partial file can be promoted without downloading it again.
- The research page reader requests gzip when supported and applies a strict size limit after decompression. It retains HTTPS-only access, pinned public-IP connections, redirect revalidation and untrusted-page handling.
- NAT64 is permitted only for the well-known prefix when its embedded IPv4 destination is public; private translated destinations remain blocked.
- Automated tests have been added for gzip decoding, range metadata validation and public/private NAT64 handling. The latest combined CI result is pending.
