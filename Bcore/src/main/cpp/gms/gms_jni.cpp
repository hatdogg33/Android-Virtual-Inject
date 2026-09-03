#include <jni.h>
#include <string>
#include <android/log.h>
#include "gms_bridge.h"

#define TAG "GmsJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static GmsNative::GmsBridge* g_bridge = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_vcore_core_GmsNativeBridge_nativeInitialize(JNIEnv* env, jobject thiz, jstring base_dir) {
    const char* dir = env->GetStringUTFChars(base_dir, nullptr);
    
    if (g_bridge) {
        delete g_bridge;
    }
    
    g_bridge = new GmsNative::GmsBridge();
    jboolean result = g_bridge->initialize(dir) ? JNI_TRUE : JNI_FALSE;
    
    env->ReleaseStringUTFChars(base_dir, dir);
    
    LOGI("Native bridge initialized: %s", result ? "success" : "failed");
    return result;
}

JNIEXPORT void JNICALL
Java_com_vcore_core_GmsNativeBridge_nativeShutdown(JNIEnv* env, jobject thiz) {
    if (g_bridge) {
        g_bridge->shutdown();
        delete g_bridge;
        g_bridge = nullptr;
    }
    LOGI("Native bridge shutdown");
}

JNIEXPORT jboolean JNICALL
Java_com_vcore_core_GmsNativeBridge_nativeStoreToken(JNIEnv* env, jobject thiz, 
                                                      jstring account_name,
                                                      jstring auth_token,
                                                      jstring refresh_token,
                                                      jstring id_token,
                                                      jlong expiry_time,
                                                      jint token_type) {
    if (!g_bridge) return JNI_FALSE;
    
    const char* account = env->GetStringUTFChars(account_name, nullptr);
    const char* auth = env->GetStringUTFChars(auth_token, nullptr);
    const char* refresh = env->GetStringUTFChars(refresh_token, nullptr);
    const char* id = env->GetStringUTFChars(id_token, nullptr);
    
    GmsNative::TokenData token;
    token.account_name = account;
    token.auth_token = auth;
    token.refresh_token = refresh;
    token.id_token = id;
    token.expiry_time = expiry_time;
    token.token_type = token_type;
    
    jboolean result = g_bridge->store_token(account, token) ? JNI_TRUE : JNI_FALSE;
    
    env->ReleaseStringUTFChars(account_name, account);
    env->ReleaseStringUTFChars(auth_token, auth);
    env->ReleaseStringUTFChars(refresh_token, refresh);
    env->ReleaseStringUTFChars(id_token, id);
    
    return result;
}

JNIEXPORT jobject JNICALL
Java_com_vcore_core_GmsNativeBridge_nativeLoadToken(JNIEnv* env, jobject thiz, jstring account_name) {
    if (!g_bridge) return nullptr;
    
    const char* account = env->GetStringUTFChars(account_name, nullptr);
    
    GmsNative::TokenData token;
    jboolean success = g_bridge->load_token(account, token) ? JNI_TRUE : JNI_FALSE;
    
    env->ReleaseStringUTFChars(account_name, account);
    
    if (!success) return nullptr;
    
    // Create Java TokenData object
    jclass tokenClass = env->FindClass("com/vcore/core/GmsNativeBridge$TokenData");
    if (!tokenClass) return nullptr;
    
    jmethodID constructor = env->GetMethodID(tokenClass, "<init>", "()V");
    jobject tokenObj = env->NewObject(tokenClass, constructor);
    
    // Set fields
    jfieldID accountField = env->GetFieldID(tokenClass, "accountName", "Ljava/lang/String;");
    jfieldID authField = env->GetFieldID(tokenClass, "authToken", "Ljava/lang/String;");
    jfieldID refreshField = env->GetFieldID(tokenClass, "refreshToken", "Ljava/lang/String;");
    jfieldID idField = env->GetFieldID(tokenClass, "idToken", "Ljava/lang/String;");
    jfieldID expiryField = env->GetFieldID(tokenClass, "expiryTime", "J");
    jfieldID typeField = env->GetFieldID(tokenClass, "tokenType", "I");
    
    env->SetObjectField(tokenObj, accountField, env->NewStringUTF(token.account_name.c_str()));
    env->SetObjectField(tokenObj, authField, env->NewStringUTF(token.auth_token.c_str()));
    env->SetObjectField(tokenObj, refreshField, env->NewStringUTF(token.refresh_token.c_str()));
    env->SetObjectField(tokenObj, idField, env->NewStringUTF(token.id_token.c_str()));
    env->SetLongField(tokenObj, expiryField, token.expiry_time);
    env->SetIntField(tokenObj, typeField, token.token_type);
    
    return tokenObj;
}

