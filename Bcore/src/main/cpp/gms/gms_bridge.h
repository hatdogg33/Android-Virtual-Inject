#pragma once

#include <string>
#include <vector>
#include <cstdint>
#include <mutex>
#include <memory>

namespace GmsNative {

struct TokenData {
    std::string account_name;
    std::string auth_token;
    std::string refresh_token;
    std::string id_token;
    int64_t expiry_time;
    int32_t token_type;
};

struct DeviceInfo {
    std::string android_id;
    std::string device_id;
    std::string gaia_id;
    std::string model;
    std::string manufacturer;
    std::string brand;
};

class GmsBridge {
public:
    GmsBridge();
    ~GmsBridge();

    bool initialize(const std::string& base_dir);
    void shutdown();

    // Token operations (encrypted storage)
    bool store_token(const std::string& account_name, const TokenData& token);
    bool load_token(const std::string& account_name, TokenData& token);
    bool delete_token(const std::string& account_name);
    bool clear_all_tokens();

    // Device info operations
    bool store_device_info(const DeviceInfo& info);
    bool load_device_info(DeviceInfo& info);

    // GMS bundle verification
    bool verify_gms_bundle(const std::string& package_name, const std::string& checksum);
    bool store_gms_bundle_hash(const std::string& package_name, const std::string& hash);

    // Encryption operations
    std::string encrypt_data(const std::string& data);
    std::string decrypt_data(const std::string& encrypted_data);

    // Hash operations
    std::string calculate_sha256(const std::string& data);
    std::string calculate_hmac(const std::string& data, const std::string& key);

    // Secure random
    std::string generate_secure_token(int32_t length);

private:
    bool m_initialized;
    std::string m_base_dir;
    std::mutex m_mutex;

    // Internal encryption key (derived from device)
    std::string m_encryption_key;

    bool derive_encryption_key();
    bool ensure_directories();
    std::string get_token_path(const std::string& account_name);
    std::string get_device_info_path();
    std::string get_gms_bundle_path();
};

} // namespace GmsNative
