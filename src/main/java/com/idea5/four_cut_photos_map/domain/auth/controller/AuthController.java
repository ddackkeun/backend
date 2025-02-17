package com.idea5.four_cut_photos_map.domain.auth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.idea5.four_cut_photos_map.domain.auth.dto.param.KakaoUserInfoParam;
import com.idea5.four_cut_photos_map.domain.auth.dto.param.LoginMemberParam;
import com.idea5.four_cut_photos_map.domain.auth.dto.request.RefreshTokenReq;
import com.idea5.four_cut_photos_map.domain.auth.dto.response.KakaoTokenResp;
import com.idea5.four_cut_photos_map.domain.auth.service.KakaoService;
import com.idea5.four_cut_photos_map.domain.member.service.MemberService;
import com.idea5.four_cut_photos_map.global.util.Util;
import com.idea5.four_cut_photos_map.job.CollectService;
import com.idea5.four_cut_photos_map.security.jwt.JwtService;
import com.idea5.four_cut_photos_map.security.jwt.dto.MemberContext;
import com.idea5.four_cut_photos_map.security.jwt.dto.response.AccessToken;
import com.idea5.four_cut_photos_map.security.jwt.dto.response.JwtToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 인증 관련 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final JwtService jwtService;
    private final MemberService memberService;
    private final KakaoService kakaoService;
    private final CollectService collectService;

    /**
     * 카카오 로그인
     * @param code 인가코드
     */
    @PreAuthorize("isAnonymous()")
    @GetMapping("/login/kakao")
    public ResponseEntity<JwtToken> kakaoLogin(@RequestParam String code, HttpServletRequest request) throws JsonProcessingException {
        log.info("카카오 로그인 콜백 요청");
        log.info("code = " + code);
        log.info("origin = " + request.getHeader("Origin"));
        log.info("referer = " + request.getHeader("referer"));
        log.info("X-Forwarded-For = " + request.getHeader("X-Forwarded-For"));
        log.info("Remote Addr = " + request.getRemoteAddr());
        log.info("Remote Host = " + request.getRemoteHost());
        log.info("client ip = " + Util.getClientIpAddr(request));

        String redirectURI = kakaoService.getRedirectURI(request);
        log.info("redirectURI = " + redirectURI);

        // 1. 인가 코드로 토큰 발급 요청
        KakaoTokenResp kakaoTokenResp = kakaoService.getKakaoTokens(code, redirectURI);

        // 2. 토큰으로 사용자 정보 가져오기 요청
        KakaoUserInfoParam kakaoUserInfoParam = kakaoService.getKakaoUserInfo(kakaoTokenResp);

        // 3. 제공받은 사용자 정보(kakaoId)로 회원 검증(새로운 회원은 회원가입) -> 서비스 로그인
        LoginMemberParam loginMemberParam = memberService.login(kakaoUserInfoParam, kakaoTokenResp);

        // 4. 신규 회원은 뉴비 칭호 부여
        if (loginMemberParam.isJoin())
            collectService.addJoinTitle(loginMemberParam.getMember());
        return ResponseEntity.ok(loginMemberParam.getJwtToken());
    }

    /**
     * refreshToken 으로 accessToken 재발급
     */
    @PostMapping("/token")
    public ResponseEntity<AccessToken> refreshToken(@RequestBody RefreshTokenReq refreshTokenReq) {
        log.info("accessToken 재발급 요청");
        AccessToken accessToken = jwtService.reissueAccessToken(refreshTokenReq.getRefreshToken());
        return ResponseEntity.ok(accessToken);
    }

    /**
     * 서비스 로그아웃
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/logout")
    public ResponseEntity<Object> logout(@AuthenticationPrincipal MemberContext memberContext) {
        // 서비스 로그아웃
        log.info("서비스 로그아웃");
        memberService.logout(memberContext.getId());
        return ResponseEntity.ok(null);
    }
}