JNIEXPORT jboolean JNICALL
Java_com_vcore_core_GmsNativeBridge_nativeDeleteToken(JNIEnv* env, jobject thiz, jstring account_name) {
    if (!g_bridge) return JNI_FALSE;
    
    const char* account = env->GetStringUTFChars(account_name, nullptr);
    jboolean result = g_bridge->delete_token(account) ? JNI_TRUE : JNI_FALSE;
    env->ReleaseStringUTFChars(account_name, account);
    
    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_vcore_core_GmsNativeBridge_nativeClearAllTokens(JNIEnv* env, jobject thiz) {
    if (!g_bridge) return JNI_FALSE;
    return g_bridge->clear_all_tokens() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_vcore_core_GmsNativeBridge_nativeStoreDeviceInfo(JNIEnv* env, jobject thiz,
                                                          jstring android_id,
                                                          jstring device_id,
                                                          jstring gaia_id,
                                                          jstring model,
                                                          jstring manufacturer,
                                                          jstring brand) {
    if (!g_bridge) return JNI_FALSE;
    
    const char* android = env->GetStringUTFChars(android_id, nullptr);
    const char* device = env->GetStringUTFChars(device_id, nullptr);
    const char* gaia = env->GetStringUTFChars(gaia_id, nullptr);
    const char* m = env->GetStringUTFChars(model, nullptr);
    const char* manuf = env->GetStringUTFChars(manufacturer, nullptr);
    const char* b = env->GetStringUTFChars(brand, nullptr);
    
    GmsNative::DeviceInfo info;
    info.android_id = android;
    info.device_id = device;
    info.gaia_id = gaia;
    info.model = m;
    info.manufacturer = manuf;
    info.brand = b;
    
    jboolean result = g_bridge->store_device_info(info) ? JNI_TRUE : JNI_FALSE;
    
    env->ReleaseStringUTFChars(android_id, android);
    env->ReleaseStringUTFChars(device_id, device);
    env->ReleaseStringUTFChars(gaia_id, gaia);
    env->ReleaseStringUTFChars(model, m);
    env->ReleaseStringUTFChars(manufacturer, manuf);
    env->ReleaseStringUTFChars(brand, b);
    
    return result;
}

JNIEXPORT jobject JNICALL
Java_com_vcore_core_GmsNativeBridge_nativeLoadDeviceInfo(JNIEnv* env, jobject thiz) {
    if (!g_bridge) return nullptr;
    
    GmsNative::DeviceInfo info;
    jboolean success = g_bridge->load_device_info(info) ? JNI_TRUE : JNI_FALSE;
    
    if (!success) return nullptr;
    
    // Create Java DeviceInfo object
    jclass infoClass = env->FindClass("com/vcore/core/GmsNativeBridge$DeviceInfo");
    if (!infoClass) return nullptr;
    
    jmethodID constructor = env->GetMethodID(infoClass, "<init>", "()V");
    jobject infoObj = env->NewObject(infoClass, constructor);
    
    // Set fields
    jfieldID androidField = env->GetFieldID(infoClass, "androidId", "Ljava/lang/String;");
    jfieldID deviceField = env->GetFieldID(infoClass, "deviceId", "Ljava/lang/String;");
    jfieldID gaiaField = env->GetFieldID(infoClass, "gaiaId", "Ljava/lang/String;");
    jfieldID modelField = env->GetFieldID(infoClass, "model", "Ljava/lang/String;");
    jfieldID manufField = env->GetFieldID(infoClass, "manufacturer", "Ljava/lang/String;");
    jfieldID brandField = env->GetFieldID(infoClass, "brand", "Ljava/lang/String;");
    
    env->SetObjectField(infoObj, androidField, env->NewStringUTF(info.android_id.c_str()));
    env->SetObjectField(infoObj, deviceField, env->NewStringUTF(info.device_id.c_str()));
    env->SetObjectField(infoObj, gaiaField, env->NewStringUTF(info.gaia_id.c_str()));
    env->SetObjectField(infoObj, modelField, env->NewStringUTF(info.model.c_str()));
    env->SetObjectField(infoObj, manufField, env->NewStringUTF(info.manufacturer.c_str()));
    env->SetObjectField(infoObj, brandField, env->NewStringUTF(info.brand.c_str()));
    
    return infoObj;
}

JNIEXPORT jstring JNICALL
Java_com_vcore_core_GmsNativeBridge_nativeEncryptData(JNIEnv* env, jobject thiz, jstring data) {
    if (!g_bridge) return nullptr;
    
    const char* str = env->GetStringUTFChars(data, nullptr);
    std::string encrypted = g_bridge->encrypt_data(str);
    env->ReleaseStringUTFChars(data, str);
    
    return env->NewStringUTF(encrypted.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_vcore_core_GmsNativeBridge_nativeDecryptData(JNIEnv* env, jobject thiz, jstring encrypted_data) {
    if (!g_bridge) return nullptr;
    
    const char* str = env->GetStringUTFChars(encrypted_data, nullptr);
    std::string decrypted = g_bridge->decrypt_data(str);
    env->ReleaseStringUTFChars(encrypted_data, str);
    
    return env->NewStringUTF(decrypted.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_vcore_core_GmsNativeBridge_nativeCalculateSha256(JNIEnv* env, jobject thiz, jstring data) {
    if (!g_bridge) return nullptr;
    
    const char* str = env->GetStringUTFChars(data, nullptr);
    std::string hash = g_bridge->calculate_sha256(str);
    env->ReleaseStringUTFChars(data, str);
    
    return env->NewStringUTF(hash.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_vcore_core_GmsNativeBridge_nativeGenerateSecureToken(JNIEnv* env, jobject thiz, jint length) {
    if (!g_bridge) return nullptr;
    
    std::string token = g_bridge->generate_secure_token(length);
    return env->NewStringUTF(token.c_str());
}

} // extern "C"
