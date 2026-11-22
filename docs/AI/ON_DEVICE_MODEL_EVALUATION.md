# On-device Brain Model Evaluation

## Goal

Select an on-device language model for Thai conversation and bounded reasoning. The model must run locally; no API key, account, paid inference service, or remote AI provider is required for the primary brain.

## Runtime direction

- Candidate runtime: llama.cpp Android native integration.
- Android target: build and package the native runtime for `arm64-v8a` after checking the actual device ABI.
- Official build guidance: https://github.com/ggml-org/llama.cpp/blob/master/docs/build.md
- Android integration guidance: https://github.com/crc-org/llama.cpp/blob/main/docs/android.md
- The runtime must expose a Kotlin `OnDeviceInferenceEngine` adapter to `core/brain/Brain.kt`.
- Do not bundle a multi-gigabyte model inside the APK. Store model files in app-private storage and provide an owner-controlled download/import flow.
- Never silently download a model over mobile data. Model acquisition must be explicit and show exact size, source, license, and storage requirement.

## Initial model candidate (not yet selected)

**Qwen3-1.7B GGUF Q4_K_M**
- Model card: https://huggingface.co/Qwen/Qwen3-1.7B
- GGUF repository: https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF
- Qwen model card states multilingual support for 100+ languages and context length up to 32,768 tokens.
- Community quantization references Q4_K_M at approximately 1.28 GB: https://huggingface.co/tensorblock/Qwen_Qwen3-1.7B-GGUF
- This is a benchmark candidate only. Thai quality, peak RAM, thermal behavior, speed, and stability must be measured on the target phone before selection.

## Required device measurements

Record device model, Android version, ABI, total RAM, available RAM before load, free storage, model hash, quantization, context size, prompt tokens, generated tokens, load time, time-to-first-token, tokens/second, peak memory, battery/temperature observations, and crash/OOM events.

Run the same Thai evaluation set for each candidate. Do not choose a model based only on parameter count or a model card claim.

## Acceptance gates

1. Model loads and unloads without crashing the app.
2. At least 18/20 Thai prompts receive a relevant answer under the agreed rubric; hallucinations and unsafe tool claims are scored separately.
3. Model does not access network or external providers during generation.
4. Router sends slash commands and exact arithmetic to deterministic handlers before the model.
5. Model failures route to an honest offline fallback.
6. No app freeze; generation is cancellable and bounded by configured time/token limits.
7. Memory pressure and thermal behavior remain acceptable during a 10-turn session.
8. Model provenance, license, and SHA-256 are recorded.

## Non-goals

- This document does not claim that a model has been installed or benchmarked on a physical phone.
- A 1.7B model is not expected to match a large hosted model.
- The on-device model is the language/reasoning engine; it does not gain permission to execute tools or modify protected files.
