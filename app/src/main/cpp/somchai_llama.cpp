#include <jni.h>
#include <android/log.h>
#include <llama.h>

#include <algorithm>
#include <cstdint>
#include <exception>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

namespace {
constexpr const char * TAG = "SomchaiLlama";
struct Engine {
    llama_model * model = nullptr;
    llama_context * context = nullptr;
    const llama_vocab * vocab = nullptr;
    uint32_t contextSize = 2048;
    std::mutex mutex;
    ~Engine() {
        if (context) llama_free(context);
        if (model) llama_model_free(model);
    }
};
void throwJava(JNIEnv * env, const char * message) {
    jclass type = env->FindClass("java/lang/IllegalStateException");
    if (type) env->ThrowNew(type, message);
}
std::string fromJava(JNIEnv * env, jstring value) {
    if (!value) return {};
    const char * chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_somchai_ai_core_brain_NativeLlamaBindings_nativeLoadModel(
    JNIEnv * env, jobject, jstring modelPath, jint contextSize, jint requestedThreads) {
    try {
        static std::once_flag backendOnce;
        std::call_once(backendOnce, [] { llama_backend_init(); });

        const std::string path = fromJava(env, modelPath);
        if (path.empty()) {
            throwJava(env, "ไม่พบตำแหน่งไฟล์โมเดล");
            return 0;
        }

        auto engine = std::make_unique<Engine>();
        llama_model_params modelParams = llama_model_default_params();
        modelParams.n_gpu_layers = 0;
        engine->model = llama_model_load_from_file(path.c_str(), modelParams);
        if (!engine->model) {
            throwJava(env, "เปิดไฟล์ GGUF ไม่สำเร็จ หรือโมเดลไม่รองรับ");
            return 0;
        }

        llama_context_params contextParams = llama_context_default_params();
        contextParams.n_ctx = static_cast<uint32_t>(std::clamp(contextSize, 512, 4096));
        contextParams.n_batch = 512;
        contextParams.n_ubatch = 512;
        const int available = static_cast<int>(std::max(2u, std::thread::hardware_concurrency()));
        const int threads = std::clamp(requestedThreads, 2, std::min(available, 6));
        contextParams.n_threads = threads;
        contextParams.n_threads_batch = threads;
        engine->contextSize = contextParams.n_ctx;
        engine->context = llama_init_from_model(engine->model, contextParams);
        if (!engine->context) {
            throwJava(env, "หน่วยความจำไม่เพียงพอสำหรับโหลดโมเดลนี้");
            return 0;
        }
        engine->vocab = llama_model_get_vocab(engine->model);
        __android_log_print(ANDROID_LOG_INFO, TAG, "Loaded model: %s", path.c_str());
        return reinterpret_cast<jlong>(engine.release());
    } catch (const std::exception & error) {
        throwJava(env, error.what());
        return 0;
    } catch (...) {
        throwJava(env, "เกิดข้อผิดพลาดระหว่างโหลดโมเดล");
        return 0;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_somchai_ai_core_brain_NativeLlamaBindings_nativeGenerate(
    JNIEnv * env, jobject, jlong handle, jstring promptValue, jint maxTokens) {
    auto * engine = reinterpret_cast<Engine *>(handle);
    if (!engine || !engine->model || !engine->context || !engine->vocab) {
        throwJava(env, "โมเดลภายในยังไม่พร้อมใช้งาน");
        return nullptr;
    }

    try {
        std::lock_guard<std::mutex> guard(engine->mutex);
        const std::string prompt = fromJava(env, promptValue);
        if (prompt.empty()) {
            throwJava(env, "คำถามว่างเปล่า");
            return nullptr;
        }

        llama_memory_clear(llama_get_memory(engine->context), true);
        const char * chatTemplate = llama_model_chat_template(engine->model, nullptr);
        llama_chat_message message = {"user", prompt.c_str()};
        std::vector<char> formatted(32768);
        int32_t formattedLength = chatTemplate
            ? llama_chat_apply_template(chatTemplate, &message, 1, true,
                                        formatted.data(), static_cast<int32_t>(formatted.size()))
            : -1;
        if (formattedLength < 0) {
            std::string fallback = "### User:\n" + prompt + "\n### Assistant:\n";
            formatted.assign(fallback.begin(), fallback.end());
        } else if (static_cast<size_t>(formattedLength) > formatted.size()) {
            formatted.resize(static_cast<size_t>(formattedLength) + 1);
            formattedLength = llama_chat_apply_template(chatTemplate, &message, 1, true,
                formatted.data(), static_cast<int32_t>(formatted.size()));
            if (formattedLength < 0) {
                throwJava(env, "จัดรูปแบบคำถามตามโมเดลไม่สำเร็จ");
                return nullptr;
            }
            formatted.resize(static_cast<size_t>(formattedLength));
        } else {
            formatted.resize(static_cast<size_t>(formattedLength));
        }

        const std::string input(formatted.begin(), formatted.end());
        const int32_t capacity = static_cast<int32_t>(input.size() + 256);
        std::vector<llama_token> tokens(static_cast<size_t>(capacity));
        int32_t tokenCount = llama_tokenize(engine->vocab, input.c_str(),
            static_cast<int32_t>(input.size()), tokens.data(), capacity, true, true);
        if (tokenCount < 0) {
            tokens.resize(static_cast<size_t>(-tokenCount));
            tokenCount = llama_tokenize(engine->vocab, input.c_str(),
                static_cast<int32_t>(input.size()), tokens.data(),
                static_cast<int32_t>(tokens.size()), true, true);
        }
        const int limit = std::clamp(maxTokens, 1, 512);
        const int32_t generationReserve = limit + 16;
        if (tokenCount <= 0 ||
            tokenCount > static_cast<int32_t>(engine->contextSize) - generationReserve) {
            throwJava(env, "คำถามยาวเกินขีดจำกัดของโมเดลเมื่อเผื่อพื้นที่สร้างคำตอบแล้ว");
            return nullptr;
        }
        tokens.resize(static_cast<size_t>(tokenCount));

        llama_batch batch = llama_batch_get_one(tokens.data(), tokenCount);
        if (llama_decode(engine->context, batch) != 0) {
            throwJava(env, "โมเดลประมวลผลคำถามไม่สำเร็จ");
            return nullptr;
        }

        auto samplerParams = llama_sampler_chain_default_params();
        llama_sampler * sampler = llama_sampler_chain_init(samplerParams);
        llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
        llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.90f, 1));
        llama_sampler_chain_add(sampler, llama_sampler_init_temp_ext(0.70f, 0.0f, 1.0f));
        llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

        std::string output;
        for (int i = 0; i < limit; ++i) {
            const llama_token token = llama_sampler_sample(sampler, engine->context, -1);
            if (llama_vocab_is_eog(engine->vocab, token)) break;
            llama_sampler_accept(sampler, token);

            std::vector<char> piece(256);
            int32_t pieceLength = llama_token_to_piece(engine->vocab, token,
                piece.data(), static_cast<int32_t>(piece.size()), 0, false);
            if (pieceLength < 0) {
                piece.resize(static_cast<size_t>(-pieceLength));
                pieceLength = llama_token_to_piece(engine->vocab, token,
                    piece.data(), static_cast<int32_t>(piece.size()), 0, false);
            }
            if (pieceLength > 0) output.append(piece.data(), static_cast<size_t>(pieceLength));

            llama_batch next = llama_batch_get_one(const_cast<llama_token *>(&token), 1);
            if (llama_decode(engine->context, next) != 0) break;
        }
        llama_sampler_free(sampler);
        if (output.empty()) {
            throwJava(env, "โมเดลไม่สร้างคำตอบ กรุณาลองใหม่");
            return nullptr;
        }
        return env->NewStringUTF(output.c_str());
    } catch (const std::exception & error) {
        throwJava(env, error.what());
        return nullptr;
    } catch (...) {
        throwJava(env, "เกิดข้อผิดพลาดระหว่างประมวลผลด้วยโมเดล");
        return nullptr;
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_somchai_ai_core_brain_NativeLlamaBindings_nativeCountTokens(
    JNIEnv * env, jobject, jlong handle, jstring promptValue) {
    auto * engine = reinterpret_cast<Engine *>(handle);
    if (!engine || !engine->model || !engine->context || !engine->vocab) {
        throwJava(env, "โมเดลภายในยังไม่พร้อมใช้งาน");
        return -1;
    }

    try {
        std::lock_guard<std::mutex> guard(engine->mutex);
        const std::string prompt = fromJava(env, promptValue);
        if (prompt.empty()) {
            throwJava(env, "คำถามว่างเปล่า");
            return -1;
        }

        const char * chatTemplate = llama_model_chat_template(engine->model, nullptr);
        llama_chat_message message = {"user", prompt.c_str()};
        std::vector<char> formatted(32768);
        int32_t formattedLength = chatTemplate
            ? llama_chat_apply_template(chatTemplate, &message, 1, true,
                                        formatted.data(), static_cast<int32_t>(formatted.size()))
            : -1;
        if (formattedLength < 0) {
            const std::string fallback = "### User:\\n" + prompt + "\\n### Assistant:\\n";
            formatted.assign(fallback.begin(), fallback.end());
        } else if (static_cast<size_t>(formattedLength) > formatted.size()) {
            formatted.resize(static_cast<size_t>(formattedLength) + 1);
            formattedLength = llama_chat_apply_template(chatTemplate, &message, 1, true,
                formatted.data(), static_cast<int32_t>(formatted.size()));
            if (formattedLength < 0) {
                throwJava(env, "จัดรูปแบบคำถามตามโมเดลไม่สำเร็จ");
                return -1;
            }
            formatted.resize(static_cast<size_t>(formattedLength));
        } else {
            formatted.resize(static_cast<size_t>(formattedLength));
        }

        const std::string input(formatted.begin(), formatted.end());
        std::vector<llama_token> tokens(input.size() + 256);
        int32_t tokenCount = llama_tokenize(engine->vocab, input.c_str(),
            static_cast<int32_t>(input.size()), tokens.data(),
            static_cast<int32_t>(tokens.size()), true, true);
        if (tokenCount < 0) {
            tokens.resize(static_cast<size_t>(-tokenCount));
            tokenCount = llama_tokenize(engine->vocab, input.c_str(),
                static_cast<int32_t>(input.size()), tokens.data(),
                static_cast<int32_t>(tokens.size()), true, true);
        }
        if (tokenCount <= 0) {
            throwJava(env, "นับโทเค็นของคำถามไม่สำเร็จ");
            return -1;
        }
        return tokenCount;
    } catch (const std::exception & error) {
        throwJava(env, error.what());
        return -1;
    } catch (...) {
        throwJava(env, "เกิดข้อผิดพลาดระหว่างนับโทเค็น");
        return -1;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_somchai_ai_core_brain_NativeLlamaBindings_nativeUnloadModel(
    JNIEnv *, jobject, jlong handle) {
    delete reinterpret_cast<Engine *>(handle);
}
