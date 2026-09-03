#include "gms_bridge.h"
#include <fstream>
#include <sstream>
#include <random>
#include <algorithm>
#include <cstring>
#include <android/log.h>

#define TAG "GmsNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace GmsNative {

// Simple XOR encryption for demonstration (use proper AES in production)
static std::string xor_encrypt(const std::string& data, const std::string& key) {
    std::string result = data;
    for (size_t i = 0; i < data.size(); i++) {
        result[i] = data[i] ^ key[i % key.size()];
    }
    return result;
}

static std::string base64_encode(const std::string& data) {
    static const char encoding_table[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    std::string result;
    int val = 0, valb = -6;
    for (unsigned char c : data) {
        val = (val << 8) + c;
        valb += 8;
        while (valb >= 0) {
            result.push_back(encoding_table[(val >> valb) & 0x3F]);
            valb -= 6;
        }
    }
    while (result.size() % 4) result.push_back('=');
    return result;
}

static std::string base64_decode(const std::string& data) {
    static const int decoding_table[] = {
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,62,-1,-1,-1,63,
        52,53,54,55,56,57,58,59,60,61,-1,-1,-1,-1,-1,-1,
        -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,
        15,16,17,18,19,20,21,22,23,24,25,-1,-1,-1,-1,-1,
        -1,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,
        41,42,43,44,45,46,47,48,49,50,51,-1,-1,-1,-1,-1
    };
    std::string result;
    int val = 0, valb = -8;
    for (unsigned char c : data) {
        if (decoding_table[c] == -1) break;
        val = (val << 6) + decoding_table[c];
        valb += 6;
        if (valb >= 0) {
            result.push_back(char((val >> valb) & 0xFF));
            valb -= 8;
        }
    }
    return result;
}

GmsBridge::GmsBridge() : m_initialized(false) {
}

GmsBridge::~GmsBridge() {
    shutdown();
}

bool GmsBridge::initialize(const std::string& base_dir) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (m_initialized) {
        return true;
    }
    
    m_base_dir = base_dir;
    
    if (!ensure_directories()) {
        LOGE("Failed to create directories");
        return false;
    }
    
    if (!derive_encryption_key()) {
        LOGE("Failed to derive encryption key");
        return false;
    }
    
    m_initialized = true;
    LOGI("GmsBridge initialized successfully");
    return true;
}

void GmsBridge::shutdown() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_initialized = false;
    m_encryption_key.clear();
    LOGI("GmsBridge shutdown");
}

bool GmsBridge::ensure_directories() {
    std::string tokens_dir = m_base_dir + "/tokens";
    std::string config_dir = m_base_dir + "/config";
    std::string bundles_dir = m_base_dir + "/bundles";
    
    // Create directories (simplified - use proper mkdir in production)
    std::string cmd = "mkdir -p " + tokens_dir + " " + config_dir + " " + bundles_dir;
    return system(cmd.c_str()) == 0;
}

bool GmsBridge::derive_encryption_key() {
    // Derive key from device-specific info
    // In production, use proper key derivation (PBKDF2, Argon2, etc.)
    std::string device_seed = "virtual_inject_device_key_seed";
    m_encryption_key = calculate_sha256(device_seed);
    return !m_encryption_key.empty();
}

std::string GmsBridge::encrypt_data(const std::string& data) {
    std::string encrypted = xor_encrypt(data, m_encryption_key);
    return base64_encode(encrypted);
}

std::string GmsBridge::decrypt_data(const std::string& encrypted_data) {
    std::string decoded = base64_decode(encrypted_data);
    return xor_encrypt(decoded, m_encryption_key);
}

std::string GmsBridge::calculate_sha256(const std::string& data) {
    // Simple hash for demonstration - use proper SHA256 in production
    std::hash<std::string> hasher;
    size_t hash = hasher(data);
    std::stringstream ss;
    ss << std::hex << hash;
    return ss.str();
}

std::string GmsBridge::calculate_hmac(const std::string& data, const std::string& key) {
    // Simple HMAC for demonstration - use proper HMAC-SHA256 in production
    std::string combined = key + data + key;
    return calculate_sha256(combined);
}

std::string GmsBridge::generate_secure_token(int32_t length) {
    static const char alphanum[] = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    std::string result;
    result.reserve(length);
    
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, sizeof(alphanum) - 2);
    
    for (int32_t i = 0; i < length; i++) {
        result += alphanum[dis(gen)];
    }
    
    return result;
}

std::string GmsBridge::get_token_path(const std::string& account_name) {
    std::string safe_name = account_name;
    std::replace(safe_name.begin(), safe_name.end(), '@', '_');
    std::replace(safe_name.begin(), safe_name.end(), '.', '_');
    return m_base_dir + "/tokens/" + safe_name + ".token";
}

std::string GmsBridge::get_device_info_path() {
    return m_base_dir + "/config/device.info";
}

