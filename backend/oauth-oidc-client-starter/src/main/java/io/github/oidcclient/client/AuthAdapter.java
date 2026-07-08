package io.github.oidcclient.client;

import java.net.URI;

public interface AuthAdapter {
    // 创建授权请求并返回 provider 授权地址；实现方必须同时生成 state、nonce 和 code_verifier。
    AuthorizationRequest createAuthorizationRequest(URI redirectUri, URI originalOrigin, String originalPath, URI initPageUri);

    // 使用授权码和后端保存的 code_verifier 换 token。
    TokenResponse exchangeCode(String code, String codeVerifier, URI redirectUri);

    // 使用 refresh token 刷新 access token，调用方负责并发控制。
    TokenResponse refreshToken(String refreshToken);
}
