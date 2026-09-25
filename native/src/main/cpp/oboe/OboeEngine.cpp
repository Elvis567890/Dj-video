#include <oboe/Oboe.h>
#include <android/log.h>
#include <cmath>
#include <vector>
#include <mutex>

#define LOG_TAG "DJProOboe"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

class OboeEngine : public oboe::AudioStreamDataCallback {
public:
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream *stream, void *audioData, int32_t numFrames) override {
        float *out = static_cast<float*>(audioData);
        int ch = stream->getChannelCount();
        std::lock_guard<std::mutex> lock(mMutex);
        for (int i = 0; i < numFrames * ch; ++i) out[i] = 0.0f;
        for (auto &deck : mDecks) {
            if (!deck.active) continue;
            for (int i = 0; i < numFrames; ++i) {
                if (deck.pos >= deck.buffer.size()) { deck.active = false; break; }
                float s = deck.buffer[deck.pos++] * deck.volume;
                for (int c = 0; c < ch; ++c) out[i*ch + c] += s;
            }
        }
        for (int i = 0; i < numFrames * ch; ++i)
            out[i] = std::tanh(out[i]);
        return oboe::DataCallbackResult::Continue;
    }

    bool start() {
        oboe::AudioStreamBuilder b;
        b.setDirection(oboe::Direction::Output);
        b.setPerformanceMode(oboe::PerformanceMode::LowLatency);
        b.setFormat(oboe::AudioFormat::Float);
        b.setChannelCount(oboe::ChannelCount::Stereo);
        b.setSampleRate(48000);
        b.setDataCallback(this);
        oboe::Result r = b.openStream(mStream);
        if (r != oboe::Result::OK) return false;
        mStream->requestStart();
        return true;
    }

    void stop() { if (mStream) { mStream->requestStop(); mStream->close(); } }

    struct Deck { std::vector<float> buffer; size_t pos = 0; float volume = 1.0f; bool active = false; };

    void loadDeck(int id, const float* data, size_t n, float vol) {
        std::lock_guard<std::mutex> lock(mMutex);
        if (id >= (int)mDecks.size()) mDecks.resize(id+1);
        mDecks[id].buffer.assign(data, data+n);
        mDecks[id].pos = 0; mDecks[id].volume = vol; mDecks[id].active = true;
    }

    void setDeckVolume(int id, float v) {
        std::lock_guard<std::mutex> lock(mMutex);
        if (id < (int)mDecks.size()) mDecks[id].volume = v;
    }

private:
    std::shared_ptr<oboe::AudioStream> mStream;
    std::vector<Deck> mDecks;
    std::mutex mMutex;
};

static OboeEngine* gEngine = nullptr;

extern "C" {
    JNIEXPORT jboolean JNICALL
    Java_com_djpro_native_NativeBridge_startEngine(JNIEnv*, jobject) {
        if (!gEngine) gEngine = new OboeEngine();
        return gEngine->start();
    }
    JNIEXPORT void JNICALL
    Java_com_djpro_native_NativeBridge_stopEngine(JNIEnv*, jobject) {
        if (gEngine) { gEngine->stop(); delete gEngine; gEngine = nullptr; }
    }
    JNIEXPORT void JNICALL
    Java_com_djpro_native_NativeBridge_loadDeck(JNIEnv* env, jobject,
                                                jint id, jfloatArray data, jfloat vol) {
        jsize n = env->GetArrayLength(data);
        jfloat* buf = env->GetFloatArrayElements(data, nullptr);
        if (gEngine) gEngine->loadDeck(id, buf, n, vol);
        env->ReleaseFloatArrayElements(data, buf, 0);
    }
    JNIEXPORT void JNICALL
    Java_com_djpro_native_NativeBridge_setDeckVolume(JNIEnv*, jobject, jint id, jfloat v) {
        if (gEngine) gEngine->setDeckVolume(id, v);
    }
    JNIEXPORT void JNICALL
    Java_com_djpro_native_NativeBridge_scratchDeck(JNIEnv*, jobject, jint id, jfloat rate) {
        (void)id; (void)rate;
    }
}