std::string GmsBridge::get_gms_bundle_path() {
    return m_base_dir + "/bundles/manifest.json";
}

bool GmsBridge::store_token(const std::string& account_name, const TokenData& token) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (!m_initialized) {
        return false;
    }
    
    // Serialize token data
    std::stringstream ss;
    ss << token.account_name << "\n"
       << token.auth_token << "\n"
       << token.refresh_token << "\n"
       << token.id_token << "\n"
       << token.expiry_time << "\n"
       << token.token_type;
    
    std::string data = ss.str();
    std::string encrypted = encrypt_data(data);
    
    std::string path = get_token_path(account_name);
    std::ofstream file(path);
    if (!file.is_open()) {
        LOGE("Failed to open token file: %s", path.c_str());
        return false;
    }
    
    file << encrypted;
    file.close();
    
    LOGI("Token stored for account: %s", account_name.c_str());
    return true;
}

bool GmsBridge::load_token(const std::string& account_name, TokenData& token) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (!m_initialized) {
        return false;
    }
    
    std::string path = get_token_path(account_name);
    std::ifstream file(path);
    if (!file.is_open()) {
        LOGE("Failed to open token file: %s", path.c_str());
        return false;
    }
    
    std::string encrypted((std::istreambuf_iterator<char>(file)),
                          std::istreambuf_iterator<char>());
    file.close();
    
    std::string data = decrypt_data(encrypted);
    
    // Deserialize token data
    std::istringstream iss(data);
    std::getline(iss, token.account_name);
    std::getline(iss, token.auth_token);
    std::getline(iss, token.refresh_token);
    std::getline(iss, token.id_token);
    
    std::string expiry_str;
    std::getline(iss, expiry_str);
    token.expiry_time = std::stoll(expiry_str);
    
    std::string type_str;
    std::getline(iss, type_str);
    token.token_type = std::stoi(type_str);
    
    LOGI("Token loaded for account: %s", account_name.c_str());
    return true;
}

bool GmsBridge::delete_token(const std::string& account_name) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (!m_initialized) {
        return false;
    }
    
    std::string path = get_token_path(account_name);
    if (remove(path.c_str()) != 0) {
        LOGE("Failed to delete token file: %s", path.c_str());
        return false;
    }
    
    LOGI("Token deleted for account: %s", account_name.c_str());
    return true;
}

bool GmsBridge::clear_all_tokens() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (!m_initialized) {
        return false;
    }
    
    std::string tokens_dir = m_base_dir + "/tokens";
    std::string cmd = "rm -rf " + tokens_dir + "/*";
    return system(cmd.c_str()) == 0;
}

bool GmsBridge::store_device_info(const DeviceInfo& info) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (!m_initialized) {
        return false;
    }
    
    std::stringstream ss;
    ss << info.android_id << "\n"
       << info.device_id << "\n"
       << info.gaia_id << "\n"
       << info.model << "\n"
       << info.manufacturer << "\n"
       << info.brand;
    
    std::string data = ss.str();
    std::string encrypted = encrypt_data(data);
    
    std::string path = get_device_info_path();
    std::ofstream file(path);
    if (!file.is_open()) {
        LOGE("Failed to open device info file");
        return false;
    }
    
    file << encrypted;
    file.close();
    
    LOGI("Device info stored");
    return true;
}

bool GmsBridge::load_device_info(DeviceInfo& info) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (!m_initialized) {
        return false;
    }
    
    std::string path = get_device_info_path();
    std::ifstream file(path);
    if (!file.is_open()) {
        LOGE("Failed to open device info file");
        return false;
    }
    
    std::string encrypted((std::istreambuf_iterator<char>(file)),
                          std::istreambuf_iterator<char>());
    file.close();
    
    std::string data = decrypt_data(encrypted);
    
    std::istringstream iss(data);
    std::getline(iss, info.android_id);
    std::getline(iss, info.device_id);
    std::getline(iss, info.gaia_id);
    std::getline(iss, info.model);
    std::getline(iss, info.manufacturer);
    std::getline(iss, info.brand);
    
    LOGI("Device info loaded");
    return true;
}

bool GmsBridge::verify_gms_bundle(const std::string& package_name, const std::string& checksum) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (!m_initialized) {
        return false;
    }
    
    std::string stored_hash = calculate_sha256(package_name);
    return stored_hash == checksum;
}

bool GmsBridge::store_gms_bundle_hash(const std::string& package_name, const std::string& hash) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (!m_initialized) {
        return false;
    }
    
    // Store hash in manifest
    std::string path = get_gms_bundle_path();
    std::ofstream file(path, std::ios::app);
    if (!file.is_open()) {
        LOGE("Failed to open GMS bundle manifest");
        return false;
    }
    
    file << package_name << ":" << hash << "\n";
    file.close();
    
    LOGI("GMS bundle hash stored for: %s", package_name.c_str());
    return true;
}

} // namespace GmsNative
